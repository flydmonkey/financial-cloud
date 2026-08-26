package com.financial.cloud.authn.core;

public class BadCredentialsException extends AuthenticationException {

    public BadCredentialsException(String message) {
        super(message);
    }
}
