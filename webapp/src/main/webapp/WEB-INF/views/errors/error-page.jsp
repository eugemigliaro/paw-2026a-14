<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="${pageTitle}" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell error-shell">
				<section class="error-hero">
					<div class="error-hero__copy">
						<ui:returnButton />
						<p class="eyebrow"><c:out value="${eyebrow}" /></p>
						<header class="page-heading">
							<h1 class="page-heading__title"><c:out value="${title}" /></h1>
							<p class="page-heading__description">
								<c:out value="${description}" />
							</p>
						</header>

						<div class="error-hero__actions">
							<spring:message var="browseLabel" code="common.browseEvents" />
							<spring:message var="hostLabel" code="common.hostEvent" />
							<c:url var="browseHref" value="/" />
							<c:url var="hostHref" value="/host/matches/new" />
							<ui:button
								label="${browseLabel}"
								href="${browseHref}"
								size="lg" />
							<ui:button
								label="${hostLabel}"
								href="${hostHref}"
								variant="secondary"
								size="lg" />
						</div>
					</div>

					<div class="error-hero__display" aria-hidden="true">
						<strong class="error-hero__number"><c:out value="${number}" /></strong>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
