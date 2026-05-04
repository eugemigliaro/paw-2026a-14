<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.hostInvites" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--participation">

				<header class="page-heading">
					<h1 class="page-heading__title">
						<spring:message code="host.invites.title" />
					</h1>
					<div class="participation-event-heading">
						<h2 class="participation-event-heading__title">
							<c:out value="${match.title}" />
						</h2>
						<div class="participation-event-heading__meta">
							<span class="participation-event-heading__meta-item">
								<span class="participation-event-heading__meta-icon" aria-hidden="true">
									<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
										<rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
										<line x1="16" y1="2" x2="16" y2="6" />
										<line x1="8" y1="2" x2="8" y2="6" />
										<line x1="3" y1="10" x2="21" y2="10" />
									</svg>
								</span>
								<c:out value="${participationEventDate}" />
							</span>
							<span class="participation-event-heading__meta-item">
								<span class="participation-event-heading__meta-icon" aria-hidden="true">
									<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
										<circle cx="12" cy="12" r="10" />
										<polyline points="12 6 12 12 16 14" />
									</svg>
								</span>
								<c:out value="${participationEventTime}" />
							</span>
							<span class="participation-event-heading__meta-item">
								<span class="participation-event-heading__meta-icon" aria-hidden="true">
									<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
										<path d="M12 22s8-4.35 8-11a8 8 0 1 0-16 0c0 6.65 8 11 8 11z" />
										<circle cx="12" cy="11" r="3" />
									</svg>
								</span>
								<c:out value="${participationEventVenue}" />
							</span>
						</div>
						<p class="page-heading__description participation-event-heading__description">
							<spring:message code="host.invites.description" />
						</p>
					</div>
				</header>

					<c:if test="${action eq 'invited'}">
						<div class="notice notice--success">
							<spring:message code="host.invites.sent" />
						</div>
					</c:if>
					<c:if test="${action eq 'seriesInvited'}">
						<div class="notice notice--success">
							<spring:message code="host.invites.seriesSent" />
						</div>
				</c:if>

				<div class="participation-layout">
					<div class="participation-nav">
						<c:url var="backToEventHref" value="/matches/${matchId}" />
						<spring:message var="backToEventLabel" code="participation.backToEvent" />
						<ui:button label="${backToEventLabel}" href="${backToEventHref}" variant="secondary" className="participation-nav__button" />
						<c:url var="rosterHref" value="${rosterUrl}" />
						<spring:message var="viewRosterLabel" code="host.invites.viewRoster" />
						<ui:button label="${viewRosterLabel}" href="${rosterHref}" variant="primary" className="participation-nav__button" />
					</div>

					<section class="panel participation-panel">
						<h2 class="participation-panel__section-title">
							<spring:message code="host.invites.sendInvite" />
						</h2>

						<c:if test="${not empty inviteError}">
							<div class="notice notice--error">
								<c:out value="${inviteError}" />
							</div>
						</c:if>

						<c:url var="inviteAction" value="/host/matches/${matchId}/invites" />
						<spring:message var="invitingLabel" code="host.invites.inviting" />
						<form:form
							method="post"
							action="${inviteAction}"
							modelAttribute="inviteForm"
							data-submit-guard="true"
							data-submit-loading-label="${invitingLabel}"
							cssClass="invite-form"
							novalidate="novalidate"
						>
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<div class="invite-form__row">
								<spring:message var="emailPlaceholder" code="form.email.placeholder" />
								<label class="field" for="invite-email">
									<span class="field__label"><spring:message code="form.email.label" /></span>
									<form:input
										path="email"
										id="invite-email"
										type="email"
										cssClass="field__control invite-form__email-input"
										placeholder="${emailPlaceholder}"
									/>
									<form:errors path="email" cssClass="field__error" element="span" />
								</label>
								<spring:message var="inviteLabel" code="host.invites.invite" />
								<div class="invite-form__submit">
									<ui:button label="${inviteLabel}" type="submit" />
								</div>
							</div>
							<c:if test="${seriesInviteAvailable}">
								<label class="series-invite-option" for="invite-series">
									<form:checkbox
										path="inviteSeries"
										id="invite-series"
										cssClass="series-invite-option__input"
									/>
									<span class="series-invite-option__copy">
										<span class="series-invite-option__title">
											<spring:message code="host.invites.inviteSeries" />
										</span>
										<span class="series-invite-option__hint">
											<spring:message code="host.invites.inviteSeries.hint" />
										</span>
									</span>
								</label>
							</c:if>
						</form:form>
					</section>

					<c:if test="${not empty pendingInvites}">
						<section class="panel participation-panel">
							<h2 class="participation-panel__section-title">
								<spring:message code="host.invites.pending" />
							</h2>
							<ul class="participant-manage-list">
								<c:forEach var="p" items="${pendingInvites}">
									<li class="participant-manage-list__item">
										<div class="participant-manage-list__info">
											<span class="participant-list__avatar" aria-hidden="true">
												<c:out value="${p.avatarLabel}" />
											</span>
											<c:choose>
												<c:when test="${not empty p.profileHref}">
													<c:url var="pendingProfileHref" value="${p.profileHref}" />
													<a class="participant-manage-list__name" href="${pendingProfileHref}">
														<c:out value="${p.username}" />
													</a>
												</c:when>
												<c:otherwise>
													<strong class="participant-manage-list__name">
														<c:out value="${p.username}" />
													</strong>
												</c:otherwise>
											</c:choose>
										</div>
										<span class="participant-manage-list__status participant-manage-list__status--pending">
											<spring:message code="host.invites.status.pending" />
										</span>
									</li>
								</c:forEach>
							</ul>
						</section>
					</c:if>

					<c:if test="${not empty acceptedParticipants}">
						<section class="panel participation-panel">
							<h2 class="participation-panel__section-title">
								<spring:message code="host.invites.accepted" />
							</h2>
							<ul class="participant-manage-list">
								<c:forEach var="p" items="${acceptedParticipants}">
									<li class="participant-manage-list__item">
										<div class="participant-manage-list__info">
											<span class="participant-list__avatar" aria-hidden="true">
												<c:out value="${p.avatarLabel}" />
											</span>
											<c:choose>
												<c:when test="${not empty p.profileHref}">
													<c:url var="acceptedProfileHref" value="${p.profileHref}" />
													<a class="participant-manage-list__name" href="${acceptedProfileHref}">
														<c:out value="${p.username}" />
													</a>
												</c:when>
												<c:otherwise>
													<strong class="participant-manage-list__name">
														<c:out value="${p.username}" />
													</strong>
												</c:otherwise>
											</c:choose>
										</div>
										<div class="participant-manage-list__actions">
											<span class="participant-manage-list__status participant-manage-list__status--accepted">
												<spring:message code="host.invites.status.accepted" />
											</span>
											<c:url var="removeAction" value="${p.removeUrl}" />
											<spring:message var="removingLabel" code="host.roster.removing" />
											<form
												method="post"
												action="${removeAction}"
												data-submit-guard="true"
												data-submit-loading-label="${removingLabel}"
												class="participant-manage-list__action-form"
											>
												<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
												<spring:message var="removeLabel" code="host.roster.remove" />
												<ui:button label="${removeLabel}" type="submit" variant="danger" />
											</form>
										</div>
									</li>
								</c:forEach>
							</ul>
						</section>
					</c:if>

					<c:if test="${not empty declinedInvites}">
						<section class="panel participation-panel">
							<h2 class="participation-panel__section-title">
								<spring:message code="host.invites.declined" />
							</h2>
							<ul class="participant-manage-list">
								<c:forEach var="p" items="${declinedInvites}">
									<li class="participant-manage-list__item">
										<div class="participant-manage-list__info">
											<span class="participant-list__avatar" aria-hidden="true">
												<c:out value="${p.avatarLabel}" />
											</span>
											<c:choose>
												<c:when test="${not empty p.profileHref}">
													<c:url var="declinedProfileHref" value="${p.profileHref}" />
													<a class="participant-manage-list__name" href="${declinedProfileHref}">
														<c:out value="${p.username}" />
													</a>
												</c:when>
												<c:otherwise>
													<strong class="participant-manage-list__name">
														<c:out value="${p.username}" />
													</strong>
												</c:otherwise>
											</c:choose>
										</div>
										<span class="participant-manage-list__status participant-manage-list__status--declined">
											<spring:message code="host.invites.status.declined" />
										</span>
									</li>
								</c:forEach>
							</ul>
						</section>
					</c:if>

					<c:if test="${empty pendingInvites and empty acceptedParticipants and empty declinedInvites}">
						<section class="panel participation-panel">
							<p class="participation-empty-state">
								<spring:message code="host.invites.empty" />
							</p>
						</section>
					</c:if>
				</div>

			</main>
		</div>
	</body>
</html>
