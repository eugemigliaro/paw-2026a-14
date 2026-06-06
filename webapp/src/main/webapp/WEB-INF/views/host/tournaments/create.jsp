<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>
			<spring:message var="titlePlaceholder" code="tournament.form.title.placeholder" />
			<spring:message var="descPlaceholder" code="tournament.form.description.placeholder" />
			<spring:message var="locationPlaceholder" code="tournament.form.location.placeholder" />
			<spring:message var="locationMapAria" code="host.form.location.map.aria" />
			<spring:message var="sportPadel" code="sport.padel" />
			<spring:message var="sportFootball" code="sport.football" />
			<spring:message var="sportTennis" code="sport.tennis" />
			<spring:message var="sportBasketball" code="sport.basketball" />
			<spring:message var="sportOther" code="sport.other" />
			<c:url var="resolvedFormAction" value="${formAction}" />

			<main class="page-shell">
				<ui:returnButton />
				<section class="create-layout__main tournament-create">
					<header class="page-heading">
						<h1 class="page-heading__title"><c:out value="${formTitle}" /></h1>
						<p class="page-heading__description"><c:out value="${formDescription}" /></p>
					</header>

					<form:form
						method="post"
						action="${resolvedFormAction}"
						modelAttribute="createTournamentForm"
						enctype="multipart/form-data"
						id="create-tournament-form"
						data-submit-guard="true"
						data-submit-loading-label="${submitLoadingLabel}"
						cssClass="create-form"
						novalidate="novalidate">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

						<c:if test="${not empty formError}">
							<p class="field__error create-form__global-error"><c:out value="${formError}" /></p>
						</c:if>
						<form:errors path="*" cssClass="field__error create-form__global-error" element="p" />

						<div class="create-form__column">
						<article class="panel form-card">
							<span class="detail-label"><spring:message code="tournament.create.section.basics" /></span>
							<h2 class="form-card__title"><spring:message code="tournament.create.section.basics.subtitle" /></h2>
							<div class="create-stack">
								<label class="field" for="tournament-title">
									<span class="field__label"><spring:message code="tournament.form.title.label" /></span>
									<form:input path="title" id="tournament-title" cssClass="field__control" required="required" placeholder="${titlePlaceholder}" />
									<form:errors path="title" cssClass="field__error" element="span" />
								</label>

								<label class="field" for="tournament-sport">
									<span class="field__label"><spring:message code="tournament.form.sport.label" /></span>
									<span class="field__select-wrap">
										<form:select path="sport" id="tournament-sport" cssClass="field__control field__control--select" required="required" data-tournament-sport-select="true">
											<form:option value="padel" label="${sportPadel}" />
											<form:option value="football" label="${sportFootball}" />
											<form:option value="tennis" label="${sportTennis}" />
											<form:option value="basketball" label="${sportBasketball}" />
											<form:option value="other" label="${sportOther}" />
										</form:select>
									</span>
									<form:errors path="sport" cssClass="field__error" element="span" />
								</label>

								<label class="field" for="tournament-description">
									<span class="field__label"><spring:message code="tournament.form.description.label" /></span>
									<form:textarea path="description" id="tournament-description" cssClass="field__control field__control--textarea" placeholder="${descPlaceholder}" />
									<form:errors path="description" cssClass="field__error" element="span" />
								</label>
							</div>
						</article>

						<article class="panel form-card">
							<span class="detail-label"><spring:message code="tournament.create.section.registration" /></span>
							<h2 class="form-card__title"><spring:message code="tournament.create.section.registration.subtitle" /></h2>
							<div class="create-stack">
								<div class="form-card__grid form-card__grid--datetime">
									<label class="field" for="tournament-registration-opens-date">
										<span class="field__label"><spring:message code="tournament.form.registrationOpensDate.label" /></span>
										<form:input path="registrationOpensDate" id="tournament-registration-opens-date" type="date" cssClass="field__control" required="required" />
										<form:errors path="registrationOpensDate" cssClass="field__error" element="span" />
									</label>
									<label class="field" for="tournament-registration-opens-time">
										<span class="field__label"><spring:message code="tournament.form.registrationOpensTime.label" /></span>
										<form:input path="registrationOpensTime" id="tournament-registration-opens-time" type="time" cssClass="field__control" required="required" />
										<form:errors path="registrationOpensTime" cssClass="field__error" element="span" />
									</label>
								</div>
								<div class="form-card__grid form-card__grid--datetime">
									<label class="field" for="tournament-registration-closes-date">
										<span class="field__label"><spring:message code="tournament.form.registrationClosesDate.label" /></span>
										<form:input path="registrationClosesDate" id="tournament-registration-closes-date" type="date" cssClass="field__control" required="required" />
										<form:errors path="registrationClosesDate" cssClass="field__error" element="span" />
									</label>
									<label class="field" for="tournament-registration-closes-time">
										<span class="field__label"><spring:message code="tournament.form.registrationClosesTime.label" /></span>
										<form:input path="registrationClosesTime" id="tournament-registration-closes-time" type="time" cssClass="field__control" required="required" />
										<form:errors path="registrationClosesTime" cssClass="field__error" element="span" />
									</label>
								</div>
								<input type="hidden" name="allowSoloSignup" value="true" />
								<input type="hidden" name="allowTeamDraft" value="false" />
								<p class="tournament-mode-notice"><spring:message code="tournament.form.registrationMode.individualOnly" /></p>
								<%--
								<label class="recurrence-toggle tournament-mode-toggle" for="tournament-allow-solo">
									<form:checkbox path="allowSoloSignup" id="tournament-allow-solo" cssClass="recurrence-toggle__input" />
									<span class="recurrence-toggle__switch" aria-hidden="true">
										<span class="recurrence-toggle__knob"></span>
									</span>
									<span class="recurrence-toggle__copy">
										<span class="recurrence-toggle__title"><spring:message code="tournament.form.allowSoloSignup.label" /></span>
										<span class="recurrence-toggle__hint"><spring:message code="tournament.form.allowSoloSignup.hint" /></span>
									</span>
								</label>
								<form:errors path="allowSoloSignup" cssClass="field__error" element="span" />
								<label class="recurrence-toggle tournament-mode-toggle" for="tournament-allow-team-draft">
									<form:checkbox path="allowTeamDraft" id="tournament-allow-team-draft" cssClass="recurrence-toggle__input" />
									<span class="recurrence-toggle__switch" aria-hidden="true">
										<span class="recurrence-toggle__knob"></span>
									</span>
									<span class="recurrence-toggle__copy">
										<span class="recurrence-toggle__title"><spring:message code="tournament.form.allowTeamDraft.label" /></span>
										<span class="recurrence-toggle__hint"><spring:message code="tournament.form.allowTeamDraft.hint" /></span>
									</span>
								</label>
								<form:errors path="allowTeamDraft" cssClass="field__error" element="span" />
								--%>
								<form:errors path="allowSoloSignup" cssClass="field__error" element="span" />
								<form:errors path="allowTeamDraft" cssClass="field__error" element="span" />
							</div>
						</article>

						<article class="panel form-card">
							<span class="detail-label"><spring:message code="tournament.create.section.schedule" /></span>
							<h2 class="form-card__title"><spring:message code="tournament.create.section.schedule.subtitle" /></h2>
							<div class="create-stack">
								<div class="form-card__grid form-card__grid--datetime">
									<label class="field" for="tournament-start-date">
										<span class="field__label"><spring:message code="tournament.form.startDate.label" /></span>
										<form:input path="startDate" id="tournament-start-date" type="date" cssClass="field__control" required="required" />
										<form:errors path="startDate" cssClass="field__error" element="span" />
									</label>
									<label class="field" for="tournament-start-time">
										<span class="field__label"><spring:message code="tournament.form.startTime.label" /></span>
										<form:input path="startTime" id="tournament-start-time" type="time" cssClass="field__control" required="required" />
										<form:errors path="startTime" cssClass="field__error" element="span" />
									</label>
								</div>
								<div class="form-card__grid form-card__grid--datetime">
									<label class="field" for="tournament-end-date">
										<span class="field__label"><spring:message code="tournament.form.endDate.label" /></span>
										<form:input path="endDate" id="tournament-end-date" type="date" cssClass="field__control" required="required" />
										<form:errors path="endDate" cssClass="field__error" element="span" />
									</label>
									<label class="field" for="tournament-end-time">
										<span class="field__label"><spring:message code="tournament.form.endTime.label" /></span>
										<form:input path="endTime" id="tournament-end-time" type="time" cssClass="field__control" required="required" />
										<form:errors path="endTime" cssClass="field__error" element="span" />
									</label>
								</div>
							</div>
						</article>

						</div>
						<div class="create-form__column">
						<article class="panel form-card">
							<span class="detail-label"><spring:message code="tournament.create.section.logistics" /></span>
							<h2 class="form-card__title"><spring:message code="tournament.create.section.logistics.subtitle" /></h2>
							<div class="create-stack">
								<label class="field" for="tournament-address">
									<span class="field__label"><spring:message code="tournament.form.location.label" /></span>
									<form:input path="address" id="tournament-address" cssClass="field__control" required="required" placeholder="${locationPlaceholder}" />
									<form:errors path="address" cssClass="field__error" element="span" />
								</label>
								<form:hidden path="latitude" id="tournament-location-latitude" />
								<form:hidden path="longitude" id="tournament-location-longitude" />
								<form:errors path="latitude" cssClass="field__error" element="span" />
								<form:errors path="longitude" cssClass="field__error" element="span" />
								<c:if test="${mapPickerEnabled}">
									<c:url var="appRootUrl" value="/" />
									<c:set var="contextAwareMapTileUrlTemplate"
										value="${appRootUrl}${fn:substring(mapTileUrlTemplate, 1, fn:length(mapTileUrlTemplate))}" />
									<section
										class="location-picker"
										data-location-picker="true"
										data-latitude-input="#tournament-location-latitude"
										data-longitude-input="#tournament-location-longitude"
										data-tile-url-template="${contextAwareMapTileUrlTemplate}"
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
							</div>
						</article>

						<article class="panel upload-card">
							<span class="detail-label"><spring:message code="host.section.banner" /></span>
							<h2 class="form-card__title"><spring:message code="host.section.banner.subtitle" /></h2>
							<c:if test="${not empty currentBannerImageUrl}">
								<c:url var="currentBannerSrc" value="${currentBannerImageUrl}" />
								<div class="upload-card__preview">
									<span class="upload-card__preview-label">
										<spring:message code="host.form.bannerImage.current" />
									</span>
									<img class="upload-card__preview-image" src="${currentBannerSrc}" alt="" loading="lazy" decoding="async" />
								</div>
							</c:if>
							<label class="field upload-card__field" for="tournament-banner-image">
								<span class="field__label"><spring:message code="host.form.bannerImage" /></span>
								<span class="upload-card__dropzone">
									<span class="upload-card__dropzone-icon" aria-hidden="true">
										<icon:photoFrame />
									</span>
									<span class="upload-card__dropzone-copy">
										<span class="upload-card__dropzone-title">
											<spring:message code="host.form.bannerImage.guidanceTitle" />
										</span>
										<span class="field__hint">
											<spring:message code="host.form.bannerImage.hint" />
										</span>
									</span>
									<form:input path="bannerImage" id="tournament-banner-image" type="file"
										accept="image/png,image/jpeg,image/webp,image/gif"
										cssClass="upload-card__file-input" />
								</span>
							</label>
						</article>

						<article class="panel form-card">
							<span class="detail-label"><spring:message code="tournament.create.section.structure" /></span>
							<h2 class="form-card__title"><spring:message code="tournament.create.section.structure.subtitle" /></h2>
							<div class="form-card__grid">
								<label class="field" for="tournament-bracket-size">
									<span class="field__label"><spring:message code="tournament.form.bracketSize.label" /></span>
									<span class="field__select-wrap">
										<form:select path="bracketSize" id="tournament-bracket-size" cssClass="field__control field__control--select" required="required">
											<form:option value="4" label="4" />
											<form:option value="8" label="8" />
											<form:option value="16" label="16" />
										</form:select>
									</span>
									<form:errors path="bracketSize" cssClass="field__error" element="span" />
								</label>

								<label class="field" for="tournament-team-size">
									<span class="field__label"><spring:message code="tournament.form.teamSize.label" /></span>
									<span class="field__select-wrap">
										<form:select path="teamSize" id="tournament-team-size" cssClass="field__control field__control--select" required="required" data-tournament-team-size-select="true">
											<form:option value="1" label="1" />
											<form:option value="2" label="2" />
											<form:option value="3" label="3" />
											<form:option value="4" label="4" />
											<form:option value="5" label="5" />
											<form:option value="6" label="6" />
											<form:option value="7" label="7" />
											<form:option value="8" label="8" />
											<form:option value="9" label="9" />
											<form:option value="10" label="10" />
											<form:option value="11" label="11" />
										</form:select>
									</span>
									<form:errors path="teamSize" cssClass="field__error" element="span" />
								</label>

								<label class="field" for="tournament-price">
									<span class="field__label"><spring:message code="tournament.form.pricePerPlayer.label" /></span>
									<form:input path="pricePerPlayer" id="tournament-price" type="number" min="0" step="0.01" cssClass="field__control" required="required" />
									<form:errors path="pricePerPlayer" cssClass="field__error" element="span" />
								</label>
							</div>
						</article>
						</div>

						<div class="create-layout__actions">
							<ui:button label="${submitLabel}" type="submit" id="create-tournament-button" size="lg" fullWidth="${true}" className="create-layout__submit" />
						</div>
					</form:form>
				</section>
			</main>
		</div>
	</body>
</html>
