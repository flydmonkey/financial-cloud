package com.jinbooks.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacySecretCodecTest {

	@Test
	void roundTripsSecretWithDefaultKey() {
		LegacySecretCodec codec = new LegacySecretCodec();
		String encoded = codec.encode("smtp-password");
		assertThat(codec.decoder(encoded)).isEqualTo("smtp-password");
	}

	@Test
	void roundTripsSecretWithConfiguredKey() {
		LegacySecretCodec codec = new LegacySecretCodec("custom-suffix-key-for-test");
		String encoded = codec.encode("api-secret");
		assertThat(codec.decoder(encoded)).isEqualTo("api-secret");
	}
}
