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

			<main class="page-shell page-shell--detail tournament-detail">
				<section class="detail-top">
					<div class="detail-top__main">
						<section class="event-hero tournament-hero ${not empty tournamentPage.bannerImageUrl ? 'event-hero--with-image' : ''}">
							<c:if test="${not empty tournamentPage.bannerImageUrl}">
								<c:url var="tournamentHeroBannerSrc" value="${tournamentPage.bannerImageUrl}" />
								<img class="event-hero__image" src="${tournamentHeroBannerSrc}" alt="" loading="eager" decoding="async" />
							</c:if>
							<div class="event-heading">
								<div class="tournament-hero__badges">
									<span class="event-heading__badge"><c:out value="${tournamentPage.sportLabel}" /></span>
									<span class="tournament-status tournament-status--${tournamentPage.statusTone}">
										<c:out value="${tournamentPage.statusLabel}" />
									</span>
								</div>
								<h1 class="event-heading__title"><c:out value="${tournamentPage.title}" /></h1>
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
								<c:forEach var="paragraph" items="${tournamentPage.aboutParagraphs}">
									<p class="body-copy detail-stack__paragraph"><c:out value="${paragraph}" /></p>
								</c:forEach>
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
									<dd><c:out value="${tournamentPage.bracketSizeLabel}" /></dd>
								</div>
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.teamSize" /></dt>
									<dd><c:out value="${tournamentPage.teamSizeLabel}" /></dd>
								</div>
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.format" /></dt>
									<dd><c:out value="${tournamentPage.formatLabel}" /></dd>
								</div>
								<div class="tournament-fact">
									<dt><spring:message code="tournament.detail.registrationMode" /></dt>
									<dd><c:out value="${tournamentPage.joinModeLabel}" /></dd>
								</div>
							</dl>
						</section>
					</div>

					<aside class="detail-top__sidebar">
						<c:if test="${tournamentPage.canCloseRegistration or tournamentPage.canEditTournament or tournamentPage.canCancelTournament}">
							<article class="panel host-panel">
								<div class="host-panel__note">
									<p class="detail-label"><spring:message code="tournament.host.panel.label" /></p>
									<p><spring:message code="tournament.host.panel.detail" /></p>
								</div>
								<c:if test="${tournamentPage.canCloseRegistration}">
									<c:url var="closeRegistrationAction" value="${closeRegistrationPath}" />
									<spring:message var="closeRegistrationLabel" code="tournament.host.closeRegistration" />
									<spring:message var="closingRegistrationLabel" code="tournament.host.closeRegistration.loading" />
									<form method="post" action="${closeRegistrationAction}" data-submit-guard="true" data-submit-loading-label="${closingRegistrationLabel}">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${closeRegistrationLabel}" type="submit" fullWidth="${true}" variant="primary" disabled="${not tournamentPage.registrationOpen}" />
									</form>
								</c:if>
								<c:if test="${tournamentPage.canEditTournament}">
									<c:url var="editTournamentHref" value="${editTournamentPath}" />
									<spring:message var="editTournamentLabel" code="tournament.host.edit" />
									<ui:button label="${editTournamentLabel}" href="${editTournamentHref}" fullWidth="${true}" variant="secondary" />
								</c:if>
								<c:if test="${tournamentPage.canCancelTournament}">
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
						<c:if test="${tournamentPage.canManageBracket}">
							<article class="panel host-panel">
								<div class="host-panel__note">
									<p class="detail-label"><spring:message code="tournament.host.bracket.panel.label" /></p>
									<p><spring:message code="tournament.host.bracket.panel.detail" /></p>
								</div>
								<c:url var="bracketSetupHref" value="/host/tournaments/${tournamentPage.id}/bracket/setup" />
								<spring:message var="bracketSetupLabel" code="tournament.host.bracket.setup" />
								<ui:button label="${bracketSetupLabel}" href="${bracketSetupHref}" fullWidth="${true}" variant="secondary" />
							</article>
						</c:if>
						<c:if test="${tournamentPage.canViewBracket}">
							<article class="panel host-panel">
								<div class="host-panel__note">
									<p class="detail-label"><spring:message code="tournament.bracket.panel.label" /></p>
									<p><spring:message code="tournament.bracket.panel.detail" /></p>
								</div>
								<c:url var="publicBracketHref" value="/tournaments/${tournamentPage.id}/bracket" />
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
							<c:if test="${not empty tournamentPage.participationLabel}">
								<p class="booking-panel__notice booking-panel__notice--info">
									<c:out value="${tournamentPage.participationLabel}" />
								</p>
							</c:if>
							<c:if test="${not empty tournamentPage.nextStepLabel}">
								<p class="booking-panel__notice booking-panel__notice--info">
									<c:out value="${tournamentPage.nextStepLabel}" />
								</p>
							</c:if>

							<c:choose>
								<c:when test="${tournamentPage.canJoinSolo}">
									<c:url var="soloJoinAction" value="${soloJoinPath}" />
									<spring:message var="soloJoinLabel" code="tournament.registration.joinSolo" />
									<spring:message var="joiningSoloLabel" code="tournament.registration.joiningSolo" />
									<form method="post" action="${soloJoinAction}" data-submit-guard="true" data-submit-loading-label="${joiningSoloLabel}" class="booking-panel__request-form">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${soloJoinLabel}" type="submit" fullWidth="${true}" />
									</form>
									<p class="booking-panel__note"><spring:message code="tournament.registration.soloNote" /></p>
								</c:when>
								<c:when test="${tournamentPage.canLeaveSolo}">
									<c:url var="soloLeaveAction" value="${soloLeavePath}" />
									<spring:message var="soloLeaveLabel" code="tournament.registration.leaveSolo" />
									<spring:message var="leavingSoloLabel" code="tournament.registration.leavingSolo" />
									<form method="post" action="${soloLeaveAction}" data-submit-guard="true" data-submit-loading-label="${leavingSoloLabel}" class="booking-panel__request-form">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<ui:button label="${soloLeaveLabel}" type="submit" fullWidth="${true}" variant="secondary" />
									</form>
								</c:when>
								<c:when test="${tournamentPage.requiresLoginToJoin}">
									<c:url var="loginHref" value="/login" />
									<spring:message var="signInLabel" code="tournament.registration.signIn" />
									<ui:button label="${signInLabel}" href="${loginHref}" fullWidth="${true}" />
									<p class="booking-panel__note"><spring:message code="tournament.registration.signInNote" /></p>
								</c:when>
								<c:when test="${tournamentPage.registrationNotStarted}">
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

						<article class="panel event-info-panel">
							<dl class="event-info-panel__list">
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.price" /></dt>
									<dd><c:out value="${tournamentPage.priceLabel}" /></dd>
								</div>
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.schedule" /></dt>
									<dd><c:out value="${tournamentPage.scheduleLabel}" /></dd>
								</div>
								<div class="booking-panel__detail-row event-info-panel__row event-info-panel__row--registration-window">
									<dt><spring:message code="tournament.detail.registrationWindow" /></dt>
									<dd class="event-info-panel__registration-window">
										<c:choose>
											<c:when test="${not empty tournamentPage.registrationWindowStartLabel and not empty tournamentPage.registrationWindowEndLabel}">
												<ul class="event-info-panel__registration-window-list">
													<li class="event-info-panel__registration-window-item">
														<span class="event-info-panel__registration-window-label"><spring:message code="tournament.detail.registrationWindow.startsAt" />:</span>
														<c:out value="${tournamentPage.registrationWindowStartLabel}" />
													</li>
													<li class="event-info-panel__registration-window-item">
														<span class="event-info-panel__registration-window-label"><spring:message code="tournament.detail.registrationWindow.endsAt" />:</span>
														<c:out value="${tournamentPage.registrationWindowEndLabel}" />
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
									<dd class="event-info-panel__value--truncate"><c:out value="${tournamentPage.address}" /></dd>
								</div>
							</dl>
						</article>

						<article class="panel event-info-panel event-info-panel--hosted-by">
							<dl class="event-info-panel__list">
								<div class="booking-panel__detail-row event-info-panel__row">
									<dt><spring:message code="tournament.detail.host" /></dt>
									<dd>
										<c:url var="hostProfileImageSrc" value="${tournamentPage.hostProfileImageUrl}" />
										<span class="event-info-panel__host">
											<img class="event-info-panel__host-avatar" src="${hostProfileImageSrc}" alt="" aria-hidden="true" loading="lazy" decoding="async" />
											<c:choose>
												<c:when test="${not empty tournamentPage.hostProfileHref}">
													<c:url var="hostProfileHref" value="${tournamentPage.hostProfileHref}" />
													<a class="event-info-panel__host-name" href="${hostProfileHref}"><c:out value="${tournamentPage.hostLabel}" /></a>
												</c:when>
												<c:otherwise>
													<span class="event-info-panel__host-name"><c:out value="${tournamentPage.hostLabel}" /></span>
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
