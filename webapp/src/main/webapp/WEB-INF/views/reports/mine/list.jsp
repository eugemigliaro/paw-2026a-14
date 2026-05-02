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

			<main class="page-shell page-shell--detail">
				<header class="page-heading">
					<h1 class="page-heading__title"><c:out value="${pageTitleLabel}" /></h1>
					<p class="page-heading__description"><c:out value="${pageDescription}" /></p>
				</header>

				<spring:message var="filterAriaLabel" code="reports.mine.filter.aria" />
				<spring:message var="filterTypeLabel" code="reports.mine.filter.type" />
				<spring:message var="filterStatusLabel" code="reports.mine.filter.status" />
				<c:url var="myReportsHref" value="/reports/mine" />
				<div class="report-filter-bar" role="group" aria-label="${filterAriaLabel}">
					<form id="report-filter-form" method="get" action="${myReportsHref}">

						<div class="report-filter-group">
							<span class="report-filter-group__label"><c:out value="${filterTypeLabel}" /></span>
							<div class="report-filter-chips">
								<spring:message var="typeMatch" code="admin.reports.targetType.match" />
								<spring:message var="typeReview" code="admin.reports.targetType.review" />
								<spring:message var="typeUser" code="admin.reports.targetType.user" />

								<label class="chip ${selectedTypes.contains('match') ? 'chip--active' : ''}">
									<input type="checkbox" name="type" value="match" class="field--hidden"
										<c:if test="${selectedTypes.contains('match')}">checked</c:if> />
									<c:out value="${typeMatch}" />
								</label>
								<label class="chip ${selectedTypes.contains('review') ? 'chip--active' : ''}">
									<input type="checkbox" name="type" value="review" class="field--hidden"
										<c:if test="${selectedTypes.contains('review')}">checked</c:if> />
									<c:out value="${typeReview}" />
								</label>
								<label class="chip ${selectedTypes.contains('user') ? 'chip--active' : ''}">
									<input type="checkbox" name="type" value="user" class="field--hidden"
										<c:if test="${selectedTypes.contains('user')}">checked</c:if> />
									<c:out value="${typeUser}" />
								</label>
							</div>
						</div>

						<div class="report-filter-group">
							<span class="report-filter-group__label"><c:out value="${filterStatusLabel}" /></span>
							<div class="report-filter-chips">
								<spring:message var="statusPending" code="admin.reports.status.pending" />
								<spring:message var="statusUnderReview" code="admin.reports.status.under_review" />
								<spring:message var="statusResolved" code="admin.reports.status.resolved" />
								<spring:message var="statusAppealed" code="admin.reports.status.appealed" />
								<spring:message var="statusFinalized" code="admin.reports.status.finalized" />

								<label class="chip ${selectedStatuses.contains('pending') ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="pending" class="field--hidden"
										<c:if test="${selectedStatuses.contains('pending')}">checked</c:if> />
									<c:out value="${statusPending}" />
								</label>
								<label class="chip ${selectedStatuses.contains('under_review') ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="under_review" class="field--hidden"
										<c:if test="${selectedStatuses.contains('under_review')}">checked</c:if> />
									<c:out value="${statusUnderReview}" />
								</label>
								<label class="chip ${selectedStatuses.contains('resolved') ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="resolved" class="field--hidden"
										<c:if test="${selectedStatuses.contains('resolved')}">checked</c:if> />
									<c:out value="${statusResolved}" />
								</label>
								<label class="chip ${selectedStatuses.contains('appealed') ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="appealed" class="field--hidden"
										<c:if test="${selectedStatuses.contains('appealed')}">checked</c:if> />
									<c:out value="${statusAppealed}" />
								</label>
								<label class="chip ${selectedStatuses.contains('finalized') ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="finalized" class="field--hidden"
										<c:if test="${selectedStatuses.contains('finalized')}">checked</c:if> />
									<c:out value="${statusFinalized}" />
								</label>
							</div>
						</div>

					</form>
				</div>
				<p class="report-count-label"><strong><c:out value="${reportCountLabel}" /></strong></p>

				<c:choose>
					<c:when test="${empty reports}">
						<ui:card className="report-section">
							<p class="participation-empty-state"><c:out value="${emptyMessage}" /></p>
						</ui:card>
					</c:when>
					<c:otherwise>
						<div class="report-card-list" id="report-card-list">
							<c:forEach var="report" items="${reports}">
								<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetTypeCode}" />
								<spring:message var="reasonLabel" code="admin.reports.reason.${report.reasonCode}" />
								<spring:message var="statusLabel" code="admin.reports.status.${report.statusCode}" />

								<div class="report-card">
									<ui:card className="report-section report-card__inner">
										<div class="report-card__header">
											<div class="report-card__meta">
												<span class="report-card__id">#<c:out value="${report.id}" /></span>
												<span class="report-status-badge report-status-badge--${report.statusCode}">
													<c:out value="${statusLabel}" />
												</span>
											</div>
											<div class="report-card__chips">
												<span class="report-type-badge report-type-badge--${report.targetTypeCode}">
													<c:out value="${targetTypeLabel}" />
												</span>
												<span class="report-card__reason-chip"><c:out value="${reasonLabel}" /></span>
											</div>
										</div>

										<dl class="report-card__body stack">
											<div class="report-section-field report-section-field__row">
												<dt class="detail-label"><spring:message code="reports.mine.target" /></dt>
												<dd><c:out value="${report.targetKey}" /></dd>
											</div>
											<div class="report-section-field report-section-field__row">
												<dt class="detail-label"><spring:message code="admin.reports.createdAt" /></dt>
												<dd><c:out value="${report.createdAtLabel}" /></dd>
											</div>
											<c:if test="${not empty report.updatedAtLabel and !report.updatedAtLabel.equals(report.createdAtLabel)}">
												<div class="report-section-field report-section-field__row">
													<dt class="detail-label"><spring:message code="reports.mine.updatedAt" /></dt>
													<dd><c:out value="${report.updatedAtLabel}" /></dd>
												</div>
											</c:if>
											<c:if test="${not empty report.resolutionCode}">
												<spring:message var="resolutionLabel" code="reports.mine.resolution.${report.resolutionCode}" />
												<div class="report-section-field report-section-field__row">
													<dt class="detail-label"><spring:message code="reports.mine.resolution" /></dt>
													<dd><c:out value="${resolutionLabel}" /></dd>
												</div>
											</c:if>
										</dl>

										<div class="report-section-actions">
											<c:url var="reportDetailHref" value="/reports/mine/${report.id}" />
											<spring:message var="reportDetailLabel" code="reports.mine.action.view" />
											<ui:button label="${reportDetailLabel}" href="${reportDetailHref}" variant="secondary" />
										</div>
									</ui:card>
								</div>
							</c:forEach>
						</div>
					</c:otherwise>
				</c:choose>
				<c:if test="${not empty paginationItems}">
					<spring:message var="previousLabel" code="pagination.previous" />
					<spring:message var="nextLabel" code="pagination.next" />
					<section class="feed-pagination" aria-label="${pageTitleLabel}">
						<nav class="feed-pagination__nav" aria-label="${pageTitleLabel}">
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
											<span class="feed-pagination__ellipsis">${item.label}</span>
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
			</main>
		</div>
	</body>
</html>
