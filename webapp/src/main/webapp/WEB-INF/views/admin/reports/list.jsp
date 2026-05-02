<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.adminReports" />
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

				<c:if test="${not empty param.action}">
					<div class="notice notice--success">
						<c:choose>
							<c:when test="${param.action eq 'reviewed'}"><spring:message code="admin.reports.action.reviewed" /></c:when>
							<c:when test="${param.action eq 'dismissed'}"><spring:message code="admin.reports.action.dismissed" /></c:when>
							<c:when test="${param.action eq 'warned'}"><spring:message code="admin.reports.action.warned" /></c:when>
							<c:when test="${param.action eq 'deleted'}"><spring:message code="admin.reports.action.deleted" /></c:when>
							<c:when test="${param.action eq 'banned'}"><spring:message code="admin.reports.action.banned" /></c:when>
							<c:when test="${param.action eq 'ban_appeal_resolved'}"><spring:message code="admin.reports.action.ban_appeal_resolved" /></c:when>
							<c:otherwise><spring:message code="admin.reports.action.reviewed" /></c:otherwise>
						</c:choose>
					</div>
				</c:if>
				<c:if test="${not empty param.error}">
					<div class="notice notice--error">
						<c:choose>
							<c:when test="${param.error eq 'report_not_found'}"><spring:message code="admin.reports.error.report_not_found" /></c:when>
							<c:otherwise><spring:message code="admin.reports.error.action_failed" /></c:otherwise>
						</c:choose>
					</div>
				</c:if>

				<spring:message var="filterAriaLabel" code="reports.mine.filter.aria" />
				<spring:message var="filterTypeLabel" code="reports.mine.filter.type" />
				<spring:message var="filterStatusLabel" code="reports.mine.filter.status" />
				<c:url var="adminReportsHref" value="/admin/reports" />
				<div class="report-filter-bar" role="group" aria-label="${filterAriaLabel}">
					<form id="report-filter-form" method="get" action="${adminReportsHref}">

						<div class="report-filter-group">
							<span class="report-filter-group__label"><c:out value="${filterTypeLabel}" /></span>
							<div class="report-filter-chips">
								<spring:message var="typeMatch"  code="admin.reports.targetType.match" />
								<spring:message var="typeReview" code="admin.reports.targetType.review" />
								<spring:message var="typeUser"   code="admin.reports.targetType.user" />

								<label class="chip ${selectedTypes.contains('match')  ? 'chip--active' : ''}">
									<input type="checkbox" name="type" value="match"  class="field--hidden"
										<c:if test="${selectedTypes.contains('match')}">checked</c:if> />
									<c:out value="${typeMatch}" />
								</label>
								<label class="chip ${selectedTypes.contains('review') ? 'chip--active' : ''}">
									<input type="checkbox" name="type" value="review" class="field--hidden"
										<c:if test="${selectedTypes.contains('review')}">checked</c:if> />
									<c:out value="${typeReview}" />
								</label>
								<label class="chip ${selectedTypes.contains('user')   ? 'chip--active' : ''}">
									<input type="checkbox" name="type" value="user"   class="field--hidden"
										<c:if test="${selectedTypes.contains('user')}">checked</c:if> />
									<c:out value="${typeUser}" />
								</label>
							</div>
						</div>

						<div class="report-filter-group">
							<span class="report-filter-group__label"><c:out value="${filterStatusLabel}" /></span>
							<div class="report-filter-chips">
								<spring:message var="statusPending"     code="admin.reports.status.pending" />
								<spring:message var="statusUnderReview" code="admin.reports.status.under_review" />
								<spring:message var="statusResolved"    code="admin.reports.status.resolved" />
								<spring:message var="statusAppealed"    code="admin.reports.status.appealed" />
								<spring:message var="statusFinalized"   code="admin.reports.status.finalized" />

								<label class="chip ${selectedStatuses.contains('pending')      ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="pending"      class="field--hidden"
										<c:if test="${selectedStatuses.contains('pending')}">checked</c:if> />
									<c:out value="${statusPending}" />
								</label>
								<label class="chip ${selectedStatuses.contains('under_review') ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="under_review" class="field--hidden"
										<c:if test="${selectedStatuses.contains('under_review')}">checked</c:if> />
									<c:out value="${statusUnderReview}" />
								</label>
								<label class="chip ${selectedStatuses.contains('resolved')     ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="resolved"     class="field--hidden"
										<c:if test="${selectedStatuses.contains('resolved')}">checked</c:if> />
									<c:out value="${statusResolved}" />
								</label>
								<label class="chip ${selectedStatuses.contains('appealed')     ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="appealed"     class="field--hidden"
										<c:if test="${selectedStatuses.contains('appealed')}">checked</c:if> />
									<c:out value="${statusAppealed}" />
								</label>
								<label class="chip ${selectedStatuses.contains('finalized')    ? 'chip--active' : ''}">
									<input type="checkbox" name="status" value="finalized"    class="field--hidden"
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
								<spring:message var="reasonLabel"     code="admin.reports.reason.${report.reasonCode}" />
								<spring:message var="statusLabel"     code="admin.reports.status.${report.statusCode}" />

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
												<span class="report-card__reason-chip">
													<c:out value="${reasonLabel}" />
												</span>
											</div>
										</div>

										<dl class="report-card__body stack">
											<div class="report-section-field report-section-field__row">
												<dt class="detail-label"><spring:message code="admin.reports.reporter" /></dt>
												<dd><c:out value="${report.reporterUserId}" /></dd>
											</div>
											<div class="report-section-field report-section-field__row">
												<dt class="detail-label"><spring:message code="admin.reports.target" /></dt>
												<dd><c:out value="${report.targetKey}" /></dd>
											</div>
											<c:if test="${not empty report.details}">
												<div class="report-section-field report-section-field__row">
													<dt class="detail-label"><spring:message code="admin.reports.details" /></dt>
													<dd><c:out value="${report.details}" /></dd>
												</div>
											</c:if>
											<div class="report-section-field report-section-field__row">
												<dt class="detail-label"><spring:message code="admin.reports.createdAt" /></dt>
												<dd><c:out value="${report.createdAtLabel}" /></dd>
											</div>
											<c:if test="${not empty report.appealReason}">
												<div class="report-section-field report-section-field__row">
													<dt class="detail-label"><spring:message code="admin.reports.appeal" /></dt>
													<dd><c:out value="${report.appealReason}" /></dd>
												</div>
											</c:if>
										</dl>

										<div class="report-section-actions">
											<c:if test="${report.statusCode eq 'pending' or report.statusCode eq 'under_review'}">
												<c:url var="reviewHref" value="/admin/reports/${report.id}/under-review" />
												<form method="post" action="${reviewHref}" class="participant-manage-list__action-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="reviewLabel" code="admin.reports.action.review" />
													<ui:button label="${reviewLabel}" type="submit" variant="secondary" />
												</form>

												<!-- <c:url var="dismissHref" value="/admin/reports/${report.id}/dismiss" />
												<form method="post" action="${dismissHref}" class="participant-manage-list__action-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="dismissLabel" code="admin.reports.action.dismiss" />
													<ui:button label="${dismissLabel}" type="submit" variant="secondary" />
												</form> -->

												<c:if test="${report.targetTypeCode eq 'match' or report.targetTypeCode eq 'review'}">
													<c:url var="deleteContentHref" value="/admin/reports/${report.id}/delete-content" />
													<form method="post" action="${deleteContentHref}" class="participant-manage-list__action-form">
														<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
														<spring:message var="deleteContentLabel" code="admin.reports.action.deleteContent" />
														<ui:button label="${deleteContentLabel}" type="submit" variant="danger" />
													</form>
												</c:if>

												<c:if test="${report.targetTypeCode eq 'user'}">
													<c:url var="banUserHref" value="/admin/reports/${report.id}/ban-user" />
													<form method="post" action="${banUserHref}" class="participant-manage-list__action-form report-ban-form">
														<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
														<input type="number" name="banDays" min="1" max="365" value="7" class="field__control report-ban-form__days" />
														<spring:message var="banReasonPlaceholder" code="admin.reports.ban.reasonPlaceholder" />
														<input type="text" name="banReason" maxlength="2000" class="field__control report-ban-form__reason" placeholder="${banReasonPlaceholder}" />
														<spring:message var="banUserLabel" code="admin.reports.action.banUser" />
														<ui:button label="${banUserLabel}" type="submit" variant="danger" />
													</form>
												</c:if> -->
											</c:if>

											<c:url var="viewDetailHref" value="/admin/reports/${report.id}" />
											<spring:message var="viewDetailLabel" code="admin.reports.action.view" />
											<ui:button label="${viewDetailLabel}" href="${viewDetailHref}" variant="primary" />
										</div>

										<!-- <c:if test="${report.appealed}">
											<div class="report-appeal-panel panel">
												<p class="participation-empty-state">
													<spring:message code="admin.reports.appeal.pending" />
												</p>
												<c:url var="finalizeAppealHref" value="/admin/reports/${report.id}/finalize-appeal" />
												<form method="post" action="${finalizeAppealHref}" class="stack">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="appealResolutionLabel" code="admin.reports.appeal.resolution.label" />
													<label class="field" for="appeal-resolution-${report.id}">
														<span class="field__label"><c:out value="${appealResolutionLabel}" /></span>
														<select id="appeal-resolution-${report.id}" name="appealResolution" class="field__control field__control--select">
															<option value="dismissed">
																<spring:message code="admin.reports.appeal.resolution.dismissed" />
															</option>
															<option value="warning">
																<spring:message code="admin.reports.appeal.resolution.warning" />
															</option>
															<option value="content_deleted">
																<spring:message code="admin.reports.appeal.resolution.content_deleted" />
															</option>
															<option value="user_banned">
																<spring:message code="admin.reports.appeal.resolution.user_banned" />
															</option>
														</select>
													</label>
													<div>
														<spring:message var="finalizeAppealLabel" code="admin.reports.appeal.finalize" />
														<ui:button label="${finalizeAppealLabel}" type="submit" />
													</div>
												</form>
											</div>
										</c:if> -->

									</ui:card>
								</div>
							</c:forEach>
						</div>
					</c:otherwise>
				</c:choose>

			</main>
		</div>
	</body>
</html>
