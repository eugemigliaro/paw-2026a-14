package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;
import java.util.Map;

public final class UiViewModels {

    private UiViewModels() {
        // Utility holder for immutable UI-only view models.
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
        private final Map<String, String> params;
        private final String meta;
        private final boolean active;

        public FilterOptionViewModel(
                final String label,
                final String href,
                final Map<String, String> params,
                final String meta,
                final boolean active) {
            this.label = label;
            this.href = href;
            this.params = params;
            this.meta = meta;
            this.active = active;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public Map<String, String> getParams() {
            return params;
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
        private final Map<String, String> params;
        private final boolean selected;

        public SelectOptionViewModel(
                final String label,
                final String href,
                final Map<String, String> params,
                final boolean selected) {
            this.label = label;
            this.href = href;
            this.params = params;
            this.selected = selected;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public Map<String, String> getParams() {
            return params;
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
        private final String hostLabel;
        private final String schedule;
        private final String dateLabel;
        private final String timeLabel;
        private final String priceLabel;
        private final String badge;
        private final String relationshipBadgeType;
        private final String relationshipBadgeLabel;
        private final List<EventRelationshipBadgeViewModel> relationshipBadges;
        private final String recurringLabel;
        private final String level;
        private final String distanceLabel;
        private final String mediaClass;
        private final String bannerImageUrl;

        public EventCardViewModel(
                final String id,
                final String href,
                final String sport,
                final String title,
                final String venue,
                final String schedule,
                final String dateLabel,
                final String timeLabel,
                final String priceLabel,
                final String badge,
                final String relationshipBadgeType,
                final String relationshipBadgeLabel,
                final String recurringLabel,
                final String level,
                final String mediaClass,
                final String bannerImageUrl) {
            this.id = id;
            this.href = href;
            this.sport = sport;
            this.title = title;
            this.venue = venue;
            this.hostLabel = null;
            this.schedule = schedule;
            this.dateLabel = dateLabel;
            this.timeLabel = timeLabel;
            this.priceLabel = priceLabel;
            this.badge = badge;
            this.relationshipBadgeType = relationshipBadgeType;
            this.relationshipBadgeLabel = relationshipBadgeLabel;
            this.relationshipBadges =
                    relationshipBadgeType == null || relationshipBadgeLabel == null
                            ? List.of()
                            : List.of(
                                    new EventRelationshipBadgeViewModel(
                                            relationshipBadgeType, relationshipBadgeLabel));
            this.recurringLabel = recurringLabel;
            this.level = level;
            this.distanceLabel = null;
            this.mediaClass = mediaClass;
            this.bannerImageUrl = bannerImageUrl;
        }

        public EventCardViewModel(
                final String id,
                final String href,
                final String sport,
                final String title,
                final String venue,
                final String schedule,
                final String dateLabel,
                final String timeLabel,
                final String priceLabel,
                final String badge,
                final List<EventRelationshipBadgeViewModel> relationshipBadges,
                final String recurringLabel,
                final String level,
                final String mediaClass,
                final String bannerImageUrl) {
            this.id = id;
            this.href = href;
            this.sport = sport;
            this.title = title;
            this.venue = venue;
            this.hostLabel = null;
            this.schedule = schedule;
            this.dateLabel = dateLabel;
            this.timeLabel = timeLabel;
            this.priceLabel = priceLabel;
            this.badge = badge;
            this.relationshipBadges =
                    relationshipBadges == null ? List.of() : List.copyOf(relationshipBadges);
            final EventRelationshipBadgeViewModel firstBadge =
                    this.relationshipBadges.isEmpty() ? null : this.relationshipBadges.get(0);
            this.relationshipBadgeType = firstBadge == null ? null : firstBadge.getType();
            this.relationshipBadgeLabel = firstBadge == null ? null : firstBadge.getLabel();
            this.recurringLabel = recurringLabel;
            this.level = level;
            this.distanceLabel = null;
            this.mediaClass = mediaClass;
            this.bannerImageUrl = bannerImageUrl;
        }

        public EventCardViewModel(
                final String id,
                final String href,
                final String sport,
                final String title,
                final String venue,
                final String hostLabel,
                final String schedule,
                final String dateLabel,
                final String timeLabel,
                final String priceLabel,
                final String badge,
                final List<EventRelationshipBadgeViewModel> relationshipBadges,
                final String recurringLabel,
                final String level,
                final String mediaClass,
                final String bannerImageUrl) {
            this(
                    id,
                    href,
                    sport,
                    title,
                    venue,
                    hostLabel,
                    schedule,
                    dateLabel,
                    timeLabel,
                    priceLabel,
                    badge,
                    relationshipBadges,
                    recurringLabel,
                    level,
                    null,
                    mediaClass,
                    bannerImageUrl);
        }

        public EventCardViewModel(
                final String id,
                final String href,
                final String sport,
                final String title,
                final String venue,
                final String hostLabel,
                final String schedule,
                final String dateLabel,
                final String timeLabel,
                final String priceLabel,
                final String badge,
                final List<EventRelationshipBadgeViewModel> relationshipBadges,
                final String recurringLabel,
                final String level,
                final String distanceLabel,
                final String mediaClass,
                final String bannerImageUrl) {
            this.id = id;
            this.href = href;
            this.sport = sport;
            this.title = title;
            this.venue = venue;
            this.hostLabel = hostLabel;
            this.schedule = schedule;
            this.dateLabel = dateLabel;
            this.timeLabel = timeLabel;
            this.priceLabel = priceLabel;
            this.badge = badge;
            this.relationshipBadges =
                    relationshipBadges == null ? List.of() : List.copyOf(relationshipBadges);
            final EventRelationshipBadgeViewModel firstBadge =
                    this.relationshipBadges.isEmpty() ? null : this.relationshipBadges.get(0);
            this.relationshipBadgeType = firstBadge == null ? null : firstBadge.getType();
            this.relationshipBadgeLabel = firstBadge == null ? null : firstBadge.getLabel();
            this.recurringLabel = recurringLabel;
            this.level = level;
            this.distanceLabel = distanceLabel;
            this.mediaClass = mediaClass;
            this.bannerImageUrl = bannerImageUrl;
        }

        public EventCardViewModel(
                final String id,
                final String href,
                final String sport,
                final String title,
                final String venue,
                final String schedule,
                final String dateLabel,
                final String timeLabel,
                final String priceLabel,
                final String badge,
                final String level,
                final String mediaClass,
                final String bannerImageUrl) {
            this(
                    id,
                    href,
                    sport,
                    title,
                    venue,
                    schedule,
                    dateLabel,
                    timeLabel,
                    priceLabel,
                    badge,
                    (String) null,
                    (String) null,
                    (String) null,
                    level,
                    mediaClass,
                    bannerImageUrl);
        }

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
            this(
                    id,
                    href,
                    sport,
                    title,
                    venue,
                    schedule,
                    null,
                    null,
                    priceLabel,
                    badge,
                    (String) null,
                    (String) null,
                    (String) null,
                    level,
                    mediaClass,
                    bannerImageUrl);
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

        public String getHostLabel() {
            return hostLabel;
        }

        public String getSchedule() {
            return schedule;
        }

        public String getDateLabel() {
            return dateLabel;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public String getPriceLabel() {
            return priceLabel;
        }

        public String getBadge() {
            return badge;
        }

        public String getRelationshipBadgeType() {
            return relationshipBadgeType;
        }

        public String getRelationshipBadgeLabel() {
            return relationshipBadgeLabel;
        }

        public boolean hasRelationshipBadge() {
            return !relationshipBadges.isEmpty();
        }

        public boolean isRelationshipBadge() {
            return hasRelationshipBadge();
        }

        public List<EventRelationshipBadgeViewModel> getRelationshipBadges() {
            return relationshipBadges;
        }

        public String getRecurringLabel() {
            return recurringLabel;
        }

        public boolean isRecurring() {
            return recurringLabel != null && !recurringLabel.isBlank();
        }

        public String getLevel() {
            return level;
        }

        public String getDistanceLabel() {
            return distanceLabel;
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

    public static final class EventRelationshipBadgeViewModel {
        private final String type;
        private final String label;

        public EventRelationshipBadgeViewModel(final String type, final String label) {
            this.type = type;
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public String getLabel() {
            return label;
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

    public static final class EventOccurrenceViewModel {
        private final String href;
        private final String schedule;
        private final String statusLabel;
        private final String statusTone;
        private final boolean current;
        private final String spotsLabel;
        private final String spotsTone;

        public EventOccurrenceViewModel(
                final String href,
                final String schedule,
                final String statusLabel,
                final String statusTone,
                final boolean current,
                final String spotsLabel,
                final String spotsTone) {
            this.href = href;
            this.schedule = schedule;
            this.statusLabel = statusLabel;
            this.statusTone = statusTone;
            this.current = current;
            this.spotsLabel = spotsLabel;
            this.spotsTone = spotsTone;
        }

        public String getHref() {
            return href;
        }

        public String getSchedule() {
            return schedule;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public String getStatusTone() {
            return statusTone;
        }

        public boolean isCurrent() {
            return current;
        }

        public String getSpotsLabel() {
            return spotsLabel;
        }

        public String getSpotsTone() {
            return spotsTone;
        }
    }

    public static final class ParticipantViewModel {
        private final String username;
        private final String avatarLabel;
        private final String profileHref;
        private final String profileImageUrl;
        private final String reviewHref;
        private final String removeUrl;

        public ParticipantViewModel(
                final String username,
                final String avatarLabel,
                final String profileHref,
                final String profileImageUrl,
                final String reviewHref) {
            this(username, avatarLabel, profileHref, profileImageUrl, reviewHref, null);
        }

        public ParticipantViewModel(
                final String username,
                final String avatarLabel,
                final String profileHref,
                final String profileImageUrl,
                final String reviewHref,
                final String removeUrl) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.profileHref = profileHref;
            this.profileImageUrl = profileImageUrl;
            this.reviewHref = reviewHref;
            this.removeUrl = removeUrl;
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

        public String getRemoveUrl() {
            return removeUrl;
        }
    }

    public static final class PendingRequestViewModel {
        private final String username;
        private final String avatarLabel;
        private final String approveUrl;
        private final String rejectUrl;
        private final String profileHref;
        private final String profileImageUrl;
        private final String matchTitle;
        private final String matchHref;
        private final boolean seriesRequest;

        public PendingRequestViewModel(
                final String username,
                final String avatarLabel,
                final String approveUrl,
                final String rejectUrl,
                final String profileHref) {
            this(
                    username,
                    avatarLabel,
                    approveUrl,
                    rejectUrl,
                    profileHref,
                    null,
                    null,
                    null,
                    false);
        }

        public PendingRequestViewModel(
                final String username,
                final String avatarLabel,
                final String approveUrl,
                final String rejectUrl,
                final String matchTitle,
                final String matchHref,
                final boolean seriesRequest) {
            this(
                    username,
                    avatarLabel,
                    approveUrl,
                    rejectUrl,
                    null,
                    null,
                    matchTitle,
                    matchHref,
                    seriesRequest);
        }

        public PendingRequestViewModel(
                final String username,
                final String avatarLabel,
                final String approveUrl,
                final String rejectUrl,
                final String profileHref,
                final String matchTitle,
                final String matchHref,
                final boolean seriesRequest) {
            this(
                    username,
                    avatarLabel,
                    approveUrl,
                    rejectUrl,
                    profileHref,
                    null,
                    matchTitle,
                    matchHref,
                    seriesRequest);
        }

        public PendingRequestViewModel(
                final String username,
                final String avatarLabel,
                final String approveUrl,
                final String rejectUrl,
                final String profileHref,
                final String profileImageUrl,
                final String matchTitle,
                final String matchHref,
                final boolean seriesRequest) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.approveUrl = approveUrl;
            this.rejectUrl = rejectUrl;
            this.profileHref = profileHref;
            this.profileImageUrl = profileImageUrl;
            this.matchTitle = matchTitle;
            this.matchHref = matchHref;
            this.seriesRequest = seriesRequest;
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

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public String getMatchTitle() {
            return matchTitle;
        }

        public String getMatchHref() {
            return matchHref;
        }

        public boolean isSeriesRequest() {
            return seriesRequest;
        }
    }

    public static final class InviteParticipantViewModel {
        private final String username;
        private final String avatarLabel;
        private final String profileHref;
        private final String profileImageUrl;

        public InviteParticipantViewModel(
                final String username, final String avatarLabel, final String profileHref) {
            this(username, avatarLabel, profileHref, null);
        }

        public InviteParticipantViewModel(
                final String username,
                final String avatarLabel,
                final String profileHref,
                final String profileImageUrl) {
            this.username = username;
            this.avatarLabel = avatarLabel;
            this.profileHref = profileHref;
            this.profileImageUrl = profileImageUrl;
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
    }
}
