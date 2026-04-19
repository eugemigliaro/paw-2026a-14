<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.login" />
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
					<div class="auth-panel__main">
						<p class="eyebrow"><spring:message code="auth.login.eyebrow" /></p>
						<h1 class="page-heading__title auth-panel__title"><spring:message code="auth.login.title" /></h1>
						<p class="page-heading__description auth-panel__description">
							<spring:message code="auth.login.description" />
						</p>

						<c:if test="${verificationConfirmed}">
							<p class="auth-notice auth-notice--success">
								<spring:message code="auth.login.notice.verified" />
							</p>
						</c:if>
						<c:if test="${passwordResetCompleted}">
							<p class="auth-notice auth-notice--success">
								<spring:message code="auth.login.notice.reset" />
							</p>
						</c:if>
						<c:if test="${loggedOut}">
							<p class="auth-notice auth-notice--success">
								<spring:message code="auth.login.notice.logout" />
							</p>
						</c:if>
						<c:if test="${not empty loginError}">
							<p class="auth-notice auth-notice--error"><c:out value="${loginError}" /></p>
						</c:if>

						<spring:message var="emailLabel" code="form.email.label" />
						<spring:message var="emailPlaceholder" code="form.email.placeholder" />
						<spring:message var="passwordLabel" code="form.password.label" />
						<spring:message var="passwordPlaceholder" code="form.password.placeholder" />
						<spring:message var="showPasswordLabel" code="form.password.show" text="Show password" />
						<spring:message var="hidePasswordLabel" code="form.password.hide" text="Hide password" />
						<spring:message var="loginSubmitLabel" code="auth.login.submit" />
						<c:url var="forgotPasswordHref" value="/forgot-password" />
						<c:url var="registerHref" value="/register" />
						<c:url var="loginAction" value="/login" />
						<form method="post" action="${loginAction}" class="auth-form">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<ui:textInput
								label="${emailLabel}"
								name="email"
								type="email"
								value="${loginEmail}"
								placeholder="${emailPlaceholder}"
								required="${true}"
								autocomplete="email" />
							<ui:passwordInput
								label="${passwordLabel}"
								name="password"
								id="login-password"
								placeholder="${passwordPlaceholder}"
								required="${true}"
								autocomplete="current-password"
								showLabel="${showPasswordLabel}"
								hideLabel="${hidePasswordLabel}" />
							<div class="auth-form__support">
								<a class="auth-link auth-link--inline" href="${forgotPasswordHref}">
									<spring:message code="auth.login.forgotPassword" />
								</a>
							</div>
							<ui:button label="${loginSubmitLabel}" type="submit" fullWidth="${true}" />
						</form>

						<div class="auth-links">
							<p class="auth-links__meta">
								<spring:message code="auth.login.noAccount" />
								<a class="auth-link auth-link--strong" href="${registerHref}">
									<spring:message code="auth.login.createAccount" />
								</a>
							</p>
						</div>
					</div>

					<c:if test="${showResendVerification and not empty loginEmail}">
						<div class="auth-support">
							<h2 class="auth-support__title"><spring:message code="auth.login.resendVerification" /></h2>
							<p class="auth-support__copy">
								<spring:message code="auth.login.resendVerificationDescription" />
							</p>
							<spring:message var="resendVerificationLabel" code="auth.login.resendVerificationAction" />
							<c:url var="resendVerificationAction" value="/register/resend-verification" />
							<form method="post" action="${resendVerificationAction}" class="auth-support__form">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<input type="hidden" name="email" value="<c:out value='${loginEmail}' />" />
								<ui:button
									label="${resendVerificationLabel}"
									type="submit"
									variant="secondary"
									fullWidth="${true}" />
							</form>
						</div>
					</c:if>
				</section>
			</main>
		</div>
	</body>
</html>
