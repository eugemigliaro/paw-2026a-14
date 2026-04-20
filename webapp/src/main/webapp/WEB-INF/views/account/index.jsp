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
					<h1 class="page-heading__title account-panel__title"><c:out value="${accountTitle}" /></h1>
					<div class="account-panel__header">
						<p class="page-heading__description account-panel__description">
							<c:out value="${accountDescription}" />
						</p>
						<div class="account-actions account-actions--top">
							<c:url var="accountEditHref" value="/account/edit" />
							<ui:button label="${accountEditLabel}" href="${accountEditHref}" variant="secondary" />
						</div>
					</div>
					<c:if test="${not empty accountUpdated}">
						<p class="auth-notice auth-notice--success"><c:out value="${accountUpdated}" /></p>
					</c:if>
					<spring:message code="account.phone.empty" var="accountPhoneEmptyLabel" />
					<c:set var="accountPhoneValue" value="${empty accountProfile.phone ? accountPhoneEmptyLabel : accountProfile.phone}" />

					<div class="account-summary">
						<label class="field" for="account-email">
							<span class="field__label"><spring:message code="form.email.label" /></span>
							<input
								id="account-email"
								type="email"
								class="field__control account-readonly-control"
								value="${accountProfile.email}"
								readonly="readonly"
								aria-readonly="true" />
						</label>
						<label class="field" for="account-username">
							<span class="field__label"><spring:message code="form.username.label" /></span>
							<input
								id="account-username"
								type="text"
								class="field__control account-readonly-control"
								value="${accountProfile.username}"
								readonly="readonly"
								aria-readonly="true" />
						</label>
						<label class="field" for="account-name">
							<span class="field__label"><spring:message code="form.name.label" /></span>
							<input
								id="account-name"
								type="text"
								class="field__control account-readonly-control"
								value="${accountProfile.name}"
								readonly="readonly"
								aria-readonly="true" />
						</label>
						<label class="field" for="account-last-name">
							<span class="field__label"><spring:message code="form.lastName.label" /></span>
							<input
								id="account-last-name"
								type="text"
								class="field__control account-readonly-control"
								value="${accountProfile.lastName}"
								readonly="readonly"
								aria-readonly="true" />
						</label>
						<label class="field" for="account-phone">
							<span class="field__label"><spring:message code="form.phone.label" /></span>
							<input
								id="account-phone"
								type="tel"
								class="field__control account-readonly-control"
								value="${accountPhoneValue}"
								readonly="readonly"
								aria-readonly="true" />
						</label>
					</div>

					<c:url var="logoutAction" value="/logout" />
					<form method="post" action="${logoutAction}" class="account-logout">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<ui:button label="${logoutLabel}" type="submit" variant="secondary" />
					</form>
				</section>
			</main>
		</div>
	</body>
</html>
