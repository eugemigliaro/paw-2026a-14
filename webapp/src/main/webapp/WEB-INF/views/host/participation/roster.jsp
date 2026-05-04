<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.hostRoster" />
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
						<spring:message code="host.roster.title" />
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
							<spring:message code="host.roster.description" />
						</p>
					</div>
				</header>

					<c:if test="${action eq 'removed'}">
						<div class="notice notice--success">
							<spring:message code="host.roster.removed" />
						</div>
				</c:if>
				<c:if test="${not empty param.error}">
					<div class="notice notice--error">
						<c:choose>
							<c:when test="${param.error eq 'not_participant'}">
								<spring:message code="host.roster.error.notParticipant" />
							</c:when>
							<c:otherwise>
								<spring:message code="host.roster.error.notParticipant" />
							</c:otherwise>
						</c:choose>
					</div>
				</c:if>

				<div class="participation-layout">
					<div class="participation-nav">
						<c:url var="backToEventHref" value="/matches/${matchId}" />
						<spring:message var="backToEventLabel" code="participation.backToEvent" />
						<ui:button label="${backToEventLabel}" href="${backToEventHref}" variant="secondary" className="participation-nav__button" />
						<c:choose>
							<c:when test="${isPrivateEvent}">
								<c:url var="invitesHref" value="${invitesUrl}" />
								<spring:message var="viewInvitesLabel" code="host.roster.viewInvites" />
								<ui:button label="${viewInvitesLabel}" href="${invitesHref}" variant="primary" className="participation-nav__button" />
							</c:when>
							<c:when test="${isApprovalRequired}">
								<c:url var="requestsHref" value="${requestsUrl}" />
								<spring:message var="viewRequestsLabel" code="host.roster.viewRequests" />
								<ui:button label="${viewRequestsLabel}" href="${requestsHref}" variant="primary" className="participation-nav__button" />
							</c:when>
						</c:choose>
					</div>

					<section class="panel participation-panel">
						<c:choose>
							<c:when test="${empty participants}">
								<p class="participation-empty-state">
									<c:out value="${emptyMessage}" />
								</p>
							</c:when>
							<c:otherwise>
								<ul class="participant-manage-list">
									<c:forEach var="p" items="${participants}">
										<li class="participant-manage-list__item">
											<div class="participant-manage-list__info">
												<span class="participant-list__avatar" aria-hidden="true">
													<c:out value="${p.avatarLabel}" />
												</span>
												<c:choose>
													<c:when test="${not empty p.profileHref}">
														<c:url var="participantProfileHref" value="${p.profileHref}" />
														<a class="participant-manage-list__name" href="${participantProfileHref}">
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
										</li>
									</c:forEach>
								</ul>
							</c:otherwise>
						</c:choose>
					</section>
				</div>

			</main>
		</div>
	</body>
</html>
