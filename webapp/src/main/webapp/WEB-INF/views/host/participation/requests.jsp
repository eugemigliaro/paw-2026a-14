<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.hostRequests" />
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
						<spring:message code="host.requests.title" />
					</h1>
					<c:choose>
						<c:when test="${aggregateRequests}">
							<p class="page-heading__description">
								<spring:message code="host.requests.all.description" />
							</p>
						</c:when>
						<c:otherwise>
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
									<spring:message code="host.requests.description" />
								</p>
							</div>
						</c:otherwise>
					</c:choose>
				</header>

					<c:if test="${action eq 'approved'}">
						<div class="notice notice--success">
							<spring:message code="host.requests.approved" />
						</div>
					</c:if>
					<c:if test="${action eq 'rejected'}">
						<div class="notice notice--info">
							<spring:message code="host.requests.rejected" />
						</div>
				</c:if>
				<c:if test="${not empty param.error}">
					<div class="notice notice--error">
						<c:choose>
							<c:when test="${param.error eq 'full'}">
								<spring:message code="host.requests.error.full" />
							</c:when>
							<c:otherwise>
								<spring:message code="host.requests.error.noPendingRequest" />
							</c:otherwise>
						</c:choose>
					</div>
				</c:if>

				<div class="participation-layout">
					<div class="participation-nav">
						<c:choose>
							<c:when test="${aggregateRequests}">
								<c:url var="matchesHref" value="${matchesUrl}" />
								<spring:message var="matchesLabel" code="nav.player.events" />
								<ui:button label="${matchesLabel}" href="${matchesHref}" variant="primary" className="participation-nav__button" />
							</c:when>
							<c:otherwise>
								<c:url var="backToEventHref" value="/matches/${matchId}" />
								<spring:message var="backToEventLabel" code="participation.backToEvent" />
								<ui:button label="${backToEventLabel}" href="${backToEventHref}" variant="secondary" className="participation-nav__button" />
								<c:url var="rosterHref" value="${rosterUrl}" />
								<spring:message var="viewRosterLabel" code="host.requests.viewRoster" />
								<ui:button label="${viewRosterLabel}" href="${rosterHref}" variant="primary" className="participation-nav__button" />
							</c:otherwise>
						</c:choose>
					</div>

					<section class="panel participation-panel">
						<c:choose>
							<c:when test="${empty pendingRequests}">
								<p class="participation-empty-state">
									<c:out value="${emptyMessage}" />
								</p>
							</c:when>
							<c:otherwise>
								<ul class="participant-manage-list">
									<c:forEach var="req" items="${pendingRequests}">
										<li class="participant-manage-list__item">
											<div class="participant-manage-list__info">
												<span class="participant-list__avatar" aria-hidden="true">
													<c:out value="${req.avatarLabel}" />
												</span>
												<div class="participant-manage-list__details">
													<c:choose>
														<c:when test="${not empty req.profileHref}">
															<c:url var="requestProfileHref" value="${req.profileHref}" />
															<a class="participant-manage-list__name" href="${requestProfileHref}">
																<c:out value="${req.username}" />
															</a>
														</c:when>
														<c:otherwise>
															<strong class="participant-manage-list__name">
																<c:out value="${req.username}" />
															</strong>
														</c:otherwise>
													</c:choose>
													<c:if test="${not empty req.matchHref}">
														<c:url var="requestMatchHref" value="${req.matchHref}" />
														<a class="participant-manage-list__meta-link" href="${requestMatchHref}">
															<c:out value="${req.matchTitle}" />
														</a>
													</c:if>
													<c:if test="${req.seriesRequest}">
														<span class="participant-manage-list__status participant-manage-list__status--pending">
															<spring:message code="host.requests.seriesBadge" />
														</span>
													</c:if>
												</div>
											</div>
											<div class="participant-manage-list__actions">
												<c:url var="approveAction" value="${req.approveUrl}" />
												<spring:message var="approvingLabel" code="host.requests.approving" />
												<form
													method="post"
													action="${approveAction}"
													data-submit-guard="true"
													data-submit-loading-label="${approvingLabel}"
													class="participant-manage-list__action-form"
												>
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="approveLabel" code="host.requests.approve" />
													<ui:button label="${approveLabel}" type="submit" />
												</form>
												<c:url var="rejectAction" value="${req.rejectUrl}" />
												<spring:message var="rejectingLabel" code="host.requests.rejecting" />
												<form
													method="post"
													action="${rejectAction}"
													data-submit-guard="true"
													data-submit-loading-label="${rejectingLabel}"
													class="participant-manage-list__action-form"
												>
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="rejectLabel" code="host.requests.reject" />
													<ui:button label="${rejectLabel}" type="submit" variant="secondary" />
												</form>
											</div>
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
