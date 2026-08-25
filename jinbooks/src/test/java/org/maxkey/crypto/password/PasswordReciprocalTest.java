package org.maxkey.crypto.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jinbooks.util.LegacySecretCodec;

class PasswordReciprocalTest {

    @Test
    void roundTripSecret() {
        String plain = "x8zPbCya";
        String encoded = LegacySecretCodec.getInstance().encode(plain);
        assertThat(LegacySecretCodec.getInstance().decoder(encoded)).isEqualTo(plain);
        assertThat(LegacySecretCodec.getInstance().matches(plain, encoded)).isTrue();
    }
}
