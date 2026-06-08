<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
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
			<p class="eyebrow"><c:out value="${empty eyebrow ? defaultEyebrow : eyebrow}" /></p>
			<h1 class="page-heading__title"><c:out value="${title}" /></h1>
			<p class="page-heading__description"><c:out value="${summary}" /></p>
			<c:if test="${not empty expiresAtLabel}">
				<p class="muted-copy">
					<spring:message code="verification.expiresOn" arguments="${expiresAtLabel}" />
				</p>
			</c:if>
		</section>
	</main>
	</div>
</body>
</html>
