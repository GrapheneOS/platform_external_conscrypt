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

package android.security.net.config;

import static android.security.NetworkSecurityPolicy.DOMAIN_ENCRYPTION_MODE_DISABLED;
import static android.security.NetworkSecurityPolicy.DOMAIN_ENCRYPTION_MODE_ENABLED;
import static android.security.NetworkSecurityPolicy.DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC;
import static android.security.net.config.NetworkSecurityConfig.defaultDomainEncryptionMode;

import static com.android.org.conscrypt.net.flags.Flags.FLAG_CERTIFICATE_TRANSPARENCY_DEFAULT_ENABLED;
import static com.android.org.conscrypt.net.flags.Flags.FLAG_ENCRYPTED_CLIENT_HELLO_PLATFORM;

import static libcore.net.NetworkSecurityPolicy.CERTIFICATE_TRANSPARENCY_REASON_APP_OPT_IN;
import static libcore.net.NetworkSecurityPolicy.CERTIFICATE_TRANSPARENCY_REASON_SDK_TARGET_DEFAULT_ENABLED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import libcore.junit.util.compat.CoreCompatChangeRule.DisableCompatChanges;
import libcore.junit.util.compat.CoreCompatChangeRule.EnableCompatChanges;

import android.compat.testing.PlatformCompatChangeRule;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

