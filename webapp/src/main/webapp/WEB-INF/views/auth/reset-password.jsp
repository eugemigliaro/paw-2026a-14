<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.resetPassword" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell auth-shell">
				<section class="panel auth-panel">
					<p class="eyebrow"><spring:message code="auth.resetPassword.eyebrow" /></p>
					<h1 class="page-heading__title auth-panel__title"><spring:message code="auth.resetPassword.title" /></h1>
					<p class="page-heading__description auth-panel__description">
						<spring:message code="auth.resetPassword.description" />
					</p>

					<div class="auth-summary">
						<p class="auth-summary__item">
							<spring:message code="auth.resetPassword.requestedFor" arguments="${resetPreview.email}" />
						</p>
						<p class="auth-summary__item">
							<spring:message code="auth.resetPassword.expiresOn" arguments="${expiresAtLabel}" />
						</p>
					</div>

					<spring:message var="newPasswordPlaceholder" code="form.password.newPlaceholder" />
					<spring:message var="confirmPasswordPlaceholder" code="form.confirmPassword.placeholder" />
					<spring:message var="resetPasswordSubmitLabel" code="auth.resetPassword.submit" />
					<c:url var="resetPasswordAction" value="${resetPath}" />
					<form:form
						method="post"
						action="${resetPasswordAction}"
						modelAttribute="resetPasswordForm"
						cssClass="auth-form">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<label class="field" for="reset-password">
							<span class="field__label"><spring:message code="form.password.label" /></span>
							<form:password
								path="password"
								id="reset-password"
								cssClass="field__control"
								placeholder="${newPasswordPlaceholder}"
								autocomplete="new-password" />
							<form:errors path="password" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="reset-confirm-password">
							<span class="field__label"><spring:message code="form.confirmPassword.label" /></span>
							<form:password
								path="confirmPassword"
								id="reset-confirm-password"
								cssClass="field__control"
								placeholder="${confirmPasswordPlaceholder}"
								autocomplete="new-password" />
							<form:errors path="confirmPassword" cssClass="field__error" element="span" />
						</label>
						<ui:button label="${resetPasswordSubmitLabel}" type="submit" fullWidth="${true}" />
					</form:form>

					<div class="auth-links">
						<c:url var="loginHref" value="/login" />
						<a class="auth-link auth-link--strong" href="${loginHref}">
							<spring:message code="auth.backToLogin" />
						</a>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
