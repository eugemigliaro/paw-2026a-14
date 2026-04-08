<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | 404" />
<!DOCTYPE html>
<html lang="en">
<head>
	<%@ include file="/WEB-INF/views/includes/head.jspf" %>
</head>
<body>
	<div class="app-shell">
	<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

	<main class="page-shell error-shell">
		<section class="error-hero">
			<div class="error-hero__copy">
				<p class="eyebrow">Route missing</p>
				<header class="page-heading">
					<h1 class="page-heading__title">Page not found.</h1>
					<p class="page-heading__description">
						The link you opened does not point to an active Match Point page.
						Go back to event discovery or switch into host mode to keep moving.
					</p>
				</header>

				<div class="error-hero__actions">
					<ui:button
						label="Browse events"
						href="${pageContext.request.contextPath}/"
						size="lg" />
					<ui:button
						label="Host an event"
						href="${pageContext.request.contextPath}/host/events/new"
						variant="secondary"
						size="lg" />
				</div>
			</div>

			<div class="error-hero__display" aria-hidden="true">
				<strong class="error-hero__number">404</strong>
			</div>
		</section>
	</main>
	</div>
</body>
</html>
