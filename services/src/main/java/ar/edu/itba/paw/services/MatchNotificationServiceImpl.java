package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MatchLifecycleMailTemplateData;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MatchNotificationServiceImpl implements MatchNotificationService {

    private final MatchParticipantDao matchParticipantDao;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;

    @Autowired
    public MatchNotificationServiceImpl(
            final MatchParticipantDao matchParticipantDao,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource) {
        this.matchParticipantDao = matchParticipantDao;
        this.mailDispatchService = mailDispatchService;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
    }

    @Override
    public void notifyMatchUpdated(final Match match) {
        final List<User> participants = matchParticipantDao.findConfirmedParticipants(match.getId());
        for (final User participant : participants) {
            final MatchLifecycleMailTemplateData templateData =
                    buildTemplateData(participant, match);
            final MailContent content =
                    templateRenderer.renderMatchUpdatedNotification(templateData);
            mailDispatchService.dispatch(participant.getEmail(), content);
        }
    }

    @Override
    public void notifyMatchCancelled(final Match match) {
        final List<User> participants = matchParticipantDao.findConfirmedParticipants(match.getId());
        for (final User participant : participants) {
            final MatchLifecycleMailTemplateData templateData =
                    buildTemplateData(participant, match);
            final MailContent content =
                    templateRenderer.renderMatchCancelledNotification(templateData);
            mailDispatchService.dispatch(participant.getEmail(), content);
        }
    }

    private MatchLifecycleMailTemplateData buildTemplateData(
            final User recipient, final Match match) {
        final Locale locale = LocaleContextHolder.getLocale();
        final String sportLabel =
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale);
        final String statusLabel =
                messageSource.getMessage(
                        "match.status." + match.getStatus(), null, match.getStatus(), locale);

        return new MatchLifecycleMailTemplateData(
                recipient.getEmail(),
                match.getTitle(),
                match.getAddress(),
                match.getStartsAt(),
                match.getEndsAt(),
                sportLabel,
                statusLabel,
                locale);
    }
}
