/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.org.conscrypt.java.security.AlgorithmParameterGeneratorTestDH;
import com.android.org.conscrypt.java.security.AlgorithmParameterGeneratorTestDSA;
import com.android.org.conscrypt.java.security.AlgorithmParametersPSSTest;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestAES;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDES;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDESede;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDH;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestDSA;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestGCM;
import com.android.org.conscrypt.java.security.AlgorithmParametersTestOAEP;
import com.android.org.conscrypt.java.security.KeyFactoryTestDH;
import com.android.org.conscrypt.java.security.KeyFactoryTestDSA;
import com.android.org.conscrypt.java.security.KeyFactoryTestRSA;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTest;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestDH;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestDSA;
import com.android.org.conscrypt.java.security.KeyPairGeneratorTestRSA;
import com.android.org.conscrypt.java.security.MessageDigestTest;
import com.android.org.conscrypt.java.security.SignatureTest;
import com.android.org.conscrypt.java.security.cert.CertificateFactoryTest;
import com.android.org.conscrypt.java.security.cert.X509CertificateTest;
import com.android.org.conscrypt.javax.crypto.CipherBasicsTest;
import com.android.org.conscrypt.javax.crypto.KeyGeneratorTest;
import com.android.org.conscrypt.javax.net.ssl.HttpsURLConnectionTest;
import com.android.org.conscrypt.javax.net.ssl.KeyManagerFactoryTest;
import com.android.org.conscrypt.javax.net.ssl.KeyStoreBuilderParametersTest;
import com.android.org.conscrypt.javax.net.ssl.SNIHostNameTest;
import com.android.org.conscrypt.javax.net.ssl.SSLContextTest;
import com.android.org.conscrypt.javax.net.ssl.SSLEngineTest;
import com.android.org.conscrypt.javax.net.ssl.SSLEngineVersionCompatibilityTest;
import com.android.org.conscrypt.javax.net.ssl.SSLParametersTest;
import com.android.org.conscrypt.javax.net.ssl.SSLServerSocketFactoryTest;
import com.android.org.conscrypt.javax.net.ssl.SSLServerSocketTest;
import com.android.org.conscrypt.javax.net.ssl.SSLSessionContextTest;
import com.android.org.conscrypt.javax.net.ssl.SSLSessionTest;
import com.android.org.conscrypt.javax.net.ssl.SSLSocketFactoryTest;
import com.android.org.conscrypt.javax.net.ssl.SSLSocketTest;
import com.android.org.conscrypt.javax.net.ssl.SSLSocketVersionCompatibilityTest;
import com.android.org.conscrypt.javax.net.ssl.TrustManagerFactoryTest;
import com.android.org.conscrypt.javax.net.ssl.X509KeyManagerTest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @hide This class is not part of the Android public SDK API
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        // java.security tests
        CertificateFactoryTest.class,
        X509CertificateTest.class,
        AlgorithmParameterGeneratorTestDH.class,
        AlgorithmParameterGeneratorTestDSA.class,
        AlgorithmParametersPSSTest.class,
        AlgorithmParametersTestAES.class,
        AlgorithmParametersTestDES.class,
        AlgorithmParametersTestDESede.class,
        AlgorithmParametersTestDH.class,
        AlgorithmParametersTestDSA.class,
        AlgorithmParametersTestGCM.class,
        AlgorithmParametersTestOAEP.class,
        KeyFactoryTestDH.class,
        KeyFactoryTestDSA.class,
        KeyFactoryTestRSA.class,
        KeyPairGeneratorTest.class,
        KeyPairGeneratorTestDH.class,
        KeyPairGeneratorTestDSA.class,
        KeyPairGeneratorTestRSA.class,
        MessageDigestTest.class,
        SignatureTest.class,
        // javax.crypto tests
        CipherBasicsTest.class,
        // CipherTest.class,  // Lots of weird, broken behaviors in Sun* providers on OpenJDK 7
        // ECDHKeyAgreementTest.class,  // EC keys are broken on OpenJDK 7
        KeyGeneratorTest.class,
        // javax.net.ssl tests
        HttpsURLConnectionTest.class,
        KeyManagerFactoryTest.class,
        KeyStoreBuilderParametersTest.class,
        SNIHostNameTest.class,
        SSLContextTest.class,
        SSLEngineTest.class,
        SSLEngineVersionCompatibilityTest.class,
        SSLParametersTest.class,
        SSLServerSocketFactoryTest.class,
        SSLServerSocketTest.class,
        SSLSessionContextTest.class,
        SSLSessionTest.class,
        SSLSocketFactoryTest.class,
        SSLSocketTest.class,
        SSLSocketVersionCompatibilityTest.class,
        TrustManagerFactoryTest.class,
        X509KeyManagerTest.class,
})
public class ConscryptJava7Suite {

    @BeforeClass
    public static void setupStatic() {
        installConscryptAsDefaultProvider();
    }

}
