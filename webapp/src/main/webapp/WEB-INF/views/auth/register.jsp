<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.register" />
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
					<p class="eyebrow"><spring:message code="auth.register.eyebrow" /></p>
					<h1 class="page-heading__title auth-panel__title"><spring:message code="auth.register.title" /></h1>
					<p class="page-heading__description auth-panel__description">
						<spring:message code="auth.register.description" />
					</p>

					<spring:message var="emailPlaceholder" code="form.email.placeholder" />
					<spring:message var="usernamePlaceholder" code="form.username.placeholder" />
					<spring:message var="passwordPlaceholder" code="form.password.placeholder" />
					<spring:message var="confirmPasswordPlaceholder" code="form.confirmPassword.placeholder" />
					<spring:message var="registerSubmitLabel" code="auth.register.submit" />
					<c:url var="registerAction" value="/register" />
					<form:form method="post" action="${registerAction}" modelAttribute="registerForm" cssClass="auth-form">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<label class="field" for="register-email">
							<span class="field__label"><spring:message code="form.email.label" /></span>
							<form:input
								path="email"
								id="register-email"
								type="email"
								cssClass="field__control"
								placeholder="${emailPlaceholder}"
								autocomplete="email" />
							<form:errors path="email" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="register-username">
							<span class="field__label"><spring:message code="form.username.label" /></span>
							<form:input
								path="username"
								id="register-username"
								cssClass="field__control"
								placeholder="${usernamePlaceholder}"
								autocomplete="username" />
							<form:errors path="username" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="register-password">
							<span class="field__label"><spring:message code="form.password.label" /></span>
							<form:password
								path="password"
								id="register-password"
								cssClass="field__control"
								placeholder="${passwordPlaceholder}"
								autocomplete="new-password" />
							<form:errors path="password" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="register-confirm-password">
							<span class="field__label"><spring:message code="form.confirmPassword.label" /></span>
							<form:password
								path="confirmPassword"
								id="register-confirm-password"
								cssClass="field__control"
								placeholder="${confirmPasswordPlaceholder}"
								autocomplete="new-password" />
							<form:errors path="confirmPassword" cssClass="field__error" element="span" />
						</label>
						<ui:button label="${registerSubmitLabel}" type="submit" fullWidth="${true}" />
					</form:form>

					<div class="auth-links">
						<c:url var="loginHref" value="/login" />
						<p class="auth-links__meta">
							<spring:message code="auth.register.haveAccount" />
							<a class="auth-link auth-link--strong" href="${loginHref}">
								<spring:message code="auth.register.signIn" />
							</a>
						</p>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
