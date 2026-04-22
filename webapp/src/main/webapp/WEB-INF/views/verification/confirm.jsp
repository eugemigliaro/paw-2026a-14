<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
	<spring:message var="pageTitle" code="page.title.verifyAction" />
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
			<p class="eyebrow"><spring:message code="verification.oneTime" /></p>
			<h1 class="page-heading__title"><c:out value="${preview.title}" /></h1>
			<p class="page-heading__description"><c:out value="${preview.summary}" /></p>
			<p class="muted-copy">
				<spring:message code="verification.requestedFor" /> <strong><c:out value="${preview.email}" /></strong>.
			</p>

			<div class="verification-details">
				<c:forEach var="detail" items="${preview.details}">
					<div class="verification-details__row">
						<span><c:out value="${detail.label}" /></span>
						<strong><c:out value="${detail.value}" /></strong>
					</div>
				</c:forEach>
			</div>

			<p class="muted-copy"><spring:message code="verification.expiresOnShort" arguments="${expiresAtLabel}" /></p>

			<c:url var="confirmAction" value="${confirmPath}" />
			<form method="post" action="${confirmAction}">
				<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
				<ui:button label="${preview.confirmLabel}" type="submit" fullWidth="${true}" />
			</form>
		</section>
	</main>
	</div>
</body>
</html>
