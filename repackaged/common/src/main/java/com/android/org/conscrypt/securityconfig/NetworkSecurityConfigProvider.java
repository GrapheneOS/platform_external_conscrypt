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

import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;

/**
 * Security Provider backed by the app's Network Security Config.
 * @hide This class is not part of the Android public SDK API
 */
public final class NetworkSecurityConfigProvider extends Provider {
    private static final String LOG_TAG = "nsconfig";
    private static final Logger logger = Logger.getLogger(LOG_TAG);
    private static final String PREFIX =
            NetworkSecurityConfigProvider.class.getPackage().getName() + ".";

    public NetworkSecurityConfigProvider() {
        // TODO: More clever name than this
        super("AndroidNSSP", 1.0, "Android Network Security Policy Provider");
        put("TrustManagerFactory.PKIX", PREFIX + "RootTrustManagerFactorySpi");
        put("Alg.Alias.TrustManagerFactory.X509", "PKIX");
    }

    /**
     * Installs the NetworkSecurityConfigProvider as the highest priority provider.
     *
     * <p>If the provider cannot be installed with highest priority, the installation will still
     * complete but this method will throw an exception.
     */
    public static void install() {
        ApplicationConfig config = new ApplicationConfig();
        ApplicationConfig.setDefaultInstance(config);
        int pos = Security.insertProviderAt(new NetworkSecurityConfigProvider(), 1);
        if (pos != 1) {
            // TODO(b/404518910): remove the provider if the installation fails.
            throw new RuntimeException("Failed to install provider as highest priority provider."
                    + " Provider was installed at position " + pos);
        }
    }

    /**
     * The network security config needs to be aware of multiple applications in the same process to
     * handle discrepancies.
     *
     * <p>For such a shared process, conflicting values of usesCleartextTraffic are resolved as
     * follows:
     *
     * <p>1. Throws a RuntimeException if the shared process with conflicting usesCleartextTraffic
     * values have per domain rules, otherwise
     *
     * <p>2. Sets the default instance to the least strict config.
     *
     * @param processName the name of the process hosting mutiple applications.
     */
    public static void handleNewApplication(String processName) {
        ApplicationConfig config = new ApplicationConfig();
        ApplicationConfig defaultConfig = ApplicationConfig.getDefaultInstance();
        if (defaultConfig != null) {
            if (defaultConfig.isCleartextTrafficPermitted()
                    != config.isCleartextTrafficPermitted()) {
                logger.warning((processName == null ? "Unknown process" : processName)
                        + ": New config does not match the previously set config.");

                if (defaultConfig.hasPerDomainConfigs() || config.hasPerDomainConfigs()) {
                    throw new RuntimeException("Found multiple conflicting per-domain rules");
                }
                config = defaultConfig.isCleartextTrafficPermitted() ? defaultConfig : config;
            }
        }
        ApplicationConfig.setDefaultInstance(config);
    }
}
