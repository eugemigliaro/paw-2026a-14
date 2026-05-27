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
					<c:if test="${not empty matchDatesSetupPath}">
						<c:url var="matchDatesSetupHref" value="${matchDatesSetupPath}" />
						<spring:message var="defineMatchDatesLabel" code="tournament.bracket.defineMatchDates" />
						<ui:button label="${defineMatchDatesLabel}" href="${matchDatesSetupHref}" variant="secondary" />
					</c:if>
				</header>

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
															<%-- Trophy icon for the champion --%>
															<svg class="bracket-team-icon bracket-team-icon--trophy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" focusable="false" aria-hidden="true">
																<path d="M6 9H4a2 2 0 0 1-2-2V5h4"/>
																<path d="M18 9h2a2 2 0 0 0 2-2V5h-4"/>
																<path d="M6 9a6 6 0 0 0 12 0V3H6z"/>
																<path d="M12 15v4"/>
																<path d="M8 19h8"/>
															</svg>
														</c:when>
														<c:otherwise>
															<%-- Checkmark icon for regular round winner --%>
															<svg class="bracket-team-icon bracket-team-icon--check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" focusable="false" aria-hidden="true">
																<polyline points="20 6 9 17 4 12"/>
															</svg>
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
															<svg class="bracket-team-icon bracket-team-icon--trophy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" focusable="false" aria-hidden="true">
																<path d="M6 9H4a2 2 0 0 1-2-2V5h4"/>
																<path d="M18 9h2a2 2 0 0 0 2-2V5h-4"/>
																<path d="M6 9a6 6 0 0 0 12 0V3H6z"/>
																<path d="M12 15v4"/>
																<path d="M8 19h8"/>
															</svg>
														</c:when>
														<c:otherwise>
															<svg class="bracket-team-icon bracket-team-icon--check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" focusable="false" aria-hidden="true">
																<polyline points="20 6 9 17 4 12"/>
															</svg>
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
