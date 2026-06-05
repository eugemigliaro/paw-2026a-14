<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
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
							<c:forEach var="teamRoster" items="${bracketPage.teamRosters}">
								<tr>
									<td><c:out value="${teamRoster.teamName}" /></td>
									<td>
										<c:choose>
											<c:when test="${teamRoster.hasMembers}"><c:out value="${teamRoster.membersLabel}" /></c:when>
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
					<c:forEach var="round" items="${bracketPage.rounds}">
						<section class="tournament-bracket-round">
							<h2 class="tournament-bracket-round__title"><c:out value="${round.label}" /></h2>
							<div class="tournament-bracket-round__matches">
								<c:forEach var="match" items="${round.matches}">
									<article class="tournament-bracket-match">
										<p class="tournament-bracket-match__label"><c:out value="${match.label}" /></p>
										<div class="tournament-bracket-match__teams">
											<%-- Team A row --%>
											<span class="tournament-bracket-match__team
												${match.teamAViewerTeam ? ' tournament-bracket-match__team--viewer' : ''}
												${match.teamAIsWinner ? ' tournament-bracket-match__team--winner' : ''}
												${(not match.teamAIsWinner and match.teamBIsWinner) ? ' tournament-bracket-match__team--loser' : ''}">
												<c:if test="${match.teamAIsWinner}">
													<c:choose>
														<c:when test="${match.finalRound}">
															<icon:trophy cssClass="bracket-team-icon bracket-team-icon--trophy" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
														</c:when>
														<c:otherwise>
															<icon:checkmark cssClass="bracket-team-icon bracket-team-icon--check" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
														</c:otherwise>
													</c:choose>
												</c:if>
												<c:out value="${match.teamA}" />
											</span>
											<%-- Team B row --%>
											<span class="tournament-bracket-match__team
												${match.teamBViewerTeam ? ' tournament-bracket-match__team--viewer' : ''}
												${match.teamBIsWinner ? ' tournament-bracket-match__team--winner' : ''}
												${(not match.teamBIsWinner and match.teamAIsWinner) ? ' tournament-bracket-match__team--loser' : ''}">
												<c:if test="${match.teamBIsWinner}">
													<c:choose>
														<c:when test="${match.finalRound}">
															<icon:trophy cssClass="bracket-team-icon bracket-team-icon--trophy" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
														</c:when>
														<c:otherwise>
															<icon:checkmark cssClass="bracket-team-icon bracket-team-icon--check" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
														</c:otherwise>
													</c:choose>
												</c:if>
												<c:out value="${match.teamB}" />
											</span>
										</div>

										<%-- Compact schedule + venue meta row --%>
										<div class="tournament-bracket-match__meta">
											<span class="tournament-bracket-match__meta-schedule"><c:out value="${match.scheduleLabel}" /></span>
											<c:if test="${not empty match.address}">
												<span class="tournament-bracket-match__meta-address"><c:out value="${match.address}" /></span>
											</c:if>
										</div>

										<%-- Status badge --%>
										<p class="tournament-bracket-match__status tournament-bracket-match__status--${match.statusTone}"><c:out value="${match.statusLabel}" /></p>

										<c:if test="${bracketPage.canManageResults && match.canRecordResult}">
											<div class="tournament-result-actions">
												<c:url var="winnerAction" value="/host/tournaments/${bracketPage.tournamentId}/matches/${match.id}/winner" />
												<form method="post" action="${winnerAction}" data-submit-guard="true">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<input type="hidden" name="winnerTeamId" value="${match.teamAId}" />
													<spring:message var="declareTeamALabel" code="tournament.bracket.result.declareWinner" arguments="${match.teamA}" />
													<ui:button label="${declareTeamALabel}" type="submit" size="sm" fullWidth="${true}" variant="secondary" />
												</form>
												<form method="post" action="${winnerAction}" data-submit-guard="true">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<input type="hidden" name="winnerTeamId" value="${match.teamBId}" />
													<spring:message var="declareTeamBLabel" code="tournament.bracket.result.declareWinner" arguments="${match.teamB}" />
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
