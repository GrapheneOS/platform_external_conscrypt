/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.org.conscrypt;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.android.org.conscrypt.flags.Flags;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @hide This class is not part of the Android public SDK API
 */
@Internal
public final class CertBlocklistImpl implements CertBlocklist {
    private static final Logger logger = Logger.getLogger(CertBlocklistImpl.class.getName());
    private static final String DIGEST_SHA1 = "SHA-1";
    private static final String DIGEST_SHA256 = "SHA-256";

    private final Set<BigInteger> serialBlocklist;
    private final Set<ByteArray> sha1PubkeyBlocklist;
    private final Set<ByteArray> sha256PubkeyBlocklist;
    private Map<ByteArray, Boolean> cache;

    /**
     * Number of entries in the cache. The cache contains public keys which are
     * at most 4096 bits (512 bytes) for RSA. For a cache size of 64, that is
     * at most 512 * 64 = 32,768 bytes.
     */
    private static final int CACHE_SIZE = 64;

    /**
     * public for testing only.
     */
    public CertBlocklistImpl(Set<BigInteger> serialBlocklist, Set<ByteArray> sha1PubkeyBlocklist) {
        this(serialBlocklist, sha1PubkeyBlocklist, Collections.emptySet());
    }

    public CertBlocklistImpl(Set<BigInteger> serialBlocklist, Set<ByteArray> sha1PubkeyBlocklist,
            Set<ByteArray> sha256PubkeyBlocklist) {
        this.cache = Collections.synchronizedMap(new LinkedHashMap<ByteArray, Boolean>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ByteArray, Boolean> eldest) {
                return size() > CACHE_SIZE;
            }
        });
        this.serialBlocklist = serialBlocklist;
        this.sha1PubkeyBlocklist = sha1PubkeyBlocklist;
        this.sha256PubkeyBlocklist = sha256PubkeyBlocklist;
    }

    public static CertBlocklist getDefault() {
        String androidData = System.getenv("ANDROID_DATA");
        String blocklistRoot = androidData + "/misc/keychain/";
        String defaultPubkeyBlocklistPath = blocklistRoot + "pubkey_blacklist.txt";
        String defaultSerialBlocklistPath = blocklistRoot + "serial_blacklist.txt";
        String defaultPubkeySha256BlocklistPath = blocklistRoot + "pubkey_sha256_blocklist.txt";

        Set<ByteArray> sha1PubkeyBlocklist =
                readPublicKeyBlockList(defaultPubkeyBlocklistPath, DIGEST_SHA1);
        Set<ByteArray> sha256PubkeyBlocklist =
                readPublicKeyBlockList(defaultPubkeySha256BlocklistPath, DIGEST_SHA256);
        Set<BigInteger> serialBlocklist = readSerialBlockList(defaultSerialBlocklistPath);
        return new CertBlocklistImpl(serialBlocklist, sha1PubkeyBlocklist, sha256PubkeyBlocklist);
    }

    private static boolean isHex(String value) {
        try {
            new BigInteger(value, 16);
            return true;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Could not parse hex value " + value, e);
            return false;
        }
    }

    private static boolean isPubkeyHash(String value, int expectedHashLength) {
        if (value.length() != expectedHashLength) {
            logger.log(Level.WARNING, "Invalid pubkey hash length: " + value.length());
            return false;
        }
        return isHex(value);
    }

    private static String readBlocklist(String path) {
        try {
            return readFileAsString(path);
        } catch (FileNotFoundException ignored) {
            // Ignored
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read blocklist", e);
        }
        return "";
    }

    // From IoUtils.readFileAsString
    private static String readFileAsString(String path) throws IOException {
        return readFileAsBytes(path).toString("UTF-8");
    }

    // Based on IoUtils.readFileAsBytes
    private static ByteArrayOutputStream readFileAsBytes(String path) throws IOException {
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(path, "r");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) f.length());
            byte[] buffer = new byte[8192];
            while (true) {
                int byteCount = f.read(buffer);
                if (byteCount == -1) {
                    return bytes;
                }
                bytes.write(buffer, 0, byteCount);
            }
        } finally {
            closeQuietly(f);
        }
    }

    // Base on IoUtils.closeQuietly
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
                // Ignored
            }
        }
    }

    private static Set<BigInteger> readSerialBlockList(String path) {
        /*
         * Deprecated. Serials may inadvertently match a certificate that was
         * issued not in compliance with the Baseline Requirements. Prefer
         * using the certificate public key.
         */
        Set<BigInteger> bl = new HashSet<BigInteger>();
        String serialBlocklist = readBlocklist(path);
        if (!serialBlocklist.equals("")) {
            for (String value : serialBlocklist.split(",", -1)) {
                try {
                    bl.add(new BigInteger(value, 16));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Tried to blacklist invalid serial number " + value, e);
                }
            }
        }

        // whether that succeeds or fails, send it on its merry way
        return Collections.unmodifiableSet(bl);
    }

    // clang-format off
    static final byte[] SHA1_BUILTIN = {
            // Blocklist test cert for CTS. The cert and key can be found in
            // src/test/resources/blocklist_test_ca.pem and
            // src/test/resources/blocklist_test_ca_key.pem.
            // bae78e6bed65a2bf60ddedde7fd91e825865e93d
          (byte) 0xba, (byte) 0xe7, (byte) 0x8e, (byte) 0x6b, (byte) 0xed,
          (byte) 0x65, (byte) 0xa2, (byte) 0xbf, (byte) 0x60, (byte) 0xdd,
          (byte) 0xed, (byte) 0xde, (byte) 0x7f, (byte) 0xd9, (byte) 0x1e,
          (byte) 0x82, (byte) 0x58, (byte) 0x65, (byte) 0xe9, (byte) 0x3d,
    };

    static final byte[][] SHA1_DEPRECATED_BUILTINS = {
        // "410f36363258f30b347d12ce4863e433437806a8"
        {
            (byte) 0x41, (byte) 0x0f, (byte) 0x36, (byte) 0x36, (byte) 0x32,
            (byte) 0x58, (byte) 0xf3, (byte) 0x0b, (byte) 0x34, (byte) 0x7d,
            (byte) 0x12, (byte) 0xce, (byte) 0x48, (byte) 0x63, (byte) 0xe4,
            (byte) 0x33, (byte) 0x43, (byte) 0x78, (byte) 0x06, (byte) 0xa8,
        },
        // "ba3e7bd38cd7e1e6b9cd4c219962e59d7a2f4e37"
        {
            (byte) 0xba, (byte) 0x3e, (byte) 0x7b, (byte) 0xd3, (byte) 0x8c,
            (byte) 0xd7, (byte) 0xe1, (byte) 0xe6, (byte) 0xb9, (byte) 0xcd,
            (byte) 0x4c, (byte) 0x21, (byte) 0x99, (byte) 0x62, (byte) 0xe5,
            (byte) 0x9d, (byte) 0x7a, (byte) 0x2f, (byte) 0x4e, (byte) 0x37,
        },
        // "e23b8d105f87710a68d9248050ebefc627be4ca6"
        {
            (byte) 0xe2, (byte) 0x3b, (byte) 0x8d, (byte) 0x10, (byte) 0x5f,
            (byte) 0x87, (byte) 0x71, (byte) 0x0a, (byte) 0x68, (byte) 0xd9,
            (byte) 0x24, (byte) 0x80, (byte) 0x50, (byte) 0xeb, (byte) 0xef,
            (byte) 0xc6, (byte) 0x27, (byte) 0xbe, (byte) 0x4c, (byte) 0xa6,
        },
        // "7b2e16bc39bcd72b456e9f055d1de615b74945db"
        {
            (byte) 0x7b, (byte) 0x2e, (byte) 0x16, (byte) 0xbc, (byte) 0x39,
            (byte) 0xbc, (byte) 0xd7, (byte) 0x2b, (byte) 0x45, (byte) 0x6e,
            (byte) 0x9f, (byte) 0x05, (byte) 0x5d, (byte) 0x1d, (byte) 0xe6,
            (byte) 0x15, (byte) 0xb7, (byte) 0x49, (byte) 0x45, (byte) 0xdb,
        },
        // "e8f91200c65cee16e039b9f883841661635f81c5"
        {
            (byte) 0xe8, (byte) 0xf9, (byte) 0x12, (byte) 0x00, (byte) 0xc6,
            (byte) 0x5c, (byte) 0xee, (byte) 0x16, (byte) 0xe0, (byte) 0x39,
            (byte) 0xb9, (byte) 0xf8, (byte) 0x83, (byte) 0x84, (byte) 0x16,
            (byte) 0x61, (byte) 0x63, (byte) 0x5f, (byte) 0x81, (byte) 0xc5,
        },
        // "0129bcd5b448ae8d2496d1c3e19723919088e152"
        {
            (byte) 0x01, (byte) 0x29, (byte) 0xbc, (byte) 0xd5, (byte) 0xb4,
            (byte) 0x48, (byte) 0xae, (byte) 0x8d, (byte) 0x24, (byte) 0x96,
            (byte) 0xd1, (byte) 0xc3, (byte) 0xe1, (byte) 0x97, (byte) 0x23,
            (byte) 0x91, (byte) 0x90, (byte) 0x88, (byte) 0xe1, (byte) 0x52,
        },
        // "5f3ab33d55007054bc5e3e5553cd8d8465d77c61"
        {
            (byte) 0x5f, (byte) 0x3a, (byte) 0xb3, (byte) 0x3d, (byte) 0x55,
            (byte) 0x00, (byte) 0x70, (byte) 0x54, (byte) 0xbc, (byte) 0x5e,
            (byte) 0x3e, (byte) 0x55, (byte) 0x53, (byte) 0xcd, (byte) 0x8d,
            (byte) 0x84, (byte) 0x65, (byte) 0xd7, (byte) 0x7c, (byte) 0x61,
        },
        // "783333c9687df63377efceddd82efa9101913e8e"
        {
            (byte) 0x78, (byte) 0x33, (byte) 0x33, (byte) 0xc9, (byte) 0x68,
            (byte) 0x7d, (byte) 0xf6, (byte) 0x33, (byte) 0x77, (byte) 0xef,
            (byte) 0xce, (byte) 0xdd, (byte) 0xd8, (byte) 0x2e, (byte) 0xfa,
            (byte) 0x91, (byte) 0x01, (byte) 0x91, (byte) 0x3e, (byte) 0x8e,
        },
        // "3ecf4bbbe46096d514bb539bb913d77aa4ef31bf"
        {
            (byte) 0x3e, (byte) 0xcf, (byte) 0x4b, (byte) 0xbb, (byte) 0xe4,
            (byte) 0x60, (byte) 0x96, (byte) 0xd5, (byte) 0x14, (byte) 0xbb,
            (byte) 0x53, (byte) 0x9b, (byte) 0xb9, (byte) 0x13, (byte) 0xd7,
            (byte) 0x7a, (byte) 0xa4, (byte) 0xef, (byte) 0x31, (byte) 0xbf,
        },
    };

    static final byte[] SHA256_BUILTIN = {
            // Blocklist test cert for CTS. The cert and key can be found in
            // src/test/resources/blocklist_test_ca2.pem and
            // src/test/resources/blocklist_test_ca2_key.pem.
            // 809964b15e9bd312993d9984045551f503f2cf8e68f39188921ba30fe623f9fd
          (byte) 0x80, (byte) 0x99, (byte) 0x64, (byte) 0xb1, (byte) 0x5e,
          (byte) 0x9b, (byte) 0xd3, (byte) 0x12, (byte) 0x99, (byte) 0x3d,
          (byte) 0x99, (byte) 0x84, (byte) 0x04, (byte) 0x55, (byte) 0x51,
          (byte) 0xf5, (byte) 0x03, (byte) 0xf2, (byte) 0xcf, (byte) 0x8e,
          (byte) 0x68, (byte) 0xf3, (byte) 0x91, (byte) 0x88, (byte) 0x92,
          (byte) 0x1b, (byte) 0xa3, (byte) 0x0f, (byte) 0xe6, (byte) 0x23,
          (byte) 0xf9, (byte) 0xfd,
    };
    // clang-format on

    private static Set<ByteArray> readPublicKeyBlockList(String path, String hashType) {
        Set<ByteArray> bl = new HashSet<ByteArray>();

        switch (hashType) {
            case DIGEST_SHA1:
                bl.add(new ByteArray(SHA1_BUILTIN));
                if (!Flags.useChromiumCertBlocklist()) {
                    for (byte[] staticPubKey : SHA1_DEPRECATED_BUILTINS) {
                        bl.add(new ByteArray(staticPubKey));
                    }
                }
                break;
            case DIGEST_SHA256:
                bl.add(new ByteArray(SHA256_BUILTIN));
                if (Flags.useChromiumCertBlocklist()) {
                    // Blocklist statically included in Conscrypt. See constants/.
                    for (byte[] staticPubKey : StaticBlocklist.PUBLIC_KEYS) {
                        bl.add(new ByteArray(staticPubKey));
                    }
                }
                break;
            default:
                throw new RuntimeException(
                        "Unknown hashType: " + hashType + ". Expected SHA-1 or SHA-256");
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance(hashType);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Unable to get " + hashType + " MessageDigest", e);
            return bl;
        }

        // The hashes are encoded with hexadecimal values. There should be
        // twice as many characters as the digest length in bytes.
        int hashLength = md.getDigestLength() * 2;

        // Attempt to augment it with values taken from /data/misc/keychain.
        String pubkeyBlocklist = readBlocklist(path);
        if (!pubkeyBlocklist.equals("")) {
            for (String value : pubkeyBlocklist.split(",", -1)) {
                value = value.trim();
                if (isPubkeyHash(value, hashLength)) {
                    bl.add(new ByteArray(Hex.decodeHex(value)));
                } else {
                    logger.log(Level.WARNING, "Tried to blocklist invalid pubkey " + value);
                }
            }
        }

        return bl;
    }

    private static boolean isPublicKeyBlockListed(
            byte[] encodedPublicKey, Set<ByteArray> blocklist, String hashType) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(hashType);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Unable to get " + hashType + " MessageDigest", e);
            return false;
        }
        ByteArray out = new ByteArray(md.digest(encodedPublicKey));
        if (blocklist.contains(out)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPublicKeyBlockListed(PublicKey publicKey) {
        byte[] encodedPublicKey = publicKey.getEncoded();
        // cacheKey is a view on encodedPublicKey. Because it is used as a key
        // for a Map, its underlying array (encodedPublicKey) should not be
        // modified.
        ByteArray cacheKey = new ByteArray(encodedPublicKey);
        Boolean cachedResult = cache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult.booleanValue();
        }
        if (!sha1PubkeyBlocklist.isEmpty()) {
            if (isPublicKeyBlockListed(encodedPublicKey, sha1PubkeyBlocklist, DIGEST_SHA1)) {
                cache.put(cacheKey, true);
                return true;
            }
        }
        if (!sha256PubkeyBlocklist.isEmpty()) {
            if (isPublicKeyBlockListed(encodedPublicKey, sha256PubkeyBlocklist, DIGEST_SHA256)) {
                cache.put(cacheKey, true);
                return true;
            }
        }
        cache.put(cacheKey, false);
        return false;
    }

    @Override
    public boolean isSerialNumberBlockListed(BigInteger serial) {
        return serialBlocklist.contains(serial);
    }
}
