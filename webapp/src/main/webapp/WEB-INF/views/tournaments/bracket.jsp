<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<spring:message var="pageTitle" code="page.title.tournamentBracket" arguments="${bracketTournament.title}" />
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
						<span class="tournament-status tournament-status--${fn:replace(bracketTournament.status.dbValue, '_', '-')}">
							<spring:message code="tournament.status.${bracketTournament.status.dbValue}" />
						</span>
						<h1 class="page-heading__title"><c:out value="${bracketTournament.title}" /></h1>
						<p class="page-heading__description">
							<spring:message code="tournament.bracket.public.description" />
						</p>
					</div>
					<c:if test="${not empty matchDatesSetupPath}">
						<c:url var="matchDatesSetupHref" value="${matchDatesSetupPath}" />
						<spring:message var="defineMatchDatesLabel" code="tournament.bracket.defineMatchDates" />
						<ui:button label="${defineMatchDatesLabel}" href="${matchDatesSetupHref}" variant="secondary" />
					</c:if>
				</header>

				<spring:message var="rosterTitle" code="tournament.bracket.roster.title" />
				<spring:message var="rosterTeamLabel" code="tournament.bracket.roster.team" />
				<spring:message var="rosterMembersLabel" code="tournament.bracket.roster.members" />
				<spring:message var="rosterEmptyLabel" code="tournament.bracket.roster.emptyTeam" />
				<section class="panel tournament-roster-panel" aria-labelledby="tournament-roster-title">
					<h2 id="tournament-roster-title" class="form-card__title"><c:out value="${rosterTitle}" /></h2>
					<table class="tournament-roster-table">
						<thead>
							<tr>
								<th scope="col"><c:out value="${rosterTeamLabel}" /></th>
								<th scope="col"><c:out value="${rosterMembersLabel}" /></th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="team" items="${bracketView.teams}">
								<tr>
									<td>
										<c:choose>
											<c:when test="${not empty team.name}">
												<c:out value="${team.name}" />
											</c:when>
											<c:otherwise>
												<spring:message code="tournament.team.solo.name" arguments="${bracketTeamDisplayNumbers[team.id]}" />
											</c:otherwise>
										</c:choose>
									</td>
									<td>
										<c:choose>
											<c:when test="${not empty bracketMembersByTeamId[team.id]}">
												<c:forEach var="memberUsername" items="${bracketMembersByTeamId[team.id]}" varStatus="memberStatus">
													<c:if test="${not memberStatus.first}">, </c:if><c:out value="${memberUsername}" />
												</c:forEach>
											</c:when>
											<c:otherwise><c:out value="${rosterEmptyLabel}" /></c:otherwise>
										</c:choose>
									</td>
								</tr>
							</c:forEach>
						</tbody>
					</table>
				</section>

				<c:if test="${not empty tournamentNoticeCode}">
					<p class="booking-panel__notice booking-panel__notice--success">
						<spring:message code="${tournamentNoticeCode}" />
					</p>
				</c:if>
				<c:if test="${not empty tournamentErrorCode}">
					<p class="booking-panel__notice booking-panel__notice--error">
						<spring:message code="${tournamentErrorCode}" />
					</p>
				</c:if>

				<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
				<section class="tournament-bracket-grid" aria-label="${bracketGridLabel}">
					<c:forEach var="round" items="${bracketMatchesByRound}">
						<section class="tournament-bracket-round">
							<h2 class="tournament-bracket-round__title">
								<c:choose>
									<c:when test="${round.key == bracketRoundCount}">
										<spring:message code="tournament.bracket.round.final" />
									</c:when>
									<c:otherwise>
										<spring:message code="tournament.bracket.round.number" arguments="${round.key}" />
									</c:otherwise>
								</c:choose>
							</h2>
							<div class="tournament-bracket-round__matches">
								<c:forEach var="match" items="${round.value}">
									<article class="tournament-bracket-match">
										<p class="tournament-bracket-match__label">
											<spring:message code="tournament.bracket.match.label" arguments="${match.matchIndex + 1}" />
										</p>
										<div class="tournament-bracket-match__teams">
											<%-- Team A row --%>
											<span class="tournament-bracket-match__team
												${match.teamA.id == bracketViewerTeamId ? ' tournament-bracket-match__team--viewer' : ''}
												${match.teamA.id == match.winnerTeam.id ? ' tournament-bracket-match__team--winner' : ''}
												${(match.teamA.id != match.winnerTeam.id and match.teamB.id == match.winnerTeam.id) ? ' tournament-bracket-match__team--loser' : ''}">
												<c:if test="${match.teamA.id == match.winnerTeam.id}">
													<c:choose>
														<c:when test="${round.key == bracketRoundCount}">
															<icon:trophy cssClass="bracket-team-icon bracket-team-icon--trophy" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
														</c:when>
														<c:otherwise>
															<icon:checkmark cssClass="bracket-team-icon bracket-team-icon--check" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
														</c:otherwise>
													</c:choose>
												</c:if>
												<c:choose>
													<c:when test="${empty match.teamA}">
														<spring:message code="tournament.bracket.team.tbd" />
													</c:when>
													<c:when test="${not empty match.teamA.name}">
														<c:out value="${match.teamA.name}" />
													</c:when>
													<c:otherwise>
														<spring:message code="tournament.team.solo.name" arguments="${bracketTeamDisplayNumbers[match.teamA.id]}" />
													</c:otherwise>
												</c:choose>
											</span>
											<%-- Team B row --%>
											<span class="tournament-bracket-match__team
												${match.teamB.id == bracketViewerTeamId ? ' tournament-bracket-match__team--viewer' : ''}
												${match.teamB.id == match.winnerTeam.id ? ' tournament-bracket-match__team--winner' : ''}
												${(match.teamB.id != match.winnerTeam.id and match.teamA.id == match.winnerTeam.id) ? ' tournament-bracket-match__team--loser' : ''}">
												<c:if test="${match.teamB.id == match.winnerTeam.id}">
													<c:choose>
														<c:when test="${round.key == bracketRoundCount}">
															<icon:trophy cssClass="bracket-team-icon bracket-team-icon--trophy" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
														</c:when>
														<c:otherwise>
															<icon:checkmark cssClass="bracket-team-icon bracket-team-icon--check" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
														</c:otherwise>
													</c:choose>
												</c:if>
												<c:choose>
													<c:when test="${empty match.teamB}">
														<spring:message code="tournament.bracket.team.tbd" />
													</c:when>
													<c:when test="${not empty match.teamB.name}">
														<c:out value="${match.teamB.name}" />
													</c:when>
													<c:otherwise>
														<spring:message code="tournament.team.solo.name" arguments="${bracketTeamDisplayNumbers[match.teamB.id]}" />
													</c:otherwise>
												</c:choose>
											</span>
										</div>

										<%-- Compact schedule + venue meta row --%>
										<div class="tournament-bracket-match__meta">
											<span class="tournament-bracket-match__meta-schedule">
												<c:choose>
													<c:when test="${empty match.scheduledStartsAt}">
														<spring:message code="tournament.bracket.schedule.tbd" />
													</c:when>
													<c:when test="${empty match.scheduledEndsAt}">
														<c:out value="${tf:dateTime(match.scheduledStartsAtDateTime)}" />
													</c:when>
													<c:otherwise>
														<spring:message code="tournament.bracket.schedule.range">
															<spring:argument value="${tf:dateTime(match.scheduledStartsAtDateTime)}" />
															<spring:argument value="${tf:dateTime(match.scheduledEndsAtDateTime)}" />
														</spring:message>
													</c:otherwise>
												</c:choose>
											</span>
											<c:if test="${not empty match.address}">
												<span class="tournament-bracket-match__meta-address"><c:out value="${match.address}" /></span>
											</c:if>
										</div>

										<%-- Status badge --%>
										<p class="tournament-bracket-match__status tournament-bracket-match__status--${fn:replace(match.status.dbValue, '_', '-')}">
											<spring:message code="tournament.match.status.${match.status.dbValue}" />
										</p>

										<c:if test="${bracketCanManageResults && match.recordable}">
											<div class="tournament-result-actions">
												<c:url var="winnerAction" value="/host/tournaments/${bracketTournament.id}/matches/${match.id}/winner" />
												<form method="post" action="${winnerAction}" data-submit-guard="true">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<input type="hidden" name="winnerTeamId" value="${match.teamA.id}" />
													<spring:message var="declareTeamALabel" code="tournament.bracket.result.declareWinner" arguments="${empty match.teamA.name ? bracketTeamDisplayNumbers[match.teamA.id] : match.teamA.name}" />
													<ui:button label="${declareTeamALabel}" type="submit" size="sm" fullWidth="${true}" variant="secondary" />
												</form>
												<form method="post" action="${winnerAction}" data-submit-guard="true">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<input type="hidden" name="winnerTeamId" value="${match.teamB.id}" />
													<spring:message var="declareTeamBLabel" code="tournament.bracket.result.declareWinner" arguments="${empty match.teamB.name ? bracketTeamDisplayNumbers[match.teamB.id] : match.teamB.name}" />
													<ui:button label="${declareTeamBLabel}" type="submit" size="sm" fullWidth="${true}" variant="secondary" />
												</form>
											</div>
										</c:if>
									</article>
								</c:forEach>
							</div>
						</section>
					</c:forEach>
				</section>
			</main>
		</div>
	</body>
</html>
