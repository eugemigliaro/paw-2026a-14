package ar.edu.itba.paw.services;

public interface ActionVerificationService {

    VerificationRequestResult requestMatchReservation(Long matchId, String email);

    VerificationPreview getPreview(String rawToken);

    VerificationConfirmationResult confirm(String rawToken);
}
