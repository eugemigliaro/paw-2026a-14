package ar.edu.itba.paw.services.mail;

final class MailLog {

    private MailLog() {}

    static String maskEmail(final String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        final int atIndex = email.indexOf('@');
        if (atIndex <= 1 || atIndex == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
}
