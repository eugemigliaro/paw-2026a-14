package ar.edu.itba.paw.services.mail;

public interface MailDispatchService {

    void dispatch(String recipientEmail, MailContent content);
}
