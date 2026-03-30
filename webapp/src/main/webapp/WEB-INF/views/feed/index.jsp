<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | Explore" />
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="/WEB-INF/views/includes/head.jspf" %>
  </head>
  <body>
    <div class="app-shell">
      <%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

      <main class="page-shell page-shell--feed">
        <aside class="feed-sidebar" aria-label="Event filters">
          <div class="panel filter-rail">
            <c:forEach var="group" items="${feedPage.filterGroups}">
              <section class="filter-rail__group">
                <h2 class="filter-rail__title"><c:out value="${group.title}" /></h2>
                <div class="filter-rail__options" data-filter-group="${group.title}">
                  <c:forEach var="option" items="${group.options}">
                    <button
                      type="button"
                      class="filter-rail__option ${option.active ? 'filter-rail__option--active' : ''}"
                      aria-pressed="${option.active ? 'true' : 'false'}">
                      <span><c:out value="${option.label}" /></span>
                      <c:if test="${not empty option.meta}">
                        <span><c:out value="${option.meta}" /></span>
                      </c:if>
                    </button>
                  </c:forEach>
                </div>
              </section>
            </c:forEach>
          </div>
        </aside>

        <section class="feed-main">
          <section class="feed-hero-stage">
            <section class="hero-panel">
              <c:if test="${not empty feedPage.eyebrow}">
                <p class="eyebrow"><c:out value="${feedPage.eyebrow}" /></p>
              </c:if>
              <h1 class="hero-panel__title"><c:out value="${feedPage.title}" /></h1>
              <p class="hero-panel__description"><c:out value="${feedPage.description}" /></p>
            </section>

            <section class="search-panel" aria-label="Search events">
              <div class="search-panel__row">
                <div class="search-panel__input">
                  <span class="search-panel__icon" aria-hidden="true"></span>
                  <span><c:out value="${feedPage.searchPlaceholder}" /></span>
                </div>
                <ui:button label="${feedPage.searchButtonLabel}" disabled="${true}" />
              </div>
            </section>

            <div class="chip-row chip-row--centered" data-chip-group="quick-filters">
              <c:forEach var="chip" items="${feedPage.quickFilters}">
                <c:set var="chipHref" value="" />
                <c:if test="${not empty chip.href}">
                  <c:set var="chipHref" value="${pageContext.request.contextPath}${chip.href}" />
                </c:if>
                <ui:chip
                  label="${chip.label}"
                  href="${chipHref}"
                  active="${chip.active}"
                  tone="${chip.tone}" />
              </c:forEach>
            </div>
          </section>

          <section>
            <div class="section-head">
              <div>
                <h2 class="section-head__title">Trending this week</h2>
                <p class="section-head__meta">Popular community sessions around the city.</p>
              </div>
            </div>

            <div class="event-grid">
              <c:forEach var="event" items="${feedPage.featuredEvents}">
                <ui:card
                  href="${pageContext.request.contextPath}${event.href}"
                  className="event-card"
                  ariaLabel="${event.title}">
                  <div class="event-card__media ${event.mediaClass}">
                    <span class="event-card__badge"><c:out value="${event.badge}" /></span>
                  </div>

                  <div class="event-card__body">
                    <span class="event-card__sport"><c:out value="${event.sport}" /></span>
                    <h3 class="event-card__title"><c:out value="${event.title}" /></h3>
                    <div class="event-card__meta">
                      <span><c:out value="${event.venue}" /></span>
                      <span><c:out value="${event.schedule}" /></span>
                      <span><c:out value="${event.level}" /></span>
                    </div>

                    <div class="event-card__footer">
                      <div class="event-card__avatars" aria-hidden="true">
                        <c:forEach var="attendee" items="${event.attendeeInitials}">
                          <span class="avatar-badge"><c:out value="${attendee}" /></span>
                        </c:forEach>
                      </div>

                      <div class="event-card__cta">
                        <span><c:out value="${event.priceLabel}" /></span>
                        <span>&middot; View event</span>
                      </div>
                    </div>
                  </div>
                </ui:card>
              </c:forEach>
            </div>
          </section>

        </section>
      </main>
    </div>
    <script>
      document.querySelectorAll("[data-filter-group]").forEach(function(group) {
        group.addEventListener("click", function(event) {
          var option = event.target.closest(".filter-rail__option");
          if (!option) {
            return;
          }

          group.querySelectorAll(".filter-rail__option").forEach(function(button) {
            button.classList.remove("filter-rail__option--active");
            button.setAttribute("aria-pressed", "false");
          });

          option.classList.add("filter-rail__option--active");
          option.setAttribute("aria-pressed", "true");
        });
      });

      document.querySelectorAll("[data-chip-group]").forEach(function(group) {
        group.addEventListener("click", function(event) {
          var chip = event.target.closest(".chip");
          if (!chip || chip.tagName === "A") {
            return;
          }

          group.querySelectorAll(".chip").forEach(function(button) {
            button.classList.remove("chip--active");
            button.setAttribute("aria-pressed", "false");
          });

          chip.classList.add("chip--active");
          chip.setAttribute("aria-pressed", "true");
        });
      });
    </script>
  </body>
</html>
