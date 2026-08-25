package com.jinbooks.password.onetimepwd;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpServiceTest {
    @Test
    void generateAndVerify() {
        TotpService svc = new TotpService();
        String secret = svc.createSecret();
        GoogleAuthenticator ga = new GoogleAuthenticator();
        int code = ga.getTotpPassword(secret);
        assertTrue(svc.verify(secret, code));
    }
}
