<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
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

				<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetType.dbValue}" />
				<spring:message var="reasonLabel"     code="admin.reports.reason.${report.reason.dbValue}" />
				<spring:message var="statusLabel"     code="admin.reports.status.${report.status.dbValue}" />

				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="admin.reports.section.original" /></h2>
						<div class="report-section__badges">
							<span class="report-type-badge report-type-badge--${report.targetType.dbValue}">
								<c:out value="${targetTypeLabel}" />
							</span>
							<span class="report-status-badge report-status-badge--${report.status.dbValue}">
								<c:out value="${statusLabel}" />
							</span>
						</div>
					</div>

					<dl class="stack">
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.reporter" /></dt>
							<dd><c:out value="${reporterUsername}" /></dd>
						</div>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.target" /></dt>
							<dd>
								<c:choose>
									<c:when test="${targetSummary.found and targetSummary.targetType.dbValue eq 'review'}">
										<spring:message code="moderation.target.review.label" arguments="${targetSummary.displayName}" />
									</c:when>
									<c:when test="${targetSummary.found}">
										<c:out value="${targetSummary.displayName}" />
									</c:when>
									<c:otherwise>
										<spring:message code="moderation.target.${targetSummary.targetType.dbValue}.fallback" arguments="${targetSummary.targetId}" />
									</c:otherwise>
								</c:choose>
							</dd>
						</div>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="moderation.report.reason" /></dt>
							<dd><span class="report-card__reason-chip"><c:out value="${reasonLabel}" /></span></dd>
						</div>
						<c:if test="${not empty report.details}">
							<div class="report-section-field">
								<dt class="detail-label"><spring:message code="admin.reports.details" /></dt>
								<dd class="body-copy"><c:out value="${report.details}" /></dd>
							</div>
						</c:if>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.createdAt" /></dt>
							<dd><c:out value="${tf:dateTime(report.createdAtDateTime)}" /></dd>
						</div>
					</dl>
				</ui:card>

				<c:if test="${showResolution}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="admin.reports.section.resolution" /></h2>
						</div>

						<c:if test="${not empty report.resolution}">
							<spring:message var="resolutionLabel" code="reports.mine.resolution.${report.resolution.dbValue}" />
							<dl class="stack report-section__top">
								<div class="report-section-field report-section-field__row">
									<dt class="detail-label"><spring:message code="admin.reports.resolution" /></dt>
									<dd><c:out value="${resolutionLabel}" /></dd>
								</div>
								<c:if test="${not empty report.resolutionDetails}">
									<div class="report-section-field">
										<dt class="detail-label"><spring:message code="admin.reports.resolutionDetails" /></dt>
										<dd class="body-copy"><c:out value="${report.resolutionDetails}" /></dd>
									</div>
								</c:if>
								<c:if test="${not empty reviewerUsername}">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.reviewedBy" /></dt>
										<dd><c:out value="${reviewerUsername}" /></dd>
									</div>
								</c:if>
								<c:if test="${not empty report.reviewedAt}">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.reviewedAt" /></dt>
										<dd><c:out value="${tf:dateTime(report.reviewedAtDateTime)}" /></dd>
									</div>
								</c:if>
								<c:if test="${not empty userBan}">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.ban.until" /></dt>
										<dd><c:out value="${tf:dateTime(userBan.bannedUntilDateTime)}" /></dd>
									</div>
								</c:if>
							</dl>
						</c:if>

						<c:if test="${report.status.dbValue eq 'pending' or report.status.dbValue eq 'under_review'}">
							<div class="stack">
								<form:form method="post" modelAttribute="resolutionForm" cssClass="stack">
									<form:errors path="" cssClass="notice notice--error" element="div" />
									<div class="report-section-field">
										<label class="field" for="resolution-details-${report.id}">
											<span class="detail-label"><spring:message code="admin.reports.resolutionDetails" /></span>
											<form:textarea id="resolution-details-${report.id}" path="resolutionDetails" maxlength="4000" cssClass="field__control field__control--textarea" rows="3" />
											<form:errors path="resolutionDetails" cssClass="field__error" element="span" />
										</label>
									</div>

									<div class="report-resolution__actions">
										<c:if test="${report.targetType.dbValue eq 'match' or report.targetType.dbValue eq 'review'}">
											<c:url var="deleteContentHref" value="/admin/reports/${report.id}/delete-content" />
											<spring:message var="deleteContentLabel" code="admin.reports.action.deleteContent" />
												<ui:button label="${deleteContentLabel}" type="submit" variant="danger" submitAction="${deleteContentHref}" />
										</c:if>

										<c:if test="${report.targetType.dbValue eq 'user'}">
											<div class="report-section-field__row">
												<c:url var="banUserHref" value="/admin/reports/${report.id}/ban-user" />
												<spring:message var="banUserLabel" code="admin.reports.action.banUser" />
												<ui:button label="${banUserLabel}" type="submit" variant="danger" submitAction="${banUserHref}" />
												<label class="field" for="ban-days-${report.id}">
													<span class="detail-label"><spring:message code="admin.reports.ban.days" /></span>
													<form:input id="ban-days-${report.id}" path="banDays" cssClass="field__control field__control--ban-days" />
													<form:errors path="banDays" cssClass="field__error" element="span" />
												</label>
											</div>
										</c:if>

										<c:url var="dismissHref" value="/admin/reports/${report.id}/dismiss" />
										<spring:message var="dismissLabel" code="admin.reports.action.dismiss" />
											<ui:button label="${dismissLabel}" type="submit" variant="secondary" submitAction="${dismissHref}" />
									</div>
								</form:form>
							</div>
						</c:if>
					</ui:card>
				</c:if>

				<c:if test="${showAppeal}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="admin.reports.section.appeal" /></h2>
						</div>

						<c:choose>
							<c:when test="${not empty report.appealReason}">
								<dl class="stack report-section__top">
									<div class="report-section-field">
										<dt class="detail-label"><spring:message code="admin.reports.appeal" /></dt>
										<dd class="body-copy"><c:out value="${report.appealReason}" /></dd>
									</div>
									<c:if test="${not empty report.appealedAt}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="admin.reports.appeal.appealedAt" /></dt>
											<dd><c:out value="${tf:dateTime(report.appealedAtDateTime)}" /></dd>
										</div>
									</c:if>
								</dl>
							</c:when>
							<c:otherwise>
								<p class="report-section__top body-copy"><spring:message code="admin.reports.section.empty" /></p>
							</c:otherwise>
						</c:choose>
					</ui:card>
				</c:if>

				<c:if test="${showAppealResolution}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="admin.reports.section.appealResolution" /></h2>
						</div>

						<c:choose>
							<c:when test="${report.status.dbValue eq 'appealed'}">
								<c:url var="finalizeAppealHref" value="/admin/reports/${report.id}/finalize-appeal" />
								<form:form method="post" modelAttribute="appealResolutionForm" action="${finalizeAppealHref}" cssClass="stack report-section__top">
									<form:errors path="" cssClass="notice notice--error" element="div" />
									<spring:message var="appealResolutionLabel" code="admin.reports.appeal.resolution.label" />
									<div class="report-section-field">
										<label for="appeal-decision-${report.id}" class="detail-label"><c:out value="${appealResolutionLabel}" /></label>
										<div class="report-dropdown">
											<button type="button" class="report-dropdown__toggle">
												<span id="selected-appeal-decision-label"><spring:message code="admin.reports.appealDecision.upheld" /></span>
											</button>
											<div class="report-dropdown__panel">
												<button type="button" class="report-dropdown__item report-dropdown__item--active" data-value="upheld">
													<spring:message code="admin.reports.appealDecision.upheld" />
												</button>
												<button type="button" class="report-dropdown__item" data-value="lifted">
													<spring:message code="admin.reports.appealDecision.lifted" />
												</button>
											</div>
											<form:select id="appeal-decision-${report.id}" path="appealDecision" cssClass="field--hidden">
												<form:option value="upheld"><spring:message code="admin.reports.appealDecision.upheld" /></form:option>
												<form:option value="lifted"><spring:message code="admin.reports.appealDecision.lifted" /></form:option>
											</form:select>
										</div>
										<form:errors path="appealDecision" cssClass="field__error" element="span" />
									</div>
									<div class="report-section-actions">
										<spring:message var="finalizeAppealLabel" code="admin.reports.appeal.finalize" />
											<ui:button label="${finalizeAppealLabel}" type="submit" submitAction="${finalizeAppealHref}"/>
									</div>
								</form:form>
							</c:when>
							<c:when test="${not empty report.appealDecision}">
								<spring:message var="appealDecisionLabel" code="admin.reports.appealDecision.${report.appealDecision.dbValue}" />
								<dl class="stack report-section__top">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.appeal.resolution.label" /></dt>
										<dd><c:out value="${appealDecisionLabel}" /></dd>
									</div>
									<c:if test="${not empty report.appealResolvedBy}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="admin.reports.appealResolvedBy" /></dt>
											<dd><c:out value="${report.appealResolvedBy.username}" /></dd>
										</div>
									</c:if>
									<c:if test="${not empty report.appealResolvedAt}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="admin.reports.appealResolvedAt" /></dt>
											<dd><c:out value="${tf:dateTime(report.appealResolvedAtDateTime)}" /></dd>
										</div>
									</c:if>
								</dl>
							</c:when>
							<c:otherwise>
								<p class="report-section__top body-copy"><spring:message code="admin.reports.section.empty" /></p>
							</c:otherwise>
						</c:choose>
					</ui:card>
				</c:if>
			</main>
		</div>
	</body>
</html>
