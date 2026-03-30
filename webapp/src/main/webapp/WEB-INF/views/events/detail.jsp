<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | ${eventPage.event.title}" />
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="/WEB-INF/views/includes/head.jspf" %>
  </head>
  <body>
    <div class="app-shell">
      <%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

      <main class="page-shell page-shell--detail">
        <section class="event-hero ${eventPage.event.mediaClass}">
          <div class="event-hero__content">
            <span class="event-hero__badge"><c:out value="${eventPage.heroSubtitle}" /></span>
            <h1 class="event-hero__title"><c:out value="${eventPage.event.title}" /></h1>
            <div class="event-hero__meta">
              <span class="event-hero__meta-item">
                <span class="meta-icon meta-icon--pin" aria-hidden="true"></span>
                <c:out value="${eventPage.locationTitle}" />
              </span>
              <span class="event-hero__meta-item">
                <span class="meta-icon meta-icon--calendar" aria-hidden="true"></span>
                <c:out value="${eventPage.event.schedule}" />
              </span>
            </div>
          </div>
        </section>

        <section class="detail-layout">
          <div class="detail-layout__main">
            <article class="panel host-card">
              <div class="host-card__main">
                <div class="host-card__avatar" aria-hidden="true"></div>
                <div class="host-card__copy">
                  <span class="detail-label">Hosted by</span>
                  <strong class="host-card__name"><c:out value="${eventPage.hostLabel}" /></strong>
                  <span class="host-card__meta">Pro padel coach · 4.9 (124 reviews)</span>
                </div>
              </div>
              <a class="host-card__action" href="#">Message Host</a>
            </article>

            <section class="detail-section">
              <h2 class="detail-section__title">About this tournament</h2>
              <div class="detail-stack">
                <c:forEach var="paragraph" items="${eventPage.aboutParagraphs}">
                  <p class="body-copy"><c:out value="${paragraph}" /></p>
                </c:forEach>
              </div>
            </section>

            <section class="detail-feature-grid">
              <c:forEach var="highlight" items="${eventPage.highlights}">
                <article class="panel feature-card feature-card--${highlight.tone}">
                  <div class="feature-card__icon" aria-hidden="true"></div>
                  <div class="feature-card__content">
                    <strong class="feature-card__title"><c:out value="${highlight.title}" /></strong>
                    <p class="muted-copy"><c:out value="${highlight.description}" /></p>
                  </div>
                </article>
              </c:forEach>
            </section>

            <section class="detail-section">
              <div class="detail-section__head">
                <h2 class="detail-section__title">Location</h2>
                <a class="detail-link" href="#">Open in Maps</a>
              </div>
              <div class="map-placeholder map-placeholder--detail">
                <span class="map-placeholder__pin">9</span>
              </div>
              <div class="location-copy">
                <strong><c:out value="${eventPage.locationTitle}" /></strong>
                <span><c:out value="${eventPage.locationHint}" /></span>
              </div>
            </section>
          </div>

          <aside class="detail-layout__sidebar">
            <article class="panel booking-panel">
              <div class="booking-panel__header">
                <div>
                  <span class="detail-label">Price per person</span>
                  <div class="booking-panel__price"><c:out value="${eventPage.bookingPrice}" /></div>
                </div>
                <span class="booking-panel__pill">Best Value</span>
              </div>

              <div class="booking-panel__field">
                <span class="detail-label">Select date</span>
                <strong>Sat, Oct 14, 2023</strong>
                <span class="booking-panel__chevron" aria-hidden="true"></span>
              </div>

              <div class="booking-panel__field">
                <span class="detail-label">Time slot</span>
                <strong>08:00 AM - 10:30 AM</strong>
                <span class="booking-panel__status-dot" aria-hidden="true"></span>
              </div>

              <div class="booking-panel__rows">
                <div class="booking-panel__row">
                  <span>Registration Fee</span>
                  <strong>$12.50</strong>
                </div>
                <div class="booking-panel__row">
                  <span>Equipment Hire</span>
                  <strong>$2.50</strong>
                </div>
                <div class="booking-panel__row booking-panel__row--total">
                  <span>Total Price</span>
                  <strong><c:out value="${eventPage.bookingPrice}" /></strong>
                </div>
              </div>

              <ui:button label="Reserve a spot" fullWidth="${true}" disabled="${true}" />

              <div class="booking-panel__availability">
                <span>Spots available</span>
                <strong>4 of 16 left</strong>
              </div>
              <div class="booking-panel__progress" aria-hidden="true">
                <span></span>
              </div>
              <p class="booking-panel__note">Hurry! Only 4 spots remaining for this Saturday.</p>
            </article>

            <article class="booking-confirmation">
              <span class="booking-confirmation__icon" aria-hidden="true"></span>
              <p>Instant confirmation. Your spot is guaranteed immediately after payment.</p>
            </article>
          </aside>
        </section>

        <section class="detail-recommendations">
          <div class="section-head">
            <div>
              <h2 class="section-head__title section-head__title--detail">More sports near you</h2>
              <p class="section-head__meta">Discover popular events in your area.</p>
            </div>
            <a class="section-link" href="#">View all events</a>
          </div>

          <div class="event-grid event-grid--detail">
            <c:forEach var="event" items="${eventPage.nearbyEvents}">
              <ui:card
                href="${pageContext.request.contextPath}${event.href}"
                className="event-card"
                ariaLabel="${event.title}">
                <div class="event-card__media ${event.mediaClass}">
                  <span class="event-card__badge"><c:out value="${event.sport}" /></span>
                </div>

                <div class="event-card__body">
                  <h3 class="event-card__title"><c:out value="${event.title}" /></h3>
                  <div class="event-card__meta">
                    <span><c:out value="${event.schedule}" /></span>
                    <span><c:out value="${event.venue}" /></span>
                  </div>

                  <div class="event-card__footer">
                    <strong class="event-card__price"><c:out value="${event.priceLabel}" /> / person</strong>
                    <span class="event-card__spots"><c:out value="${event.badge}" /></span>
                  </div>
                </div>
              </ui:card>
            </c:forEach>
          </div>
        </section>
      </main>
    </div>
  </body>
</html>
