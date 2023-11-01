package org.conscrypt;

import static org.conscrypt.TestUtils.installConscryptAsDefaultProvider;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // org.conscrypt tests
        AddressUtilsTest.class,
        ApplicationProtocolSelectorAdapterTest.class,
        CertPinManagerTest.class,
        ChainStrengthAnalyzerTest.class,
        ClientSessionContextTest.class,
        ConscryptSocketTest.class,
        ConscryptTest.class,
        DuckTypedHpkeSpiTest.class,
        DuckTypedPSKKeyManagerTest.class,
        FileClientSessionCacheTest.class,
        HostnameVerifierTest.class,
        NativeCryptoArgTest.class,
        NativeCryptoTest.class,
        NativeRefTest.class,
        NativeSslSessionTest.class,
        OpenSSLKeyTest.class,
        OpenSSLX509CertificateTest.class,
        PlatformTest.class,
        SSLUtilsTest.class,
        ServerSessionContextTest.class,
        TestSessionBuilderTest.class,
        TrustManagerImplTest.class,
        // org.conscrypt.ct tests
        CTVerifierTest.class,
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
        KeyFactoryTestRSA.class,
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
        CipherTest.class,
        MacTest.class,
        ECDHKeyAgreementTest.class,
        KeyGeneratorTest.class,
        XDHKeyAgreementTest.class,
        // javax.net.ssl tests
        HttpsURLConnectionTest.class,
        KeyManagerFactoryTest.class,
        KeyStoreBuilderParametersTest.class,
        OptionalMethodTest.class,
        ProtocolTest.class,
        ScryptTest.class,
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
        VeryBasicHttpServerTest.class,
        X509KeyManagerTest.class,
})
public class ConscryptOpenJdkSuite {

  @BeforeClass
  public static void setupStatic() {
    installConscryptAsDefaultProvider();
  }

}
