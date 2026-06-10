<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<%@ attribute name="match" required="true" rtexprvalue="true" type="ar.edu.itba.paw.models.TournamentMatch" %>
<%@ attribute name="badgeCode" required="false" rtexprvalue="true" %>
<%@ attribute name="relationshipBadgeCodes" required="false" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="teamDisplayNumbers" required="false" rtexprvalue="true" type="java.util.Map" %>
<%@ attribute name="headingLevel" required="false" rtexprvalue="true" %>

<c:set var="resolvedHeadingLevel" value="${empty headingLevel ? 'h3' : headingLevel}" />
<c:set var="tournamentHref" value="/tournaments/${match.tournament.id}" />
<c:url var="resolvedTournamentHref" value="${tournamentHref}" />

<c:set var="mediaClass" value="${tf:mediaClass(match.tournament.sport)}" />

<ui:card href="${resolvedTournamentHref}" className="event-card" ariaLabel="${match.tournament.title}">
	<div class="event-card__media ${mediaClass}">
		<div class="event-card__media-badges">
			<c:if test="${not empty badgeCode}">
				<span class="event-card__badge">
					<spring:message code="${badgeCode}" />
				</span>
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
			<span class="event-card__sport"><spring:message code="sport.${match.tournament.sport.dbValue}" /></span>
		</div>

		<c:choose>
			<c:when test="${resolvedHeadingLevel == 'h2'}">
				<h2 class="event-card__title"><c:out value="${match.tournament.title}" /></h2>
			</c:when>
			<c:otherwise>
				<h3 class="event-card__title"><c:out value="${match.tournament.title}" /></h3>
			</c:otherwise>
		</c:choose>

		<div class="event-card__meta">
			<span class="event-card__meta-item">
				<spring:message code="tournament.bracket.round.number" arguments="${match.roundNumber}" />
			</span>
			<span class="event-card__meta-item">
				<c:choose>
					<c:when test="${not empty match.teamA and not empty match.teamB}">
						<c:choose>
							<c:when test="${not empty match.teamA.name}">
								<c:out value="${match.teamA.name}" />
							</c:when>
							<c:otherwise>
								<spring:message code="tournament.team.solo.name" arguments="${teamDisplayNumbers[match.teamA.id]}" />
							</c:otherwise>
						</c:choose> <spring:message code="tournament.match.versus"/> <c:choose>
							<c:when test="${not empty match.teamB.name}">
								<c:out value="${match.teamB.name}" />
							</c:when>
							<c:otherwise>
								<spring:message code="tournament.team.solo.name" arguments="${teamDisplayNumbers[match.teamB.id]}" />
							</c:otherwise>
						</c:choose>
					</c:when>
					<c:otherwise>
						<c:choose>
							<c:when test="${not empty match.teamA}">
								<c:choose>
									<c:when test="${not empty match.teamA.name}">
										<c:out value="${match.teamA.name}" />
									</c:when>
									<c:otherwise>
										<spring:message code="tournament.team.solo.name" arguments="${teamDisplayNumbers[match.teamA.id]}" />
									</c:otherwise>
								</c:choose>
							</c:when>
							<c:otherwise>
								<c:choose>
									<c:when test="${not empty match.teamB.name}">
										<c:out value="${match.teamB.name}" />
									</c:when>
									<c:otherwise>
										<spring:message code="tournament.team.solo.name" arguments="${teamDisplayNumbers[match.teamB.id]}" />
									</c:otherwise>
								</c:choose>
							</c:otherwise>
						</c:choose>
					</c:otherwise>
				</c:choose>
			</span>
		</div>

		<div class="event-card__meta">
			<c:choose>
				<c:when test="${not empty match.scheduledStartsAtDateTime}">
					<span class="event-card__meta-item">
						<span class="event-card__meta-icon" aria-hidden="true">
							<icon:calendar fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
						</span>
						<c:out value="${tf:cardDate(match.scheduledStartsAtDateTime)}" />
					</span>
					<span class="event-card__meta-item">
						<span class="event-card__meta-icon" aria-hidden="true">
							<icon:clock fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
						</span>
						<c:out value="${tf:time(match.scheduledStartsAtDateTime)}" />
					</span>
				</c:when>
				<c:otherwise>
					<span class="event-card__meta-item">
						<spring:message code="tournament.bracket.schedule.tbd" />
					</span>
				</c:otherwise>
			</c:choose>
		</div>

		<c:if test="${not empty match.address}">
			<div class="event-card__meta">
				<span class="event-card__meta-item">
					<span class="event-card__meta-icon" aria-hidden="true">
						<icon:locationPin fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
					</span>
					<span class="event-card__meta-text"><c:out value="${match.address}" /></span>
				</span>
			</div>
		</c:if>

		<div class="event-card__footer">
		</div>
	</div>
</ui:card>
