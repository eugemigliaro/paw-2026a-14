<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
			<c:url var="generateAction" value="${generateBracketPath}" />
			<c:url var="updateStrategyAction" value="${updateBracketStrategyPath}" />
			<c:url var="publishAction" value="${publishBracketPath}" />
			<c:url var="saveManualPairingsAction" value="${saveManualPairingsPath}" />

			<main class="page-shell tournament-bracket-page">
				<ui:returnButton href="${detailHref}" />
				<header class="page-heading tournament-bracket-page__heading">
					<div>
						<h1 class="page-heading__title"><c:out value="${bracketTournament.title}" /></h1>
						<span class="tournament-status tournament-status--${fn:replace(bracketTournament.status.dbValue, '_', '-')}">
							<spring:message code="tournament.status.${bracketTournament.status.dbValue}" />
						</span>
						<p class="page-heading__description">
							<c:choose>
								<c:when test="${bracketGenerated}">
									<spring:message code="tournament.bracket.setup.description.generated" />
								</c:when>
								<c:otherwise>
									<spring:message code="tournament.bracket.setup.description" />
								</c:otherwise>
							</c:choose>
						</p>
					</div>
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
							<c:forEach var="team" items="${bracketTeams}">
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

				<c:if test="${not bracketGenerated}">
					<spring:message var="generateLabel" code="tournament.bracket.generate" />
					<spring:message var="generatingLabel" code="tournament.bracket.generating" />
					<article class="panel form-card tournament-setup-card">
						<span class="detail-label"><spring:message code="tournament.host.bracket.panel.label" /></span>
						<h2 class="form-card__title"><spring:message code="tournament.bracket.setup.card.title" /></h2>
						<p class="body-copy"><spring:message code="tournament.bracket.setup.card.description" /></p>
						<div class="tournament-bracket-actions">
							<form method="post" action="${updateStrategyAction}" class="tournament-schedule-form" data-submit-guard="true">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<label class="field" for="pairing-strategy">
									<span class="field__label"><spring:message code="tournament.host.pairingStrategy.label" /></span>
									<select id="pairing-strategy" name="pairingStrategy" class="field__control field__control--select" required="required" data-initial-value="${selectedPairingStrategy}">
										<option value="manual" ${selectedPairingStrategy == 'manual' ? 'selected' : ''}><spring:message code="tournament.host.pairingStrategy.manual" /></option>
										<option value="random" ${selectedPairingStrategy == 'random' ? 'selected' : ''}><spring:message code="tournament.host.pairingStrategy.random" /></option>
										<option value="elo" ${selectedPairingStrategy == 'elo' ? 'selected' : ''}><spring:message code="tournament.host.pairingStrategy.elo" /></option>
									</select>
								</label>
								<spring:message var="updateStrategyLabel" code="tournament.bracket.strategy.update" />
								<ui:button
									id="update-strategy-button"
									label="${updateStrategyLabel}"
									type="submit"
									variant="secondary"
									className="tournament-bracket-actions__update-button tournament-bracket-actions__update-button--hidden" />
							</form>
							<form method="post" action="${generateAction}" data-submit-guard="true" data-submit-loading-label="${generatingLabel}">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<ui:button id="generate-bracket-button" label="${generateLabel}" type="submit" />
							</form>
						</div>
					</article>
				</c:if>
				<c:if test="${not empty tournamentNoticeCode}">
					<p class="booking-panel__notice booking-panel__notice--success tournament-notice">
						<spring:message code="${tournamentNoticeCode}" />
					</p>
				</c:if>
				<c:if test="${not empty tournamentErrorCode}">
					<p class="booking-panel__notice booking-panel__notice--error tournament-notice">
						<spring:message code="${tournamentErrorCode}" />
					</p>
				</c:if>

				<c:choose>
					<c:when test="${bracketGenerated}">
						<c:if test="${bracketPublishable}">
							<spring:message var="publishLabel" code="tournament.bracket.publish" />
							<spring:message var="publishingLabel" code="tournament.bracket.publishing" />
							<form method="post" action="${publishAction}" class="tournament-schedule-form" data-submit-guard="true" data-submit-loading-label="${publishingLabel}">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

								<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
								<section class="panel tournament-bracket-shell" aria-label="${bracketGridLabel}">
									<form:errors path="bracketPublishForm" element="div" cssClass="booking-panel__notice booking-panel__notice--error tournament-notice" />
									<div class="tournament-bracket-layout">
										<c:forEach var="schedule" items="${bracketPublishForm.schedules}" varStatus="scheduleStatus">
											<article class="tournament-schedule-match">
												<div class="tournament-bracket-match__header">
													<p class="tournament-bracket-match__label"><c:out value="${schedule.matchLabel}" /></p>
													<span class="tournament-bracket-match__round"><c:out value="${schedule.roundLabel}" /></span>
												</div>
												<div class="tournament-schedule-match__grid tournament-schedule-match__date-time-grid">
													<label class="field" for="match-start-date-${scheduleStatus.index}">
														<span class="field__label"><spring:message code="tournament.bracket.schedule.startDate" /></span>
														<input class="field__control" id="match-start-date-${scheduleStatus.index}" name="schedules[${scheduleStatus.index}].startDate" type="date" value="${schedule.startDate}" required="required" />
													</label>
													<label class="field" for="match-start-time-${scheduleStatus.index}">
														<span class="field__label"><spring:message code="tournament.bracket.schedule.startTime" /></span>
														<input class="field__control" id="match-start-time-${scheduleStatus.index}" name="schedules[${scheduleStatus.index}].startTime" type="time" value="${schedule.startTime}" required="required" />
													</label>
													<label class="field" for="match-end-date-${scheduleStatus.index}">
														<span class="field__label"><spring:message code="tournament.bracket.schedule.endDate" /></span>
														<input class="field__control" id="match-end-date-${scheduleStatus.index}" name="schedules[${scheduleStatus.index}].endDate" type="date" value="${schedule.endDate}" required="required" />
													</label>
													<label class="field" for="match-end-time-${scheduleStatus.index}">
														<span class="field__label"><spring:message code="tournament.bracket.schedule.endTime" /></span>
														<input class="field__control" id="match-end-time-${scheduleStatus.index}" name="schedules[${scheduleStatus.index}].endTime" type="time" value="${schedule.endTime}" required="required" />
													</label>
												</div>
												<input type="hidden" name="schedules[${scheduleStatus.index}].matchId" value="${schedule.matchId}" />
												<input type="hidden" name="schedules[${scheduleStatus.index}].roundNumber" value="${schedule.roundNumber}" />
												<input type="hidden" name="schedules[${scheduleStatus.index}].roundLabel" value="${schedule.roundLabel}" />
												<input type="hidden" name="schedules[${scheduleStatus.index}].matchLabel" value="${schedule.matchLabel}" />
												<input type="hidden" name="schedules[${scheduleStatus.index}].address" value="${schedule.address}" />
												<input type="hidden" name="schedules[${scheduleStatus.index}].latitude" value="${schedule.latitude}" />
												<input type="hidden" name="schedules[${scheduleStatus.index}].longitude" value="${schedule.longitude}" />
											</article>
										</c:forEach>
									</div>
								</section>

								<ui:button label="${publishLabel}" type="submit" fullWidth="${true}" />
							</form>
						</c:if>

						<c:if test="${not bracketPublishable}">
							<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
							<section class="panel tournament-bracket-shell" aria-label="${bracketGridLabel}">
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
														<div class="tournament-bracket-match__header">
															<p class="tournament-bracket-match__label">
																<spring:message code="tournament.bracket.match.label" arguments="${match.matchIndex + 1}" />
															</p>
															<span class="tournament-bracket-match__round">
																<c:choose>
																	<c:when test="${round.key == bracketRoundCount}">
																		<spring:message code="tournament.bracket.round.final" />
																	</c:when>
																	<c:otherwise>
																		<spring:message code="tournament.bracket.round.number" arguments="${round.key}" />
																	</c:otherwise>
																</c:choose>
															</span>
														</div>
														<div class="tournament-bracket-match__teams">
															<span>
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
															<span>
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
														<span class="tournament-bracket-match__status tournament-bracket-match__status--${fn:replace(match.status.dbValue, '_', '-')}">
															<spring:message code="tournament.match.status.${match.status.dbValue}" />
														</span>
													</article>
												</c:forEach>
											</div>
										</section>
									</c:forEach>
								</section>
							</section>
						</c:if>
					</c:when>
					<c:otherwise>
						<c:if test="${manualPairingEnabled}">
							<section class="panel tournament-bracket-empty" aria-labelledby="manual-pairings-title">
								<h2 id="manual-pairings-title" class="form-card__title">
									<spring:message code="tournament.bracket.manualPairings.title" />
								</h2>
								<p class="body-copy">
									<spring:message code="tournament.bracket.manualPairings.description" />
									<spring:message code="tournament.bracket.manualPairings.hint" />
								</p>
								<form method="post" action="${saveManualPairingsAction}" class="tournament-schedule-form" data-submit-guard="true">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<form:errors path="manualPairingsForm" element="div" cssClass="booking-panel__notice booking-panel__notice--error tournament-notice" />
									<input type="hidden" name="expectedTeamCount" value="${manualPairingsForm.expectedTeamCount}" />
									<c:forEach var="slotIndex" begin="1" end="${fn:length(manualPairingTeams)}">
										<label class="field" for="manual-team-slot-${slotIndex}">
											<span class="field__label"><spring:message code="tournament.bracket.manualPairings.slot" arguments="${slotIndex}" /></span>
											<select class="field__control field__control--select" id="manual-team-slot-${slotIndex}" name="teamIds[${slotIndex - 1}]" required="required">
												<c:forEach var="team" items="${manualPairingTeams}" varStatus="teamStatus">
													<option value="${team.id}" ${manualPairingsForm.teamIds[slotIndex - 1] == team.id ? 'selected="selected"' : ''}>
														<c:choose>
															<c:when test="${not empty team.name and not empty fn:trim(team.name) and team.origin.dbValue ne 'solo_pool'}">
																<c:out value="${team.name}" />
															</c:when>
															<c:otherwise>
																<spring:message code="tournament.team.solo.name" arguments="${teamStatus.index + 1}" />
															</c:otherwise>
														</c:choose>
													</option>
												</c:forEach>
											</select>
										</label>
									</c:forEach>
									<spring:message var="saveManualPairingsLabel" code="tournament.bracket.manualPairings.save" />
									<ui:button label="${saveManualPairingsLabel}" type="submit" fullWidth="${true}" />
								</form>
							</section>
						</c:if>
						<section class="panel tournament-bracket-empty" aria-labelledby="bracket-empty-title">
							<h2 id="bracket-empty-title" class="form-card__title">
								<spring:message code="tournament.bracket.empty.title" />
							</h2>
							<p class="body-copy">
								<spring:message code="tournament.bracket.empty.description" />
							</p>
						</section>
					</c:otherwise>
				</c:choose>
			</main>
		</div>
	</body>
</html>
