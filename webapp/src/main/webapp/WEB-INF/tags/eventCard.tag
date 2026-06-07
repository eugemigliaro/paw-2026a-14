<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<%@ attribute name="match" required="false" rtexprvalue="true" type="ar.edu.itba.paw.models.Match" %>
<%@ attribute name="tournament" required="false" rtexprvalue="true" type="ar.edu.itba.paw.models.Tournament" %>
<%@ attribute name="badgeLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="distanceLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="relationshipBadgeCodes" required="false" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="headingLevel" required="false" rtexprvalue="true" %>

<c:set var="event" value="${not empty match ? match : tournament}" />
<c:set var="isTournament" value="${empty match and not empty tournament}" />
<c:set var="eventHref" value="${isTournament ? '/tournaments/' : '/matches/'}${event.id}" />
<c:url var="resolvedEventHref" value="${eventHref}" />
<c:set var="resolvedHeadingLevel" value="${empty headingLevel ? 'h3' : headingLevel}" />
<c:set var="mediaClass" value="${tf:mediaClass(event.sport)}" />

<ui:card href="${resolvedEventHref}" className="event-card" ariaLabel="${event.title}">
	<div class="event-card__media ${mediaClass}">
		<c:if test="${event.hasBannerImage()}">
			<c:url var="eventBannerSrc" value="/images/${event.bannerImageMetadata.id}" />
			<img class="event-card__image" src="${eventBannerSrc}" alt="" loading="lazy" decoding="async" />
		</c:if>
		<div class="event-card__media-badges">
			<c:if test="${not empty badgeLabel}">
				<span class="event-card__badge"><c:out value="${badgeLabel}" /></span>
			</c:if>
			<c:forEach var="relationshipBadgeCode" items="${relationshipBadgeCodes}">
				<span class="event-badge event-badge--${relationshipBadgeCode}">
					<spring:message code="event.relationship.${relationshipBadgeCode}" />
				</span>
			</c:forEach>
		</div>
	</div>

	<div class="event-card__body">
		<div class="event-card__sport-row">
			<span class="event-card__sport"><spring:message code="sport.${event.sport.dbValue}" /></span>
			<c:if test="${not isTournament and event.recurringOccurrence}">
				<span class="event-card__recurring"><spring:message code="event.recurringBadge" /></span>
			</c:if>
		</div>

		<c:choose>
			<c:when test="${resolvedHeadingLevel == 'h2'}">
				<h2 class="event-card__title"><c:out value="${event.title}" /></h2>
			</c:when>
			<c:otherwise>
				<h3 class="event-card__title"><c:out value="${event.title}" /></h3>
			</c:otherwise>
		</c:choose>

		<div class="event-card__meta">
			<span class="event-card__meta-item">
				<span class="event-card__meta-icon" aria-hidden="true">
					<icon:locationPin fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
				</span>
				<span class="event-card__meta-text">
					<c:out value="${event.address}" />
				</span>
			</span>
			<c:if test="${not empty distanceLabel}">
				<span class="event-card__meta-item">
					<span class="event-card__meta-icon" aria-hidden="true">
						<icon:ruler fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
					</span>
					<span class="event-card__meta-text">
						<c:out value="${distanceLabel}" />
					</span>
				</span>
			</c:if>
			<span class="event-card__meta-item">
				<span class="event-card__meta-icon" aria-hidden="true">
					<icon:calendar fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
				</span>
				<c:choose>
					<c:when test="${empty event.startsAtDateTime}">
						<spring:message code="tournament.detail.schedule.tbd" />
					</c:when>
					<c:otherwise>
						<c:out value="${tf:cardDate(event.startsAtDateTime)}" />
					</c:otherwise>
				</c:choose>
			</span>
			<c:if test="${not empty event.startsAtDateTime}">
				<span class="event-card__meta-item">
					<span class="event-card__meta-icon" aria-hidden="true">
						<icon:clock fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
					</span>
					<c:out value="${tf:time(event.startsAtDateTime)}" />
				</span>
			</c:if>
			<c:if test="${not empty event.host and not empty event.host.username}">
				<span class="event-card__meta-item">
					<span class="event-card__meta-icon" aria-hidden="true">
						<icon:profile fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
					</span>
					<span class="event-card__meta-text">
						<spring:message code="event.card.hostedBy" />
						<c:out value="${event.host.username}" />
					</span>
				</span>
			</c:if>
		</div>

		<div class="event-card__footer">
			<div class="event-card__cta">
				<span>
					<c:choose>
						<c:when test="${empty event.pricePerPlayer}">
							<spring:message code="price.tbd" />
						</c:when>
						<c:when test="${event.pricePerPlayer.signum() == 0}">
							<spring:message code="price.free" />
						</c:when>
						<c:otherwise>
							<spring:message code="price.amount" arguments="${event.pricePerPlayer}" />
						</c:otherwise>
					</c:choose>
				</span>
			</div>
		</div>
	</div>
</ui:card>
