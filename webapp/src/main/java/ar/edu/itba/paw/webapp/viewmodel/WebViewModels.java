package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;

public final class WebViewModels {

    private WebViewModels() {
        // Utility holder for immutable UI-only view models.
    }

    public static final class ShellViewModel {
        private final String brandLabel;
        private final NavItemViewModel hostAction;
        private final List<NavItemViewModel> primaryNav;

        public ShellViewModel(
                final String brandLabel,
                final NavItemViewModel hostAction,
                final List<NavItemViewModel> primaryNav) {
            this.brandLabel = brandLabel;
            this.hostAction = hostAction;
            this.primaryNav = primaryNav;
        }

        public String getBrandLabel() {
            return brandLabel;
        }

        public NavItemViewModel getHostAction() {
            return hostAction;
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

        public ParticipantViewModel(final String username, final String avatarLabel) {
            this.username = username;
            this.avatarLabel = avatarLabel;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarLabel() {
            return avatarLabel;
        }
    }

    public static final class EventDetailPageViewModel {
        private final EventCardViewModel event;
        private final String heroSubtitle;
        private final String heroMeta;
        private final String hostLabel;
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

    public static final class SelectOptionViewModel {
        private final String value;
        private final String label;
        private final boolean selected;

        public SelectOptionViewModel(
                final String value, final String label, final boolean selected) {
            this.value = value;
            this.label = label;
            this.selected = selected;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public boolean isSelected() {
            return selected;
        }
    }
}
