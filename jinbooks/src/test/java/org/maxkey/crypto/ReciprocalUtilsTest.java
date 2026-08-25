package org.maxkey.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jinbooks.util.LegacySecretCodec;

class ReciprocalUtilsTest {

    @Test
    void roundTripAlphanumericSecret() {
        String plain = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String encoded = LegacySecretCodec.getInstance().encode(plain);
        assertThat(encoded.length()).isGreaterThan(plain.length());
        assertThat(LegacySecretCodec.getInstance().decoder(encoded)).isEqualTo(plain);
    }
}
