/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.org.conscrypt.securityconfig;

import java.security.cert.X509Certificate;

/**
 * {@link CertificateSource} based on the user-installed trusted CA store.
 * @hide This class is not part of the Android public SDK API
 */
public final class UserCertificateSource {
    private static class NoPreloadHolder {
        private static final UserCertificateSource INSTANCE = new UserCertificateSource();
    }

    /**
     * Returns the singleton instance of {@link UserCertificateSource}.
     *
     * @return the singleton instance of {@link UserCertificateSource}.
     */
    public static UserCertificateSource getInstance() {
        return NoPreloadHolder.INSTANCE;
    }

    // TODO(sandrom): move to DirectoryCertificateSource super class
    public X509Certificate findBySubjectAndPublicKey(final X509Certificate cert) {
        return null;
    }

    // TODO(sandrom): move to DirectoryCertificateSource super class
    public void handleTrustStorageUpdate() {}
}
