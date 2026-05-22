<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>
			<c:url var="detailHref" value="${tournamentDetailPath}" />

			<main class="page-shell tournament-bracket-page tournament-bracket-page--public">
				<ui:returnButton href="${detailHref}" />
				<header class="page-heading tournament-bracket-page__heading">
					<div>
						<span class="tournament-status tournament-status--${bracketPage.statusTone}">
							<c:out value="${bracketPage.statusLabel}" />
						</span>
						<h1 class="page-heading__title"><c:out value="${bracketPage.title}" /></h1>
						<p class="page-heading__description">
							<spring:message code="tournament.bracket.public.description" />
						</p>
					</div>
				</header>

				<section class="tournament-bracket-layout">
					<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
					<section class="tournament-bracket-grid" aria-label="${bracketGridLabel}">
						<c:forEach var="round" items="${bracketPage.rounds}">
							<section class="tournament-bracket-round">
								<h2 class="tournament-bracket-round__title"><c:out value="${round.label}" /></h2>
								<div class="tournament-bracket-round__matches">
									<c:forEach var="match" items="${round.matches}">
										<article class="tournament-bracket-match ${match.focused ? 'tournament-bracket-match--focused' : ''}">
											<p class="tournament-bracket-match__label"><c:out value="${match.label}" /></p>
											<div class="tournament-bracket-match__teams">
												<span class="${match.teamAViewerTeam ? 'tournament-bracket-match__team--viewer' : ''}"><c:out value="${match.teamA}" /></span>
												<span class="${match.teamBViewerTeam ? 'tournament-bracket-match__team--viewer' : ''}"><c:out value="${match.teamB}" /></span>
											</div>
											<p class="tournament-bracket-match__status"><c:out value="${match.scheduleLabel}" /></p>
											<p class="tournament-bracket-match__status"><c:out value="${match.statusLabel}" /></p>
										</article>
									</c:forEach>
								</div>
							</section>
						</c:forEach>
					</section>

					<aside class="panel tournament-bracket-focus" aria-labelledby="focused-match-title">
						<span class="detail-label"><spring:message code="tournament.bracket.focus.label" /></span>
						<h2 id="focused-match-title" class="detail-section__title">
							<c:out value="${bracketPage.focusedMatchLabel}" />
						</h2>
						<p class="tournament-bracket-focus__teams"><c:out value="${bracketPage.focusedMatchTeamsLabel}" /></p>
						<dl class="event-info-panel__list">
							<div class="booking-panel__detail-row event-info-panel__row">
								<dt><spring:message code="tournament.bracket.focus.schedule" /></dt>
								<dd><c:out value="${bracketPage.focusedMatchScheduleLabel}" /></dd>
							</div>
							<c:if test="${not empty bracketPage.focusedMatchAddress}">
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.bracket.focus.venue" /></dt>
									<dd><c:out value="${bracketPage.focusedMatchAddress}" /></dd>
								</div>
							</c:if>
						</dl>
					</aside>
				</section>
			</main>
		</div>
	</body>
</html>
