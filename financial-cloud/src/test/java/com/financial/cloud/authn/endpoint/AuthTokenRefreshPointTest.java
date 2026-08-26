package com.financial.cloud.authn.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.financial.cloud.authn.jwt.service.AuthTokenService;
import com.financial.cloud.authn.session.SessionManager;
import com.financial.cloud.common.Message;

@ExtendWith(MockitoExtension.class)
class AuthTokenRefreshPointTest {

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private SessionManager sessionManager;

    @InjectMocks
    private AuthTokenRefreshPoint authTokenRefreshPoint;

    @Test
    void blankRefreshTokenReturnsMessageJsonUnauthorized() {
        ResponseEntity<?> response = authTokenRefreshPoint.refresh(null, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Message.class);
        Message<?> body = (Message<?>) response.getBody();
        assertThat(body.getCode()).isEqualTo(Message.UNAUTHORIZED);
        assertThat(body.getMessage()).isEqualTo("Refresh Token Fail !");
    }

    @Test
    void missingSessionReturnsMessageJsonUnauthorized() {
        when(sessionManager.get(anyString())).thenReturn(null);

        ResponseEntity<?> response = authTokenRefreshPoint.refresh(null, "missing-session");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Message<?> body = (Message<?>) response.getBody();
        assertThat(body.getCode()).isEqualTo(Message.UNAUTHORIZED);
    }
}
