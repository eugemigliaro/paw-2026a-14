<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
	<spring:message var="pageTitle" code="page.title.checkEmail" />
	<!DOCTYPE html>
	<html lang="${pageContext.response.locale.language}">
<head>
	<%@ include file="/WEB-INF/views/includes/head.jspf" %>
</head>
<body>
	<div class="app-shell">
	<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

	<main class="page-shell verification-shell">
		<section class="panel verification-panel">
			<spring:message var="defaultEyebrow" code="verification.actionRequested" />
			<spring:message var="eyebrow" code="${eyebrowCode}" text="${defaultEyebrow}" />
			<spring:message var="title" code="verification.checkEmail" />
			<spring:message var="summary" code="${summaryCode}" arguments="${summaryArguments}" />
			<p class="eyebrow"><c:out value="${eyebrow}" /></p>
			<h1 class="page-heading__title"><c:out value="${title}" /></h1>
			<p class="page-heading__description"><c:out value="${summary}" /></p>
			<c:if test="${not empty expiresAtLabel}">
				<p class="muted-copy">
					<spring:message code="verification.expiresOn" arguments="${expiresAtLabel}" />
				</p>
			</c:if>
			<div class="verification-actions">
				<c:url var="backUrl" value="${backHref}" />
				<c:if test="${not empty actionLabelCode}">
					<spring:message var="actionLabel" code="${actionLabelCode}" />
					<ui:button href="${backUrl}" label="${actionLabel}" />
				</c:if>
			</div>
		</section>
	</main>
	</div>
</body>
</html>
