/*
 * Copyright (C) 2017 The Android Open Source Project
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
package org.conscrypt;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.conscrypt.io.IoUtils;

/**
 * Core API for creating and configuring all Conscrypt types.
 */
@SuppressWarnings("unused")
public final class Conscrypt {
    private Conscrypt() {}

    /**
     * Returns {@code true} if the Conscrypt native library has been successfully loaded.
     */
    public static boolean isAvailable() {
        try {
            checkAvailability();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Return {@code true} if BoringSSL has been built in FIPS mode.
     */
    public static boolean isBoringSslFIPSBuild() {
        try {
            return NativeCrypto.usesBoringSsl_FIPS_mode();
        } catch (Throwable e) {
            return false;
        }
    }

    public static class Version {
        private final int major;
        private final int minor;
        private final int patch;

        private Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public int major() { return major; }
        public int minor() { return minor; }
        public int patch() { return patch; }
    }

    private static final Version VERSION;

    static {
        int major = -1;
        int minor = -1;
        int patch = -1;
        InputStream stream = null;
        try {
            stream = Conscrypt.class.getResourceAsStream("conscrypt.properties");
            if (stream != null) {
                Properties props = new Properties();
                props.load(stream);
                major = Integer.parseInt(props.getProperty("org.conscrypt.version.major", "-1"));
                minor = Integer.parseInt(props.getProperty("org.conscrypt.version.minor", "-1"));
                patch = Integer.parseInt(props.getProperty("org.conscrypt.version.patch", "-1"));
            }
        } catch (IOException e) {
            // TODO(prb): This should probably be fatal or have some fallback behaviour
        } finally {
            IoUtils.closeQuietly(stream);
        }
        if ((major >= 0) && (minor >= 0) && (patch >= 0)) {
            VERSION = new Version(major, minor, patch);
        } else {
            VERSION = null;
        }
    }

    /**
     * Returns the version of this distribution of Conscrypt.  If version information is
     * unavailable, returns {@code null}.
     */
    public static Version version() {
        return VERSION;
    }

    /**
     * Checks that the Conscrypt support is available for the system.
     *
     * @throws UnsatisfiedLinkError if unavailable
     */
    public static void checkAvailability() {
        NativeCrypto.checkAvailability();
    }

    /**
     * Indicates whether the given {@link Provider} was created by this distribution of Conscrypt.
     */
    public static boolean isConscrypt(Provider provider) {
        if (provider instanceof OpenSSLProvider) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.OpenSSLProvider").isInstance(provider)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(Provider provider) {
        return provider instanceof OpenSSLProvider;
    }

    private static OpenSSLProvider toConscrypt(Provider provider) {
        return (OpenSSLProvider) provider;
    }

    /**
     * Indicates whether the given {@link SSLContext} was created by this distribution of Conscrypt.
     */
    public static boolean isConscrypt(SSLContext context) {
        if (context.getProvider() instanceof OpenSSLProvider) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.OpenSSLProvider").isInstance(context.getProvider())) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(SSLContext context) {
        return context.getProvider() instanceof OpenSSLProvider; // isThisConscrypt(Provider) logic applies to context's provider
    }

    private static SSLContext toConscrypt(SSLContext context) {
        return (SSLContext) context;
    }

    /**
     * Indicates whether the given {@link SSLSocketFactory} was created by this distribution of
     * Conscrypt.
     */
    public static boolean isConscrypt(SSLSocketFactory factory) {
        if (factory instanceof OpenSSLSocketFactoryImpl) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.OpenSSLSocketFactoryImpl").isInstance(factory)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(SSLSocketFactory factory) {
        return factory instanceof OpenSSLSocketFactoryImpl;
    }

    private static OpenSSLSocketFactoryImpl toConscrypt(SSLSocketFactory factory) {
        return (OpenSSLSocketFactoryImpl) factory;
    }

    /**
     * Indicates whether the given {@link SSLServerSocketFactory} was created by this distribution
     * of Conscrypt.
     */
    public static boolean isConscrypt(SSLServerSocketFactory factory) {
        if (factory instanceof OpenSSLServerSocketFactoryImpl) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.OpenSSLServerSocketFactoryImpl").isInstance(factory)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(SSLServerSocketFactory factory) {
        return factory instanceof OpenSSLServerSocketFactoryImpl;
    }

    private static OpenSSLServerSocketFactoryImpl toConscrypt(SSLServerSocketFactory factory) {
        return (OpenSSLServerSocketFactoryImpl) factory;
    }

    /**
     * Indicates whether the given {@link SSLSocket} was created by this distribution of Conscrypt.
     */
    public static boolean isConscrypt(SSLSocket socket) {
        if (socket instanceof AbstractConscryptSocket) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.AbstractConscryptSocket").isInstance(socket)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(SSLSocket socket) {
        return socket instanceof AbstractConscryptSocket;
    }

    private static AbstractConscryptSocket toConscrypt(SSLSocket socket) {
        return (AbstractConscryptSocket) socket;
    }

    /**
     * Indicates whether the given {@link SSLEngine} was created by this distribution of Conscrypt.
     */
    public static boolean isConscrypt(SSLEngine engine) {
        if (engine instanceof AbstractConscryptEngine) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.AbstractConscryptEngine").isInstance(engine)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(SSLEngine engine) {
        return engine instanceof AbstractConscryptEngine;
    }

    private static AbstractConscryptEngine toConscrypt(SSLEngine engine) {
        return (AbstractConscryptEngine) engine;
    }

    /**
     * Indicates whether the given {@link TrustManager} was created by this distribution of
     * Conscrypt.
     */
    public static boolean isConscrypt(TrustManager trustManager) {
        if (trustManager instanceof TrustManagerImpl) {
            return true;
        }
        try {
            if (Class.forName("com.android.org.conscrypt.TrustManagerImpl").isInstance(trustManager)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    private static boolean isThisConscrypt(TrustManager trustManager) {
        return trustManager instanceof TrustManagerImpl;
    }

    private static TrustManagerImpl toConscrypt(TrustManager trustManager) {
        return (TrustManagerImpl) trustManager;
    }

    /**
     * Constructs a new {@link Provider} with the default name.
     */
    public static Provider newProvider() {
        checkAvailability();
        return new OpenSSLProvider();
    }

    /**
     * Constructs a new {@link Provider} with the given name.
     *
     * @deprecated Use {@link #newProviderBuilder()} instead.
     */
    @Deprecated
    public static Provider newProvider(String providerName) {
        checkAvailability();
        return newProviderBuilder().setName(providerName).build();
    }

    public static class ProviderBuilder {
        private String name = Platform.getDefaultProviderName();
        private boolean provideTrustManager = Platform.provideTrustManagerByDefault();
        private String defaultTlsProtocol = NativeCrypto.SUPPORTED_PROTOCOL_TLSV1_3;
        private boolean deprecatedTlsV1 = Platform.isTlsV1Deprecated();
        private boolean enabledTlsV1 = Platform.isTlsV1Supported();

        private ProviderBuilder() {}

        /**
         * Sets the name of the Provider to be built.
         */
        public ProviderBuilder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Causes the returned provider to provide an implementation of
         * {@link javax.net.ssl.TrustManagerFactory}.
         * @deprecated Use provideTrustManager(true)
         */
        @Deprecated
        @SuppressWarnings("InlineMeSuggester")
        public ProviderBuilder provideTrustManager() {
            return provideTrustManager(true);
        }

        /**
         * Specifies whether the returned provider will provide an implementation of
         * {@link javax.net.ssl.TrustManagerFactory}.
         */
        public ProviderBuilder provideTrustManager(boolean provide) {
            this.provideTrustManager = provide;
            return this;
        }

        /**
         * Specifies what the default TLS protocol should be for SSLContext identifiers
         * {@code TLS}, {@code SSL}, and {@code Default}.
         */
        public ProviderBuilder defaultTlsProtocol(String defaultTlsProtocol) {
            this.defaultTlsProtocol = defaultTlsProtocol;
            return this;
        }

        /** Specifies whether TLS v1.0 and 1.1 should be deprecated */
        public ProviderBuilder isTlsV1Deprecated(boolean deprecatedTlsV1) {
            this.deprecatedTlsV1 = deprecatedTlsV1;
            return this;
        }

        /** Specifies whether TLS v1.0 and 1.1 should be enabled */
        public ProviderBuilder isTlsV1Enabled(boolean enabledTlsV1) {
            this.enabledTlsV1 = enabledTlsV1;
            return this;
        }

        public Provider build() {
            return new OpenSSLProvider(name, provideTrustManager,
                defaultTlsProtocol, deprecatedTlsV1, enabledTlsV1);
        }
    }

    public static ProviderBuilder newProviderBuilder() {
        return new ProviderBuilder();
    }

    /**
     * Returns the maximum length (in bytes) of an encrypted packet.
     */
    public static int maxEncryptedPacketLength() {
        return NativeConstants.SSL3_RT_MAX_PACKET_SIZE;
    }

    /**
     * Gets the default X.509 trust manager.
     */
    @ExperimentalApi
    public static X509TrustManager getDefaultX509TrustManager() throws KeyManagementException {
        checkAvailability();
        return SSLParametersImpl.getDefaultX509TrustManager();
    }

    /**
     * Constructs a new instance of the preferred {@link SSLContextSpi}.
     */
    public static SSLContextSpi newPreferredSSLContextSpi() {
        checkAvailability();
        return OpenSSLContextImpl.getPreferred();
    }

    /**
     * Sets the client-side persistent cache to be used by the context.
     */
    public static void setClientSessionCache(SSLContext context, SSLClientSessionCache cache) {
        if (isThisConscrypt(context)) {
            invokeConscryptMethod(context.getClientSessionContext(), "setPersistentCache",
                                new Class<?>[]{ SSLClientSessionCache.class },
                                new Object[]{ cache }, void.class);
        } else if (!isConscrypt(context)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt client context: " + context.getClass().getName());
        } else {
            invokeConscryptMethod(context.getClientSessionContext(), "setPersistentCache",
                                new Class<?>[]{ SSLClientSessionCache.class },
                                new Object[]{ cache }, void.class);
        }
    }

    /**
     * Sets the server-side persistent cache to be used by the context.
     */
    public static void setServerSessionCache(SSLContext context, SSLServerSessionCache cache) {
        if (isThisConscrypt(context)) {
            invokeConscryptMethod(context.getServerSessionContext(), "setPersistentCache",
                                new Class<?>[]{ SSLServerSessionCache.class },
                                new Object[]{ cache }, void.class);
        } else if (!isConscrypt(context)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt client context: " + context.getClass().getName());
        } else {
            invokeConscryptMethod(context.getServerSessionContext(), "setPersistentCache",
                                new Class<?>[]{ SSLServerSessionCache.class },
                                new Object[]{ cache }, void.class);
        }
    }

    /**
     * Configures the default socket to be created for all socket factory instances.
     */
    @ExperimentalApi
    public static void setUseEngineSocketByDefault(boolean useEngineSocket) {
        OpenSSLSocketFactoryImpl.setUseEngineSocketByDefault(useEngineSocket);
        OpenSSLServerSocketFactoryImpl.setUseEngineSocketByDefault(useEngineSocket);
    }

    /**
     * Configures the socket to be created for the given socket factory instance.
     */
    @ExperimentalApi
    public static void setUseEngineSocket(SSLSocketFactory factory, boolean useEngineSocket) {
        if (isThisConscrypt(factory)) {
            toConscrypt(factory).setUseEngineSocket(useEngineSocket);
        } else if (!isConscrypt(factory)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket factory: " + factory.getClass().getName());
        } else {
            invokeConscryptMethod(factory, "setUseEngineSocket", new Class<?>[]{ boolean.class },
                                new Object[]{ useEngineSocket }, void.class);
        }
    }

    /**
     * Configures the socket to be created for the given server socket factory instance.
     */
    @ExperimentalApi
    public static void setUseEngineSocket(SSLServerSocketFactory factory, boolean useEngineSocket) {
        if (isThisConscrypt(factory)) {
            toConscrypt(factory).setUseEngineSocket(useEngineSocket);
        } else if (!isConscrypt(factory)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt server socket factory: " + factory.getClass().getName());
        } else {
            invokeConscryptMethod(factory, "setUseEngineSocket", new Class<?>[]{ boolean.class },
                                new Object[]{ useEngineSocket }, void.class);
        }
    }

    /**
     * This method enables Server Name Indication (SNI) and overrides the hostname supplied
     * during socket creation.  If the hostname is not a valid SNI hostname, the SNI extension
     * will be omitted from the handshake.
     *
     * @param socket the socket
     * @param hostname the desired SNI hostname, or null to disable
     */
    public static void setHostname(SSLSocket socket, String hostname) {
        if (isThisConscrypt(socket)) {
            toConscrypt(socket).setHostname(hostname);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            invokeConscryptMethod(socket, "setHostname", new Class<?>[] { String.class },
                    new Object[] { hostname }, void.class);
        }
    }

    /**
     * Returns either the hostname supplied during socket creation or via
     * {@link #setHostname(SSLSocket, String)}. No DNS resolution is attempted before
     * returning the hostname.
     */
    public static String getHostname(SSLSocket socket) {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).getHostname();
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "getHostname", new Class<?>[] { },
                    new Object[] { }, String.class);
        }
    }

    /**
     * This method attempts to create a textual representation of the peer host or IP. Does
     * not perform a reverse DNS lookup. This is typically used during session creation.
     */
    public static String getHostnameOrIP(SSLSocket socket) {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).getHostnameOrIP();
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "getHostnameOrIP", new Class<?>[] { },
                    new Object[] { }, String.class);
        }
    }

    /**
     * This method enables session ticket support.
     *
     * @param socket the socket
     * @param useSessionTickets True to enable session tickets
     */
    public static void setUseSessionTickets(SSLSocket socket, boolean useSessionTickets) {
        if (isThisConscrypt(socket)) {
            toConscrypt(socket).setUseSessionTickets(useSessionTickets);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            invokeConscryptMethod(socket, "setUseSessionTickets", new Class<?>[] { boolean.class },
                    new Object[] { useSessionTickets }, void.class);
        }
    }

    /**
     * Enables/disables TLS Channel ID for the given server-side socket.
     *
     * <p>This method needs to be invoked before the handshake starts.
     *
     * @param socket the socket
     * @param enabled Whether to enable channel ID.
     * @throws IllegalStateException if this is a client socket or if the handshake has already
     * started.
     */
    public static void setChannelIdEnabled(SSLSocket socket, boolean enabled) {
        if (isThisConscrypt(socket)) {
            toConscrypt(socket).setChannelIdEnabled(enabled);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            invokeConscryptMethod(socket, "setChannelIdEnabled", new Class<?>[] { boolean.class },
                    new Object[] { enabled }, void.class);
        }
    }

    /**
     * Gets the TLS Channel ID for the given server-side socket. Channel ID is only available
     * once the handshake completes.
     *
     * @param socket the socket
     * @return channel ID or {@code null} if not available.
     * @throws IllegalStateException if this is a client socket or if the handshake has not yet
     * completed.
     * @throws SSLException if channel ID is available but could not be obtained.
     */
    public static byte[] getChannelId(SSLSocket socket) throws SSLException {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).getChannelId();
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "getChannelId", new Class<?>[] { },
                    new Object[] { }, byte[].class);
        }
    }

    /**
     * Sets the {@link PrivateKey} to be used for TLS Channel ID by this client socket.
     *
     * <p>This method needs to be invoked before the handshake starts.
     *
     * @param socket the socket
     * @param privateKey private key (enables TLS Channel ID) or {@code null} for no key
     * (disables TLS Channel ID).
     * The private key must be an Elliptic Curve (EC) key based on the NIST P-256 curve (aka
     * SECG secp256r1 or ANSI
     * X9.62 prime256v1).
     * @throws IllegalStateException if this is a server socket or if the handshake has already
     * started.
     */
    public static void setChannelIdPrivateKey(SSLSocket socket, PrivateKey privateKey) {
        if (isThisConscrypt(socket)) {
            toConscrypt(socket).setChannelIdPrivateKey(privateKey);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            invokeConscryptMethod(socket, "setChannelIdPrivateKey", new Class<?>[] { PrivateKey.class },
                    new Object[] { privateKey }, void.class);
        }
    }

    /**
     * Returns the ALPN protocol agreed upon by client and server.
     *
     * @param socket the socket
     * @return the selected protocol or {@code null} if no protocol was agreed upon.
     */
    public static String getApplicationProtocol(SSLSocket socket) {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).getApplicationProtocol();
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "getApplicationProtocol", new Class<?>[] { },
                    new Object[] { }, String.class);
        }
    }

    /**
     * Sets an application-provided ALPN protocol selector. If provided, this will override
     * the list of protocols set by {@link #setApplicationProtocols(SSLSocket, String[])}.
     *
     * @param socket the socket
     * @param selector the ALPN protocol selector
     */
    public static void setApplicationProtocolSelector(SSLSocket socket,
            ApplicationProtocolSelector selector) {
        if (isThisConscrypt(socket)) {
            toConscrypt(socket).setApplicationProtocolSelector(selector);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            invokeConscryptMethod(socket, "setApplicationProtocolSelector", new Class<?>[] { ApplicationProtocolSelector.class },
                    new Object[] { selector }, void.class);
        }
    }

    /**
     * Sets the application-layer protocols (ALPN) in prioritization order.
     *
     * @param socket the socket being configured
     * @param protocols the protocols in descending order of preference. If empty, no protocol
     * indications will be used. This array will be copied.
     * @throws IllegalArgumentException - if protocols is null, or if any element in a non-empty
     * array is null or an empty (zero-length) string
     */
    public static void setApplicationProtocols(SSLSocket socket, String[] protocols) {
        if (isThisConscrypt(socket)) {
            toConscrypt(socket).setApplicationProtocols(protocols);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            invokeConscryptMethod(socket, "setApplicationProtocols", new Class<?>[] { String[].class },
                    new Object[] { protocols }, void.class);
        }
    }

    /**
     * Gets the application-layer protocols (ALPN) in prioritization order.
     *
     * @param socket the socket
     * @return the protocols in descending order of preference, or an empty array if protocol
     * indications are not being used. Always returns a new array.
     */
    public static String[] getApplicationProtocols(SSLSocket socket) {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).getApplicationProtocols();
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "getApplicationProtocols", new Class<?>[] { },
                    new Object[] { }, String[].class);
        }
    }

    /**
     * Returns the tls-unique channel binding value for this connection, per RFC 5929. This
     * will return {@code null} if there is no such value available, such as if the handshake
     * has not yet completed or this connection is closed.
     */
    public static byte[] getTlsUnique(SSLSocket socket) {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).getTlsUnique();
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "getTlsUnique", new Class<?>[] { },
                    new Object[] { }, byte[].class);
        }
    }

    /**
     * Exports a value derived from the TLS master secret as described in RFC 5705.
     *
     * @param label the label to use in calculating the exported value. This must be
     * an ASCII-only string.
     * @param context the application-specific context value to use in calculating the
     * exported value. This may be {@code null} to use no application context, which is
     * treated differently than an empty byte array.
     * @param length the number of bytes of keying material to return.
     * @return a value of the specified length, or {@code null} if the handshake has not yet
     * completed or the connection is closed.
     * @throws SSLException if the value could not be exported.
     */
    public static byte[] exportKeyingMaterial(SSLSocket socket, String label, byte[] context,
            int length) throws SSLException {
        if (isThisConscrypt(socket)) {
            return toConscrypt(socket).exportKeyingMaterial(label, context, length);
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            return invokeConscryptMethod(socket, "exportKeyingMaterial",
                    new Class<?>[] { String.class, byte[].class, int.class },
                    new Object[] { label, context, length }, byte[].class);
        }
    }

    /**
     * Provides the given engine with the provided bufferAllocator.
     * @throws IllegalArgumentException if the provided engine is not a Conscrypt engine.
     * @throws IllegalStateException if the provided engine has already begun its handshake.
     */
    @ExperimentalApi
    public static void setBufferAllocator(SSLEngine engine, BufferAllocator bufferAllocator) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setBufferAllocator(bufferAllocator);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setBufferAllocator", new Class<?>[] { BufferAllocator.class },
                    new Object[] { bufferAllocator }, void.class);
        }
    }

    /**
     * Provides the given socket with the provided bufferAllocator. If the given socket is a
     * Conscrypt socket but does not use buffer allocators, this method does nothing.
     * @throws IllegalArgumentException if the provided socket is not a Conscrypt socket.
     * @throws IllegalStateException if the provided socket has already begun its handshake.
     */
    @ExperimentalApi
    public static void setBufferAllocator(SSLSocket socket, BufferAllocator bufferAllocator) {
        // This method has special logic. The reflection should only happen if isThisConscrypt returns false.
        if (isThisConscrypt(socket)) {
            AbstractConscryptSocket s = toConscrypt(socket);
            if (s instanceof ConscryptEngineSocket) {
                ((ConscryptEngineSocket) s).setBufferAllocator(bufferAllocator);
            }
        } else if (!isConscrypt(socket)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt socket: " + socket.getClass().getName());
        } else {
            // Apply reflection fallback if not a Conscrypt socket
            invokeConscryptMethod(socket, "setBufferAllocator", new Class<?>[] { BufferAllocator.class },
                    new Object[] { bufferAllocator }, void.class);
        }
    }

    /**
     * Configures the default {@link BufferAllocator} to be used by all future
     * {@link SSLEngine} instances from this provider.
     */
    @ExperimentalApi
    public static void setDefaultBufferAllocator(BufferAllocator bufferAllocator) {
        ConscryptEngine.setDefaultBufferAllocator(bufferAllocator);
    }

    /**
     * This method enables Server Name Indication (SNI) and overrides the hostname supplied
     * during engine creation.
     *
     * @param engine the engine
     * @param hostname the desired SNI hostname, or {@code null} to disable
     */
    public static void setHostname(SSLEngine engine, String hostname) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setHostname(hostname);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setHostname", new Class<?>[] { String.class },
                    new Object[] { hostname }, void.class);
        }
    }

    /**
     * Returns either the hostname supplied during socket creation or via
     * {@link #setHostname(SSLEngine, String)}. No DNS resolution is attempted before
     * returning the hostname.
     */
    public static String getHostname(SSLEngine engine) {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).getHostname();
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "getHostname", new Class<?>[] { },
                    new Object[] { }, String.class);
        }
    }

    /**
     * Returns the maximum overhead, in bytes, of sealing a record with SSL.
     */
    public static int maxSealOverhead(SSLEngine engine) {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).maxSealOverhead();
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "maxSealOverhead", new Class<?>[] { },
                    new Object[] { }, int.class);
        }
    }

    /**
     * Sets a listener on the given engine for completion of the TLS handshake
     */
    public static void setHandshakeListener(SSLEngine engine, HandshakeListener handshakeListener) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setHandshakeListener(handshakeListener);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setHandshakeListener", new Class<?>[] { HandshakeListener.class },
                    new Object[] { handshakeListener }, void.class);
        }
    }

    /**
     * Enables/disables TLS Channel ID for the given server-side engine.
     *
     * <p>This method needs to be invoked before the handshake starts.
     *
     * @param engine the engine
     * @param enabled Whether to enable channel ID.
     * @throws IllegalStateException if this is a client engine or if the handshake has already
     * started.
     */
    public static void setChannelIdEnabled(SSLEngine engine, boolean enabled) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setChannelIdEnabled(enabled);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setChannelIdEnabled", new Class<?>[] { boolean.class },
                    new Object[] { enabled }, void.class);
        }
    }

    /**
     * Gets the TLS Channel ID for the given server-side engine. Channel ID is only available
     * once the handshake completes.
     *
     * @param engine the engine
     * @return channel ID or {@code null} if not available.
     * @throws IllegalStateException if this is a client engine or if the handshake has not yet
     * completed.
     * @throws SSLException if channel ID is available but could not be obtained.
     */
    public static byte[] getChannelId(SSLEngine engine) throws SSLException {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).getChannelId();
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "getChannelId", new Class<?>[] { },
                    new Object[] { }, byte[].class);
        }
    }

    /**
     * Sets the {@link PrivateKey} to be used for TLS Channel ID by this client engine.
     *
     * <p>This method needs to be invoked before the handshake starts.
     *
     * @param engine the engine
     * @param privateKey private key (enables TLS Channel ID) or {@code null} for no key
     * (disables TLS Channel ID).
     * The private key must be an Elliptic Curve (EC) key based on the NIST P-256 curve (aka
     * SECG secp256r1 or ANSI
     * X9.62 prime256v1).
     * @throws IllegalStateException if this is a server engine or if the handshake has already
     * started.
     */
    public static void setChannelIdPrivateKey(SSLEngine engine, PrivateKey privateKey) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setChannelIdPrivateKey(privateKey);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setChannelIdPrivateKey", new Class<?>[] { PrivateKey.class },
                    new Object[] { privateKey }, void.class);
        }
    }

    /**
     * Extended unwrap method for multiple source and destination buffers.
     *
     * @param engine the target engine for the unwrap
     * @param srcs the source buffers
     * @param dsts the destination buffers
     * @return the result of the unwrap operation
     * @throws SSLException thrown if an SSL error occurred
     */
    public static SSLEngineResult unwrap(SSLEngine engine, final ByteBuffer[] srcs,
            final ByteBuffer[] dsts) throws SSLException {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).unwrap(srcs, dsts);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "unwrap",
                    new Class<?>[] { ByteBuffer[].class, ByteBuffer[].class },
                    new Object[] { srcs, dsts }, SSLEngineResult.class);
        }
    }

    /**
     * Exteneded unwrap method for multiple source and destination buffers.
     *
     * @param engine the target engine for the unwrap.
     * @param srcs the source buffers
     * @param srcsOffset the offset in the {@code srcs} array of the first source buffer
     * @param srcsLength the number of source buffers starting at {@code srcsOffset}
     * @param dsts the destination buffers
     * @param dstsOffset the offset in the {@code dsts} array of the first destination buffer
     * @param dstsLength the number of destination buffers starting at {@code dstsOffset}
     * @return the result of the unwrap operation
     * @throws SSLException thrown if an SSL error occurred
     */
    public static SSLEngineResult unwrap(SSLEngine engine, final ByteBuffer[] srcs, int srcsOffset,
            final int srcsLength, final ByteBuffer[] dsts, final int dstsOffset,
            final int dstsLength) throws SSLException {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).unwrap(
                    srcs, srcsOffset, srcsLength, dsts, dstsOffset, dstsLength);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "unwrap",
                    new Class<?>[] { ByteBuffer[].class, int.class, int.class,
                            ByteBuffer[].class, int.class, int.class },
                    new Object[] { srcs, srcsOffset, srcsLength, dsts, dstsOffset, dstsLength },
                    SSLEngineResult.class);
        }
    }

    /**
     * This method enables session ticket support.
     *
     * @param engine the engine
     * @param useSessionTickets True to enable session tickets
     */
    public static void setUseSessionTickets(SSLEngine engine, boolean useSessionTickets) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setUseSessionTickets(useSessionTickets);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setUseSessionTickets", new Class<?>[] { boolean.class },
                    new Object[] { useSessionTickets }, void.class);
        }
    }

    /**
     * Sets the application-layer protocols (ALPN) in prioritization order.
     *
     * @param engine the engine being configured
     * @param protocols the protocols in descending order of preference. If empty, no protocol
     * indications will be used. This array will be copied.
     * @throws IllegalArgumentException - if protocols is null, or if any element in a non-empty
     * array is null or an empty (zero-length) string
     */
    public static void setApplicationProtocols(SSLEngine engine, String[] protocols) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setApplicationProtocols(protocols);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setApplicationProtocols", new Class<?>[] { String[].class },
                    new Object[] { protocols }, void.class);
        }
    }

    /**
     * Gets the application-layer protocols (ALPN) in prioritization order.
     *
     * @param engine the engine
     * @return the protocols in descending order of preference, or an empty array if protocol
     * indications are not being used. Always returns a new array.
     */
    public static String[] getApplicationProtocols(SSLEngine engine) {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).getApplicationProtocols();
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "getApplicationProtocols", new Class<?>[] { },
                    new Object[] { }, String[].class);
        }
    }

    /**
     * Sets an application-provided ALPN protocol selector. If provided, this will override
     * the list of protocols set by {@link #setApplicationProtocols(SSLEngine, String[])}.
     *
     * @param engine the engine
     * @param selector the ALPN protocol selector
     */
    public static void setApplicationProtocolSelector(SSLEngine engine,
            ApplicationProtocolSelector selector) {
        if (isThisConscrypt(engine)) {
            toConscrypt(engine).setApplicationProtocolSelector(selector);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            invokeConscryptMethod(engine, "setApplicationProtocolSelector",
                    new Class<?>[] { ApplicationProtocolSelector.class },
                    new Object[] { selector }, void.class);
        }
    }

    /**
     * Returns the ALPN protocol agreed upon by client and server.
     *
     * @param engine the engine
     * @return the selected protocol or {@code null} if no protocol was agreed upon.
     */
    public static String getApplicationProtocol(SSLEngine engine) {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).getApplicationProtocol();
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "getApplicationProtocol", new Class<?>[] { },
                    new Object[] { }, String.class);
        }
    }

    /**
     * Returns the tls-unique channel binding value for this connection, per RFC 5929. This
     * will return {@code null} if there is no such value available, such as if the handshake
     * has not yet completed or this connection is closed.
     */
    public static byte[] getTlsUnique(SSLEngine engine) {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).getTlsUnique();
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "getTlsUnique", new Class<?>[] { },
                    new Object[] { }, byte[].class);
        }
    }

    /**
     * Exports a value derived from the TLS master secret as described in RFC 5705.
     *
     * @param label the label to use in calculating the exported value. This must be
     * an ASCII-only string.
     * @param context the application-specific context value to use in calculating the
     * exported value. This may be {@code null} to use no application context, which is
     * treated differently than an empty byte array.
     * @param length the number of bytes of keying material to return.
     * @return a value of the specified length, or {@code null} if the handshake has not yet
     * completed or the connection is closed.
     * @throws SSLException if the value could not be exported.
     */
    public static byte[] exportKeyingMaterial(SSLEngine engine, String label, byte[] context,
            int length) throws SSLException {
        if (isThisConscrypt(engine)) {
            return toConscrypt(engine).exportKeyingMaterial(label, context, length);
        } else if (!isConscrypt(engine)) {
            throw new IllegalArgumentException(
                                "Not a conscrypt engine: " + engine.getClass().getName());
        } else {
            return invokeConscryptMethod(engine, "exportKeyingMaterial",
                    new Class<?>[] { String.class, byte[].class, int.class },
                    new Object[] { label, context, length }, byte[].class);
        }
    }

    /**
     * Set the default hostname verifier that will be used for HTTPS endpoint identification by
     * Conscrypt trust managers. If {@code null} (the default), endpoint identification will use
     * the default hostname verifier set in
     * {@link HttpsURLConnection#setDefaultHostnameVerifier(javax.net.ssl.HostnameVerifier)}.
     */
    public synchronized static void setDefaultHostnameVerifier(ConscryptHostnameVerifier verifier) {
        TrustManagerImpl.setDefaultHostnameVerifier(verifier);
    }

    /**
     * Returns the currently-set default hostname verifier for Conscrypt trust managers.
     *
     * @see #setDefaultHostnameVerifier(ConscryptHostnameVerifier)
     */
    public synchronized static ConscryptHostnameVerifier getDefaultHostnameVerifier(TrustManager trustManager) {
        if (isThisConscrypt(trustManager)) {
            return toConscrypt(trustManager).getDefaultHostnameVerifier();
        } else if (!isConscrypt(trustManager)) {
            throw new IllegalArgumentException(
                                "Not a Conscrypt trust manager: " + trustManager.getClass().getName());
        } else {
            return invokeConscryptMethod(trustManager, "getDefaultHostnameVerifier", new Class<?>[] { },
                    new Object[] { }, ConscryptHostnameVerifier.class);
        }
    }

    /**
     * Set the hostname verifier that will be used for HTTPS endpoint identification by the
     * given trust manager. If {@code null} (the default), endpoint identification will use the
     * default hostname verifier set in {@link #setDefaultHostnameVerifier(ConscryptHostnameVerifier)}.
     *
     * @throws IllegalArgumentException if the provided trust manager is not a Conscrypt trust
     * manager per {@link #isConscrypt(TrustManager)}
     */
    public static void setHostnameVerifier(TrustManager trustManager, ConscryptHostnameVerifier verifier) {
        if (isThisConscrypt(trustManager)) {
            toConscrypt(trustManager).setHostnameVerifier(verifier);
        } else if (!isConscrypt(trustManager)) {
            throw new IllegalArgumentException(
                                "Not a Conscrypt trust manager: " + trustManager.getClass().getName());
        } else {
            invokeConscryptMethod(trustManager, "setHostnameVerifier", new Class<?>[] { ConscryptHostnameVerifier.class },
                    new Object[] { verifier }, void.class);
        }
    }

    /**
     * Returns the currently-set hostname verifier for the given trust manager.
     *
     * @throws IllegalArgumentException if the provided trust manager is not a Conscrypt trust
     * manager per {@link #isConscrypt(TrustManager)}
     *
     * @see #setHostnameVerifier(TrustManager, ConscryptHostnameVerifier)
     */
    public static ConscryptHostnameVerifier getHostnameVerifier(TrustManager trustManager) {
        if (isThisConscrypt(trustManager)) {
            return toConscrypt(trustManager).getHostnameVerifier();
        } else if (!isConscrypt(trustManager)) {
            throw new IllegalArgumentException(
                                "Not a Conscrypt trust manager: " + trustManager.getClass().getName());
        } else {
            return invokeConscryptMethod(trustManager, "getHostnameVerifier", new Class<?>[] { },
                    new Object[] { }, ConscryptHostnameVerifier.class);
        }
    }

    /**
     * Wraps the HttpsURLConnection.HostnameVerifier into a ConscryptHostnameVerifier
     */
    public static ConscryptHostnameVerifier wrapHostnameVerifier(final HostnameVerifier verifier) {
        return new ConscryptHostnameVerifier() {
            @Override
            public boolean verify(X509Certificate[] certificates, String hostname, SSLSession session) {
                return verifier.verify(hostname, session);
            }
        };
    }

    /**
     * Generic helper method for invoking methods on potentially non-Conscrypt SSLSocket/SSLEngine
     * instances via reflection.
     *
     * @param instance The SSLSocket or SSLEngine instance.
     * @param methodName The name of the method to invoke.
     * @param paramTypes An array of Class objects representing the parameter types of the method.
     * @param args An array of objects to be passed as arguments to the method.
     * @param returnType The Class object representing the expected return type. Use `void.class` for void methods.
     * @param <T> The generic type of the return value.
     * @return The result of the method invocation, or null for void methods.
     * @throws IllegalArgumentException if the invoked method throws a checked exception, a RuntimeException, Error, or if reflection fails.
     */
    private static <T> T invokeConscryptMethod(Object instance, String methodName, Class<?>[] paramTypes,
            Object[] args, Class<T> returnType)
            throws IllegalArgumentException {
        try {
            Method method = instance.getClass().getDeclaredMethod(methodName, paramTypes);
            Object result = method.invoke(instance, args);
            if (returnType == void.class) {
                return null;
            }
            return returnType.cast(result);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SSLException || cause instanceof IOException || cause instanceof IllegalStateException) {
                IllegalArgumentException wrapped = new IllegalArgumentException(
                        "Reflected method '" + methodName + "' threw a checked exception: " + cause.getMessage(), cause);
                throw wrapped;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                IllegalArgumentException wrapped = new IllegalArgumentException(
                        "Unexpected Throwable from reflected method '" + methodName + "': " + cause.getClass().getName() + ": " + cause.getMessage(), cause);
                throw wrapped;
            }
        } catch (Exception e) {
            IllegalArgumentException wrapped = new IllegalArgumentException(
                    "Failed reflection fallback for method '" + methodName + "': " + e.getMessage(), e);
            throw wrapped;
        }
    }
}