package ar.edu.itba.paw.services;

public class VerificationConfirmationResult {

    private final Long userId;
    private final String redirectUrl;
    private final String successMessage;

    public VerificationConfirmationResult(
            final Long userId, final String redirectUrl, final String successMessage) {
        this.userId = userId;
        this.redirectUrl = redirectUrl;
        this.successMessage = successMessage;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getSuccessMessage() {
        return successMessage;
    }
}
