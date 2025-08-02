/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.platform.test.annotations.RequiresFlagsEnabled;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.InvalidParameterException;

@RunWith(JUnit4.class)
public class PakeOptionTest {
    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_valid() {
        PakeOption option = new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                                    .addMessageComponent("password", new byte[] {1, 2, 3})
                                    .build();
        assertEquals("SPAKE2PLUS_PRERELEASE", option.getAlgorithm());
        assertNotNull(option.getMessageComponent("password"));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_invalidAlgorithm() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder(null));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_emptyAlgorithm() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder(""));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_noComponents() {
        assertThrows(
                InvalidParameterException.class,
                () -> new PakeOption.Builder("SPAKE2PLUS_PRERELEASE").build());
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_invalidKey() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent(null, new byte[] {1, 2, 3}));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_emptyKey() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("", new byte[] {1, 2, 3}));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_invalidSpake2Plus_passwordWithContext() {
        PakeOption option = new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("password", new byte[] {1, 2, 3})
                .addMessageComponent("context", new byte[] {4, 2, 3})
                .build();
        assertNotNull(option.getMessageComponent("password"));
        assertNotNull(option.getMessageComponent("context"));
        assertNull(option.getMessageComponent("non_existing_key"));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_spake2Plus_passwordWithHandshakeLimits() {
        PakeOption option = new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("password", new byte[] {1, 2, 3})
                .addMessageComponent("server-handshake-limit", new byte[] {16})
                .addMessageComponent("client-handshake-limit", new byte[] {16})
                .build();
        assertNotNull(option.getMessageComponent("password"));
        assertNotNull(option.getMessageComponent("server-handshake-limit"));
        assertNotNull(option.getMessageComponent("client-handshake-limit"));
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_spake2Plus_passwordInvalidServerHandshakeLimit1() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("password", new byte[] {1, 2, 3})
                .addMessageComponent("server-handshake-limit", new byte[] {64})
                .addMessageComponent("client-handshake-limit", new byte[] {16})
                .build());
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_spake2Plus_passwordInvalidServerHandshakeLimit2() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("password", new byte[] {1, 2, 3})
                .addMessageComponent("server-handshake-limit", new byte[] {0})
                .addMessageComponent("client-handshake-limit", new byte[] {16})
                .build());
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_spake2Plus_passwordInvalidClientHandshakeLimit() {
        assertThrows(InvalidParameterException.class, () ->new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("password", new byte[] {1, 2, 3})
                .addMessageComponent("server-handshake-limit", new byte[] {16})
                .addMessageComponent("client-handshake-limit", new byte[] {64})
                .build());
    }

    @Test
    @RequiresFlagsEnabled(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public void testBuilder_invalidSpake2Plus_noPassword() {
        assertThrows(InvalidParameterException.class, () -> new PakeOption.Builder("SPAKE2PLUS_PRERELEASE")
                .addMessageComponent("w0", new byte[] {1, 2, 3})
                .build());
    }
}
