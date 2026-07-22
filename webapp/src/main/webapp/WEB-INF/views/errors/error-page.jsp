<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<spring:message var="pageTitle" code="page.title.${number}" />
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
						<p class="eyebrow"><spring:message code="error.${number}.eyebrow" /></p>
						<header class="page-heading">
							<h1 class="page-heading__title"><spring:message code="error.${number}.title" /></h1>
							<p class="page-heading__description">
								<spring:message code="error.${number}.description" />
							</p>
						</header>

						<div class="error-hero__actions">
							<spring:message var="browseLabel" code="common.browseEvents" />
							<c:url var="browseHref" value="/" />
							<ui:button
							label="${browseLabel}"
							href="${browseHref}"
							size="lg" />
							<sec:authorize access="!isAnonymous()">
								<spring:message var="hostLabel" code="common.hostEvent" />
								<c:url var="hostHref" value="/matches/new" />
								<ui:button
									label="${hostLabel}"
									href="${hostHref}"
									variant="secondary"
									size="lg" />
							</sec:authorize>
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
