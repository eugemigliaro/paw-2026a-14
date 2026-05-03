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

			<main class="page-shell">
				<ui:returnButton />
				<section class="create-layout__main">
					<header class="page-heading">
						<p class="eyebrow"><c:out value="${formEyebrow}" /></p>
						<h1 class="page-heading__title"><c:out value="${formTitle}" /></h1>
						<p class="page-heading__description">
							<c:out value="${formDescription}" />
						</p>
					</header>

					<spring:message var="titlePlaceholder" code="host.form.title.placeholder" />
					<spring:message var="descPlaceholder" code="host.form.description.placeholder" />
					<spring:message var="locationPlaceholder" code="host.form.location.placeholder" />
					<spring:message var="sportPadel" code="sport.padel" />
					<spring:message var="sportFootball" code="sport.football" />
					<spring:message var="sportTennis" code="sport.tennis" />
					<spring:message var="sportBasketball" code="sport.basketball" />
					<spring:message var="visibilityPlaceholder" code="host.form.visibility.placeholder" />
					<spring:message var="visibilityPublic" code="host.form.visibility.public" />
					<spring:message var="visibilityPrivate" code="host.form.visibility.private" />
					<spring:message var="joinPolicyPlaceholder" code="host.form.joinPolicy.placeholder" />
					<spring:message var="joinPolicyDirect" code="host.form.joinPolicy.direct" />
					<spring:message var="joinPolicyApproval" code="host.form.joinPolicy.approvalRequired" />
					<spring:message var="sportOther" code="sport.other" />
					<spring:message var="durationOneHour" code="host.form.duration.oneHour" />
					<spring:message var="durationNinetyMinutes" code="host.form.duration.ninetyMinutes" />
					<spring:message var="durationCustom" code="host.form.duration.custom" />
					<spring:message var="durationLabel" code="host.form.duration" />
					<spring:message var="recurrenceFrequencyPlaceholder" code="host.form.recurrence.frequency.placeholder" />
					<spring:message var="recurrenceDaily" code="host.form.recurrence.frequency.daily" />
					<spring:message var="recurrenceWeekly" code="host.form.recurrence.frequency.weekly" />
					<spring:message var="recurrenceMonthly" code="host.form.recurrence.frequency.monthly" />
					<spring:message var="recurrenceEndModePlaceholder" code="host.form.recurrence.endMode.placeholder" />
					<spring:message var="recurrenceEndUntilDate" code="host.form.recurrence.endMode.untilDate" />
					<spring:message var="recurrenceEndOccurrenceCount" code="host.form.recurrence.endMode.occurrenceCount" />
					<spring:message var="locationMapAria" code="host.form.location.map.aria" />
					<c:url var="resolvedFormAction" value="${formAction}" />

					<form:form
						method="post"
						action="${resolvedFormAction}"
						modelAttribute="createEventForm"
						enctype="multipart/form-data"
						id="create-match-form"
						data-submit-guard="true"
						data-submit-loading-label="${submitLoadingLabel}"
						cssClass="create-form"
						novalidate="novalidate"
					>
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<input
							type="hidden"
							name="tz"
							id="match-timezone"
							value="<c:out value='${createEventForm.tz}' />"
							data-browser-timezone-field="true" />
						<c:if test="${not empty formError}">
							<p class="field__error">
								<c:out value="${formError}" />
							</p>
						</c:if>
						<article class="panel form-card">
							<span class="detail-label"><spring:message code="host.section.basics" /></span>
							<h2 class="form-card__title">
								<spring:message code="host.section.basics.subtitle" />
							</h2>
							<div class="create-stack">
								<label class="field" for="match-title">
									<span class="field__label"
										><spring:message code="host.form.title" /></span
									>
									<form:input
										path="title"
										id="match-title"
										cssClass="field__control"
										required="required"
										placeholder="${titlePlaceholder}"
									/>
									<form:errors
										path="title"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-sport">
									<span class="field__label"><spring:message code="host.form.category" /></span>
									<span class="field__select-wrap">
										<form:select
											path="sport"
											id="match-sport"
											cssClass="field__control field__control--select"
											required="required"
										>
											<form:option value="padel" label="${sportPadel}" />
											<form:option value="football" label="${sportFootball}" />
											<form:option value="tennis" label="${sportTennis}" />
											<form:option value="basketball" label="${sportBasketball}" />
											<form:option value="other" label="${sportOther}" />
										</form:select>
									</span>
									<form:errors
										path="sport"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-description">
									<span class="field__label"
										><spring:message code="host.form.description" /></span
									>
									<form:textarea
										path="description"
										id="match-description"
										cssClass="field__control field__control--textarea"
										placeholder="${descPlaceholder}"
									/>
									<form:errors
										path="description"
										cssClass="field__error"
										element="span"
									/>
								</label>
							</div>
						</article>

						<article class="panel form-card">
							<span class="detail-label"><spring:message code="host.section.logistics" /></span>
							<h2 class="form-card__title">
								<spring:message code="host.section.logistics.subtitle" />
							</h2>
							<div class="create-stack">
								<label class="field" for="match-address">
									<span class="field__label"><spring:message code="host.form.location" /></span>
									<form:input
										path="address"
										id="match-address"
										cssClass="field__control"
										required="required"
										placeholder="${locationPlaceholder}"
									/>
									<form:errors
										path="address"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<form:hidden path="latitude" id="match-location-latitude" />
								<form:hidden path="longitude" id="match-location-longitude" />
								<form:errors path="latitude" cssClass="field__error" element="span" />
								<form:errors path="longitude" cssClass="field__error" element="span" />
								<c:if test="${mapPickerEnabled}">
									<section
										class="location-picker"
										data-location-picker="true"
										data-tile-url-template="${mapTileUrlTemplate}"
										data-attribution="${mapAttribution}"
										data-default-latitude="${mapDefaultLatitude}"
										data-default-longitude="${mapDefaultLongitude}"
										data-default-zoom="${mapDefaultZoom}">
										<div class="location-picker__header">
											<div>
												<span class="field__label"><spring:message code="host.form.location.map" /></span>
												<p class="field__hint"><spring:message code="host.form.location.map.hint" /></p>
											</div>
											<div class="location-picker__actions">
												<button type="button" class="btn btn--secondary btn--sm" data-location-current="true">
													<spring:message code="host.form.location.current" />
												</button>
												<button type="button" class="btn btn--ghost btn--sm" data-location-clear="true">
													<spring:message code="host.form.location.clear" />
												</button>
											</div>
										</div>
										<div class="location-picker__map" data-location-map="true" role="button" tabindex="0" aria-label="${locationMapAria}">
											<div class="location-picker__tiles" data-location-tiles="true"></div>
											<div class="location-picker__pin" data-location-pin="true" hidden="hidden"></div>
										</div>
										<p class="location-picker__status" data-location-status="true" role="status"></p>
										<c:if test="${not empty mapAttribution}">
											<p class="location-picker__attribution"><c:out value="${mapAttribution}" /></p>
										</c:if>
									</section>
								</c:if>

								<label class="field" for="match-date">
									<span class="field__label"><spring:message code="host.form.date" /></span>
									<form:input
										path="eventDate"
										id="match-date"
										type="date"
										cssClass="field__control"
										required="required"
									/>
									<form:errors
										path="eventDate"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-time">
									<span class="field__label"><spring:message code="host.form.startTime" /></span>
									<form:input
										path="eventTime"
										id="match-time"
										type="time"
										cssClass="field__control"
										required="required"
									/>
									<form:errors
										path="eventTime"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<div class="field">
									<span class="field__label"><spring:message code="host.form.duration" /></span>
									<div class="duration-options" role="radiogroup" aria-label="${durationLabel}">
										<label class="chip duration-option" data-duration-minutes="60">
											<input type="radio" name="durationPresetUi" value="60" class="duration-option__input" />
											<span>${durationOneHour}</span>
										</label>
										<label class="chip duration-option" data-duration-minutes="90">
											<input type="radio" name="durationPresetUi" value="90" class="duration-option__input" />
											<span>${durationNinetyMinutes}</span>
										</label>
										<label class="chip duration-option" data-duration-minutes="custom">
											<input type="radio" name="durationPresetUi" value="custom" class="duration-option__input" />
											<span>${durationCustom}</span>
										</label>
									</div>
								</div>

								<label class="field" for="match-end-date">
									<span class="field__label"><spring:message code="host.form.endDate" /></span>
									<form:input
										path="endDate"
										id="match-end-date"
										type="date"
										cssClass="field__control"
										required="required"
									/>
									<form:errors
										path="endDate"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-end-time">
									<span class="field__label"><spring:message code="host.form.endTime" /></span>
									<form:input
										path="endTime"
										id="match-end-time"
										type="time"
										cssClass="field__control"
										required="required"
									/>
									<form:errors
										path="endTime"
										cssClass="field__error"
										element="span"
									/>
								</label>
							</div>
						</article>

						<c:if test="${not isEditMode}">
							<article class="panel form-card recurrence-card">
								<span class="detail-label"><spring:message code="host.section.recurrence" /></span>
								<h2 class="form-card__title">
									<spring:message code="host.section.recurrence.subtitle" />
								</h2>
								<div class="create-stack">
									<label class="recurrence-toggle" for="match-recurring">
										<form:checkbox
											path="recurring"
											id="match-recurring"
											cssClass="recurrence-toggle__input"
										/>
										<span class="recurrence-toggle__copy">
											<span class="recurrence-toggle__title">
												<spring:message code="host.form.recurrence.enabled" />
											</span>
											<span class="recurrence-toggle__hint">
												<spring:message code="host.form.recurrence.enabled.hint" />
											</span>
										</span>
									</label>

									<div class="create-stack recurrence-settings" id="recurrence-settings">
										<label class="field" for="match-recurrence-frequency">
											<span class="field__label"><spring:message code="host.form.recurrence.frequency" /></span>
											<span class="field__select-wrap">
												<form:select
													path="recurrenceFrequency"
													id="match-recurrence-frequency"
													cssClass="field__control field__control--select"
												>
													<form:option value="" label="${recurrenceFrequencyPlaceholder}" />
													<form:option value="daily" label="${recurrenceDaily}" />
													<form:option value="weekly" label="${recurrenceWeekly}" />
													<form:option value="monthly" label="${recurrenceMonthly}" />
												</form:select>
											</span>
											<form:errors
												path="recurrenceFrequency"
												cssClass="field__error"
												element="span"
											/>
										</label>

										<label class="field" for="match-recurrence-end-mode">
											<span class="field__label"><spring:message code="host.form.recurrence.endMode" /></span>
											<span class="field__select-wrap">
												<form:select
													path="recurrenceEndMode"
													id="match-recurrence-end-mode"
													cssClass="field__control field__control--select"
												>
													<form:option value="" label="${recurrenceEndModePlaceholder}" />
													<form:option value="until_date" label="${recurrenceEndUntilDate}" />
													<form:option value="occurrence_count" label="${recurrenceEndOccurrenceCount}" />
												</form:select>
											</span>
											<form:errors
												path="recurrenceEndMode"
												cssClass="field__error"
												element="span"
											/>
										</label>

										<label class="field" for="match-recurrence-until-date" id="recurrence-until-date-field">
											<span class="field__label"><spring:message code="host.form.recurrence.untilDate" /></span>
											<form:input
												path="recurrenceUntilDate"
												id="match-recurrence-until-date"
												type="date"
												cssClass="field__control"
											/>
											<span class="field__hint"><spring:message code="host.form.recurrence.untilDate.hint" /></span>
											<form:errors
												path="recurrenceUntilDate"
												cssClass="field__error"
												element="span"
											/>
										</label>

										<label class="field" for="match-recurrence-occurrence-count" id="recurrence-count-field">
											<span class="field__label"><spring:message code="host.form.recurrence.occurrenceCount" /></span>
											<form:input
												path="recurrenceOccurrenceCount"
												id="match-recurrence-occurrence-count"
												type="number"
												min="2"
												max="52"
												cssClass="field__control"
											/>
											<span class="field__hint"><spring:message code="host.form.recurrence.occurrenceCount.hint" /></span>
											<form:errors
												path="recurrenceOccurrenceCount"
												cssClass="field__error"
												element="span"
											/>
										</label>
									</div>
								</div>
							</article>
						</c:if>

						<article class="panel form-card">
							<span class="detail-label"><spring:message code="host.section.capacity" /></span>
							<h2 class="form-card__title">
								<spring:message code="host.section.capacity.subtitle" />
							</h2>
							<div class="form-card__grid form-card__grid--three">
								<label class="field" for="match-capacity">
									<span class="field__label"><spring:message code="host.form.capacity" /></span>
									<form:input
										path="maxPlayers"
										id="match-capacity"
										type="number"
										min="1"
										cssClass="field__control"
										required="required"
									/>
									<form:errors
										path="maxPlayers"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-price">
									<span class="field__label"
										><spring:message code="host.form.pricePerPlayer" /></span
									>
									<form:input
										path="pricePerPlayer"
										id="match-price"
										type="number"
										min="0"
										step="0.01"
										cssClass="field__control"
										required="required"
									/>
									<form:errors
										path="pricePerPlayer"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-visibility">
									<span class="field__label"><spring:message code="host.form.visibility" /></span>
									<span class="field__select-wrap">
										<form:select
											path="visibility"
											id="match-visibility"
											cssClass="field__control field__control--select"
											required="required"
										>
											<form:option value="" label="${visibilityPlaceholder}" />
											<form:option value="public" label="${visibilityPublic}" />
											<form:option value="private" label="${visibilityPrivate}" />
										</form:select>
									</span>
									<form:errors
										path="visibility"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-join-policy" id="join-policy-field">
									<span class="field__label"><spring:message code="host.form.joinPolicy" /></span>
									<span class="field__select-wrap">
										<form:select
											path="joinPolicy"
											id="match-join-policy"
											cssClass="field__control field__control--select"
											required="required"
										>
											<form:option value="" label="${joinPolicyPlaceholder}" />
											<form:option value="direct" label="${joinPolicyDirect}" />
											<form:option value="approval_required" label="${joinPolicyApproval}" />
										</form:select>
									</span>
									<form:errors
										path="joinPolicy"
										cssClass="field__error"
										element="span"
									/>
								</label>
							</div>
						</article>

						<article class="panel upload-card">
							<span class="detail-label"
								><spring:message code="host.section.banner" /></span
							>
							<h2 class="form-card__title">
								<spring:message code="host.section.banner.subtitle" />
							</h2>
							<label class="field" for="match-banner-image">
								<span class="field__label"><spring:message code="host.form.bannerImage" /></span>
								<form:input
									path="bannerImage"
									id="match-banner-image"
									type="file"
									accept="image/png,image/jpeg,image/webp,image/gif"
									cssClass="field__control upload-card__file-input"
								/>
								<span class="field__hint"
									><spring:message code="host.form.bannerImage.hint" /></span
								>
							</label>
							<div class="upload-card__guidance" role="note">
								<p class="upload-card__guidance-title"><spring:message code="host.form.bannerImage.guidanceTitle" /></p>
								<p class="upload-card__guidance-copy">
									<spring:message code="host.form.bannerImage.guidanceSize" />
								</p>
								<p class="upload-card__guidance-copy">
									<spring:message code="host.form.bannerImage.guidanceCopy" />
								</p>
							</div>
						</article>

						<div class="create-layout__actions">
							<ui:button
								label="${submitLabel}"
								type="submit"
								id="${submitButtonId}"
								size="lg"
								fullWidth="${true}"
								className="create-layout__submit"
							/>
						</div>
					</form:form>
				</section>
			</main>
		</div>
		<script>
			(function () {
				var visibilitySelect = document.getElementById('match-visibility');
				var joinPolicyField = document.getElementById('join-policy-field');
				var joinPolicySelect = document.getElementById('match-join-policy');
				var recurringCheckbox = document.getElementById('match-recurring');
				var recurrenceSettings = document.getElementById('recurrence-settings');
				var recurrenceEndModeSelect = document.getElementById('match-recurrence-end-mode');
				var recurrenceUntilDateField = document.getElementById('recurrence-until-date-field');
				var recurrenceCountField = document.getElementById('recurrence-count-field');
				var recurrenceUntilDateInput = document.getElementById('match-recurrence-until-date');
				var recurrenceCountInput = document.getElementById('match-recurrence-occurrence-count');

				if (visibilitySelect && joinPolicyField && joinPolicySelect) {

		function updateJoinPolicyVisibility() {
			var isPrivate = visibilitySelect.value === 'private';
			joinPolicyField.style.display = isPrivate ? 'none' : '';

			if (isPrivate) {
				joinPolicySelect.value = '';
			}
		}

		visibilitySelect.addEventListener('change', updateJoinPolicyVisibility);
		updateJoinPolicyVisibility();
	}

				function updateRecurrenceEndFields() {
					if (!recurrenceEndModeSelect || !recurrenceUntilDateField || !recurrenceCountField) {
						return;
					}

					var mode = recurrenceEndModeSelect.value;
					recurrenceUntilDateField.style.display = mode === 'until_date' ? '' : 'none';
					recurrenceCountField.style.display = mode === 'occurrence_count' ? '' : 'none';
					if (mode === 'until_date' && recurrenceCountInput) {
						recurrenceCountInput.value = '';
					}
					if (mode === 'occurrence_count' && recurrenceUntilDateInput) {
						recurrenceUntilDateInput.value = '';
					}
				}

				function updateRecurrenceSettings() {
					if (!recurringCheckbox || !recurrenceSettings) {
						return;
					}

					recurrenceSettings.style.display = recurringCheckbox.checked ? '' : 'none';
					updateRecurrenceEndFields();
				}

				if (recurringCheckbox) {
					recurringCheckbox.addEventListener('change', updateRecurrenceSettings);
				}
				if (recurrenceEndModeSelect) {
					recurrenceEndModeSelect.addEventListener('change', updateRecurrenceEndFields);
				}
				updateRecurrenceSettings();

				var presetInputs = document.querySelectorAll('input[name="durationPresetUi"]');
				var startDateInput = document.getElementById('match-date');
				var startTimeInput = document.getElementById('match-time');
				var endDateInput = document.getElementById('match-end-date');
				var endTimeInput = document.getElementById('match-end-time');
				var activeMode = null;

				if (!presetInputs.length || !startDateInput || !startTimeInput || !endDateInput || !endTimeInput) {
					return;
				}

				function parseLocalDateTime(dateValue, timeValue) {
					if (!dateValue || !timeValue) {
						return null;
					}

					var dateParts = dateValue.split('-').map(Number);
					var timeParts = timeValue.split(':').map(Number);
					return new Date(
						dateParts[0],
						dateParts[1] - 1,
						dateParts[2],
						timeParts[0],
						timeParts[1],
						0,
						0
					);
				}

				function pad(value) {
					return String(value).padStart(2, '0');
				}

				function setEndFromPreset(durationMinutes) {
					var start = parseLocalDateTime(startDateInput.value, startTimeInput.value);
					if (!start) {
						return;
					}

					var end = new Date(start.getTime() + durationMinutes * 60 * 1000);
					endDateInput.value =
						end.getFullYear() + '-' + pad(end.getMonth() + 1) + '-' + pad(end.getDate());
					endTimeInput.value = pad(end.getHours()) + ':' + pad(end.getMinutes());
				}

				function syncPresetUi(activePresetValue) {
					var explicitValue = activePresetValue == null ? null : String(activePresetValue);

					presetInputs.forEach(function (input) {
						var chip = input.closest('.duration-option');
						var isActive = explicitValue != null && input.value === explicitValue;
						input.checked = isActive;
						if (chip) {
							chip.classList.toggle('chip--active', isActive);
						}
					});
				}

				function detectPresetMode() {
					var start = parseLocalDateTime(startDateInput.value, startTimeInput.value);
					var end = parseLocalDateTime(endDateInput.value, endTimeInput.value);
					if (!start || !end) {
						return 'custom';
					}

					var diffMinutes = Math.round((end.getTime() - start.getTime()) / 60000);
					if (diffMinutes === 60 || diffMinutes === 90) {
						return String(diffMinutes);
					}
					return 'custom';
				}

				presetInputs.forEach(function (input) {
					input.addEventListener('change', function () {
						if (!input.checked) {
							return;
						}

						activeMode = input.value;
						if (activeMode === 'custom') {
							syncPresetUi(activeMode);
							return;
						}

						setEndFromPreset(Number(activeMode));
						syncPresetUi(activeMode);
					});
				});

				[startDateInput, startTimeInput].forEach(function (input) {
					input.addEventListener('change', function () {
						if (activeMode === '60' || activeMode === '90') {
							setEndFromPreset(Number(activeMode));
							syncPresetUi(activeMode);
							return;
						}

						if (activeMode !== 'custom') {
							activeMode = detectPresetMode();
						}
						syncPresetUi(activeMode);
					});
				});

				[endDateInput, endTimeInput].forEach(function (input) {
					input.addEventListener('change', function () {
						activeMode = detectPresetMode();
						syncPresetUi(activeMode);
					});
				});

				activeMode = detectPresetMode();
				syncPresetUi(activeMode);
			})();
		</script>
	</body>
</html>
