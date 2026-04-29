package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;

public final class PawUiViewModels {

    private PawUiViewModels() {
        // Utility holder for immutable UI-only view models.
    }

    public static final class ShellViewModel {
        private final String brandLabel;
        private final NavItemViewModel hostAction;
        private final NavItemViewModel hostMatchNav;
        private final List<NavItemViewModel> primaryNav;

        public ShellViewModel(
                final String brandLabel,
                final NavItemViewModel hostAction,
                final List<NavItemViewModel> primaryNav) {
            this(brandLabel, hostAction, null, primaryNav);
        }

        public ShellViewModel(
                final String brandLabel,
                final NavItemViewModel hostAction,
                final NavItemViewModel hostMatchNav,
                final List<NavItemViewModel> primaryNav) {
            this.brandLabel = brandLabel;
            this.hostAction = hostAction;
            this.hostMatchNav = hostMatchNav;
            this.primaryNav = primaryNav;
        }

        public String getBrandLabel() {
            return brandLabel;
        }

        public NavItemViewModel getHostAction() {
            return hostAction;
        }

        public NavItemViewModel getHostMatchNav() {
            return hostMatchNav;
        }

        public List<NavItemViewModel> getPrimaryNav() {
            return primaryNav;
        }
    }

    public static final class NavItemViewModel {
        private final String label;
        private final String href;
        private final boolean active;

