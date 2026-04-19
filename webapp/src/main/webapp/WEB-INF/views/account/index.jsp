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

					<div class="account-summary">
						<label class="field" for="account-username">
							<span class="field__label"><c:out value="${accountUsernameLabel}" /></span>
							<div class="account-locked-field">
								<input
									id="account-username"
									type="text"
									class="field__control account-locked-field__control"
									value="<c:out value='${username}' />"
									readonly="readonly"
									aria-readonly="true" />
								<span class="account-locked-field__icon" aria-hidden="true">
									<svg viewBox="0 0 24 24" focusable="false">
										<path d="M8.5 10V8.25a3.5 3.5 0 1 1 7 0V10" />
										<rect x="6.5" y="10" width="11" height="9" rx="2.2" />
									</svg>
								</span>
							</div>
						</label>
						<label class="field" for="account-email">
							<span class="field__label"><c:out value="${accountEmailLabel}" /></span>
							<div class="account-locked-field">
								<input
									id="account-email"
									type="text"
									class="field__control account-locked-field__control"
									value="<c:out value='${email}' />"
									readonly="readonly"
									aria-readonly="true" />
								<span class="account-locked-field__icon" aria-hidden="true">
									<svg viewBox="0 0 24 24" focusable="false">
										<path d="M8.5 10V8.25a3.5 3.5 0 1 1 7 0V10" />
										<rect x="6.5" y="10" width="11" height="9" rx="2.2" />
									</svg>
								</span>
							</div>
						</label>
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
