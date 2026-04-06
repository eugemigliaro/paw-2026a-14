package ar.edu.itba.paw.services.mail;

public class MailProperties {

    private final MailMode mode;
    private final String baseUrl;
    private final String from;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final boolean smtpAuth;
    private final boolean smtpStarttls;
    private final int verificationTtlHours;

    public MailProperties(
            final MailMode mode,
            final String baseUrl,
            final String from,
            final String smtpHost,
            final int smtpPort,
            final String smtpUsername,
            final String smtpPassword,
            final boolean smtpAuth,
            final boolean smtpStarttls,
            final int verificationTtlHours) {
        this.mode = mode;
        this.baseUrl = baseUrl;
        this.from = from;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.smtpAuth = smtpAuth;
        this.smtpStarttls = smtpStarttls;
        this.verificationTtlHours = verificationTtlHours;
    }

    public MailMode getMode() {
        return mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getFrom() {
        return from;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public boolean isSmtpStarttls() {
        return smtpStarttls;
    }

    public int getVerificationTtlHours() {
        return verificationTtlHours;
    }
}
