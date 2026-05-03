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
				<div class="public-profile-topbar">
					<ui:returnButton />
					<c:if test="${not empty accountPublicProfileHref}">
						<c:url var="accountPublicProfileAction" value="${accountPublicProfileHref}" />
						<ui:button label="${accountPublicProfileLabel}" href="${accountPublicProfileAction}" variant="secondary" />
					</c:if>
				</div>

				<c:if test="${not empty accountUpdated}">
					<p class="auth-notice auth-notice--success">
						<c:out value="${accountUpdated}" />
					</p>
				</c:if>

				<spring:message var="usernamePlaceholder" code="form.username.placeholder" />
				<spring:message var="namePlaceholder" code="form.name.placeholder" />
				<spring:message var="lastNamePlaceholder" code="form.lastName.placeholder" />
				<spring:message var="phonePlaceholder" code="form.phone.placeholder" />
				<spring:message var="profileImageChangeLabel" code="account.profileImage.change" />
				<c:url var="accountAction" value="/account/edit" />
				<c:url var="accountProfileImageSrc" value="${accountProfileImageUrl}" />

				<c:url var="logoutAction" value="/logout" />
				<form id="logout-form" method="post" action="${logoutAction}" style="display: none;">
					<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
				</form>

				<form:form method="post" action="${accountAction}" modelAttribute="accountProfileForm"
					cssClass="account-inline-layout" id="account-edit-form" enctype="multipart/form-data"
					novalidate="novalidate">
					<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

					<div class="account-inline-left">
						<div class="account-profile-card-stack">
							<div class="field account-profile-current-field">
								<span class="field__label account-profile-media__label">
									<c:out value="${accountProfileImageTitle}" />
								</span>
								<section class="account-profile-media account-profile-media--current">
									<div class="account-profile-media__preview">
										<img class="account-profile-media__image" src="${accountProfileImageSrc}"
											alt="<c:out value='${accountProfileImageAlt}' />" loading="eager"
											decoding="async" />
									</div>
								</section>
							</div>

							<c:if test="${not empty accountProfileImageError}">
								<p class="auth-notice auth-notice--error">
									<c:out value="${accountProfileImageError}" />
								</p>
							</c:if>

							<label class="account-profile-media account-profile-media--change" for="account-profile-image">
								<span class="account-profile-media__change-content">
									<span class="upload-card__dropzone-icon" aria-hidden="true">
										<svg viewBox="0 0 24 24" focusable="false">
											<path d="M4 17.5V6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v11a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 17.5Z" />
											<path d="m8 14 2.2-2.2a1.2 1.2 0 0 1 1.7 0L16 16" />
											<path d="m13.5 13.5 1.1-1.1a1.2 1.2 0 0 1 1.7 0L20 16" />
											<circle cx="8.5" cy="8.5" r="1.2" />
										</svg>
									</span>
									<span class="upload-card__dropzone-copy">
										<span class="upload-card__dropzone-title">
											<c:out value="${profileImageChangeLabel}" />
										</span>
										<span class="field__hint">
											<c:out value="${accountProfileImageHint}" />
										</span>
									</span>
									<input id="account-profile-image" name="profileImage" type="file"
										class="upload-card__file-input" accept="image/png,image/jpeg,image/webp,image/gif" />
								</span>
							</label>
						</div>


					</div>

					<div class="account-inline-right">
						<div class="account-form">
							<label class="field" for="account-email">
								<span class="field__label">
									<spring:message code="form.email.label" />
								</span>
								<div class="account-locked-field">
									<input id="account-email" type="email"
										class="field__control account-readonly-control account-locked-field__control account-readonly-control--muted"
										value="<c:out value='${accountEmail}' />" disabled="disabled"
										aria-disabled="true" readonly="readonly" aria-readonly="true"
										autocomplete="email" />
									<span class="account-locked-field__icon" aria-hidden="true">
										<svg viewBox="0 0 24 24" focusable="false">
											<path d="M8.5 10V8.25a3.5 3.5 0 1 1 7 0V10" />
											<rect x="6.5" y="10" width="11" height="9" rx="2.2" />
										</svg>
									</span>
								</div>
							</label>

							<div class="account-summary__name-row">
								<label class="field" for="account-name">
									<span class="field__label">
										<spring:message code="form.name.label" />
									</span>
									<form:input path="name" id="account-name"
										cssClass="field__control account-readonly-control account-field--editable"
										placeholder="${namePlaceholder}" required="required"
										autocomplete="given-name" readonly="true" />
									<form:errors path="name" cssClass="field__error" element="span" />
								</label>
								<label class="field" for="account-last-name">
									<span class="field__label">
										<spring:message code="form.lastName.label" />
									</span>
									<form:input path="lastName" id="account-last-name"
										cssClass="field__control account-readonly-control account-field--editable"
										placeholder="${lastNamePlaceholder}" required="required"
										autocomplete="family-name" readonly="true" />
									<form:errors path="lastName" cssClass="field__error" element="span" />
								</label>
							</div>

							<label class="field" for="account-username">
								<span class="field__label">
									<spring:message code="form.username.label" />
								</span>
								<form:input path="username" id="account-username"
									cssClass="field__control account-readonly-control account-field--editable"
									placeholder="${usernamePlaceholder}" required="required"
									autocomplete="username" readonly="true" />
								<form:errors path="username" cssClass="field__error" element="span" />
							</label>

							<label class="field" for="account-phone">
								<span class="field__label">
									<spring:message code="form.phone.label" />
								</span>
								<form:input path="phone" id="account-phone" type="tel"
									cssClass="field__control account-readonly-control account-field--editable"
									placeholder="${phonePlaceholder}" autocomplete="tel" readonly="true" />
								<form:errors path="phone" cssClass="field__error" element="span" />
							</label>

							<div class="account-edit-actions" id="account-edit-actions">
								<ui:button label="${logoutLabel}" type="submit" form="logout-form" variant="danger" />
								<div class="account-edit-actions__confirm" id="account-edit-confirm">
									<ui:button label="${accountSaveLabel}" type="submit" id="account-save-button" />
									<ui:button label="${accountCancelLabel}" variant="secondary" id="account-cancel-button" />
								</div>
							</div>
						</div>
					</div>
				</form:form>

			</section>
		</main>
	</div>

	<script>
		document.addEventListener('DOMContentLoaded', () => {
			const form = document.getElementById('account-edit-form');
			const editableFields = form.querySelectorAll('.account-field--editable');
			const fileInput = document.getElementById('account-profile-image');
			const actionsBar = document.getElementById('account-edit-confirm');
			const cancelButton = document.getElementById('account-cancel-button');

			// Store original values
			const originalValues = new Map();
			editableFields.forEach(field => {
				originalValues.set(field, field.value);
			});
			let fileChanged = false;

			// Enter edit mode
			function enterEditMode() {
				editableFields.forEach(field => {
					field.removeAttribute('readonly');
					field.classList.remove('account-readonly-control');
				});
			}

			function checkForChanges() {
				let hasChanges = fileChanged;
				editableFields.forEach(field => {
					if (field.value !== originalValues.get(field)) {
						hasChanges = true;
					}
				});

				if (hasChanges) {
					actionsBar.classList.add('account-edit-actions__confirm--visible');
				} else {
					actionsBar.classList.remove('account-edit-actions__confirm--visible');
				}
			}

			editableFields.forEach(field => {
				field.addEventListener('focus', enterEditMode);
				field.addEventListener('click', enterEditMode);
				field.addEventListener('input', checkForChanges);
			});

			if (fileInput) {
				fileInput.addEventListener('change', () => {
					fileChanged = !!fileInput.value;
					checkForChanges();
				});
			}

			if (cancelButton) {
				cancelButton.addEventListener('click', (e) => {
					e.preventDefault();
					// Restore original values
					editableFields.forEach(field => {
						field.value = originalValues.get(field);
						field.setAttribute('readonly', 'true');
						field.classList.add('account-readonly-control');
					});
					if (fileInput) {
						fileInput.value = '';
					}
					fileChanged = false;
					actionsBar.classList.remove('account-edit-actions__confirm--visible');
				});
			}
		});
	</script>
</body>

</html>
