package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.UserAccount;
import java.util.Optional;

public class VerificationConfirmationResult {

    private final Long userId;
    private final String successMessage;
    private final UserAccount account;

    public VerificationConfirmationResult(final Long userId, final String successMessage) {
        this(userId, successMessage, null);
    }

    public VerificationConfirmationResult(final UserAccount account, final String successMessage) {
        this(account == null ? null : account.getId(), successMessage, account);
    }

    private VerificationConfirmationResult(
            final Long userId, final String successMessage, final UserAccount account) {
        this.userId = userId;
        this.successMessage = successMessage;
        this.account = account;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public Optional<UserAccount> getAccount() {
        return Optional.ofNullable(account);
    }
}
