<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
			<spring:message var="summaryTitle" code="tournament.bracket.schedule.validationItem" />
			<spring:message var="validationTitle" code="tournament.bracket.schedule.validationTitle" />
			<spring:message var="validationEmpty" code="tournament.bracket.schedule.validationEmpty" />
			<spring:message var="invalidRangeError" code="tournament.bracket.schedule.validation.invalidRange" />
			<spring:message var="beforeNowError" code="tournament.bracket.schedule.validation.beforeNow" />
			<spring:message var="invalidRoundOrderError" code="tournament.bracket.schedule.validation.roundOrder" />

			<main class="page-shell tournament-bracket-page">
				<ui:returnButton href="${detailHref}" />
				<header class="page-heading tournament-bracket-page__heading">
					<div>
						<h1 class="page-heading__title"><c:out value="${bracketPage.title}" /></h1>
						<span class="tournament-status tournament-status--${bracketPage.statusTone}">
							<c:out value="${bracketPage.statusLabel}" />
						</span>
						<p class="page-heading__description">
							<c:choose>
								<c:when test="${bracketPage.generated}">
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

				<c:if test="${not bracketPage.generated}">
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
					<c:when test="${bracketPage.generated}">
						<c:if test="${bracketPage.publishable}">
							<spring:message var="publishLabel" code="tournament.bracket.publish" />
							<spring:message var="publishingLabel" code="tournament.bracket.publishing" />
							<form method="post" action="${publishAction}" class="tournament-schedule-form" data-submit-guard="true" data-submit-loading-label="${publishingLabel}">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<input type="hidden" name="tz" data-browser-timezone-field="true" />

								<section class="tournament-schedule-validation" aria-live="polite">
									<h3 class="tournament-schedule-validation__title"><c:out value="${validationTitle}" /></h3>
									<p class="tournament-schedule-validation__empty" data-validation-empty="true"><c:out value="${validationEmpty}" /></p>
									<ul class="tournament-schedule-validation__list" data-validation-summary="true" hidden="hidden"></ul>
								</section>

								<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
								<section class="panel tournament-bracket-shell" aria-label="${bracketGridLabel}" data-bracket-schedule-editor="true" data-summary-title="${summaryTitle}" data-error-before-now="${beforeNowError}" data-error-invalid-range="${invalidRangeError}" data-error-round-order="${invalidRoundOrderError}">
									<div class="tournament-bracket-layout">
										<c:forEach var="round" items="${bracketPage.rounds}">
											<section class="tournament-round-row" data-round-number="${round.roundNumber}">
												<header class="tournament-round-schedule__head">
													<h2 class="tournament-bracket-round__title"><c:out value="${round.label}" /></h2>
													<c:if test="${round.roundNumber > 1}">
														<p class="tournament-round-schedule__helper" data-round-helper="true">
															<spring:message code="tournament.bracket.schedule.roundHelper" arguments="${round.label}" />
														</p>
													</c:if>
												</header>
												<div class="tournament-round-row__matches">
													<c:forEach var="match" items="${round.matches}">
														<div class="tournament-match-row">

															<article class="tournament-bracket-match">
																<div class="tournament-bracket-match__header">
																	<p class="tournament-bracket-match__label"><c:out value="${match.label}" /></p>
																	<span class="tournament-bracket-match__round"><c:out value="${round.label}" /></span>
																</div>
																<div class="tournament-bracket-match__teams">
																	<span><c:out value="${match.teamA}" /></span>
																	<span><c:out value="${match.teamB}" /></span>
																</div>
															</article>

															<article class="tournament-schedule-match" data-match-schedule="true" data-round-number="${round.roundNumber}" data-round-label="${round.label}" data-match-label="${match.label}">
																<div class="tournament-schedule-match__grid tournament-schedule-match__date-time-grid">
																	<label class="field" for="match-start-date-${match.id}" data-start-field="true">
																		<span class="field__label"><spring:message code="tournament.bracket.schedule.startDate" /></span>
																		<input class="field__control" id="match-start-date-${match.id}" data-start-date="true" name="startDate_${match.id}" type="date" value="<c:out value='${match.startDate}' />" required="required" />
																		<span class="field__error" data-field-error="true"></span>
																	</label>
																	<label class="field" for="match-start-time-${match.id}">
																		<span class="field__label"><spring:message code="tournament.bracket.schedule.startTime" /></span>
																		<input class="field__control" id="match-start-time-${match.id}" data-start-time="true" name="startTime_${match.id}" type="time" value="<c:out value='${match.startTime}' />" required="required" />
																		<span class="field__error" data-field-error="true"></span>
																	</label>
																	<label class="field" for="match-end-date-${match.id}" data-end-field="true">
																		<span class="field__label"><spring:message code="tournament.bracket.schedule.endDate" /></span>
																		<input class="field__control" id="match-end-date-${match.id}" data-end-date="true" name="endDate_${match.id}" type="date" value="<c:out value='${match.endDate}' />" required="required" />
																		<span class="field__error" data-field-error="true"></span>
																	</label>
																	<label class="field" for="match-end-time-${match.id}">
																		<span class="field__label"><spring:message code="tournament.bracket.schedule.endTime" /></span>
																		<input class="field__control" id="match-end-time-${match.id}" data-end-time="true" name="endTime_${match.id}" type="time" value="<c:out value='${match.endTime}' />" required="required" />
																		<span class="field__error" data-field-error="true"></span>
																	</label>
																</div>
																<input type="hidden" name="address_${match.id}" value="<c:out value='${match.address}' />" />
																<input type="hidden" name="latitude_${match.id}" value="<c:out value='${match.latitude}' />" />
																<input type="hidden" name="longitude_${match.id}" value="<c:out value='${match.longitude}' />" />
															</article>

														</div>
													</c:forEach>
												</div>
											</section>
										</c:forEach>
									</div>
								</section>

								<ui:button label="${publishLabel}" type="submit" fullWidth="${true}" />
							</form>
						</c:if>

						<c:if test="${not bracketPage.publishable}">
							<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
							<section class="panel tournament-bracket-shell" aria-label="${bracketGridLabel}">
								<section class="tournament-bracket-grid" aria-label="${bracketGridLabel}">
									<c:forEach var="round" items="${bracketPage.rounds}">
										<section class="tournament-bracket-round">
											<h2 class="tournament-bracket-round__title"><c:out value="${round.label}" /></h2>
											<div class="tournament-bracket-round__matches">
												<c:forEach var="match" items="${round.matches}">
													<article class="tournament-bracket-match">
														<div class="tournament-bracket-match__header">
															<p class="tournament-bracket-match__label"><c:out value="${match.label}" /></p>
															<span class="tournament-bracket-match__round"><c:out value="${round.label}" /></span>
														</div>
														<div class="tournament-bracket-match__teams">
															<span><c:out value="${match.teamA}" /></span>
															<span><c:out value="${match.teamB}" /></span>
														</div>
														<span class="tournament-bracket-match__status tournament-bracket-match__status--${match.statusKey}">
															<c:out value="${match.statusLabel}" />
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
									<c:forEach var="slotIndex" begin="1" end="${fn:length(manualPairingTeams)}">
										<label class="field" for="manual-team-slot-${slotIndex}">
											<span class="field__label"><spring:message code="tournament.bracket.manualPairings.slot" arguments="${slotIndex}" /></span>
											<select class="field__control field__control--select" id="manual-team-slot-${slotIndex}" name="teamIds" required="required">
												<c:forEach var="team" items="${manualPairingTeams}" varStatus="teamStatus">
													<option value="${team.id}" ${teamStatus.index + 1 == slotIndex ? 'selected="selected"' : ''}>
														<c:out value="${team.name}" />
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
