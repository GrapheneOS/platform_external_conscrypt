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

package android.net.ssl;

import com.android.org.conscrypt.Conscrypt;
import javax.net.ssl.SSLEngine;

/**
 * Static utility methods for accessing additional functionality of supported instances of
 * {@link SSLEngine}.  Engines from the platform TLS provider will be compatible with all
 * methods in this class.
 */
public class SSLEngines {
    private SSLEngines() {}

    /**
     * Returns whether the given engine can be used with the methods in this class.  In general,
     * only engines from the platform TLS provider are supported.
     */
    public static boolean isSupportedEngine(SSLEngine engine) {
        return Conscrypt.isConscrypt(engine);
    }

    private static void checkSupported(SSLEngine e) {
        if (!isSupportedEngine(e)) {
            throw new IllegalArgumentException("Engine is not a supported engine.");
        }
    }

    /**
     * Enables or disables the use of session tickets.
     *
     * <p>This function must be called before the handshake is started or it will have no effect.
     *
     * @param engine the engine
     * @param useSessionTickets whether to enable or disable the use of session tickets
     * @throws IllegalArgumentException if the given engine is not a platform engine
     */
    public static void setUseSessionTickets(SSLEngine engine, boolean useSessionTickets) {
        checkSupported(engine);
        Conscrypt.setUseSessionTickets(engine, useSessionTickets);
    }
}