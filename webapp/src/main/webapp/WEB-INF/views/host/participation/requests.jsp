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
					<p class="page-heading__description">
						<c:choose>
							<c:when test="${aggregateRequests}">
								<spring:message code="host.requests.all.description" />
							</c:when>
							<c:otherwise>
								<c:out value="${match.title}" /> &mdash;
								<spring:message code="host.requests.description" />
							</c:otherwise>
						</c:choose>
					</p>
				</header>

				<c:if test="${param.action eq 'approved'}">
					<div class="notice notice--success">
						<spring:message code="host.requests.approved" />
					</div>
				</c:if>
				<c:if test="${param.action eq 'rejected'}">
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
								<a class="section-link" href="${matchesHref}">
									<spring:message code="nav.player.events" />
								</a>
							</c:when>
							<c:otherwise>
								<c:url var="backToEventHref" value="/matches/${matchId}" />
								<a class="section-link" href="${backToEventHref}">
									<spring:message code="participation.backToEvent" />
								</a>
								<c:url var="rosterHref" value="${rosterUrl}" />
								<a class="section-link" href="${rosterHref}">
									<spring:message code="host.requests.viewRoster" />
								</a>
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
