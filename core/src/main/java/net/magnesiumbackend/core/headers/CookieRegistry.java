package net.magnesiumbackend.core.headers;

public final class CookieRegistry {

    public static final int SESSION = 0;
    public static final int CSRF = 1;
    public static final int USER_ID = 2;

    public static final int COUNT = 8;

    public static int resolve(String name) {
        if (name == null) return -1;

        return switch (name.length()) {
            case 7 -> name.equalsIgnoreCase("session") ? SESSION : -1;
            case 4 -> name.equalsIgnoreCase("csrf") ? CSRF : -1;
            case 6 -> name.equalsIgnoreCase("userId") ? USER_ID : -1;
            default -> -1;
        };
    }

    public static int resolve(Slice slice) {
        if (slice == null) return -1;

        int len = slice.length();

        return switch (len) {
            case 7 -> slice.equalsIgnoreCase("session") ? SESSION : -1;
            case 4 -> slice.equalsIgnoreCase("csrf") ? CSRF : -1;
            case 6 -> slice.equalsIgnoreCase("userId") ? USER_ID : -1;
            default -> -1;
        };
    }

    private CookieRegistry() {}
}