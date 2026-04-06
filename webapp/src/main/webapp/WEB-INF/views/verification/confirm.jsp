<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | Verify Action" />
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
			<p class="eyebrow">One-time verification</p>
			<h1 class="page-heading__title"><c:out value="${preview.title}" /></h1>
			<p class="page-heading__description"><c:out value="${preview.summary}" /></p>
			<p class="muted-copy">
				This link was requested for <strong><c:out value="${preview.email}" /></strong>.
			</p>

			<div class="verification-details">
				<c:forEach var="detail" items="${preview.details}">
					<div class="verification-details__row">
						<span><c:out value="${detail.label}" /></span>
						<strong><c:out value="${detail.value}" /></strong>
					</div>
				</c:forEach>
			</div>

			<p class="muted-copy">Expires on <c:out value="${expiresAtLabel}" />.</p>

			<form method="post" action="${pageContext.request.contextPath}${confirmPath}">
				<ui:button label="${preview.confirmLabel}" type="submit" fullWidth="${true}" />
			</form>
		</section>
	</main>
	</div>
</body>
</html>
