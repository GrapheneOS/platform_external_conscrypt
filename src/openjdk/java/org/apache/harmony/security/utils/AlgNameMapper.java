/*
 * Copyright 2014 The Android Open Source Project
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

package org.apache.harmony.security.utils;

import java.security.NoSuchAlgorithmException;
import org.conscrypt.NativeCrypto;
import sun.security.x509.AlgorithmId;

public class AlgNameMapper {
    private AlgNameMapper() {
    }

    public static String map2AlgName(String oid) {
        try {
            // This gives us the common name in the Java language.
            AlgorithmId algId = AlgorithmId.get(oid);
            if (algId != null) {
                return algId.getName();
            }
        } catch (NoSuchAlgorithmException ignored) {
        }

        // Otherwise fall back to OpenSSL or BoringSSL's name for it.
        return NativeCrypto.OBJ_txt2nid_longName(oid);
    }

    public static void setSource(Object o) {
    }
}