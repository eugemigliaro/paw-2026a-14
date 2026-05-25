package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;

public class TournamentDetailViewModel {

    private final Long id;
    private final String title;
    private final String description;
    private final String sportLabel;
    private final String statusLabel;
    private final String statusTone;
    private final String address;
    private final String scheduleLabel;
    private final String registrationWindowLabel;
    private final String bracketSizeLabel;
    private final String teamSizeLabel;
    private final String formatLabel;
    private final String joinModeLabel;
    private final String priceLabel;
    private final String hostLabel;
    private final String hostProfileHref;
    private final String hostProfileImageUrl;
    private final String bannerImageUrl;
    private final String participationLabel;
    private final String nextStepLabel;
    private final List<String> aboutParagraphs;
    private final boolean registrationOpen;
    private final boolean allowSoloSignup;
    private final boolean canJoinSolo;
    private final boolean canLeaveSolo;
    private final boolean requiresLoginToJoin;
    private final boolean canCloseRegistration;
    private final boolean canCancelTournament;

    public TournamentDetailViewModel(
            final Long id,
            final String title,
            final String description,
            final String sportLabel,
            final String statusLabel,
            final String statusTone,
            final String address,
            final String scheduleLabel,
            final String registrationWindowLabel,
            final String bracketSizeLabel,
            final String teamSizeLabel,
            final String formatLabel,
            final String joinModeLabel,
            final String priceLabel,
            final String hostLabel,
            final String hostProfileHref,
            final String hostProfileImageUrl,
            final String bannerImageUrl,
            final String participationLabel,
            final String nextStepLabel,
            final List<String> aboutParagraphs,
            final boolean registrationOpen,
            final boolean allowSoloSignup,
            final boolean canJoinSolo,
            final boolean canLeaveSolo,
            final boolean requiresLoginToJoin,
            final boolean canCloseRegistration,
            final boolean canCancelTournament) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.sportLabel = sportLabel;
        this.statusLabel = statusLabel;
        this.statusTone = statusTone;
        this.address = address;
        this.scheduleLabel = scheduleLabel;
        this.registrationWindowLabel = registrationWindowLabel;
        this.bracketSizeLabel = bracketSizeLabel;
        this.teamSizeLabel = teamSizeLabel;
        this.formatLabel = formatLabel;
        this.joinModeLabel = joinModeLabel;
        this.priceLabel = priceLabel;
        this.hostLabel = hostLabel;
        this.hostProfileHref = hostProfileHref;
        this.hostProfileImageUrl = hostProfileImageUrl;
        this.bannerImageUrl = bannerImageUrl;
        this.participationLabel = participationLabel;
        this.nextStepLabel = nextStepLabel;
        this.aboutParagraphs = aboutParagraphs == null ? List.of() : List.copyOf(aboutParagraphs);
        this.registrationOpen = registrationOpen;
        this.allowSoloSignup = allowSoloSignup;
        this.canJoinSolo = canJoinSolo;
        this.canLeaveSolo = canLeaveSolo;
        this.requiresLoginToJoin = requiresLoginToJoin;
        this.canCloseRegistration = canCloseRegistration;
        this.canCancelTournament = canCancelTournament;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSportLabel() {
        return sportLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusTone() {
        return statusTone;
    }

    public String getAddress() {
        return address;
    }

    public String getScheduleLabel() {
        return scheduleLabel;
    }

    public String getRegistrationWindowLabel() {
        return registrationWindowLabel;
    }

    public String getBracketSizeLabel() {
        return bracketSizeLabel;
    }

    public String getTeamSizeLabel() {
        return teamSizeLabel;
    }

    public String getFormatLabel() {
        return formatLabel;
    }

    public String getJoinModeLabel() {
        return joinModeLabel;
    }

    public String getPriceLabel() {
        return priceLabel;
    }

    public String getHostLabel() {
        return hostLabel;
    }

    public String getHostProfileHref() {
        return hostProfileHref;
    }

    public String getHostProfileImageUrl() {
        return hostProfileImageUrl;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public String getParticipationLabel() {
        return participationLabel;
    }

    public String getNextStepLabel() {
        return nextStepLabel;
    }

    public List<String> getAboutParagraphs() {
        return aboutParagraphs;
    }

    public boolean isRegistrationOpen() {
        return registrationOpen;
    }

    public boolean isAllowSoloSignup() {
        return allowSoloSignup;
    }

    public boolean isCanJoinSolo() {
        return canJoinSolo;
    }

    public boolean isCanLeaveSolo() {
        return canLeaveSolo;
    }

    public boolean isRequiresLoginToJoin() {
        return requiresLoginToJoin;
    }

    public boolean isCanCloseRegistration() {
        return canCloseRegistration;
    }

    public boolean isCanCancelTournament() {
        return canCancelTournament;
    }
}
