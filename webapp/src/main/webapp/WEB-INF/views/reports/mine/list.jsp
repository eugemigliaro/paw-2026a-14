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
		<h1 class="page-heading__title"><c:out value="${pageTitleLabel}" /></h1>
		<p class="page-heading__description"><c:out value="${pageDescription}" /></p>
		</header>

		<section class="panel participation-panel">
		<strong><c:out value="${reportCountLabel}" /></strong>
		<c:choose>
			<c:when test="${empty reports}">
			<p class="participation-empty-state"><c:out value="${emptyMessage}" /></p>
			</c:when>
			<c:otherwise>
			<ul class="participant-manage-list">
				<c:forEach var="report" items="${reports}">
				<li class="participant-manage-list__item">
					<div class="participant-manage-list__info" style="display: grid; gap: 0.5rem;">
					<div><strong>#<c:out value="${report.id}" /></strong></div>
					<div>
						<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetTypeCode}" />
						<spring:message var="reasonLabel" code="admin.reports.reason.${report.reasonCode}" />
						<spring:message var="statusLabel" code="admin.reports.status.${report.statusCode}" />
						<span class="chip"><c:out value="${targetTypeLabel}" /></span>
						<span class="chip"><c:out value="${reasonLabel}" /></span>
						<span class="chip"><c:out value="${statusLabel}" /></span>
					</div>
					<div>
						<spring:message code="reports.mine.target" />: <c:out value="${report.targetKey}" />
					</div>
					<div>
						<spring:message code="admin.reports.createdAt" />: <c:out value="${report.createdAtLabel}" />
					</div>
					</div>
					<div class="participant-manage-list__actions">
					<c:url var="reportDetailHref" value="/reports/mine/${report.id}" />
					<spring:message var="reportDetailLabel" code="reports.mine.action.view" />
					<ui:button label="${reportDetailLabel}" href="${reportDetailHref}" variant="secondary" />
					</div>
				</li>
				</c:forEach>
			</ul>
			</c:otherwise>
		</c:choose>
		</section>
	</main>
	</div>
</body>
</html>
