package com.financial.cloud.authn.core;

public record SimpleAuthority(String authority) implements Authority {

    @Override
    public String getAuthority() {
        return authority;
    }
}
