/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.security.net.config;

import static libcore.net.NetworkSecurityPolicy.CERTIFICATE_TRANSPARENCY_REASON_APP_OPT_IN;
import static libcore.net.NetworkSecurityPolicy.CERTIFICATE_TRANSPARENCY_REASON_DOMAIN_OPT_IN;
import static libcore.net.NetworkSecurityPolicy.CERTIFICATE_TRANSPARENCY_REASON_SDK_TARGET_DEFAULT_ENABLED;
import static libcore.net.NetworkSecurityPolicy.CERTIFICATE_TRANSPARENCY_REASON_UNKNOWN;

import android.annotation.NonNull;
import android.util.Pair;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

/**
 * An application's network security configuration.
 *
 * <p>{@link #getConfigForHostname(String)} provides a means to obtain network security
 * configuration to be used for communicating with a specific hostname.</p>
 *
 * @hide
 */
public final class ApplicationConfig {
    private static ApplicationConfig sInstance;
    private static Object sLock = new Object();

    private Set<Pair<Domain, NetworkSecurityConfig>> mConfigs;
    private NetworkSecurityConfig mDefaultConfig;
    private NetworkSecurityConfig mLocalhostConfig;
    private X509TrustManager mTrustManager;

    private ConfigSource mConfigSource;
    private boolean mInitialized;
    private final Object mLock = new Object();

    public ApplicationConfig(ConfigSource configSource) {
        mConfigSource = configSource;
        mInitialized = false;
    }

    /**
     * @hide
     */
    public boolean hasPerDomainConfigs() {
        ensureInitialized();
        return mConfigs != null && !mConfigs.isEmpty();
    }

    /**
     * Get the {@link NetworkSecurityConfig} corresponding to the provided hostname.
     * The most specific matching domain rule will be used. If no match exists
     * and the hostname is considered to be localhost (according to {@link
     * Domain#isLocalhost()}), the localhost configuration will be returned.
     * Otherwise, the default configuration will be returned.
     *
     * {@code NetworkSecurityConfig} objects returned by this method can be safely cached for
     * {@code hostname}. Subsequent calls with the same hostname will always return the same
     * {@code NetworkSecurityConfig}.
     *
     * @return {@link NetworkSecurityConfig} to be used to determine
     * the network security configuration for connections to {@code hostname}.
     */
    public NetworkSecurityConfig getConfigForHostname(String hostname) {
        ensureInitialized();
        if (hostname == null || hostname.isEmpty()
                || (mConfigs == null && mLocalhostConfig == null)) {
            return mDefaultConfig;
        }
        if (hostname.charAt(0) == '.') {
            throw new IllegalArgumentException("hostname must not begin with a .");
        }
        // Domains are case insensitive.
        hostname = hostname.toLowerCase(Locale.US);
        // Normalize hostname by removing trailing . if present, all Domain hostnames are
        // absolute.
        if (hostname.charAt(hostname.length() - 1) == '.') {
            hostname = hostname.substring(0, hostname.length() - 1);
        }
        // Find the Domain -> NetworkSecurityConfig entry with the most specific matching
        // Domain entry for hostname.
        if (mConfigs != null) {
            Pair<Domain, NetworkSecurityConfig> bestMatch = null;
            for (Pair<Domain, NetworkSecurityConfig> entry : mConfigs) {
                Domain domain = entry.first;
                NetworkSecurityConfig config = entry.second;
                // Check for an exact match.
                if (domain.hostname.equals(hostname)) {
                    return config;
                }
                // Otherwise check if the Domain includes sub-domains and that the hostname is a
                // sub-domain of the Domain.
                if (domain.subdomainsIncluded && hostname.endsWith(domain.hostname)
                        && hostname.charAt(hostname.length() - domain.hostname.length() - 1)
                                == '.') {
                    if (bestMatch == null) {
                        bestMatch = entry;
                    } else if (domain.hostname.length() > bestMatch.first.hostname.length()) {
                        bestMatch = entry;
                    }
                }
            }
            if (bestMatch != null) {
                return bestMatch.second;
            }
        }
        if (mLocalhostConfig != null && Domain.isLocalhost(hostname)) {
            return mLocalhostConfig;
        }
        // If no match was found use the default configuration.
        return mDefaultConfig;
    }

