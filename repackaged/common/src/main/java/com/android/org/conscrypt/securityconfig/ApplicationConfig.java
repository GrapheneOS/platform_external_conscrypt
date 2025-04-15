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

import javax.net.ssl.X509TrustManager;

/**
 * An application's network security configuration.
 *
 * <p>{@link #getConfigForHostname(String)} provides a means to obtain network security
 * configuration to be used for communicating with a specific hostname.
 * @hide This class is not part of the Android public SDK API
 */
public final class ApplicationConfig {
    private static ApplicationConfig sInstance;
    private static Object sLock = new Object();

    private X509TrustManager mTrustManager;

    private boolean mInitialized;
    private final Object mLock = new Object();

    /** Constructs a new {@code ApplicationConfig} instance. */
    public ApplicationConfig() {
        mInitialized = false;
    }

    public boolean hasPerDomainConfigs() {
        ensureInitialized();
        // TODO(b/397646538): implement
        return false;
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
     * case only if all configurations permit cleartext traffic. For finer-grained policy use {@link
     * #isCleartextTrafficPermitted(String)}.
     */
    public boolean isCleartextTrafficPermitted() {
        ensureInitialized();
        // TODO(b/397646538): implement
        return false;
    }

    /**
     * Returns {@code true} if cleartext traffic is permitted for this application when connecting
     * to {@code hostname}.
     */
    public boolean isCleartextTrafficPermitted(String hostname) {
        // TODO(b/397646538): implement
        return false;
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
    public boolean isCertificateTransparencyVerificationRequired(String hostname) {
        // TODO(b/397646538): implement
        return false;
    }

    /** Handle an update to the system or user certificate stores. */
    public void handleTrustStorageUpdate() {}

    private void ensureInitialized() {
        synchronized (mLock) {
            if (mInitialized) {
                return;
            }
            mInitialized = true;
        }
    }

    /**
     * Sets the default {@link ApplicationConfig} instance.
     *
     * @param config the {@link ApplicationConfig} to set as the default instance.
     */
    public static void setDefaultInstance(ApplicationConfig config) {
        synchronized (sLock) {
            sInstance = config;
        }
    }

    /**
     * Gets the default {@link ApplicationConfig} instance.
     *
     * @return the default {@link ApplicationConfig} instance.
     */
    public static ApplicationConfig getDefaultInstance() {
        synchronized (sLock) {
            return sInstance;
        }
    }
}
