package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.exceptions.moderation.ModerationException;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.webapp.form.ReportAppealForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/ban")
public class UserBanAppealController {

    private final ModerationService moderationService;

    public UserBanAppealController(final ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @ModelAttribute("reportAppealForm")
    public ReportAppealForm reportAppealForm() {
        return new ReportAppealForm();
    }

    @GetMapping
    public ModelAndView showBanPage(
            @AuthenticatedUser final User user, final Model model, final Locale locale) {
        final UserBan activeBan = moderationService.findActiveBan(user).orElse(null);
        final ModerationReport report =
                moderationService
                        .findReportById(activeBan.getModerationReport().getId())
                        .orElse(null);
        final ModelAndView mav = new ModelAndView("account/banned");
        mav.addObject(
                "banDescriptionCode",
                report.getAppealCount() > 0
                        ? "account.ban.description"
                        : "account.ban.description.appeal.available");
        mav.addObject(
                "banUntilLabel",
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(locale)
                        .withZone(PlatformTime.ZONE)
                        .format(activeBan.getBannedUntil()));
        mav.addObject("banReasonCode", "moderation.reason." + report.getReason().getDbValue());
        mav.addObject("appealReason", report.getAppealReason());
        mav.addObject("appealAllowed", report.getAppealCount() < 1);
        mav.addObject("action", model.asMap().get("action"));
        return mav;
    }

    @PostMapping("/appeal")
    public ModelAndView appealBan(
            @AuthenticatedUser final User user,
            @Valid @ModelAttribute("reportAppealForm") final ReportAppealForm reportAppealForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final UserBan activeBan = moderationService.findActiveBan(user).orElse(null);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorCode", "moderation.report.error.invalid");
            return new ModelAndView("redirect:/account/ban");
        }

        try {
            moderationService.appealReport(
                    activeBan.getModerationReport().getId(), user, reportAppealForm.getDetails());
            redirectAttributes.addFlashAttribute("action", "appealed");
            return new ModelAndView("redirect:/account/ban");
        } catch (final ModerationException e) {
            return new ModelAndView("redirect:/account/ban?error=" + e.getMessage());
        }
    }
}
