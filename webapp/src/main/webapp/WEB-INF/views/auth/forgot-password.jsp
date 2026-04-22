<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.forgotPassword" />
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
					<p class="eyebrow"><spring:message code="auth.forgotPassword.eyebrow" /></p>
					<h1 class="page-heading__title auth-panel__title"><spring:message code="auth.forgotPassword.title" /></h1>
					<p class="page-heading__description auth-panel__description">
						<spring:message code="auth.forgotPassword.description" />
					</p>

					<spring:message var="emailPlaceholder" code="form.email.placeholder" />
					<spring:message var="forgotPasswordSubmitLabel" code="auth.forgotPassword.submit" />
					<c:url var="forgotPasswordAction" value="/forgot-password" />
					<form:form
						method="post"
						action="${forgotPasswordAction}"
						modelAttribute="forgotPasswordForm"
						cssClass="auth-form"
						novalidate="novalidate">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<label class="field" for="forgot-password-email">
							<span class="field__label"><spring:message code="form.email.label" /></span>
							<form:input
								path="email"
								id="forgot-password-email"
								type="email"
								cssClass="field__control"
								placeholder="${emailPlaceholder}"
								autocomplete="email" />
							<form:errors path="email" cssClass="field__error" element="span" />
						</label>
						<ui:button label="${forgotPasswordSubmitLabel}" type="submit" fullWidth="${true}" />
					</form:form>

					<div class="auth-links">
						<c:url var="loginHref" value="/login" />
						<a class="auth-link auth-link--strong" href="${loginHref}">
							<spring:message code="auth.forgotPassword.backToLogin" />
						</a>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
