package net.magnesiumbackend.core.security;

import static net.magnesiumbackend.core.security.CsrfConfig.ascii;

public enum SameSite {
    STRICT("; SameSite=Strict"),
    LAX("; SameSite=Lax"),
    NONE("; SameSite=None");

    final byte[] fragment;

    SameSite(String s) {
        this.fragment = ascii(s);
    }
}
