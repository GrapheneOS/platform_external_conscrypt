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

import static android.security.Flags.FLAG_DEPRECATE_USES_CLEARTEXT_TRAFFIC2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.compat.testing.PlatformCompatChangeRule;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import libcore.junit.util.compat.CoreCompatChangeRule.DisableCompatChanges;
import libcore.junit.util.compat.CoreCompatChangeRule.EnableCompatChanges;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UsesCleartextTrafficDeprecationTest {
    @Rule(order = 0) public TestRule compatChangeRule = new PlatformCompatChangeRule();

    @Rule(order = 1)
    public final CheckFlagsRule mSetFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    private Context getContextWithUsesCleartextTrafficFlag() {
        return new ContextWrapper(mContext) {
            @Override
            public ApplicationInfo getApplicationInfo() {
                ApplicationInfo info = new ApplicationInfo(super.getApplicationInfo());
                info.flags |= ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC;
                return info;
            }
        };
    }

    @Test
    @RequiresFlagsDisabled({FLAG_DEPRECATE_USES_CLEARTEXT_TRAFFIC2})
    @DisableCompatChanges({ManifestConfigSource.DEPRECATE_USES_CLEARTEXT_TRAFFIC})
    public void testWithDisabledFlagAndDisabledChange_isTrue() throws Exception {
        ManifestConfigSource source =
                new ManifestConfigSource(getContextWithUsesCleartextTrafficFlag());

        ApplicationConfig appConfig = new ApplicationConfig(source);

        assertTrue(appConfig.isCleartextTrafficPermitted());
    }

    @Test
    @RequiresFlagsDisabled({FLAG_DEPRECATE_USES_CLEARTEXT_TRAFFIC2})
    @EnableCompatChanges({ManifestConfigSource.DEPRECATE_USES_CLEARTEXT_TRAFFIC})
    public void testWithDisabledFlagAndEnabledChange_isTrue() throws Exception {
        ManifestConfigSource source =
                new ManifestConfigSource(getContextWithUsesCleartextTrafficFlag());

        ApplicationConfig appConfig = new ApplicationConfig(source);

        assertTrue(appConfig.isCleartextTrafficPermitted());
    }

    @Test
    @RequiresFlagsEnabled({FLAG_DEPRECATE_USES_CLEARTEXT_TRAFFIC2})
    @DisableCompatChanges({ManifestConfigSource.DEPRECATE_USES_CLEARTEXT_TRAFFIC})
    public void testWithEnabledFlagAndDisabledChange_isTrue() throws Exception {
        ManifestConfigSource source =
                new ManifestConfigSource(getContextWithUsesCleartextTrafficFlag());

        ApplicationConfig appConfig = new ApplicationConfig(source);

        assertTrue(appConfig.isCleartextTrafficPermitted());
    }

    @Test
    @RequiresFlagsEnabled({FLAG_DEPRECATE_USES_CLEARTEXT_TRAFFIC2})
    @EnableCompatChanges({ManifestConfigSource.DEPRECATE_USES_CLEARTEXT_TRAFFIC})
    public void testWithEnabledFlagAndEnabledChange_isFalse() throws Exception {
        ManifestConfigSource source =
                new ManifestConfigSource(getContextWithUsesCleartextTrafficFlag());

        ApplicationConfig appConfig = new ApplicationConfig(source);

        assertFalse(appConfig.isCleartextTrafficPermitted());
    }
}
