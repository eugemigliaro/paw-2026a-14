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
				<ui:returnButton />
				<header class="page-heading">
					<h1 class="page-heading__title"><c:out value="${pageTitleLabel}" /></h1>
					<p class="page-heading__description"><c:out value="${pageDescription}" /></p>
				</header>

				<%-- Flash notices --%>
				<c:if test="${param.action eq 'appealed'}">
					<div class="notice notice--success"><spring:message code="reports.mine.action.appealed" /></div>
				</c:if>
				<c:if test="${not empty param.error}">
					<div class="notice notice--error">
						<c:choose>
							<c:when test="${param.error eq 'appeal_limit'}"><spring:message code="reports.mine.error.appeal_limit" /></c:when>
							<c:when test="${param.error eq 'appeal_rejected'}"><spring:message code="reports.mine.error.appeal_rejected" /></c:when>
							<c:otherwise><spring:message code="reports.mine.error.generic" /></c:otherwise>
						</c:choose>
					</div>
				</c:if>

				<%-- Report summary card --%>
				<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetTypeCode}" />
				<spring:message var="reasonLabel" code="admin.reports.reason.${report.reasonCode}" />
				<spring:message var="statusLabel" code="admin.reports.status.${report.statusCode}" />

				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="report.page.summary.title" /></h2>
						<div class="report-section__badges">
							<span class="report-type-badge report-type-badge--${report.targetTypeCode}">
								<c:out value="${targetTypeLabel}" />
							</span>
							<span class="report-status-badge report-status-badge--${report.statusCode}">
								<c:out value="${statusLabel}" />
							</span>
						</div>
					</div>

					<dl class="stack report-section__top">
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="reports.mine.target" /></dt>
							<dd><c:out value="${report.targetKey}" /></dd>
						</div>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="moderation.report.reason" /></dt>
							<dd><span class="chip chip--muted"><c:out value="${reasonLabel}" /></span></dd>
						</div>
						<c:if test="${not empty report.details}">
						<div class="report-section-field">
							<dt class="detail-label"><spring:message code="admin.reports.details" /></dt>
							<dd class="body-copy"><c:out value="${report.details}" /></dd>
						</div>
						</c:if>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.createdAt" /></dt>
							<dd><c:out value="${report.createdAtLabel}" /></dd>
						</div>
						<c:if test="${not empty report.updatedAtLabel}">
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="reports.mine.updatedAt" /></dt>
							<dd><c:out value="${report.updatedAtLabel}" /></dd>
						</div>
						</c:if>
					</dl>
				</ui:card>

				<%-- Resolution card --%>
				<c:if test="${not empty report.resolutionCode}">
					<spring:message var="resolutionLabel" code="reports.mine.resolution.${report.resolutionCode}" />
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="reports.mine.resolution" /></h2>
						</div>
						<dl class="stack report-section__top">
							<div class="report-section-field report-section-field__row">
								<dt class="detail-label"><spring:message code="reports.mine.resolution" /></dt>
								<dd><c:out value="${resolutionLabel}" /></dd>
							</div>
							<c:if test="${not empty report.resolutionDetails}">
							<div class="report-section-field">
								<dt class="detail-label"><spring:message code="reports.mine.resolutionDetails" /></dt>
								<dd class="body-copy"><c:out value="${report.resolutionDetails}" /></dd>
							</div>
							</c:if>
							<c:if test="${not empty report.reviewedAtLabel}">
							<div class="report-section-field report-section-field__row">
								<dt class="detail-label"><spring:message code="reports.mine.updatedAt" /></dt>
								<dd><c:out value="${report.reviewedAtLabel}" /></dd>
							</div>
							</c:if>
						</dl>
					</ui:card>
				</c:if>

				<%-- Appeal card --%>
				<c:if test="${not empty report.appealReason}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="reports.mine.appealReason" /></h2>
						</div>
						<dl class="stack report-section__top">
							<div class="report-section-field">
								<dt class="detail-label"><spring:message code="reports.mine.appealReason" /></dt>
								<dd class="body-copy"><c:out value="${report.appealReason}" /></dd>
							</div>
							<c:if test="${not empty report.appealedAtLabel}">
							<div class="report-section-field report-section-field__row">
								<dt class="detail-label"><spring:message code="admin.reports.createdAt" /></dt>
								<dd><c:out value="${report.appealedAtLabel}" /></dd>
							</div>
							</c:if>
							<c:if test="${not empty report.appealResolutionCode}">
								<spring:message var="appealResolutionLabel" code="reports.mine.resolution.${report.appealResolutionCode}" />
								<div class="report-section-field report-section-field__row">
									<dt class="detail-label"><spring:message code="reports.mine.appealResolution" /></dt>
									<dd><c:out value="${appealResolutionLabel}" /></dd>
								</div>
							</c:if>
							<c:if test="${not empty report.appealResolvedAtLabel}">
							<div class="report-section-field report-section-field__row">
								<dt class="detail-label"><spring:message code="reports.mine.updatedAt" /></dt>
								<dd><c:out value="${report.appealResolvedAtLabel}" /></dd>
							</div>
							</c:if>
						</dl>
					</ui:card>
				</c:if>

				<%-- Appeal form card --%>
				<c:if test="${appealAllowed}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 id="appeal-form-title" class="field__label"><spring:message code="reports.mine.appealReason" /></h2>
						</div>
						<c:url var="appealAction" value="/reports/mine/${report.id}/appeal" />
						<form method="post" action="${appealAction}" class="report-section__top">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<div class="report-section-field">
								<label for="appeal-reason" class="detail-label">
									<spring:message code="reports.mine.appealReason" />
								</label>
								<textarea
									id="appeal-reason"
									name="appealReason"
									rows="4"
									maxlength="4000"
									class="field__control field__control--textarea"></textarea>
							</div>
							<div class="report-section-actions">
								<spring:message var="appealSubmitLabel" code="reports.mine.action.appeal" />
								<ui:button label="${appealSubmitLabel}" type="submit" />
							</div>
						</form>
					</ui:card>
				</c:if>
			</main>
		</div>
	</body>
</html>
