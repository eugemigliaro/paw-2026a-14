package ar.edu.itba.paw.services.config;

import ar.edu.itba.paw.services.mail.LoggingMailService;
import ar.edu.itba.paw.services.mail.MailMode;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.MailService;
import ar.edu.itba.paw.services.mail.SmtpMailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class MailConfiguration {

    @Bean
    public MailProperties mailProperties(
            @Value("${mail.mode}") final String mode,
            @Value("${mail.baseUrl}") final String baseUrl,
            @Value("${mail.from}") final String from,
            @Value("${mail.smtp.host}") final String smtpHost,
            @Value("${mail.smtp.port}") final int smtpPort,
            @Value("${mail.smtp.username}") final String smtpUsername,
            @Value("${mail.smtp.password}") final String smtpPassword,
            @Value("${mail.smtp.auth}") final boolean smtpAuth,
            @Value("${mail.smtp.starttls}") final boolean smtpStarttls,
            @Value("${mail.verificationTtlHours}") final int verificationTtlHours) {
        final MailMode mailMode = MailMode.fromProperty(mode);
        final String resolvedBaseUrl = requireNonBlank(baseUrl, "mail.baseUrl");
        final String resolvedFrom = requireNonBlank(from, "mail.from");

        if (mailMode == MailMode.SMTP) {
            requireNonBlank(smtpHost, "mail.smtp.host");
            if (smtpAuth) {
                requireNonBlank(smtpUsername, "mail.smtp.username");
                requireNonBlank(smtpPassword, "mail.smtp.password");
            }
        }

        if (verificationTtlHours <= 0) {
            throw new IllegalStateException("mail.verificationTtlHours must be greater than zero");
        }

        return new MailProperties(
                mailMode,
                resolvedBaseUrl,
                resolvedFrom,
                smtpHost,
                smtpPort,
                smtpUsername,
                smtpPassword,
                smtpAuth,
                smtpStarttls,
                verificationTtlHours);
    }

    @Bean
    public TemplateEngine htmlMailTemplateEngine() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    @Bean
    public TemplateEngine textMailTemplateEngine() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode("TEXT");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    @Bean
    public JavaMailSender javaMailSender(final MailProperties mailProperties) {
        final JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getSmtpHost());
        sender.setPort(mailProperties.getSmtpPort());
        sender.setUsername(mailProperties.getSmtpUsername());
        sender.setPassword(mailProperties.getSmtpPassword());

        final Properties properties = sender.getJavaMailProperties();
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.auth", String.valueOf(mailProperties.isSmtpAuth()));
        properties.setProperty(
                "mail.smtp.starttls.enable", String.valueOf(mailProperties.isSmtpStarttls()));
        return sender;
    }

    @Bean
    public MailService mailService(
            final MailProperties mailProperties, final JavaMailSender javaMailSender) {
        if (mailProperties.getMode() == MailMode.SMTP) {
            return new SmtpMailService(javaMailSender, mailProperties);
        }
        return new LoggingMailService(mailProperties);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    private static String requireNonBlank(final String value, final String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + propertyName);
        }
        return value;
    }
}
