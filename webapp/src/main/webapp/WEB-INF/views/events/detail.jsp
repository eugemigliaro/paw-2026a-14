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

      <main class="page-shell">
        <section class="event-hero ${eventPage.event.mediaClass}">
          <div class="event-hero__content">
            <p class="eyebrow"><c:out value="${eventPage.heroSubtitle}" /></p>
            <h1 class="event-hero__title"><c:out value="${eventPage.event.title}" /></h1>
            <div class="event-hero__meta">
              <span><c:out value="${eventPage.event.sport}" /></span>
              <span>&middot;</span>
              <span><c:out value="${eventPage.heroMeta}" /></span>
              <span>&middot;</span>
              <span><c:out value="${eventPage.hostLabel}" /></span>
            </div>
          </div>
        </section>

        <section class="page-shell--split">
          <div class="detail-layout__main">
            <section class="detail-grid">
              <c:forEach var="highlight" items="${eventPage.highlights}">
                <article class="panel metrics-card metrics-card--${highlight.tone}">
                  <span class="metrics-card__eyebrow"><c:out value="${highlight.eyebrow}" /></span>
                  <strong class="metrics-card__value"><c:out value="${highlight.title}" /></strong>
                  <p class="muted-copy"><c:out value="${highlight.description}" /></p>
                </article>
              </c:forEach>
            </section>

            <article class="panel detail-card">
              <span class="detail-label">About this session</span>
              <h2 class="detail-card__title">Built for premium community play</h2>
              <div class="detail-stack">
                <c:forEach var="paragraph" items="${eventPage.aboutParagraphs}">
                  <p class="body-copy"><c:out value="${paragraph}" /></p>
                </c:forEach>
              </div>
            </article>

            <article class="panel detail-card">
              <span class="detail-label">Location</span>
              <h2 class="detail-card__title"><c:out value="${eventPage.locationTitle}" /></h2>
              <p class="body-copy"><c:out value="${eventPage.locationHint}" /></p>
              <div class="map-placeholder">
                <span class="map-placeholder__pin">9</span>
              </div>
            </article>

          </div>

          <aside>
            <article class="panel booking-card">
              <span class="detail-label">Reserve your spot</span>
              <div class="booking-card__price">
                <strong><c:out value="${eventPage.bookingPrice}" /></strong>
                <span class="body-copy">per attendee</span>
              </div>

              <div class="booking-card__rows">
                <c:forEach var="row" items="${eventPage.bookingDetails}">
                  <div class="booking-card__row">
                    <span><c:out value="${row.label}" /></span>
                    <strong><c:out value="${row.value}" /></strong>
                  </div>
                </c:forEach>
              </div>

              <ui:button label="${eventPage.ctaLabel}" fullWidth="${true}" disabled="${true}" />
              <p class="muted-copy">This card is layout-only for now. The sign-up flow is intentionally disabled in the MVP.</p>
            </article>
          </aside>
        </section>
      </main>
    </div>
  </body>
</html>
