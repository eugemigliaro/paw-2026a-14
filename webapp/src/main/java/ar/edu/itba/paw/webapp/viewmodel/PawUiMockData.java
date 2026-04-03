package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.BookingDetailViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ChipViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ComponentPreviewPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.CreateEventPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventDetailPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.InfoTileViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.SelectOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PawUiMockData {

    private static final List<EventSeed> EVENT_SEEDS =
            List.of(
                    new EventSeed(
                            "sunrise-padel-championship",
                            "Padel",
                            "Sunrise Padel Championship",
                            "Club de Padel Estrella, Madrid",
                            "Every Saturday, 08:00 AM",
                            "$15.00",
                            "6 spots left",
                            "Intermediate",
                            "media-tile--padel",
                            "A premium sunrise session focused on doubles rotation, structured matchups, and a social breakfast cooldown.",
                            "Marcos Silva",
                            "Two curated rounds, level-matched pairings, and an easy flow from warm-up to competitive play."),
                    new EventSeed(
                            "svs-midweek-football-match",
                            "Football",
                            "5v5 Midweek Football Match",
                            "Ciudad Verde",
                            "Tomorrow - 19:30",
                            "$10.00",
                            "6 spots left",
                            "All levels",
                            "media-tile--football",
                            "A weekday match designed for quick check-in, balanced teams, and an easy after-office rhythm.",
                            "Sofia Varela",
                            "Expect a sharp kickoff, organized rotations, and a low-friction join flow for casual players."),
                    new EventSeed(
                            "doubles-social-night",
                            "Padel",
                            "Doubles Social Night",
                            "Riverside Club",
                            "Friday - 20:00",
                            "Free",
                            "2 spots left",
                            "Beginner friendly",
                            "media-tile--tennis",
                            "A friendly social padel evening with flexible doubles pairings and plenty of coaching cues.",
                            "Lucia Mendez",
                            "Warm lighting, fast turnaround on courts, and just enough structure to keep play moving."),
                    new EventSeed(
                            "sunset-flow-in-the-park",
                            "Yoga",
                            "Sunset Flow in the Park",
                            "Parque Central",
                            "Today - 18:00",
                            "Free",
                            "8 spots left",
                            "All levels",
                            "media-tile--basketball",
                            "A restorative outdoor flow with mobility work, mindful breathing, and a golden-hour cooldown.",
                            "Mila Ortega",
                            "Bring a mat, expect a calm pace, and stay for a short social circle after the session."),
                    new EventSeed(
                            "weekend-loft-run",
                            "Running",
                            "Weekend Loft Run",
                            "Loft Trail Hub",
                            "Sun - 08:00",
                            "$12.00",
                            "12 spots left",
                            "Intermediate",
                            "media-tile--running",
                            "A paced urban run with route markers, hydration support, and a short coffee stop at the finish.",
                            "Tomas Rivas",
                            "Ideal for runners who want a coached rhythm and an easy community-first weekend plan."),
                    new EventSeed(
                            "pickup-basketball",
                            "Basketball",
                            "Pick-up Basketball",
                            "Urban Court 23",
                            "Tonight - 20:30",
                            "$8.00",
                            "3 spots left",
                            "All levels",
                            "media-tile--basketball",
                            "A pickup session with balanced teams, scoreboard tracking, and quick on/off court rotations.",
                            "Jordan Molina",
                            "Fast transitions, community energy, and enough structure to keep games competitive."));

    private static final Map<String, EventSeed> EVENTS_BY_ID = buildEventMap();
    private static final List<SelectOptionViewModel> SKILL_LEVELS =
            List.of(
                    new SelectOptionViewModel("all-levels", "All levels", false),
                    new SelectOptionViewModel("beginner-friendly", "Beginner friendly", false),
                    new SelectOptionViewModel("intermediate", "Intermediate", true),
                    new SelectOptionViewModel("advanced", "Advanced", false));
    private static final List<SelectOptionViewModel> PRICE_MODES =
            List.of(
                    new SelectOptionViewModel("paid", "Paid", true),
                    new SelectOptionViewModel("free", "Free", false));

    private PawUiMockData() {
        // Static mock content only.
    }

    public static ShellViewModel browseShell() {
        return new ShellViewModel(
                "Match Point",
                new NavItemViewModel("Switch to Hosting", "/host/events/new", false),
                List.of());
    }

    public static ShellViewModel hostShell() {
        return new ShellViewModel(
                "Match Point", new NavItemViewModel("Switch to Joining", "/", false), List.of());
    }

    public static FeedPageViewModel feedPage() {
        return new FeedPageViewModel(
                "",
                "Find your next game.",
                "Discover local sports events, join communities, and get active with Match Point.",
                "What sports event are you looking for?",
                "Find Matches",
                List.of(
                        new ChipViewModel("All", null, true, "default"),
                        new ChipViewModel("Football", null, false, "default"),
                        new ChipViewModel("Tennis", null, false, "default"),
                        new ChipViewModel("Basketball", null, false, "default"),
                        new ChipViewModel("Padel", null, false, "default")),
                List.of(
                        new FilterGroupViewModel(
                                "Categories",
                                List.of(
                                        new FilterOptionViewModel("Football", null, "14", true),
                                        new FilterOptionViewModel("Tennis", null, "8", false),
                                        new FilterOptionViewModel("Basketball", null, "7", false),
                                        new FilterOptionViewModel("Padel", null, "11", false))),
                        new FilterGroupViewModel(
                                "Time",
                                List.of(
                                        new FilterOptionViewModel("Today", null, null, false),
                                        new FilterOptionViewModel("Tomorrow", null, null, false),
                                        new FilterOptionViewModel("Weekend", null, null, true))),
                        new FilterGroupViewModel(
                                "Price",
                                List.of(
                                        new FilterOptionViewModel("Free", null, null, false),
                                        new FilterOptionViewModel("Paid", null, null, true))),
                        new FilterGroupViewModel(
                                "Skill level",
                                List.of(
                                        new FilterOptionViewModel("Beginner", null, null, false),
                                        new FilterOptionViewModel("Intermediate", null, null, true),
                                        new FilterOptionViewModel("Advanced", null, null, false)))),
                EVENT_SEEDS.stream().map(PawUiMockData::toCard).toList(),
                1,
                1,
                null,
                null);
    }

    public static Optional<EventDetailPageViewModel> findEventPage(final String eventId) {
        final EventSeed seed = EVENTS_BY_ID.get(eventId);

        if (seed == null) {
            return Optional.empty();
        }

        return Optional.of(
                new EventDetailPageViewModel(
                        toCard(seed),
                        seed.getSport() + " tournament",
                        seed.getSchedule() + " - " + seed.getVenue(),
                        seed.getHostLabel(),
                        List.of(seed.getAboutLead(), seed.getAboutFollowUp()),
                        List.of(
                                new InfoTileViewModel(
                                        "Format",
                                        "Structured pairings",
                                        "Hosted rounds and easy rotations keep the session moving from warm-up to final point.",
                                        "mint"),
                                new InfoTileViewModel(
                                        "Atmosphere",
                                        "Community-first hosting",
                                        "Expect quick check-in, level-aware matchmaking, and a social rhythm around the courts.",
                                        "sand")),
                        seed.getVenue(),
                        "Check in 15 minutes before kickoff and head to the main reception for court assignment.",
                        seed.getPriceLabel(),
                        List.of(
                                new BookingDetailViewModel("Session", seed.getSchedule()),
                                new BookingDetailViewModel("Level", seed.getLevel()),
                                new BookingDetailViewModel("Venue", seed.getVenue()),
                                new BookingDetailViewModel("Format", "Hosted community event")),
                        seed.getBadge(),
                        "Sign up",
                        EVENT_SEEDS.stream()
                                .filter(event -> !event.getId().equals(seed.getId()))
                                .sorted(Comparator.comparing(EventSeed::getSport))
                                .limit(3)
                                .map(PawUiMockData::toCard)
                                .toList()));
    }

    public static CreateEventPageViewModel createEventPage() {
        return new CreateEventPageViewModel(
                "Host mode layout",
                "Create your event",
                "This MVP only defines the form layout, upload area, and sidebar composition for the hosting screen.",
                List.of(
                        new ChipViewModel("Football", null, true, "default"),
                        new ChipViewModel("Tennis", null, false, "default"),
                        new ChipViewModel("Basketball", null, false, "default"),
                        new ChipViewModel("Padel", null, false, "default"),
                        new ChipViewModel("Other", null, false, "default")),
                SKILL_LEVELS,
                PRICE_MODES,
                List.of("Public listed event", "Auto-approve attendees", "Visual placeholder only"),
                "Upload cover photo",
                "Recommended size: 1600 x 900 px");
    }

    public static ComponentPreviewPageViewModel componentPreviewPage() {
        return new ComponentPreviewPageViewModel(
                toCard(EVENT_SEEDS.get(0)),
                List.of(
                        new ChipViewModel("Featured", null, true, "default"),
                        new ChipViewModel("Open spots", null, false, "default"),
                        new ChipViewModel("Premium", null, false, "muted")),
                SKILL_LEVELS,
                PRICE_MODES);
    }

    private static EventCardViewModel toCard(final EventSeed seed) {
        return new EventCardViewModel(
                seed.getId(),
                "/events/" + seed.getId(),
                seed.getSport(),
                seed.getTitle(),
                seed.getVenue(),
                seed.getSchedule(),
                seed.getPriceLabel(),
                seed.getBadge(),
                seed.getLevel(),
                seed.getMediaClass(),
                attendeesFor(seed.getSport()));
    }

    private static List<String> attendeesFor(final String sport) {
        switch (sport) {
            case "Football":
                return List.of("AL", "JM", "SR");
            case "Basketball":
                return List.of("MK", "PS", "TR");
            case "Tennis":
                return List.of("LC", "DA");
            default:
                return List.of("MP", "IA", "JV");
        }
    }

    private static Map<String, EventSeed> buildEventMap() {
        final Map<String, EventSeed> eventMap = new LinkedHashMap<>();
        EVENT_SEEDS.forEach(seed -> eventMap.put(seed.getId(), seed));
        return eventMap;
    }

    private static final class EventSeed {
        private final String id;
        private final String sport;
        private final String title;
        private final String venue;
        private final String schedule;
        private final String priceLabel;
        private final String badge;
        private final String level;
        private final String mediaClass;
        private final String aboutLead;
        private final String hostLabel;
        private final String aboutFollowUp;

        private EventSeed(
                final String id,
                final String sport,
                final String title,
                final String venue,
                final String schedule,
                final String priceLabel,
                final String badge,
                final String level,
                final String mediaClass,
                final String aboutLead,
                final String hostLabel,
                final String aboutFollowUp) {
            this.id = id;
            this.sport = sport;
            this.title = title;
            this.venue = venue;
            this.schedule = schedule;
            this.priceLabel = priceLabel;
            this.badge = badge;
            this.level = level;
            this.mediaClass = mediaClass;
            this.aboutLead = aboutLead;
            this.hostLabel = hostLabel;
            this.aboutFollowUp = aboutFollowUp;
        }

        public String getId() {
            return id;
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

        public String getAboutLead() {
            return aboutLead;
        }

        public String getHostLabel() {
            return hostLabel;
        }

        public String getAboutFollowUp() {
            return aboutFollowUp;
        }
    }
}
