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
    private final String registrationWindowStartLabel;
    private final String registrationWindowEndLabel;
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
    private final List<ParticipantViewModel> participants;
    private final String closeRegistrationDisabledMessage;
    private final boolean closeRegistrationDisabled;
    private final boolean registrationOpen;
    private final boolean allowSoloSignup;
    private final boolean canJoinSolo;
    private final boolean canLeaveSolo;
    private final boolean requiresLoginToJoin;
    private final boolean registrationNotStarted;
    private final boolean canCloseRegistration;
    private final boolean canEditTournament;
    private final boolean canCancelTournament;
    private final boolean canManageBracket;
    private final boolean canViewBracket;

    public TournamentDetailViewModel(
            final Long id,
            final String title,
            final String description,
            final String sportLabel,
            final String statusLabel,
            final String statusTone,
            final String address,
            final String scheduleLabel,
            final String registrationWindowStartLabel,
            final String registrationWindowEndLabel,
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
            final List<ParticipantViewModel> participants,
            final String closeRegistrationDisabledMessage,
            final boolean closeRegistrationDisabled,
            final boolean registrationOpen,
            final boolean allowSoloSignup,
            final boolean canJoinSolo,
            final boolean canLeaveSolo,
            final boolean requiresLoginToJoin,
            final boolean registrationNotStarted,
            final boolean canCloseRegistration,
            final boolean canEditTournament,
            final boolean canCancelTournament,
            final boolean canManageBracket,
            final boolean canViewBracket) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.sportLabel = sportLabel;
        this.statusLabel = statusLabel;
        this.statusTone = statusTone;
        this.address = address;
        this.scheduleLabel = scheduleLabel;
        this.registrationWindowStartLabel = registrationWindowStartLabel;
        this.registrationWindowEndLabel = registrationWindowEndLabel;
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
        this.participants = participants == null ? List.of() : List.copyOf(participants);
        this.closeRegistrationDisabledMessage = closeRegistrationDisabledMessage;
        this.closeRegistrationDisabled = closeRegistrationDisabled;
        this.registrationOpen = registrationOpen;
        this.allowSoloSignup = allowSoloSignup;
        this.canJoinSolo = canJoinSolo;
        this.canLeaveSolo = canLeaveSolo;
        this.requiresLoginToJoin = requiresLoginToJoin;
        this.registrationNotStarted = registrationNotStarted;
        this.canCloseRegistration = canCloseRegistration;
        this.canEditTournament = canEditTournament;
        this.canCancelTournament = canCancelTournament;
        this.canManageBracket = canManageBracket;
        this.canViewBracket = canViewBracket;
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

    public String getRegistrationWindowStartLabel() {
        return registrationWindowStartLabel;
    }

    public String getRegistrationWindowEndLabel() {
        return registrationWindowEndLabel;
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

    public List<ParticipantViewModel> getParticipants() {
        return participants;
    }

    public String getCloseRegistrationDisabledMessage() {
        return closeRegistrationDisabledMessage;
    }

    public boolean isCloseRegistrationDisabled() {
        return closeRegistrationDisabled;
    }

    public boolean isHasParticipants() {
        return !participants.isEmpty();
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

    public boolean isRegistrationNotStarted() {
        return registrationNotStarted;
    }

    public boolean isCanCloseRegistration() {
        return canCloseRegistration;
    }

    public boolean isCanEditTournament() {
        return canEditTournament;
    }

    public boolean isCanCancelTournament() {
        return canCancelTournament;
    }

    public boolean isCanManageBracket() {
        return canManageBracket;
    }

    public boolean isCanViewBracket() {
        return canViewBracket;
    }

    public static class ParticipantViewModel {

        private final String primaryLabel;
        private final String secondaryLabel;

        public ParticipantViewModel(final String primaryLabel, final String secondaryLabel) {
            this.primaryLabel = primaryLabel;
            this.secondaryLabel = secondaryLabel;
        }

        public String getPrimaryLabel() {
            return primaryLabel;
        }

        public String getSecondaryLabel() {
            return secondaryLabel;
        }
    }
}
