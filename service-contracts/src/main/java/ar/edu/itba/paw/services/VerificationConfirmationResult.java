package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.UserAccount;
import java.util.Optional;

public class VerificationConfirmationResult {

    private final Long userId;
    private final UserAccount account;

    public VerificationConfirmationResult(final Long userId) {
        this(userId, null);
    }

    public VerificationConfirmationResult(final UserAccount account) {
        this(account == null ? null : account.getId(), account);
    }

    private VerificationConfirmationResult(final Long userId, final UserAccount account) {
        this.userId = userId;
        this.account = account;
    }

    public Long getUserId() {
        return userId;
    }

    public Optional<UserAccount> getAccount() {
        return Optional.ofNullable(account);
    }
}
