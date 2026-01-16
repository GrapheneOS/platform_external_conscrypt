/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.org.conscrypt.TestUtils.installConscryptAsDefaultProvider;

import com.android.org.conscrypt.ct.SerializationTest;
import com.android.org.conscrypt.ct.VerifierTest;
import com.android.org.conscrypt.java.security.AlgorithmParameterGeneratorTestDH;
import com.android.org.conscrypt.java.security.AlgorithmParameterGeneratorTestDSA;
import com.android.org.conscrypt.java.security.AlgorithmParametersPSSTest;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestAES;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDES;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDESede;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDH;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDSA;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestEC;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestGCM;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestOAEP;
import com.android.org.conscrypt.java.security.KeyFactoryTestDH;
import com.android.org.conscrypt.java.security.KeyFactoryTestDSA;
import com.android.org.conscrypt.java.security.KeyFactoryTestEC;
import com.android.org.conscrypt.java.security.KeyFactoryTestRSACrt;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTest;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestDH;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestDSA;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestRSA;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestXDH;
import com.android.org.conscrypt.java.security.MessageDigestTest;
import com.android.org.conscrypt.java.security.SignatureTest;
import com.android.org.conscrypt.java.security.cert.CertificateFactoryTest;
import com.android.org.conscrypt.java.security.cert.X509CRLTest;
import com.android.org.conscrypt.java.security.cert.X509CertificateTest;
import com.android.org.conscrypt.javax.crypto.AeadCipherTest;
import com.android.org.conscrypt.javax.crypto.CipherBasicsTest;
import com.android.org.conscrypt.javax.crypto.ECDHKeyAgreementTest;
import com.android.org.conscrypt.javax.crypto.KeyGeneratorTest;
import com.android.org.conscrypt.javax.crypto.ScryptTest;
import com.android.org.conscrypt.javax.crypto.XDHKeyAgreementTest;
import com.android.org.conscrypt.javax.crypto.XdhKeyFactoryTest;
import com.android.org.conscrypt.javax.crypto.XdhKeyTest;
import com.android.org.conscrypt.javax.net.ssl.KeyManagerFactoryTest;
import com.android.org.conscrypt.javax.net.ssl.KeyStoreBuilderParametersTest;
import com.android.org.conscrypt.javax.net.ssl.SNIHostNameTest;
import com.android.org.conscrypt.javax.net.ssl.SSLParametersTest;
import com.android.org.conscrypt.javax.net.ssl.X509KeyManagerTest;
import com.android.org.conscrypt.metrics.CipherSuiteTest;
import com.android.org.conscrypt.metrics.OptionalMethodTest;
import com.android.org.conscrypt.metrics.ProtocolTest;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @hide This class is not part of the Android public SDK API
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        // org.conscrypt tests
        AddressUtilsTest.class,
        ApplicationProtocolSelectorAdapterTest.class,
        ArrayUtilsTest.class,
        CertPinManagerTest.class,
        ChainStrengthAnalyzerTest.class,
        DuckTypedHpkeSpiTest.class,
        EdDsaTest.class,
        ExposedByteArrayOutputStreamTest.class,
        FileClientSessionCacheTest.class,
        HostnameVerifierTest.class,
        HpkeContextTest.class,
        HpkeContextRecipientTest.class,
        HpkeContextSenderTest.class,
        HpkeSuiteTest.class,
        HpkeTestVectorsTest.class,
        KeySpecUtilTest.class,
        MlDsaTest.class,
        NativeCryptoArgTest.class,
        NativeCryptoTest.class,
        NativeRefTest.class,
        NativeSslSessionTest.class,
        OpenSSLKeyTest.class,
        OpenSSLX509CertificateTest.class,
        SSLUtilsTest.class,
        SlhDsaTest.class,
        TestSessionBuilderTest.class,
        TrustManagerImplTest.class,
        X25519Test.class,
        XwingTest.class,
        // org.conscrypt.ct tests
        VerifierTest.class,
        SerializationTest.class,
        // java.security tests
        CertificateFactoryTest.class,
        X509CertificateTest.class,
        X509CRLTest.class,
        AlgorithmParameterGeneratorTestDH.class,
        AlgorithmParameterGeneratorTestDSA.class,
        AlgorithmParametersPSSTest.class,
        AlgorithmParametersTestAES.class,
        AlgorithmParametersTestDES.class,
        AlgorithmParametersTestDESede.class,
        AlgorithmParametersTestDH.class,
        AlgorithmParametersTestDSA.class,
        AlgorithmParametersTestEC.class,
        AlgorithmParametersTestGCM.class,
        AlgorithmParametersTestOAEP.class,
        BufferUtilsTest.class,
        CipherSuiteTest.class,
        KeyFactoryTestDH.class,
        KeyFactoryTestDSA.class,
        KeyFactoryTestEC.class,
        KeyFactoryTestRSACrt.class,
        KeyPairGeneratorTest.class,
        KeyPairGeneratorTestDH.class,
        KeyPairGeneratorTestDSA.class,
        KeyPairGeneratorTestRSA.class,
        KeyPairGeneratorTestXDH.class,
        MessageDigestTest.class,
        SignatureTest.class,
        // javax.crypto tests
        AeadCipherTest.class,
        CipherBasicsTest.class,
        MacTest.class,
        ECDHKeyAgreementTest.class,
        KeyGeneratorTest.class,
        XDHKeyAgreementTest.class,
        XdhKeyFactoryTest.class,
        XdhKeyTest.class,
        // javax.net.ssl tests
        KeyManagerFactoryTest.class,
        KeyStoreBuilderParametersTest.class,
        OptionalMethodTest.class,
        ProtocolTest.class,
        ScryptTest.class,
        SNIHostNameTest.class,
        SSLParametersTest.class,
        VeryBasicHttpServerTest.class,
        X509KeyManagerTest.class,
})
public class ConscryptAndroidSuite {
    @BeforeClass
    public static void setupStatic() {
        installConscryptAsDefaultProvider();
    }
}
