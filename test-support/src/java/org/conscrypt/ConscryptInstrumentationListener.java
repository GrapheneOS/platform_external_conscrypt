/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package org.conscrypt;

import android.os.Bundle;
import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.listener.InstrumentationRunListener;
import com.android.org.conscrypt.Conscrypt;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Objects;
import javax.net.ssl.SSLSocketFactory;
import org.junit.runner.Description;

/**
 * An @link(InstrumentationRunListener) which can be used in CTS tests to control the
 * implementation of @link{SSLSocket} returned by Conscrypt, allowing both implementations
 * to be tested using the same test classes.
 *
 * This listener looks for an instrumentation arg named "conscrypt_sslsocket_implementation".
 * If its value is "fd" then the file descriptor based implementation will be used, or
 * if its value is "engine" then the SSLEngine based implementation will be used. Any other
 * value is invalid.
 *
 * The default is set from an @code{testRunStarted} method, i.e. before any tests run and
 * persists until the ART VM exits, i.e. until all tests in this @code{<test>} clause complete.
 */
public class ConscryptInstrumentationListener extends InstrumentationRunListener {
    private static final String IMPLEMENTATION_ARG_NAME = "conscrypt_sslsocket_implementation";

    private enum Implementation {
        ENGINE(true, "com.android.org.conscrypt.ConscryptEngineSocket"),
        FD(false, "com.android.org.conscrypt.ConscryptFileDescriptorSocket");

        private final boolean useEngine;
        private final String expectedClassName;

        private Implementation(boolean useEngine, String expectedClassName) {
            this.useEngine = useEngine;
            this.expectedClassName = expectedClassName;
        }

        private boolean shouldUseEngine() {
            return useEngine;
        }

        private Class<? extends Socket> getExpectedClass() {
            try {
                return Class.forName(expectedClassName).asSubclass(Socket.class);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Invalid SSLSocket class: '" + expectedClassName + "'");
            }
        }

        private static Implementation lookup(String name) {
            try {
                return valueOf(name.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid SSLSocket implementation: '" + name + "'");
            }
        }
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        Bundle argsBundle = InstrumentationRegistry.getArguments();
        String implementationName = argsBundle.getString(IMPLEMENTATION_ARG_NAME);
        Implementation implementation = Implementation.lookup(implementationName);
        selectImplementation(implementation);
        super.testRunStarted(description);
    }

    private void selectImplementation(Implementation implementation) {
        // Invoke setUseEngineSocketByDefault by reflection as it is an "ExperimentalApi which is
        // not visible to tests.
        try {
            Method method =
                    Conscrypt.class.getDeclaredMethod("setUseEngineSocketByDefault", boolean.class);
            method.invoke(null, implementation.shouldUseEngine());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to set SSLSocket implementation", e);
        }

        // Verify that the default socket factory returns the expected implementation class for
        // SSLSocket or, more likely, a subclass of it.
        Socket socket;
        try {
            socket = SSLSocketFactory.getDefault().createSocket();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create an SSLSocket", e);
        }

        Objects.requireNonNull(socket);
        Class<? extends Socket> expectedClass = implementation.getExpectedClass();
        if (!expectedClass.isAssignableFrom(socket.getClass())) {
            throw new IllegalArgumentException("Expected SSLSocket class or subclass of "
                    + expectedClass.getSimpleName() + " but got "
                    + socket.getClass().getSimpleName());
        }
    }
}
