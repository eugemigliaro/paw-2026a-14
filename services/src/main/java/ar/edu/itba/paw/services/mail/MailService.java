package ar.edu.itba.paw.services.mail;

public interface MailService {

    void send(String recipientEmail, MailContent content);
}
