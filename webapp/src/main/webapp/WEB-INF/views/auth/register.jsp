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
					<spring:message var="namePlaceholder" code="form.name.placeholder" />
					<spring:message var="lastNamePlaceholder" code="form.lastName.placeholder" />
					<spring:message var="phonePlaceholder" code="form.phone.placeholder" />
					<spring:message var="passwordLabel" code="form.password.label" />
					<spring:message var="passwordPlaceholder" code="form.password.placeholder" />
					<spring:message var="confirmPasswordLabel" code="form.confirmPassword.label" />
					<spring:message var="confirmPasswordPlaceholder" code="form.confirmPassword.placeholder" />
					<spring:message var="passwordRequirement" code="RegisterForm.password.Size" />
					<spring:message var="showPasswordLabel" code="form.password.show" text="Show password" />
					<spring:message var="hidePasswordLabel" code="form.password.hide" text="Hide password" />
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
								required="required"
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
								required="required"
								autocomplete="username" />
							<form:errors path="username" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="register-name">
							<span class="field__label"><spring:message code="form.name.label" /></span>
							<form:input
								path="name"
								id="register-name"
								cssClass="field__control"
								placeholder="${namePlaceholder}"
								required="required"
								autocomplete="given-name" />
							<form:errors path="name" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="register-last-name">
							<span class="field__label"><spring:message code="form.lastName.label" /></span>
							<form:input
								path="lastName"
								id="register-last-name"
								cssClass="field__control"
								placeholder="${lastNamePlaceholder}"
								required="required"
								autocomplete="family-name" />
							<form:errors path="lastName" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="register-phone">
							<span class="field__label"><spring:message code="form.phone.label" /></span>
							<form:input
								path="phone"
								id="register-phone"
								type="tel"
								cssClass="field__control"
								placeholder="${phonePlaceholder}"
								autocomplete="tel" />
							<form:errors path="phone" cssClass="field__error" element="span" />
						</label>
						<ui:passwordInput
							label="${passwordLabel}"
							path="password"
							id="register-password"
							placeholder="${passwordPlaceholder}"
							required="${true}"
							autocomplete="new-password"
							hint="${passwordRequirement}"
							showLabel="${showPasswordLabel}"
							hideLabel="${hidePasswordLabel}" />
						<ui:passwordInput
							label="${confirmPasswordLabel}"
							path="confirmPassword"
							id="register-confirm-password"
							placeholder="${confirmPasswordPlaceholder}"
							required="${true}"
							autocomplete="new-password"
							showLabel="${showPasswordLabel}"
							hideLabel="${hidePasswordLabel}" />
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