@RunWith(AndroidJUnit4.class)
public class XmlConfigTests {
    private static final String DEBUG_CA_SUBJ = "O=AOSP, CN=Test debug CA";
    private Context mContext;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public TestRule compatChangeRule = new PlatformCompatChangeRule();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testEmptyConfigFile() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.empty_config, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertFalse(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Try some connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "google.com", 443);
    }

    @Test
    public void testEmptyAnchors() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.empty_trust, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertTrue(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionFails(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
    }

    @Test
    public void testBasicDomainConfig() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.domain1, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertTrue(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Check android.com.
        config = appConfig.getConfigForHostname("android.com");
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertFalse(config.getTrustAnchors().isEmpty());
        pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
        // Check that sockets created without the hostname fail with per-domain configs
        SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(
                InetAddress.getByName("android.com"), 443);
        try {
            socket.startHandshake();
            socket.getInputStream();
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void testBasicPinning() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.pins1, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
    }

    @Test
    public void testExpiredPin() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.expired_pin, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testOverridesPins() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.override_pins, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testBadPin() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.bad_pin, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionFails(context, "android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
    }

    @Test
    public void testMultipleDomains() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.multiple_domains, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertFalse(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Both android.com and google.com should use the same config
        NetworkSecurityConfig other = appConfig.getConfigForHostname("google.com");
        assertEquals(config, other);
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testMultipleDomainConfigs() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.multiple_configs, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Should be two different config objects
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        NetworkSecurityConfig other = appConfig.getConfigForHostname("google.com");
        assertNotEquals(config, other);
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testIncludeSubdomains() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.subdomains, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "developer.android.com", 443);
        TestUtils.assertConnectionFails(context, "google.com", 443);
    }

    @Test
    public void testAttributes() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.attributes, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertTrue(config.isHstsEnforced());
        assertFalse(config.isCleartextTrafficPermitted());
    }

    @Test
    public void testResourcePemCertificateSource() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.resource_anchors_pem, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertEquals(2, config.getTrustAnchors().size());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testResourceDerCertificateSource() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.resource_anchors_der, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        assertFalse(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertEquals(2, config.getTrustAnchors().size());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testNestedDomainConfigs() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.nested_domains, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig parent = appConfig.getConfigForHostname("android.com");
        NetworkSecurityConfig child = appConfig.getConfigForHostname("developer.android.com");
        assertNotEquals(parent, child);
        assertTrue(parent.getPins().pins.isEmpty());
        assertFalse(child.getPins().pins.isEmpty());
        // Check that the child inherited the cleartext value and anchors.
        assertFalse(child.isCleartextTrafficPermitted());
        assertFalse(child.getTrustAnchors().isEmpty());
        // Test connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
    }

    @Test
    public void testNestedDomainConfigsOverride() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.nested_domains_override, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig parent = appConfig.getConfigForHostname("android.com");
        NetworkSecurityConfig child = appConfig.getConfigForHostname("developer.android.com");
        assertNotEquals(parent, child);
        assertTrue(parent.isCleartextTrafficPermitted());
        assertFalse(child.isCleartextTrafficPermitted());
    }

    @Test
    public void testDebugOverridesDisabled() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.debug_basic, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        Set<TrustAnchor> anchors = config.getTrustAnchors();
        assertTrue(anchors.isEmpty());
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionFails(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
    }

    @Test
    public void testBasicDebugOverrides() throws Exception {
        ApplicationInfo info = TestUtils.makeApplicationInfo();
        info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        XmlConfigSource source = new XmlConfigSource(mContext, R.xml.debug_basic, info);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        Set<TrustAnchor> anchors = config.getTrustAnchors();
        assertFalse(anchors.isEmpty());
        for (TrustAnchor anchor : anchors) {
            assertTrue(anchor.overridesPins);
        }
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
    }

    @Test
    public void testDebugOverridesWithDomain() throws Exception {
        ApplicationInfo info = TestUtils.makeApplicationInfo();
        info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        XmlConfigSource source = new XmlConfigSource(mContext, R.xml.debug_domain, info);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        Set<TrustAnchor> anchors = config.getTrustAnchors();
        boolean foundDebugCA = false;
        for (TrustAnchor anchor : anchors) {
            if (anchor.certificate.getSubjectDN().toString().equals(DEBUG_CA_SUBJ)) {
                foundDebugCA = true;
                assertTrue(anchor.overridesPins);
            }
        }
        assertTrue(foundDebugCA);
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
    }

    @Test
    public void testDebugInherit() throws Exception {
        ApplicationInfo info = TestUtils.makeApplicationInfo();
        info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        XmlConfigSource source = new XmlConfigSource(mContext, R.xml.debug_domain, info);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        Set<TrustAnchor> anchors = config.getTrustAnchors();
        boolean foundDebugCA = false;
        for (TrustAnchor anchor : anchors) {
            if (anchor.certificate.getSubjectDN().toString().equals(DEBUG_CA_SUBJ)) {
                foundDebugCA = true;
                assertTrue(anchor.overridesPins);
            }
        }
        assertTrue(foundDebugCA);
        assertTrue(anchors.size() > 1);
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
    }

    private void testBadConfig(int configId) throws Exception {
        try {
            XmlConfigSource source =
                    new XmlConfigSource(mContext, configId, TestUtils.makeApplicationInfo());
            ApplicationConfig appConfig = new ApplicationConfig(source);
            appConfig.getConfigForHostname("android.com");
            fail("Bad config " + mContext.getResources().getResourceName(configId)
                    + " did not fail to parse");
        } catch (RuntimeException e) {
            assertTrue(XmlConfigSource.ParserException.class.isAssignableFrom(
                    e.getCause().getClass()));
        }
    }

    @Test
    public void testBadConfig0() throws Exception {
        testBadConfig(R.xml.bad_config0);
    }

    @Test
    public void testBadConfig1() throws Exception {
        testBadConfig(R.xml.bad_config1);
    }

    @Test
    public void testBadConfig2() throws Exception {
        testBadConfig(R.xml.bad_config2);
    }

    @Test
    public void testBadConfig3() throws Exception {
        testBadConfig(R.xml.bad_config3);
    }

    @Test
    public void testBadConfig4() throws Exception {
        testBadConfig(R.xml.bad_config4);
    }

    @Test
    public void testBadConfig5() throws Exception {
        testBadConfig(R.xml.bad_config5);
    }

    @Test
    public void testTrustManagerKeystore() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.bad_pin, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        Provider provider = new NetworkSecurityConfigProvider();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", provider);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null);
        int i = 0;
        for (X509Certificate cert : SystemCertificateSource.getInstance().getCertificates()) {
            keystore.setEntry(String.valueOf(i), new KeyStore.TrustedCertificateEntry(cert), null);
            i++;
        }
        tmf.init(keystore);
        TrustManager[] tms = tmf.getTrustManagers();
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tms, null);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testDebugDedup() throws Exception {
        ApplicationInfo info = TestUtils.makeApplicationInfo();
        info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        XmlConfigSource source = new XmlConfigSource(mContext, R.xml.override_dedup, info);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Check that all TrustAnchors come from the override pins debug source.
        for (TrustAnchor anchor : config.getTrustAnchors()) {
            assertTrue(anchor.overridesPins);
        }
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    @Test
    public void testExtraDebugResource() throws Exception {
        ApplicationInfo info = TestUtils.makeApplicationInfo();
        info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        XmlConfigSource source = new XmlConfigSource(mContext, R.xml.extra_debug_resource, info);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertFalse(config.getTrustAnchors().isEmpty());

        // Check that the _debug file is ignored if debug is false.
        source = new XmlConfigSource(
                mContext, R.xml.extra_debug_resource, TestUtils.makeApplicationInfo());
        appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        config = appConfig.getConfigForHostname("");
        assertTrue(config.getTrustAnchors().isEmpty());
    }

    @Test
    public void testExtraDebugResourceIgnored() throws Exception {
        // Verify that parsing the extra debug config resource fails only when debugging is true.
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.bad_extra_debug_resource, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        // Force parsing the config file.
        appConfig.getConfigForHostname("");

        ApplicationInfo info = TestUtils.makeApplicationInfo();
        info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        source = new XmlConfigSource(mContext, R.xml.bad_extra_debug_resource, info);
        appConfig = new ApplicationConfig(source);
        try {
            appConfig.getConfigForHostname("");
            fail("Bad extra debug resource did not fail to parse");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void testDomainWhitespaceTrimming() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.domain_whitespace, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        NetworkSecurityConfig defaultConfig = appConfig.getConfigForHostname("");
        assertNotEquals(defaultConfig, appConfig.getConfigForHostname("developer.android.com"));
        assertNotEquals(defaultConfig, appConfig.getConfigForHostname("android.com"));
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
    }

    @Test
    public void testCertificateTransparencyDomainConfig() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.ct_domains, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertTrue(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("android.com");
        assertTrue(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("subdomain_user.android.com");
        assertFalse(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("subdomain_user_ct.android.com");
        assertTrue(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("subdomain_inline.android.com");
        assertFalse(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("subdomain_inline_ct.android.com");
        assertTrue(config.isCertificateTransparencyVerificationRequired());
    }

    @Test
    @RequiresFlagsEnabled(FLAG_CERTIFICATE_TRANSPARENCY_DEFAULT_ENABLED)
    public void getCertificateTransparencyVerificationReason_post37_isSdkTargetDefaultEnabled()
            throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.ct_domains, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);

        assertEquals(CERTIFICATE_TRANSPARENCY_REASON_SDK_TARGET_DEFAULT_ENABLED,
                appConfig.getCertificateTransparencyVerificationReason(""));
    }

    @Test
    @RequiresFlagsDisabled(FLAG_CERTIFICATE_TRANSPARENCY_DEFAULT_ENABLED)
    public void getCertificateTransparencyVerificationReason_pre37_isAppOptIn() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.ct_domains, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);

        assertEquals(CERTIFICATE_TRANSPARENCY_REASON_APP_OPT_IN,
                appConfig.getCertificateTransparencyVerificationReason(""));
    }

    @Test
    public void testCertificateTransparencyUserConfig() throws Exception {
        XmlConfigSource source =
                new XmlConfigSource(mContext, R.xml.ct_users, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertFalse(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("android.com");
        assertFalse(config.isCertificateTransparencyVerificationRequired());

        config = appConfig.getConfigForHostname("subdomain.android.com");
        assertTrue(config.isCertificateTransparencyVerificationRequired());
    }

    @Test
    @RequiresFlagsEnabled(FLAG_ENCRYPTED_CLIENT_HELLO_PLATFORM)
    public void testDomainEncryptionBaseConfig_returnsSpecifiedValue() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.domain_encryption_base_config, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname(/* hostname= */ "");
        assertNotNull(config);

        assertEquals(DOMAIN_ENCRYPTION_MODE_ENABLED, config.getDomainEncryptionMode());
    }

    @Test
    @RequiresFlagsEnabled(FLAG_ENCRYPTED_CLIENT_HELLO_PLATFORM)
    public void testDomainEncryptionDomainConfig_returnsSpecifiedValues() throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.domain_encryption_domain_config, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname(/* hostname= */ "");
        assertNotNull(config);

        // Assert base config setting
        assertEquals(DOMAIN_ENCRYPTION_MODE_DISABLED, config.getDomainEncryptionMode());

        // Assert domain config settings
        config = appConfig.getConfigForHostname("android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC, config.getDomainEncryptionMode());

        config = appConfig.getConfigForHostname("enabled.android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_ENABLED, config.getDomainEncryptionMode());

        // TODO(b/476104302): update this assert once we have added back in support for required.
        // Ensure that even with "required" set, we fall back to opportunistic at this point.
        config = appConfig.getConfigForHostname("required.android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC, config.getDomainEncryptionMode());
    }

    @Test
    @RequiresFlagsEnabled(FLAG_ENCRYPTED_CLIENT_HELLO_PLATFORM)
    @EnableCompatChanges({NetworkSecurityConfig.ENABLE_DEFAULT_ENCRYPTED_CLIENT_HELLO})
    public void testDomainEncryptionInvalidValues_enabledAppCompat_returnsDefaultOpportunisticMode()
            throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.domain_encryption_invalid, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname(/* hostname= */ "");
        assertNotNull(config);

        // Assert default base config setting
        assertEquals(DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC, config.getDomainEncryptionMode());

        // Assert domain config settings
        config = appConfig.getConfigForHostname("android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC, config.getDomainEncryptionMode());

        config = appConfig.getConfigForHostname("whitespace.android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC, config.getDomainEncryptionMode());

        config = appConfig.getConfigForHostname("capitalized.android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC, config.getDomainEncryptionMode());
    }

    @Test
    @RequiresFlagsEnabled(FLAG_ENCRYPTED_CLIENT_HELLO_PLATFORM)
    @DisableCompatChanges({NetworkSecurityConfig.ENABLE_DEFAULT_ENCRYPTED_CLIENT_HELLO})
    public void testDomainEncryptionInvalidValues_disabledAppCompat_returnsDefaultDisabledMode()
            throws Exception {
        XmlConfigSource source = new XmlConfigSource(
                mContext, R.xml.domain_encryption_invalid, TestUtils.makeApplicationInfo());
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname(/* hostname= */ "");
        assertNotNull(config);

        // Assert default base config setting
        assertEquals(DOMAIN_ENCRYPTION_MODE_DISABLED, config.getDomainEncryptionMode());

        // Assert domain config settings
        config = appConfig.getConfigForHostname("android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_DISABLED, config.getDomainEncryptionMode());

        config = appConfig.getConfigForHostname("whitespace.android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_DISABLED, config.getDomainEncryptionMode());

        config = appConfig.getConfigForHostname("capitalized.android.com");
        assertEquals(DOMAIN_ENCRYPTION_MODE_DISABLED, config.getDomainEncryptionMode());
    }
}
