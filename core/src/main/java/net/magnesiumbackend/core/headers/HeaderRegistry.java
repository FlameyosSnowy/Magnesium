package net.magnesiumbackend.core.headers;

public final class HeaderRegistry {

    public static final int HOST            = 0;
    public static final int CONTENT_TYPE    = 1;
    public static final int CONTENT_LENGTH  = 2;
    public static final int AUTHORIZATION   = 3;
    public static final int COOKIE          = 4;
    public static final int USER_AGENT      = 5;
    public static final int ACCEPT          = 6;
    public static final int CONNECTION      = 7;

    public static final int COUNT = 8;

    private HeaderRegistry() {}
}