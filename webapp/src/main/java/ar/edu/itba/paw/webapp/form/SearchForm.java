package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.query.EventCategory;
import ar.edu.itba.paw.models.query.EventFilter;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.webapp.validation.ValidSearchForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

@ValidSearchForm
public class SearchForm {

    private static final EventFilter DEFAULT_FILTER = EventFilter.UPCOMING;
    private static final EventType DEFAULT_TYPE = EventType.MATCH;
    private static final EventCategory DEFAULT_CATEGORY = EventCategory.HOSTED;

    @Size(max = 150, message = "{SearchForm.q.Size}")
    @Pattern(regexp = "^[\\p{L}\\p{N} ]*$", message = "{SearchForm.q.Pattern}")
    private String q = "";

    private EventSort sort = EventSort.SOONEST;
    private EventFilter filter = DEFAULT_FILTER;
    private EventType type = DEFAULT_TYPE;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ZoneId timezone;
    private List<Sport> sport = new ArrayList<>();
    private List<EventStatus> status = new ArrayList<>();
    private List<EventCategory> category = new ArrayList<>();
    private List<EventVisibility> visibility = new ArrayList<>();
    private Double latitude;
    private Double longitude;

    @Min(value = 1)
    private int page = 1;

    public String getQ() {
        return q;
    }

    public void setQ(final String q) {
        this.q = q;
    }

    public EventSort getSort() {
        return sort;
    }

    public void setSort(final EventSort sort) {
        this.sort = sort;
    }

    public EventFilter getFilter() {
        return filter;
    }

    public String getFilterName() {
        return filter == null ? null : filter.name();
    }

    public void setFilter(final EventFilter filter) {
        this.filter = filter;
    }

    public EventType getType() {
        return type;
    }

    public void setType(final EventType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(final BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(final BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public void setTimezone(final ZoneId timezone) {
        this.timezone = timezone;
    }

    public List<Sport> getSport() {
        return sport;
    }

    public void setSport(final List<Sport> sport) {
        this.sport = copyEnumValues(sport);
    }

    public List<EventStatus> getStatus() {
        return status;
    }

    public void setStatus(final List<EventStatus> status) {
        this.status = copyEnumValues(status);
    }

    public List<EventCategory> getCategory() {
        return category;
    }

    public void setCategory(final List<EventCategory> category) {
        this.category = copyEnumValues(category);
    }

    public List<EventVisibility> getVisibility() {
        return visibility;
    }

    public void setVisibility(final List<EventVisibility> visibility) {
        this.visibility = copyEnumValues(visibility);
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    public int getPage() {
        return page;
    }

    public void setPage(final int page) {
        this.page = page;
    }

    public void normalizeDefaults() {
        if (sort == null) {
            sort = EventSort.SOONEST;
        }
        if (filter == null) {
            filter = DEFAULT_FILTER;
        }
        if (type == null) {
            type = DEFAULT_TYPE;
        }
        if (category == null || category.isEmpty()) {
            category.add(DEFAULT_CATEGORY);
        }
    }

    private static <T> List<T> copyEnumValues(final List<T> values) {
        return values == null
                ? new ArrayList<>()
                : values.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(ArrayList::new));
    }
}
