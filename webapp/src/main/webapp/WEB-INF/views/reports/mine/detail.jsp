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

	<main class="page-shell page-shell--matches-list">
		<header class="page-heading">
		<ui:returnButton />
		<h1 class="page-heading__title"><c:out value="${pageTitleLabel}" /></h1>
		<p class="page-heading__description"><c:out value="${pageDescription}" /></p>
		</header>

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

		<section class="panel participation-panel">
		<div class="participant-manage-list__info" style="display: grid; gap: 0.75rem;">
			<div><strong>#<c:out value="${report.id}" /></strong></div>
			<div>
			<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetTypeCode}" />
			<spring:message var="reasonLabel" code="admin.reports.reason.${report.reasonCode}" />
			<spring:message var="statusLabel" code="admin.reports.status.${report.statusCode}" />
			<span class="chip"><c:out value="${targetTypeLabel}" /></span>
			<span class="chip"><c:out value="${reasonLabel}" /></span>
			<span class="chip"><c:out value="${statusLabel}" /></span>
			</div>
			<div><spring:message code="reports.mine.target" />: <c:out value="${report.targetKey}" /></div>
			<div><spring:message code="admin.reports.details" />: <c:out value="${report.details}" /></div>
			<div><spring:message code="admin.reports.createdAt" />: <c:out value="${report.createdAtLabel}" /></div>
			<c:if test="${not empty report.updatedAtLabel}">
			<div><spring:message code="reports.mine.updatedAt" />: <c:out value="${report.updatedAtLabel}" /></div>
			</c:if>
			<c:if test="${not empty report.resolutionCode}">
			<spring:message var="resolutionLabel" code="reports.mine.resolution.${report.resolutionCode}" />
			<div><spring:message code="reports.mine.resolution" />: <c:out value="${resolutionLabel}" /></div>
			</c:if>
			<c:if test="${not empty report.resolutionDetails}">
			<div><spring:message code="reports.mine.resolutionDetails" />: <c:out value="${report.resolutionDetails}" /></div>
			</c:if>
			<c:if test="${not empty report.appealReason}">
			<div><spring:message code="reports.mine.appealReason" />: <c:out value="${report.appealReason}" /></div>
			</c:if>
			<c:if test="${not empty report.appealResolutionCode}">
			<spring:message var="appealResolutionLabel" code="reports.mine.resolution.${report.appealResolutionCode}" />
			<div><spring:message code="reports.mine.appealResolution" />: <c:out value="${appealResolutionLabel}" /></div>
			</c:if>
		</div>

		<c:if test="${appealAllowed}">
			<c:url var="appealAction" value="/reports/mine/${report.id}/appeal" />
			<form method="post" action="${appealAction}" class="stack" style="margin-top: 1rem;">
			<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
			<label class="field" for="appeal-reason">
				<span class="field__label"><spring:message code="reports.mine.appealReason" /></span>
				<textarea id="appeal-reason" name="appealReason" rows="4" maxlength="4000" class="field__control"></textarea>
			</label>
			<spring:message var="appealSubmitLabel" code="reports.mine.action.appeal" />
			<ui:button label="${appealSubmitLabel}" type="submit" />
			</form>
		</c:if>
		</section>
	</main>
	</div>
</body>
</html>
