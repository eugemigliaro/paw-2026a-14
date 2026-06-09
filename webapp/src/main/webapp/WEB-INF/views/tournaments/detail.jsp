<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.tournamentDetail" arguments="${tournament.title}" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--detail tournament-detail">
				<section class="detail-top">
					<div class="detail-top__main">
						<section class="event-hero tournament-hero ${not empty tournamentBannerImageUrl ? 'event-hero--with-image' : ''}">
							<c:if test="${not empty tournamentBannerImageUrl}">
								<c:url var="tournamentHeroBannerSrc" value="${tournamentBannerImageUrl}" />
								<img class="event-hero__image" src="${tournamentHeroBannerSrc}" alt="" loading="eager" decoding="async" />
							</c:if>
							<div class="event-heading">
								<div class="tournament-hero__badges">
									<span class="event-heading__badge"><spring:message code="sport.${tournament.sport.dbValue}" /></span>
									<span class="tournament-status tournament-status--${fn:replace(tournament.status.dbValue, '_', '-')}">
										<spring:message code="tournament.status.${tournament.status.dbValue}" />
									</span>
									<c:forEach var="relationshipBadgeCode" items="${tournamentRelationshipBadgeCodes}">
										<span class="event-badge event-badge--${relationshipBadgeCode}">
											<spring:message code="event.relationship.${relationshipBadgeCode}" />
										</span>
									</c:forEach>
								</div>
								<h1 class="event-heading__title"><c:out value="${tournament.title}" /></h1>
							</div>
						</section>

						<section class="detail-section detail-section--about" aria-labelledby="about-tournament-title">
							<div class="section-head section-head--detail-compact">
								<div>
									<span class="detail-label"><spring:message code="tournament.detail.overview" /></span>
									<h2 id="about-tournament-title" class="detail-section__title">
										<spring:message code="tournament.detail.about" />
									</h2>
								</div>
							</div>
							<div class="detail-stack">
								<c:choose>
									<c:when test="${empty tournamentAboutParagraphs}">
										<p class="body-copy detail-stack__paragraph">
											<spring:message code="tournament.detail.defaultDescription" />
										</p>
									</c:when>
									<c:otherwise>
										<c:forEach var="paragraph" items="${tournamentAboutParagraphs}">
											<p class="body-copy detail-stack__paragraph"><c:out value="${paragraph}" /></p>
										</c:forEach>
									</c:otherwise>
								</c:choose>
							</div>
						</section>

						<section class="panel detail-section tournament-facts" aria-labelledby="tournament-facts-title">
							<div class="section-head section-head--detail-compact">
								<div>
									<span class="detail-label"><spring:message code="tournament.detail.format" /></span>
									<h2 id="tournament-facts-title" class="detail-section__title">
										<spring:message code="tournament.detail.structure" />
									</h2>
								</div>
							</div>
							<dl class="tournament-facts__grid">
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.bracketSize" /></dt>
									<dd><spring:message code="tournament.detail.bracketSize.value" arguments="${tournament.bracketSize}" /></dd>
								</div>
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.teamSize" /></dt>
									<dd><spring:message code="tournament.detail.teamSize.value" arguments="${tournament.teamSize}" /></dd>
								</div>
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.format" /></dt>
									<dd><spring:message code="tournament.format.${tournament.format.dbValue}" /></dd>
								</div>
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.registrationMode" /></dt>
									<dd><spring:message code="${tournamentJoinModeCode}" /></dd>
								</div>
							</dl>
						</section>
					</div>

					<aside class="detail-top__sidebar">
						<c:if test="${tournamentCapabilities.canCloseRegistration or tournamentCapabilities.canEditTournament or tournamentCapabilities.canCancelTournament}">
							<article class="panel host-panel">
								<div class="host-panel__note">
									<p class="detail-label"><spring:message code="tournament.host.panel.label" /></p>
									<p><spring:message code="tournament.host.panel.detail" /></p>
								</div>
								<c:if test="${tournamentCapabilities.canCloseRegistration}">
									<c:url var="closeRegistrationAction" value="${closeRegistrationPath}" />
									<spring:message var="closeRegistrationLabel" code="tournament.host.closeRegistration" />
									<spring:message var="closingRegistrationLabel" code="tournament.host.closeRegistration.loading" />
									<form method="post" action="${closeRegistrationAction}" data-submit-guard="true" data-submit-loading-label="${closingRegistrationLabel}">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${closeRegistrationLabel}" type="submit" fullWidth="${true}" variant="primary" disabled="${tournamentCapabilities.closeRegistrationDisabled}" />
									</form>
									<c:if test="${not empty tournamentCloseRegistrationDisabledMessage}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<spring:message code="${tournamentCloseRegistrationDisabledMessage}" />
										</p>
									</c:if>
								</c:if>
								<c:if test="${tournamentCapabilities.canEditTournament}">
									<c:url var="editTournamentHref" value="${editTournamentPath}" />
									<spring:message var="editTournamentLabel" code="tournament.host.edit" />
									<ui:button label="${editTournamentLabel}" href="${editTournamentHref}" fullWidth="${true}" variant="secondary" />
								</c:if>
								<c:if test="${tournamentCapabilities.canCancelTournament}">
									<c:url var="cancelTournamentAction" value="${cancelTournamentPath}" />
									<spring:message var="cancelTournamentLabel" code="tournament.host.cancel" />
									<spring:message var="cancellingTournamentLabel" code="tournament.host.cancel.loading" />
									<form method="post" action="${cancelTournamentAction}" data-submit-guard="true" data-submit-loading-label="${cancellingTournamentLabel}">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${cancelTournamentLabel}" type="submit" fullWidth="${true}" variant="danger" />
									</form>
								</c:if>
							</article>
						</c:if>
						<c:if test="${tournamentCapabilities.canManageBracket}">
							<article class="panel host-panel">
								<div class="host-panel__note">
									<p class="detail-label"><spring:message code="tournament.host.bracket.panel.label" /></p>
									<p><spring:message code="tournament.host.bracket.panel.detail" /></p>
								</div>
								<c:url var="bracketSetupHref" value="/host/tournaments/${tournament.id}/bracket/setup" />
								<spring:message var="bracketSetupLabel" code="tournament.host.bracket.setup" />
								<ui:button label="${bracketSetupLabel}" href="${bracketSetupHref}" fullWidth="${true}" variant="secondary" />
							</article>
						</c:if>
						<c:if test="${tournamentCapabilities.canViewBracket}">
							<article class="panel host-panel">
								<div class="host-panel__note">
									<p class="detail-label"><spring:message code="tournament.bracket.panel.label" /></p>
									<p><spring:message code="tournament.bracket.panel.detail" /></p>
								</div>
								<c:url var="publicBracketHref" value="/tournaments/${tournament.id}/bracket" />
								<spring:message var="publicBracketLabel" code="tournament.bracket.view" />
								<ui:button label="${publicBracketLabel}" href="${publicBracketHref}" fullWidth="${true}" variant="secondary" />
							</article>
						</c:if>

						<article class="panel player-actions-panel">
							<p class="detail-label player-actions-panel__title">
								<spring:message code="tournament.registration.panel.title" />
							</p>
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
							<c:if test="${not empty tournamentParticipationCode}">
								<p class="booking-panel__notice booking-panel__notice--info">
									<c:choose>
										<c:when test="${empty tournamentParticipationTeam}">
											<spring:message code="${tournamentParticipationCode}" />
										</c:when>
										<c:otherwise>
											<c:choose>
												<c:when test="${not empty tournamentParticipationTeam.name}">
													<c:set var="tournamentParticipationTeamLabel" value="${tournamentParticipationTeam.name}" />
												</c:when>
												<c:otherwise>
													<spring:message var="tournamentParticipationTeamLabel" code="tournament.team.solo.name" arguments="${tournamentTeamDisplayNumbers[tournamentParticipationTeam.id]}" />
												</c:otherwise>
											</c:choose>
											<spring:message code="${tournamentParticipationCode}" arguments="${tournamentParticipationTeamLabel}" />
										</c:otherwise>
									</c:choose>
								</p>
							</c:if>
							<c:if test="${not empty tournamentNextStepCode}">
								<p class="booking-panel__notice booking-panel__notice--info">
									<spring:message code="${tournamentNextStepCode}" />
								</p>
							</c:if>

							<c:choose>
								<c:when test="${tournamentCapabilities.canJoinSolo}">
									<c:url var="soloJoinAction" value="${soloJoinPath}" />
									<spring:message var="soloJoinLabel" code="tournament.registration.joinSolo" />
									<spring:message var="joiningSoloLabel" code="tournament.registration.joiningSolo" />
									<form method="post" action="${soloJoinAction}" data-submit-guard="true" data-submit-loading-label="${joiningSoloLabel}" class="booking-panel__request-form">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${soloJoinLabel}" type="submit" fullWidth="${true}" />
									</form>
									<p class="booking-panel__note"><spring:message code="tournament.registration.soloNote" /></p>
								</c:when>
								<c:when test="${tournamentCapabilities.canLeaveSolo}">
									<c:url var="soloLeaveAction" value="${soloLeavePath}" />
									<spring:message var="soloLeaveLabel" code="tournament.registration.leaveSolo" />
									<spring:message var="leavingSoloLabel" code="tournament.registration.leavingSolo" />
									<form method="post" action="${soloLeaveAction}" data-submit-guard="true" data-submit-loading-label="${leavingSoloLabel}" class="booking-panel__request-form">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${soloLeaveLabel}" type="submit" fullWidth="${true}" variant="secondary" />
									</form>
								</c:when>
								<c:when test="${tournamentCapabilities.requiresLoginToJoin}">
									<c:url var="loginHref" value="/login" />
									<spring:message var="signInLabel" code="tournament.registration.signIn" />
									<ui:button label="${signInLabel}" href="${loginHref}" fullWidth="${true}" />
									<p class="booking-panel__note"><spring:message code="tournament.registration.signInNote" /></p>
								</c:when>
								<c:when test="${tournamentCapabilities.registrationNotStarted}">
									<spring:message var="registrationTurnedOffLabel" code="tournament.registration.turnedOff" />
									<spring:message var="registrationNotStartedNote" code="tournament.registration.turnedOffNote" />
									<ui:button label="${registrationTurnedOffLabel}" type="button" fullWidth="${true}" disabled="${true}" />
									<p class="booking-panel__note"><c:out value="${registrationNotStartedNote}" /></p>
								</c:when>
								<c:otherwise>
									<spring:message var="registrationUnavailableLabel" code="tournament.registration.unavailable" />
									<ui:button label="${registrationUnavailableLabel}" type="button" fullWidth="${true}" disabled="${true}" />
									<p class="booking-panel__note"><spring:message code="tournament.registration.unavailableNote" /></p>
								</c:otherwise>
							</c:choose>
						</article>

						<c:if test="${tournament.status.dbValue eq 'registration' and (not empty tournamentTeamMembers or not empty tournamentActiveSoloEntries)}">
							<article class="panel event-info-panel">
								<p class="detail-label player-actions-panel__title">
									<spring:message code="tournament.participants.title" />
								</p>
								<ul class="event-info-panel__list">
									<c:forEach var="member" items="${tournamentTeamMembers}">
										<li class="booking-panel__detail-row event-info-panel__row">
											<span><c:out value="${member.user.username}" /></span>
											<span>
												<c:choose>
													<c:when test="${not empty member.team.name}">
														<c:out value="${member.team.name}" />
													</c:when>
													<c:otherwise>
														<spring:message code="tournament.team.solo.name" arguments="${tournamentTeamDisplayNumbers[member.team.id]}" />
													</c:otherwise>
												</c:choose>
											</span>
										</li>
									</c:forEach>
									<c:forEach var="entry" items="${tournamentActiveSoloEntries}">
										<li class="booking-panel__detail-row event-info-panel__row">
											<span><c:out value="${entry.user.username}" /></span>
											<span><spring:message code="tournament.participants.soloPool" /></span>
										</li>
									</c:forEach>
								</ul>
							</article>
						</c:if>

						<article class="panel event-info-panel">
							<dl class="event-info-panel__list">
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.price" /></dt>
									<dd>
										<c:choose>
											<c:when test="${empty tournament.pricePerPlayer}">
												<spring:message code="price.tbd" />
											</c:when>
											<c:when test="${tournament.pricePerPlayer.signum() == 0}">
												<spring:message code="price.free" />
											</c:when>
											<c:otherwise>
												<spring:message code="price.amount" arguments="${tournament.pricePerPlayer}" />
											</c:otherwise>
										</c:choose>
									</dd>
								</div>
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.schedule" /></dt>
									<dd>
										<c:choose>
											<c:when test="${empty tournament.startsAt}">
												<spring:message code="tournament.detail.schedule.tbd" />
											</c:when>
											<c:when test="${empty tournament.endsAt}">
												<c:out value="${tf:dateTime(tournament.startsAtDateTime)}" />
											</c:when>
											<c:when test="${tf:date(tournament.startsAtDateTime) eq tf:date(tournament.endsAtDateTime)}">
												<spring:message code="tournament.detail.schedule.sameDay">
													<spring:argument value="${tf:date(tournament.startsAtDateTime)}" />
													<spring:argument value="${tf:time(tournament.startsAtDateTime)}" />
													<spring:argument value="${tf:time(tournament.endsAtDateTime)}" />
												</spring:message>
											</c:when>
											<c:otherwise>
												<spring:message code="tournament.detail.schedule.range">
													<spring:argument value="${tf:dateTime(tournament.startsAtDateTime)}" />
													<spring:argument value="${tf:dateTime(tournament.endsAtDateTime)}" />
												</spring:message>
											</c:otherwise>
										</c:choose>
									</dd>
								</div>
								<div class="booking-panel__detail-row event-info-panel__row event-info-panel__row--registration-window">
									<dt><spring:message code="tournament.detail.registrationWindow" /></dt>
									<dd class="event-info-panel__registration-window">
										<c:choose>
											<c:when test="${not empty tournament.registrationOpensAt and not empty tournament.registrationClosesAt}">
												<ul class="event-info-panel__registration-window-list">
													<li class="event-info-panel__registration-window-item">
														<span class="event-info-panel__registration-window-label"><spring:message code="tournament.detail.registrationWindow.startsAt" />:</span>
														<c:out value="${tf:dateTime(tournament.registrationOpensAtDateTime)}" />
													</li>
													<li class="event-info-panel__registration-window-item">
														<span class="event-info-panel__registration-window-label"><spring:message code="tournament.detail.registrationWindow.endsAt" />:</span>
														<c:out value="${tf:dateTime(tournament.registrationClosesAtDateTime)}" />
													</li>
												</ul>
											</c:when>
											<c:otherwise>
												<span class="event-info-panel__registration-window-tbd">
													<spring:message code="tournament.detail.registrationWindow.tbd" />
												</span>
											</c:otherwise>
										</c:choose>
									</dd>
								</div>
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.venue" /></dt>
									<dd class="event-info-panel__value--truncate"><c:out value="${tournament.address}" /></dd>
								</div>
							</dl>
						</article>
						<c:if test="${mapAvailable}">
							<spring:message var="eventMapAria" code="event.detail.locationMap.aria" />
							<c:url var="appRootUrl" value="/" />
							<c:set var="contextAwareMapTileUrlTemplate"
								value="${appRootUrl}${fn:substring(mapTileUrlTemplate, 1, fn:length(mapTileUrlTemplate))}" />
							<div
								class="event-detail-map"
								data-event-map="true"
								data-tile-url-template="${contextAwareMapTileUrlTemplate}"
								data-attribution="${mapAttribution}"
								data-latitude="${mapLatitude}"
								data-longitude="${mapLongitude}"
								data-zoom="${mapZoom}"
								role="img"
								aria-label="${eventMapAria}">
								<c:if test="${not empty mapAttribution}">
									<p class="event-detail-map__attribution"><c:out value="${mapAttribution}" /></p>
								</c:if>
							</div>
						</c:if>

						<article class="panel event-info-panel event-info-panel--hosted-by">
							<dl class="event-info-panel__list">
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.host" /></dt>
									<dd>
										<c:url var="hostProfileImageSrc" value="${tournamentHostProfileImageUrl}" />
										<span class="event-info-panel__host">
											<img class="event-info-panel__host-avatar" src="${hostProfileImageSrc}" alt="" aria-hidden="true" loading="lazy" decoding="async" />
												<c:choose>
													<c:when test="${not empty tournamentHostProfileHref}">
														<c:url var="hostProfileHref" value="${tournamentHostProfileHref}" />
														<a class="event-info-panel__host-name" href="${hostProfileHref}"><c:out value="${tournamentHostUsername}" /></a>
													</c:when>
													<c:otherwise>
														<span class="event-info-panel__host-name">
															<spring:message code="event.detail.unknownHost" arguments="${tournamentUnknownHostArgument}" />
														</span>
													</c:otherwise>
												</c:choose>
										</span>
									</dd>
								</div>
							</dl>
						</article>
					</aside>
				</section>
			</main>
		</div>
	</body>
</html>
