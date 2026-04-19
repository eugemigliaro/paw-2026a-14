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

			<main class="page-shell account-shell">
				<section class="panel account-panel">
					<p class="eyebrow"><spring:message code="account.eyebrow" text="Account" /></p>
					<h1 class="page-heading__title account-panel__title"><c:out value="${accountTitle}" /></h1>
					<p class="page-heading__description account-panel__description">
						<c:out value="${accountDescription}" />
					</p>

					<div class="account-summary" role="list">
						<div class="account-summary__item" role="listitem">
							<span class="detail-label"><c:out value="${accountUsernameLabel}" /></span>
							<p class="account-summary__value"><c:out value="${username}" /></p>
						</div>
						<div class="account-summary__item" role="listitem">
							<span class="detail-label"><c:out value="${accountEmailLabel}" /></span>
							<p class="account-summary__value"><c:out value="${email}" /></p>
						</div>
					</div>

					<c:url var="logoutAction" value="/logout" />
					<form method="post" action="${logoutAction}" class="account-actions">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<ui:button label="${logoutLabel}" type="submit" variant="secondary" />
					</form>
				</section>
			</main>
		</div>
	</body>
</html>
