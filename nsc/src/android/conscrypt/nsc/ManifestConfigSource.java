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

package android.conscrypt.nsc;

import static com.android.org.conscrypt.net.flags.Flags.networkSecurityConfigLocalhost;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.util.Pair;

import java.util.Set;

/** @hide */
public class ManifestConfigSource implements ConfigSource {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "NetworkSecurityConfig";

    private final Object mLock = new Object();
    private final Context mContext;
    private final ApplicationInfo mApplicationInfo;

    private ConfigSource mConfigSource;

    public ManifestConfigSource(Context context) {
        mContext = context;
        // Cache the info because ApplicationInfo is mutable and apps do modify it :(
        mApplicationInfo = new ApplicationInfo(context.getApplicationInfo());
    }

    @Override
    public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
        return getConfigSource().getPerDomainConfigs();
    }

    @Override
    public NetworkSecurityConfig getDefaultConfig() {
        return getConfigSource().getDefaultConfig();
    }

    @Override
    public NetworkSecurityConfig getLocalhostConfig() {
        return getConfigSource().getLocalhostConfig();
    }

    private ConfigSource getConfigSource() {
        synchronized (mLock) {
            if (mConfigSource != null) {
                return mConfigSource;
            }
            int configResource =
                    (android.security.Flags.conscryptNetworkSecurityConfig())
                            ? mApplicationInfo.getNetworkSecurityConfigResourceId()
                            : 0;
            ConfigSource source;
            if (configResource != 0) {
                boolean debugBuild =
                        (mApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                if (DBG) {
                    Log.d(
                            LOG_TAG,
                            "Using Network Security Config from resource "
                                    + mContext.getResources().getResourceEntryName(configResource)
                                    + " debugBuild: "
                                    + debugBuild);
                }
                source = new XmlConfigSource(mContext, configResource, mApplicationInfo);
            } else {
                if (DBG) {
                    Log.d(LOG_TAG, "No Network Security Config specified, using platform default");
                }
                // the legacy FLAG_USES_CLEARTEXT_TRAFFIC is not supported for Ephemeral apps, they
                // should use the network security config.
                boolean usesCleartextTraffic =
                        (mApplicationInfo.flags & ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
                                && !mApplicationInfo.isInstantApp();
                source = new DefaultConfigSource(usesCleartextTraffic, mApplicationInfo);
            }
            mConfigSource = source;
            return mConfigSource;
        }
    }

    private static final class DefaultConfigSource implements ConfigSource {

        private final NetworkSecurityConfig mDefaultConfig;
        private final NetworkSecurityConfig mLocalhostConfig;

        DefaultConfigSource(boolean usesCleartextTraffic, ApplicationInfo info) {
            mDefaultConfig =
                    NetworkSecurityConfig.getDefaultBuilder(info)
                            .setCleartextTrafficPermitted(usesCleartextTraffic)
                            .build();
            if (networkSecurityConfigLocalhost()) {
                mLocalhostConfig = NetworkSecurityConfig.getLocalhostBuilder().build();
            } else {
                mLocalhostConfig = null;
            }
        }

        @Override
        public NetworkSecurityConfig getDefaultConfig() {
            return mDefaultConfig;
        }

        @Override
        public NetworkSecurityConfig getLocalhostConfig() {
            return mLocalhostConfig;
        }

        @Override
        public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
            return null;
        }
    }
}
