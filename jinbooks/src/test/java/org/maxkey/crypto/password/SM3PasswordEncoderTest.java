package org.maxkey.crypto.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jinbooks.configuration.LegacyPasswordEncoders;

class SM3PasswordEncoderTest {

    @Test
    void sm3EncodeAndMatch() {
        var encoder = LegacyPasswordEncoders.sm3();
        String encoded = encoder.encode("maxkeypassword");
        assertThat(encoder.matches("maxkeypassword", encoded)).isTrue();
        assertThat(encoder.matches("wrong", encoded)).isFalse();
    }
}
