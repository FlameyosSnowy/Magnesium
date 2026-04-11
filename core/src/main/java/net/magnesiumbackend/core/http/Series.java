package net.magnesiumbackend.core.http;

public enum Series {
    INFORMATIONAL,
    SUCCESS,
    REDIRECTION,
    CLIENT_ERROR,
    SERVER_ERROR;

    public static Series from(int code) {
        return switch (code / 100) {
            case 1 -> INFORMATIONAL;
            case 2 -> SUCCESS;
            case 3 -> REDIRECTION;
            case 4 -> CLIENT_ERROR;
            case 5 -> SERVER_ERROR;
            default -> throw new IllegalArgumentException("Invalid HTTP status: " + code);
        };
    }
}