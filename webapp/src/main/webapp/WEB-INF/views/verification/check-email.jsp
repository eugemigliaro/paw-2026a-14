<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | Check Email" />
<!DOCTYPE html>
<html lang="en">
<head>
	<%@ include file="/WEB-INF/views/includes/head.jspf" %>
</head>
<body>
	<div class="app-shell">
	<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

	<main class="page-shell verification-shell">
		<section class="panel verification-panel">
			<p class="eyebrow"><c:out value="${empty eyebrow ? 'Action requested' : eyebrow}" /></p>
			<h1 class="page-heading__title"><c:out value="${title}" /></h1>
			<p class="page-heading__description"><c:out value="${summary}" /></p>
			<p class="muted-copy">
				The confirmation link expires on <c:out value="${expiresAtLabel}" />.
			</p>
			<div class="verification-actions">
				<ui:button
					href="${pageContext.request.contextPath}${backHref}"
					label="${empty actionLabel ? 'Back' : actionLabel}" />
			</div>
		</section>
	</main>
	</div>
</body>
</html>
