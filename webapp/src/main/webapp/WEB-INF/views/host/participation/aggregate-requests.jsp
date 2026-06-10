<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
						<spring:message code="host.requests.all.description" />
					</p>
				</header>

				<div class="participation-layout">
					<div class="participation-nav">
						<c:url var="matchesHref" value="${matchesUrl}" />
						<spring:message var="matchesLabel" code="nav.player.matches" />
						<ui:button label="${matchesLabel}" href="${matchesHref}" variant="primary" className="participation-nav__button" />
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
													<c:out value="${fn:toUpperCase(fn:substring(req.user.username, 0, 1))}" />
												</span>
												<div class="participant-manage-list__details">
													<strong class="participant-manage-list__name">
														<c:out value="${req.user.username}" />
													</strong>
													<c:if test="${not empty req.match}">
														<c:url var="requestMatchHref" value="/matches/${req.match.id}" />
														<a class="participant-manage-list__meta-link" href="${requestMatchHref}">
															<c:out value="${req.match.title}" />
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
												<c:url var="approveAction" value="/host/matches/${req.match.id}/requests/${req.user.id}/approve" />
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
												<c:url var="rejectAction" value="/host/matches/${req.match.id}/requests/${req.user.id}/reject" />
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
						<c:if test="${not empty paginationItems}">
							<spring:message var="previousLabel" code="pagination.previous" />
							<spring:message var="nextLabel" code="pagination.next" />
							<spring:message var="paginationAria" code="host.requests.pagination.aria" />
							<section class="feed-pagination" aria-label="${paginationAria}">
								<nav class="feed-pagination__nav" aria-label="${paginationAria}">
									<div>
										<c:choose>
											<c:when test="${hasPreviousPage}">
												<c:url var="prevHref" value="${previousPageHref}" />
												<a class="feed-pagination__control" href="${prevHref}">
													<c:out value="${previousLabel}" />
												</a>
											</c:when>
											<c:otherwise>
												<span class="feed-pagination__control feed-pagination__control--disabled">
													<c:out value="${previousLabel}" />
												</span>
											</c:otherwise>
										</c:choose>
									</div>
									<div class="feed-pagination__pages">
										<c:forEach var="item" items="${paginationItems}">
											<c:choose>
												<c:when test="${item.ellipsis}">
													<span class="feed-pagination__ellipsis"><c:out value="${item.label}" /></span>
												</c:when>
												<c:when test="${item.current}">
													<span class="feed-pagination__page feed-pagination__page--current" aria-current="page">
														<c:out value="${item.label}" />
													</span>
												</c:when>
												<c:otherwise>
													<c:url var="pageHref" value="${item.href}" />
													<a class="feed-pagination__page" href="${pageHref}">
														<c:out value="${item.label}" />
													</a>
												</c:otherwise>
											</c:choose>
										</c:forEach>
									</div>
									<div>
										<c:choose>
											<c:when test="${hasNextPage}">
												<c:url var="nextHref" value="${nextPageHref}" />
												<a class="feed-pagination__control" href="${nextHref}">
													<c:out value="${nextLabel}" />
												</a>
											</c:when>
											<c:otherwise>
												<span class="feed-pagination__control feed-pagination__control--disabled">
													<c:out value="${nextLabel}" />
												</span>
											</c:otherwise>
										</c:choose>
									</div>
								</nav>
							</section>
						</c:if>
					</section>
				</div>
			</main>
		</div>
	</body>
</html>