        public NavItemViewModel(final String label, final String href, final boolean active) {
            this.label = label;
            this.href = href;
            this.active = active;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static final class FilterGroupViewModel {
        private final String title;
        private final List<FilterOptionViewModel> options;

        public FilterGroupViewModel(final String title, final List<FilterOptionViewModel> options) {
            this.title = title;
            this.options = options;
        }

        public String getTitle() {
            return title;
        }

        public List<FilterOptionViewModel> getOptions() {
            return options;
        }
    }

    public static final class FilterOptionViewModel {
        private final String label;
        private final String href;
        private final String meta;
        private final boolean active;

        public FilterOptionViewModel(
                final String label, final String href, final String meta, final boolean active) {
            this.label = label;
            this.href = href;
            this.meta = meta;
            this.active = active;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public String getMeta() {
            return meta;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static final class SelectOptionViewModel {
        private final String label;
        private final String href;
        private final boolean selected;

        public SelectOptionViewModel(
                final String label, final String href, final boolean selected) {
            this.label = label;
            this.href = href;
            this.selected = selected;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public static final class MatchListControlsViewModel {
        private final String cleanSearchAction;
        private final String searchAction;
        private final String searchLabel;
        private final String searchQuery;
        private final String searchPlaceholder;
        private final String searchButtonLabel;
        private final String sortLabel;
        private final List<SelectOptionViewModel> sortOptions;
        private final String filterTitle;
        private final List<FilterGroupViewModel> filterGroups;

        public MatchListControlsViewModel(
                final String cleanSearchAction,
                final String searchAction,
                final String searchLabel,
                final String searchQuery,
                final String searchPlaceholder,
                final String searchButtonLabel,
                final String sortLabel,
                final List<SelectOptionViewModel> sortOptions,
                final String filterTitle,
                final List<FilterGroupViewModel> filterGroups) {
            this.cleanSearchAction = cleanSearchAction;
            this.searchAction = searchAction;
            this.searchLabel = searchLabel;
            this.searchQuery = searchQuery;
            this.searchPlaceholder = searchPlaceholder;
            this.searchButtonLabel = searchButtonLabel;
            this.sortLabel = sortLabel;
            this.sortOptions = sortOptions;
            this.filterTitle = filterTitle;
            this.filterGroups = filterGroups;
        }

        public String getCleanSearchAction() {
            return cleanSearchAction;
        }

        public String getSearchAction() {
            return searchAction;
        }

        public String getSearchLabel() {
            return searchLabel;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public String getSearchPlaceholder() {
            return searchPlaceholder;
        }

        public String getSearchButtonLabel() {
            return searchButtonLabel;
        }

        public String getSortLabel() {
            return sortLabel;
        }

        public List<SelectOptionViewModel> getSortOptions() {
            return sortOptions;
        }

        public String getFilterTitle() {
            return filterTitle;
        }

        public List<FilterGroupViewModel> getFilterGroups() {
            return filterGroups;
        }
    }

    public static final class ChipViewModel {
        private final String label;
        private final String href;
        private final boolean active;
        private final String tone;

        public ChipViewModel(
                final String label, final String href, final boolean active, final String tone) {
            this.label = label;
            this.href = href;
            this.active = active;
            this.tone = tone;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public boolean isActive() {
            return active;
        }

        public String getTone() {
            return tone;
        }
    }

    public static final class PaginationItemViewModel {
        private final String label;
        private final String href;
        private final boolean current;
        private final boolean ellipsis;

        public PaginationItemViewModel(
                final String label,
                final String href,
                final boolean current,
                final boolean ellipsis) {
            this.label = label;
            this.href = href;
            this.current = current;
            this.ellipsis = ellipsis;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public boolean isCurrent() {
            return current;
        }

        public boolean isEllipsis() {
            return ellipsis;
        }
    }

    public static final class EventCardViewModel {
        private final String id;
        private final String href;
        private final String sport;
        private final String title;
        private final String venue;
        private final String schedule;
        private final String priceLabel;
        private final String badge;
        private final String level;
        private final String mediaClass;
        private final String bannerImageUrl;

        public EventCardViewModel(
                final String id,
                final String href,
                final String sport,
                final String title,
                final String venue,
                final String schedule,
                final String priceLabel,
                final String badge,
                final String level,
                final String mediaClass,
                final String bannerImageUrl) {
            this.id = id;
            this.href = href;
            this.sport = sport;
            this.title = title;
            this.venue = venue;
            this.schedule = schedule;
            this.priceLabel = priceLabel;
            this.badge = badge;
            this.level = level;
            this.mediaClass = mediaClass;
            this.bannerImageUrl = bannerImageUrl;
        }

        public String getId() {
            return id;
        }

        public String getHref() {
            return href;
        }

        public String getSport() {
            return sport;
        }

        public String getTitle() {
            return title;
        }

        public String getVenue() {
            return venue;
        }

        public String getSchedule() {
            return schedule;
        }

        public String getPriceLabel() {
            return priceLabel;
        }

        public String getBadge() {
            return badge;
        }

        public String getLevel() {
            return level;
        }

        public String getMediaClass() {
            return mediaClass;
        }

        public String getBannerImageUrl() {
            return bannerImageUrl;
        }

        public boolean hasBannerImage() {
            return bannerImageUrl != null && !bannerImageUrl.isBlank();
        }
    }

    public static final class FeedPageViewModel {
        private final String eyebrow;
        private final String title;
        private final String description;
        private final String searchPlaceholder;
        private final String searchButtonLabel;
        private final List<ChipViewModel> quickFilters;
        private final List<FilterGroupViewModel> filterGroups;
        private final List<EventCardViewModel> featuredEvents;
        private final int page;
        private final int totalPages;
        private final List<PaginationItemViewModel> paginationItems;
        private final String previousPageHref;
        private final String nextPageHref;

        public FeedPageViewModel(
                final String eyebrow,
                final String title,
                final String description,
                final String searchPlaceholder,
                final String searchButtonLabel,
                final List<ChipViewModel> quickFilters,
                final List<FilterGroupViewModel> filterGroups,
                final List<EventCardViewModel> featuredEvents,
                final int page,
                final int totalPages,
                final List<PaginationItemViewModel> paginationItems,
                final String previousPageHref,
                final String nextPageHref) {
            this.eyebrow = eyebrow;
            this.title = title;
            this.description = description;
            this.searchPlaceholder = searchPlaceholder;
            this.searchButtonLabel = searchButtonLabel;
            this.quickFilters = quickFilters;
            this.filterGroups = filterGroups;
            this.featuredEvents = featuredEvents;
            this.page = page;
            this.totalPages = totalPages;
            this.paginationItems = paginationItems;
            this.previousPageHref = previousPageHref;
            this.nextPageHref = nextPageHref;
        }

        public String getEyebrow() {
            return eyebrow;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getSearchPlaceholder() {
            return searchPlaceholder;
        }

        public String getSearchButtonLabel() {
            return searchButtonLabel;
        }

        public List<ChipViewModel> getQuickFilters() {
            return quickFilters;
        }

        public List<FilterGroupViewModel> getFilterGroups() {
            return filterGroups;
        }

        public List<EventCardViewModel> getFeaturedEvents() {
            return featuredEvents;
        }

        public int getPage() {
            return page;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public List<PaginationItemViewModel> getPaginationItems() {
            return paginationItems;
        }

        public String getPreviousPageHref() {
            return previousPageHref;
        }

        public String getNextPageHref() {
            return nextPageHref;
        }
    }

    public static final class BookingDetailViewModel {
        private final String label;
        private final String value;

        public BookingDetailViewModel(final String label, final String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public static final class ParticipantViewModel {
        private final String username;
        private final String avatarLabel;
        private final String profileHref;
        private final String profileImageUrl;
        private final String reviewHref;

        public ParticipantViewModel(
                final String username,
                final String avatarLabel,
                final String profileHref,
                final String profileImageUrl,
                final String reviewHref) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.profileHref = profileHref;
            this.profileImageUrl = profileImageUrl;
            this.reviewHref = reviewHref;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarLabel() {
            return avatarLabel;
        }

        public String getProfileHref() {
            return profileHref;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public String getReviewHref() {
            return reviewHref;
        }
    }

    public static final class RosterParticipantViewModel {
        private final String username;
        private final String avatarLabel;
        private final String removeUrl;
        private final String profileHref;

        public RosterParticipantViewModel(
                final String username,
                final String avatarLabel,
                final String removeUrl,
                final String profileHref) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.removeUrl = removeUrl;
            this.profileHref = profileHref;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarLabel() {
            return avatarLabel;
        }

        public String getRemoveUrl() {
            return removeUrl;
        }

        public String getProfileHref() {
            return profileHref;
        }
    }

    public static final class PendingRequestViewModel {
        private final String username;
        private final String avatarLabel;
        private final String approveUrl;
        private final String rejectUrl;
        private final String profileHref;

        public PendingRequestViewModel(
                final String username,
                final String avatarLabel,
                final String approveUrl,
                final String rejectUrl,
                final String profileHref) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.approveUrl = approveUrl;
            this.rejectUrl = rejectUrl;
            this.profileHref = profileHref;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarLabel() {
            return avatarLabel;
        }

        public String getApproveUrl() {
            return approveUrl;
        }

        public String getRejectUrl() {
            return rejectUrl;
        }

        public String getProfileHref() {
            return profileHref;
        }
    }

    public static final class PendingJoinMatchViewModel {
        private final EventCardViewModel card;
        private final String cancelUrl;

        public PendingJoinMatchViewModel(final EventCardViewModel card, final String cancelUrl) {
            this.card = card;
            this.cancelUrl = cancelUrl;
        }

        public EventCardViewModel getCard() {
            return card;
        }

        public String getCancelUrl() {
            return cancelUrl;
        }
    }

    public static final class InviteParticipantViewModel {
        private final String username;
        private final String avatarLabel;
        private final String profileHref;

        public InviteParticipantViewModel(
                final String username, final String avatarLabel, final String profileHref) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.profileHref = profileHref;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarLabel() {
            return avatarLabel;
        }

        public String getProfileHref() {
            return profileHref;
        }
    }

    public static final class InvitedMatchViewModel {
        private final EventCardViewModel card;
        private final String acceptUrl;
        private final String declineUrl;

        public InvitedMatchViewModel(
                final EventCardViewModel card, final String acceptUrl, final String declineUrl) {
            this.card = card;
            this.acceptUrl = acceptUrl;
            this.declineUrl = declineUrl;
        }

        public EventCardViewModel getCard() {
            return card;
        }

        public String getAcceptUrl() {
            return acceptUrl;
        }

        public String getDeclineUrl() {
            return declineUrl;
        }
    }

    public static final class EventDetailPageViewModel {
        private final EventCardViewModel event;
        private final String heroSubtitle;
        private final String heroMeta;
        private final String hostLabel;
        private final String hostProfileHref;
        private final String hostProfileImageUrl;
        private final List<ParticipantViewModel> participants;
        private final String participantCountLabel;
        private final String participantsEmptyState;
        private final List<String> aboutParagraphs;
        private final String bookingPrice;
        private final List<BookingDetailViewModel> bookingDetails;
        private final String availabilityLabel;
        private final String ctaLabel;
        private final List<EventCardViewModel> nearbyEvents;

        public EventDetailPageViewModel(
                final EventCardViewModel event,
                final String heroSubtitle,
                final String heroMeta,
                final String hostLabel,
                final String hostProfileHref,
                final String hostProfileImageUrl,
                final List<ParticipantViewModel> participants,
                final String participantCountLabel,
                final String participantsEmptyState,
                final List<String> aboutParagraphs,
                final String bookingPrice,
                final List<BookingDetailViewModel> bookingDetails,
                final String availabilityLabel,
                final String ctaLabel,
                final List<EventCardViewModel> nearbyEvents) {
            this.event = event;
            this.heroSubtitle = heroSubtitle;
            this.heroMeta = heroMeta;
            this.hostLabel = hostLabel;
            this.hostProfileHref = hostProfileHref;
            this.hostProfileImageUrl = hostProfileImageUrl;
            this.participants = participants;
            this.participantCountLabel = participantCountLabel;
            this.participantsEmptyState = participantsEmptyState;
            this.aboutParagraphs = aboutParagraphs;
            this.bookingPrice = bookingPrice;
            this.bookingDetails = bookingDetails;
            this.availabilityLabel = availabilityLabel;
            this.ctaLabel = ctaLabel;
            this.nearbyEvents = nearbyEvents;
        }

        public EventCardViewModel getEvent() {
            return event;
        }

        public String getHeroSubtitle() {
            return heroSubtitle;
        }

        public String getHeroMeta() {
            return heroMeta;
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

        public List<ParticipantViewModel> getParticipants() {
            return participants;
        }

        public String getParticipantCountLabel() {
            return participantCountLabel;
        }

        public String getParticipantsEmptyState() {
            return participantsEmptyState;
        }

        public List<String> getAboutParagraphs() {
            return aboutParagraphs;
        }

        public String getBookingPrice() {
            return bookingPrice;
        }

        public List<BookingDetailViewModel> getBookingDetails() {
            return bookingDetails;
        }

        public String getAvailabilityLabel() {
            return availabilityLabel;
        }

        public String getCtaLabel() {
            return ctaLabel;
        }

        public List<EventCardViewModel> getNearbyEvents() {
            return nearbyEvents;
        }
    }

    public static final class PublicProfilePageViewModel {
        private final String username;
        private final String name;
        private final String lastName;
        private final String phone;
        private final String profileImageUrl;

        public PublicProfilePageViewModel(
                final String username,
                final String name,
                final String lastName,
                final String phone,
                final String profileImageUrl) {
            this.username = username;
            this.name = name;
            this.lastName = lastName;
            this.phone = phone;
            this.profileImageUrl = profileImageUrl;
        }

        public String getUsername() {
            return username;
        }

        public String getName() {
            return name;
        }

        public String getLastName() {
            return lastName;
        }

        public String getPhone() {
            return phone;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }
    }

    public static final class PlayerReviewViewModel {
        private final String reviewerUsername;
        private final String reviewerProfileHref;
        private final String reaction;
        private final String reactionLabel;
        private final String comment;
        private final String updatedAtLabel;

        public PlayerReviewViewModel(
                final String reviewerUsername,
                final String reviewerProfileHref,
                final String reaction,
                final String reactionLabel,
                final String comment,
                final String updatedAtLabel) {
            this.reviewerUsername = reviewerUsername;
            this.reviewerProfileHref = reviewerProfileHref;
            this.reaction = reaction;
            this.reactionLabel = reactionLabel;
            this.comment = comment;
            this.updatedAtLabel = updatedAtLabel;
        }

        public String getReviewerUsername() {
            return reviewerUsername;
        }

        public String getReviewerProfileHref() {
            return reviewerProfileHref;
        }

        public String getReaction() {
            return reaction;
        }

        public String getReactionLabel() {
            return reactionLabel;
        }

        public String getComment() {
            return comment;
        }

        public String getUpdatedAtLabel() {
            return updatedAtLabel;
        }
    }
}
