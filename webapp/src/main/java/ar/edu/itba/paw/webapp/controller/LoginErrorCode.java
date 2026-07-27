package ar.edu.itba.paw.webapp.controller;

public enum LoginErrorCode {
    VERIFY("verify"),
    PASSWORD_SETUP("passwordSetup"),
    INVALID("invalid");

    private final String code;

    LoginErrorCode(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
