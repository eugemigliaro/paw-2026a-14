<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
	<spring:message var="pageTitle" code="page.title.verificationError" />
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
			<p class="eyebrow"><spring:message code="verification.unavailable" /></p>
			<h1 class="page-heading__title"><c:out value="${title}" /></h1>
			<p class="page-heading__description"><c:out value="${message}" /></p>
			<div class="verification-actions">
				<spring:message var="browseEventsLabel" code="common.browseEvents" />
				<c:url var="backUrl" value="${backHref}" />
				<ui:button href="${backUrl}" label="${browseEventsLabel}" />
			</div>
		</section>
	</main>
	</div>
</body>
</html>
