package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.UserAccount;
import java.util.Optional;

public interface AccountAuthService {

    VerificationRequestResult register(RegisterAccountRequest request);

    Optional<VerificationRequestResult> resendVerification(String email);

    VerificationPreview getVerificationPreview(String rawToken);

    VerificationConfirmationResult confirmVerification(String rawToken);

    Optional<VerificationRequestResult> requestPasswordReset(String email);

    PasswordResetPreview getPasswordResetPreview(String rawToken);

    VerificationConfirmationResult resetPassword(String rawToken, String newPassword);

    Optional<UserAccount> findAccountByEmail(String email);
}