    /**
     * Returns the {@link X509TrustManager} that implements the checking of trust anchors and
     * certificate pinning based on this configuration.
     */
    public X509TrustManager getTrustManager() {
        ensureInitialized();
        return mTrustManager;
    }

    /**
     * Returns {@code true} if cleartext traffic is permitted for this application, which is the
     * case only if all configurations permit cleartext traffic. For finer-grained policy use
     * {@link #isCleartextTrafficPermitted(String)}.
     */
    public boolean isCleartextTrafficPermitted() {
        ensureInitialized();
        if (mConfigs != null) {
            for (Pair<Domain, NetworkSecurityConfig> entry : mConfigs) {
                if (!entry.second.isCleartextTrafficPermitted()) {
                    return false;
                }
            }
        }

        return mDefaultConfig.isCleartextTrafficPermitted();
    }

    /**
     * Returns {@code true} if cleartext traffic is permitted for this application when connecting
     * to {@code hostname}.
     */
    public boolean isCleartextTrafficPermitted(String hostname) {
        return getConfigForHostname(hostname).isCleartextTrafficPermitted();
    }

    /**
     * Returns {@code true} if Certificate Transparency information is required to be verified by
     * the client in TLS connections to {@code hostname}.
     *
     * <p>See RFC6962 section 3.3 for more details.
     *
     * @param hostname hostname to check whether certificate transparency verification is required
     * @return {@code true} if certificate transparency verification is required and {@code false}
     *     otherwise
     */
    public boolean isCertificateTransparencyVerificationRequired(@NonNull String hostname) {
        return getConfigForHostname(hostname).isCertificateTransparencyVerificationRequired();
    }

    /**
     * @hide
     */
    public int getCertificateTransparencyVerificationReason(@NonNull String hostname) {
        if (NetworkSecurityConfig.certificateTransparencyVerificationRequiredDefault()) {
            return CERTIFICATE_TRANSPARENCY_REASON_SDK_TARGET_DEFAULT_ENABLED;
        }
        if (getConfigForHostname(null).isCertificateTransparencyVerificationRequired()) {
            return CERTIFICATE_TRANSPARENCY_REASON_APP_OPT_IN;
        }
        if (getConfigForHostname(hostname).isCertificateTransparencyVerificationRequired()) {
            return CERTIFICATE_TRANSPARENCY_REASON_DOMAIN_OPT_IN;
        }
        return CERTIFICATE_TRANSPARENCY_REASON_UNKNOWN;
    }

    int getDomainEncryptionMode(@NonNull String hostname) {
        return getConfigForHostname(hostname).getDomainEncryptionMode();
    }

    public void handleTrustStorageUpdate() {
        synchronized (mLock) {
            // If the config is uninitialized then there is no work to be done to handle an update,
            // avoid needlessly parsing configs.
            if (!mInitialized) {
                return;
            }
            mDefaultConfig.handleTrustStorageUpdate();
            if (mConfigs != null) {
                Set<NetworkSecurityConfig> updatedConfigs =
                        new HashSet<NetworkSecurityConfig>(mConfigs.size());
                for (Pair<Domain, NetworkSecurityConfig> entry : mConfigs) {
                    if (updatedConfigs.add(entry.second)) {
                        entry.second.handleTrustStorageUpdate();
                    }
                }
            }
        }
    }

    private void ensureInitialized() {
        synchronized (mLock) {
            if (mInitialized) {
                return;
            }
            mConfigs = mConfigSource.getPerDomainConfigs();
            mDefaultConfig = mConfigSource.getDefaultConfig();
            mLocalhostConfig = mConfigSource.getLocalhostConfig();
            mConfigSource = null;
            mTrustManager = new RootTrustManager(this);
            mInitialized = true;
        }
    }

    public static void setDefaultInstance(ApplicationConfig config) {
        synchronized (sLock) {
            sInstance = config;
        }
    }

    public static ApplicationConfig getDefaultInstance() {
        synchronized (sLock) {
            return sInstance;
        }
    }
}
