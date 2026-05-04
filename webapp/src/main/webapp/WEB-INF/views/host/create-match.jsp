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
								<spring:message var="titlePlaceholder" code="host.form.title.placeholder" />
								<spring:message var="descPlaceholder" code="host.form.description.placeholder" />
								<spring:message var="locationPlaceholder" code="host.form.location.placeholder" />
								<spring:message var="locationMapAria" code="host.form.location.map.aria" />
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
								<spring:message var="recurrenceDaily" code="host.form.recurrence.frequency.daily" />
								<spring:message var="recurrenceWeekly" code="host.form.recurrence.frequency.weekly" />
								<spring:message var="recurrenceMonthly" code="host.form.recurrence.frequency.monthly" />
								<spring:message var="recurrenceEndUntilDate"
									code="host.form.recurrence.endMode.untilDate" />
								<spring:message var="recurrenceEndOccurrenceCount"
									code="host.form.recurrence.endMode.occurrenceCount" />
									<c:url var="resolvedFormAction" value="${formAction}" />

								<main class="page-shell">
									<ui:returnButton />
									<section class="create-layout__main">
										<header class="page-heading">
											<h1 class="page-heading__title">
												<c:out value="${formTitle}" />
											</h1>
											<p class="page-heading__description">
												<c:out value="${formDescription}" />
											</p>
										</header>

										<spring:message var="titlePlaceholder" code="host.form.title.placeholder" />
										<spring:message var="descPlaceholder"
											code="host.form.description.placeholder" />
										<spring:message var="locationPlaceholder"
											code="host.form.location.placeholder" />
										<spring:message var="sportPadel" code="sport.padel" />
										<spring:message var="sportFootball" code="sport.football" />
										<spring:message var="sportTennis" code="sport.tennis" />
										<spring:message var="sportBasketball" code="sport.basketball" />
										<spring:message var="visibilityPlaceholder"
											code="host.form.visibility.placeholder" />
										<spring:message var="visibilityPublic" code="host.form.visibility.public" />
										<spring:message var="visibilityPrivate" code="host.form.visibility.private" />
										<spring:message var="joinPolicyPlaceholder"
											code="host.form.joinPolicy.placeholder" />
										<spring:message var="joinPolicyDirect" code="host.form.joinPolicy.direct" />
										<spring:message var="joinPolicyApproval"
											code="host.form.joinPolicy.approvalRequired" />
										<spring:message var="sportOther" code="sport.other" />
										<spring:message var="durationOneHour" code="host.form.duration.oneHour" />
										<spring:message var="durationNinetyMinutes"
											code="host.form.duration.ninetyMinutes" />
										<spring:message var="durationCustom" code="host.form.duration.custom" />
										<spring:message var="durationLabel" code="host.form.duration" />
											<c:url var="resolvedFormAction" value="${formAction}" />
										<spring:message var="recurrenceFrequencyPlaceholder"
											code="host.form.recurrence.frequency.placeholder" />
										<spring:message var="recurrenceDaily" code="host.form.recurrence.frequency.daily" />
										<spring:message var="recurrenceWeekly" code="host.form.recurrence.frequency.weekly" />
										<spring:message var="recurrenceMonthly" code="host.form.recurrence.frequency.monthly" />
										<spring:message var="recurrenceEndModePlaceholder"
											code="host.form.recurrence.endMode.placeholder" />
										<spring:message var="recurrenceEndUntilDate"
											code="host.form.recurrence.endMode.untilDate" />
										<spring:message var="recurrenceEndOccurrenceCount"
											code="host.form.recurrence.endMode.occurrenceCount" />
										<spring:url value="${formAction}" var="resolvedFormAction" />

										<form:form method="post" action="${resolvedFormAction}"
											modelAttribute="createEventForm" enctype="multipart/form-data"
											id="create-match-form" data-submit-guard="true"
											data-submit-loading-label="${submitLoadingLabel}" cssClass="create-form"
											novalidate="novalidate">
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<input type="hidden" name="tz" id="match-timezone"
												value="<c:out value='${createEventForm.tz}' />"
												data-browser-timezone-field="true" />
											<c:if test="${not empty formError}">
												<p class="field__error">
													<c:out value="${formError}" />
												</p>
											</c:if>
											<!-- basics -->
											<article class="panel form-card">
												<span class="detail-label">
													<spring:message code="host.section.basics" />
												</span>
												<h2 class="form-card__title">
													<spring:message code="host.section.basics.subtitle" />
												</h2>
												<div class="create-stack">
													<label class="field" for="match-title">
														<span class="field__label">
															<spring:message code="host.form.title" />
														</span>
														<form:input path="title" id="match-title"
															cssClass="field__control" required="required"
															placeholder="${titlePlaceholder}" />
														<form:errors path="title" cssClass="field__error"
															element="span" />
													</label>

													<label class="field" for="match-sport">
														<span class="field__label">
															<spring:message code="host.form.category" />
														</span>
														<span class="field__select-wrap">
															<form:select path="sport" id="match-sport"
																cssClass="field__control field__control--select"
																required="required">
																<form:option value="padel" label="${sportPadel}" />
																<form:option value="football"
																	label="${sportFootball}" />
																<form:option value="tennis" label="${sportTennis}" />
																<form:option value="basketball"
																	label="${sportBasketball}" />
																<form:option value="other" label="${sportOther}" />
															</form:select>
														</span>
														<form:errors path="sport" cssClass="field__error"
															element="span" />
													</label>

													<label class="field" for="match-description">
														<span class="field__label">
															<spring:message code="host.form.description" />
														</span>
														<form:textarea path="description" id="match-description"
															cssClass="field__control field__control--textarea"
															placeholder="${descPlaceholder}" />
														<form:errors path="description" cssClass="field__error"
															element="span" />
													</label>
												</div>
											</article>
											<!-- logistis-->
											<article class="panel form-card">
												<span class="detail-label">
													<spring:message code="host.section.logistics" />
												</span>
												<h2 class="form-card__title">
													<spring:message code="host.section.logistics.subtitle" />
												</h2>
												<div class="create-stack">
													<label class="field" for="match-address">
														<span class="field__label">
															<spring:message code="host.form.location" />
														</span>
														<form:input path="address" id="match-address"
															cssClass="field__control" required="required"
															placeholder="${locationPlaceholder}" />
														<form:errors path="address" cssClass="field__error"
															element="span" />
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
																</div>
																<div class="location-picker__actions">
																	<button type="button" class="btn btn--ghost btn--sm" data-location-zoom-out="true">
																		<spring:message code="host.form.location.zoomOut" />
																	</button>
																	<button type="button" class="btn btn--ghost btn--sm" data-location-zoom-in="true">
																		<spring:message code="host.form.location.zoomIn" />
																	</button>
																	<button type="button" class="btn btn--secondary btn--sm" data-location-current="true">
																		<spring:message code="host.form.location.current" />
																	</button>
																	<button type="button" class="btn btn--ghost btn--sm" data-location-clear="true">
																		<spring:message code="host.form.location.clear" />
																	</button>
																</div>
															</div>
															<div class="location-picker__map" data-location-map="true" aria-label="${locationMapAria}"></div>
															<c:if test="${not empty mapAttribution}">
																<p class="location-picker__attribution"><c:out value="${mapAttribution}" /></p>
															</c:if>
														</section>
													</c:if>
													<div class="form-card__grid form-card__grid--datetime">
														<label class="field" for="match-date">
															<span class="field__label">
																<spring:message code="host.form.date" />
															</span>
															<form:input path="eventDate" id="match-date" type="date"
																cssClass="field__control" required="required" />
															<form:errors path="eventDate" cssClass="field__error"
																element="span" />
														</label>

														<label class="field" for="match-time">
															<span class="field__label">
																<spring:message code="host.form.startTime" />
															</span>
															<form:input path="eventTime" id="match-time" type="time"
																cssClass="field__control" required="required" />
															<form:errors path="eventTime" cssClass="field__error"
																element="span" />
														</label>
													</div>

													<div class="field">
														<span class="field__label">
															<spring:message code="host.form.duration" />
														</span>
														<div class="duration-options" role="radiogroup"
															aria-label="${durationLabel}">
															<label class="chip duration-option"
																data-duration-minutes="60">
																<input type="radio" name="durationPresetUi" value="60"
																	class="duration-option__input" />
																<span>${durationOneHour}</span>
															</label>
															<label class="chip duration-option"
																data-duration-minutes="90">
																<input type="radio" name="durationPresetUi" value="90"
																	class="duration-option__input" />
																<span>${durationNinetyMinutes}</span>
															</label>
															<label class="chip duration-option"
																data-duration-minutes="custom">
																<input type="radio" name="durationPresetUi"
																	value="custom" class="duration-option__input" />
																<span>${durationCustom}</span>
															</label>
														</div>
													</div>
													<div class="form-card__grid form-card__grid--datetime">
														<label class="field" for="match-end-date">
															<span class="field__label">
																<spring:message code="host.form.endDate" />
															</span>
															<form:input path="endDate" id="match-end-date" type="date"
																cssClass="field__control" required="required" />
															<form:errors path="endDate" cssClass="field__error"
																element="span" />
														</label>

														<label class="field" for="match-end-time">
															<span class="field__label">
																<spring:message code="host.form.endTime" />
															</span>
															<form:input path="endTime" id="match-end-time" type="time"
																cssClass="field__control" required="required" />
															<form:errors path="endTime" cssClass="field__error"
																element="span" />
														</label>
													</div>

												</div>
											</article>

											<article class="panel form-card">
												<span class="detail-label">
													<spring:message code="host.section.visibility" />
												</span>
												<h2 class="form-card__title">
													<spring:message code="host.section.visibility.subtitle" />
												</h2>
												<div class="form-card__grid">
													<div class="field">
														<span class="field__label">
															<spring:message code="host.form.visibility" />
														</span>
														<c:choose>
															<c:when test="${isSeriesEditMode}">
																<div class="account-locked-field">
																	<input type="text"
																		class="field__control account-readonly-control account-readonly-control--muted"
																		value="${createEventForm.visibility}"
																		readonly="readonly" aria-readonly="true" />
																	<span class="account-locked-field__icon"
																		aria-hidden="true">
																		<svg viewBox="0 0 24 24" focusable="false">
																			<path
																				d="M8.5 10V8.25a3.5 3.5 0 1 1 7 0V10" />
																			<rect x="6.5" y="10" width="11" height="9"
																				rx="2.2" />
																		</svg>
																	</span>
																</div>
																<input type="hidden" name="visibility"
																	value="${createEventForm.visibility}" />
															</c:when>
															<c:otherwise>
																<ui:eventsFilterToggle id="match-visibility-toggle"
																	currentValue="${createEventForm.visibility}"
																	inputName="visibility" leftValue="public"
																	rightValue="private" leftLabel="${visibilityPublic}"
																	rightLabel="${visibilityPrivate}"
																	forceLeftOnEmpty="${true}" />
																<form:errors path="visibility" cssClass="field__error"
																	element="span" />
															</c:otherwise>
														</c:choose>
													</div>

													<div class="field" id="join-policy-field">
														<span class="field__label">
															<spring:message code="host.form.joinPolicy" />
														</span>
														<c:choose>
															<c:when test="${isSeriesEditMode}">
																<div class="account-locked-field">
																	<input type="text"
																		class="field__control account-readonly-control account-readonly-control--muted"
																		value="${createEventForm.joinPolicy}"
																		readonly="readonly" aria-readonly="true" />
																	<span class="account-locked-field__icon"
																		aria-hidden="true">
																		<svg viewBox="0 0 24 24" focusable="false">
																			<path
																				d="M8.5 10V8.25a3.5 3.5 0 1 1 7 0V10" />
																			<rect x="6.5" y="10" width="11" height="9"
																				rx="2.2" />
																		</svg>
																	</span>
																</div>
																<input type="hidden" name="joinPolicy"
																	value="${createEventForm.joinPolicy}" />
															</c:when>
															<c:otherwise>
																<ui:eventsFilterToggle id="match-join-policy-toggle"
																	currentValue="${createEventForm.joinPolicy}"
																	inputName="joinPolicy" leftValue="direct"
																	rightValue="approval_required"
																	leftLabel="${joinPolicyDirect}"
																	rightLabel="${joinPolicyApproval}"
																	forceLeftOnEmpty="${true}" />
																<form:errors path="joinPolicy" cssClass="field__error"
																	element="span" />
															</c:otherwise>
														</c:choose>
													</div>
												</div>
											</article>

											<article class="panel form-card">
												<span class="detail-label">
													<spring:message code="host.section.capacity" />
												</span>
												<h2 class="form-card__title">
													<spring:message code="host.section.capacity.subtitle" />
												</h2>
												<div class="form-card__grid">
													<label class="field" for="match-capacity">
														<span class="field__label">
															<spring:message code="host.form.capacity" />
														</span>
														<form:input path="maxPlayers" id="match-capacity" type="number"
															min="1" max="1000" cssClass="field__control" required="required" />
														<form:errors path="maxPlayers" cssClass="field__error"
															element="span" />
													</label>

													<label class="field" for="match-price">
														<span class="field__label">
															<spring:message code="host.form.pricePerPlayer" />
														</span>
														<form:input path="pricePerPlayer" id="match-price" type="number"
															min="0" step="0.01" cssClass="field__control"
															required="required" />
														<form:errors path="pricePerPlayer" cssClass="field__error"
															element="span" />
													</label>
												</div>
											</article>

											<c:if test="${not isEditMode}">
												<article class="panel form-card">
													<span class="detail-label">
														<spring:message code="host.section.recurrence" />
													</span>
													<h2 class="form-card__title">
														<spring:message code="host.section.recurrence.subtitle" />
													</h2>
													<div class="create-stack">
														<label class="recurrence-toggle" for="match-recurring">
															<form:checkbox path="recurring" id="match-recurring"
																cssClass="recurrence-toggle__input" />
															<span class="recurrence-toggle__switch" aria-hidden="true">
																<span class="recurrence-toggle__knob"></span>
															</span>
															<span class="recurrence-toggle__copy">
																<span class="recurrence-toggle__title">
																	<spring:message
																		code="host.form.recurrence.enabled" />
																</span>
																<span class="recurrence-toggle__hint">
																	<spring:message
																		code="host.form.recurrence.enabled.hint" />
																</span>
															</span>
														</label>

														<div class="create-stack recurrence-settings"
															id="recurrence-settings">
															<div class="field">
																<span class="field__label" id="match-recurrence-frequency-label">
																	<spring:message
																		code="host.form.recurrence.frequency" />
																</span>
																<c:set var="selectedRecurrenceFrequency"
																	value="${empty createEventForm.recurrenceFrequency ? 'daily' : createEventForm.recurrenceFrequency}" />
																<div class="events-toggle-wrapper recurrence-segmented-toggle recurrence-segmented-toggle--three"
																	id="match-recurrence-frequency-toggle"
																	data-events-toggle="true"
																	aria-labelledby="match-recurrence-frequency-label">
																	<input type="hidden" name="recurrenceFrequency"
																		id="match-recurrence-frequency"
																		value="${selectedRecurrenceFrequency}"
																		data-events-toggle-input="true" />
																	<div class="events-toggle-slider"
																		data-events-toggle-slider="true"></div>
																	<button type="button"
																		class="events-toggle-btn ${selectedRecurrenceFrequency eq 'daily' ? 'active' : ''}"
																		data-value="daily"
																		aria-pressed="${selectedRecurrenceFrequency eq 'daily' ? 'true' : 'false'}">
																		<c:out value="${recurrenceDaily}" />
																	</button>
																	<button type="button"
																		class="events-toggle-btn ${selectedRecurrenceFrequency eq 'weekly' ? 'active' : ''}"
																		data-value="weekly"
																		aria-pressed="${selectedRecurrenceFrequency eq 'weekly' ? 'true' : 'false'}">
																		<c:out value="${recurrenceWeekly}" />
																	</button>
																	<button type="button"
																		class="events-toggle-btn ${selectedRecurrenceFrequency eq 'monthly' ? 'active' : ''}"
																		data-value="monthly"
																		aria-pressed="${selectedRecurrenceFrequency eq 'monthly' ? 'true' : 'false'}">
																		<c:out value="${recurrenceMonthly}" />
																	</button>
																</div>
																<form:errors path="recurrenceFrequency"
																	cssClass="field__error" element="span" />
															</div>

															<div class="field">
																<span class="field__label" id="match-recurrence-end-mode-label">
																	<spring:message
																		code="host.form.recurrence.endMode" />
																</span>
																<c:set var="selectedRecurrenceEndMode"
																	value="${empty createEventForm.recurrenceEndMode ? 'until_date' : createEventForm.recurrenceEndMode}" />
																<div class="events-toggle-wrapper recurrence-segmented-toggle recurrence-segmented-toggle--two"
																	id="match-recurrence-end-mode-toggle"
																	data-events-toggle="true"
																	data-events-toggle-right-value="occurrence_count"
																	aria-labelledby="match-recurrence-end-mode-label">
																	<input type="hidden" name="recurrenceEndMode"
																		id="match-recurrence-end-mode"
																		value="${selectedRecurrenceEndMode}"
																		data-events-toggle-input="true" />
																	<div class="events-toggle-slider ${selectedRecurrenceEndMode eq 'occurrence_count' ? 'right' : ''}"
																		data-events-toggle-slider="true"></div>
																	<button type="button"
																		class="events-toggle-btn ${selectedRecurrenceEndMode eq 'until_date' ? 'active' : ''}"
																		data-value="until_date"
																		aria-pressed="${selectedRecurrenceEndMode eq 'until_date' ? 'true' : 'false'}">
																		<c:out value="${recurrenceEndUntilDate}" />
																	</button>
																	<button type="button"
																		class="events-toggle-btn ${selectedRecurrenceEndMode eq 'occurrence_count' ? 'active' : ''}"
																		data-value="occurrence_count"
																		aria-pressed="${selectedRecurrenceEndMode eq 'occurrence_count' ? 'true' : 'false'}">
																		<c:out value="${recurrenceEndOccurrenceCount}" />
																	</button>
																</div>
																<form:errors path="recurrenceEndMode"
																	cssClass="field__error" element="span" />
															</div>

															<label class="field" for="match-recurrence-until-date"
																id="recurrence-until-date-field">
																<span class="field__label">
																	<spring:message
																		code="host.form.recurrence.untilDate" />
																</span>
																<form:input path="recurrenceUntilDate"
																	id="match-recurrence-until-date" type="date"
																	cssClass="field__control" />
																<span class="field__hint">
																	<spring:message
																		code="host.form.recurrence.untilDate.hint" />
																</span>
																<form:errors path="recurrenceUntilDate"
																	cssClass="field__error" element="span" />
															</label>

															<label class="field" for="match-recurrence-occurrence-count"
																id="recurrence-count-field">
																<span class="field__label">
																	<spring:message
																		code="host.form.recurrence.occurrenceCount" />
																</span>
																<form:input path="recurrenceOccurrenceCount"
																	id="match-recurrence-occurrence-count" type="number"
																	min="2" max="52" cssClass="field__control" />
																<span class="field__hint">
																	<spring:message
																		code="host.form.recurrence.occurrenceCount.hint" />
																</span>
																<form:errors path="recurrenceOccurrenceCount"
																	cssClass="field__error" element="span" />
															</label>
														</div>
													</div>
												</article>
											</c:if>


											<!-- image upload -->
											<article class="panel upload-card">
												<span class="detail-label">
													<spring:message code="host.section.banner" />
												</span>
												<h2 class="form-card__title">
													<spring:message code="host.section.banner.subtitle" />
												</h2>
												<c:if test="${not empty currentBannerImageUrl}">
													<c:url var="currentBannerSrc" value="${currentBannerImageUrl}" />
													<div class="upload-card__preview">
														<span class="upload-card__preview-label">
															<spring:message code="host.form.bannerImage.current" />
														</span>
														<img class="upload-card__preview-image" src="${currentBannerSrc}" alt="" loading="lazy" decoding="async" />
													</div>
												</c:if>
												<label class="field upload-card__field" for="match-banner-image">
													<span class="field__label">
														<spring:message code="host.form.bannerImage" />
													</span>
													<span class="upload-card__dropzone">
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
																<spring:message code="host.form.bannerImage.guidanceTitle" />
															</span>
															<span class="field__hint">
																<spring:message code="host.form.bannerImage.hint" />
															</span>
														</span>
														<form:input path="bannerImage" id="match-banner-image" type="file"
															accept="image/png,image/jpeg,image/webp,image/gif"
															cssClass="upload-card__file-input" />
													</span>
												</label>
											</article>



											<div class="create-layout__actions">
												<ui:button label="${submitLabel}" type="submit" id="${submitButtonId}"
													size="lg" fullWidth="${true}" className="create-layout__submit" />
											</div>
										</form:form>
									</section>
								</main>
						</div>

				</body>

					</html>
