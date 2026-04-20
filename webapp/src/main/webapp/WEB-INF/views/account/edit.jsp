<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
					<p class="page-heading__description account-panel__description">
						<c:out value="${accountDescription}" />
					</p>

					<spring:message var="usernamePlaceholder" code="form.username.placeholder" />
					<spring:message var="namePlaceholder" code="form.name.placeholder" />
					<spring:message var="lastNamePlaceholder" code="form.lastName.placeholder" />
					<spring:message var="phonePlaceholder" code="form.phone.placeholder" />
					<spring:message var="profileImageLabel" code="account.profileImage.field" />
					<c:url var="accountAction" value="/account/edit" />
					<form:form
						method="post"
						action="${accountAction}"
						modelAttribute="accountProfileForm"
						cssClass="account-form"
						id="account-edit-form"
						data-account-edit-form="true"
						enctype="multipart/form-data">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<c:url var="accountProfileImageSrc" value="${accountProfileImageUrl}" />
						<label class="field field--required" for="account-email">
							<span class="field__label"><spring:message code="form.email.label" /></span>
							<div class="account-locked-field">
								<input
									id="account-email"
									type="email"
									class="field__control account-readonly-control account-locked-field__control"
									value="<c:out value='${accountEmail}' />"
									disabled="disabled"
									aria-disabled="true"
									readonly="readonly"
									aria-readonly="true"
									autocomplete="email" />
								<span class="account-locked-field__icon" aria-hidden="true">
									<svg viewBox="0 0 24 24" focusable="false">
										<path d="M8.5 10V8.25a3.5 3.5 0 1 1 7 0V10" />
										<rect x="6.5" y="10" width="11" height="9" rx="2.2" />
									</svg>
								</span>
							</div>
						</label>
						<label class="field" for="account-username">
							<span class="field__label"><spring:message code="form.username.label" /></span>
							<form:input
								path="username"
								id="account-username"
								cssClass="field__control"
								placeholder="${usernamePlaceholder}"
								required="required"
								autocomplete="username" />
							<form:errors path="username" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="account-name">
							<span class="field__label"><spring:message code="form.name.label" /></span>
							<form:input
								path="name"
								id="account-name"
								cssClass="field__control"
								placeholder="${namePlaceholder}"
								required="required"
								autocomplete="given-name" />
							<form:errors path="name" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="account-last-name">
							<span class="field__label"><spring:message code="form.lastName.label" /></span>
							<form:input
								path="lastName"
								id="account-last-name"
								cssClass="field__control"
								placeholder="${lastNamePlaceholder}"
								required="required"
								autocomplete="family-name" />
							<form:errors path="lastName" cssClass="field__error" element="span" />
						</label>
						<label class="field" for="account-phone">
							<span class="field__label"><spring:message code="form.phone.label" /></span>
							<form:input
								path="phone"
								id="account-phone"
								type="tel"
								cssClass="field__control"
								placeholder="${phonePlaceholder}"
								autocomplete="tel" />
							<form:errors path="phone" cssClass="field__error" element="span" />
						</label>
						<div class="field">
							<span class="field__label">
								<c:out value="${profileImageLabel}" />
							</span>
							<section class="account-profile-media account-profile-media--editable">
								<div class="account-profile-media__preview">
									<img
										class="account-profile-media__image"
										src="${accountProfileImageSrc}"
										alt="<c:out value='${accountProfileImageAlt}' />"
										loading="eager"
										decoding="async" />
									<div class="account-profile-media__copy">
										<h2 class="account-profile-media__title"><c:out value="${accountProfileImageTitle}" /></h2>
										<p class="account-profile-media__description">
											<c:out value="${accountProfileImageDescription}" />
										</p>
									</div>
								</div>

								<c:if test="${not empty accountProfileImageError}">
									<p class="auth-notice auth-notice--error"><c:out value="${accountProfileImageError}" /></p>
								</c:if>

								<label class="field" for="account-profile-image">
									<span class="field__label"><c:out value="${profileImageLabel}" /></span>
									<input
										id="account-profile-image"
										name="profileImage"
										type="file"
										class="field__control"
										accept="image/png,image/jpeg,image/webp,image/gif" />
								</label>
								<p class="auth-links__meta"><c:out value="${accountProfileImageHint}" /></p>
							</section>
						</div>
						<div class="account-actions">
							<ui:button
								label="${accountSaveLabel}"
								type="submit"
								id="account-save-button" />
							<c:url var="accountHref" value="/account" />
							<ui:button label="${accountCancelLabel}" href="${accountHref}" variant="secondary" />
						</div>
					</form:form>
				</section>
			</main>
		</div>
	</body>
</html>
