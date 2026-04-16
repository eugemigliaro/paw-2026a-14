<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
	<spring:message var="pageTitle" code="page.title.hostMode" />
	<!DOCTYPE html>
	<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell">
				<section class="create-layout__main">
					<header class="page-heading">
						<p class="eyebrow"><spring:message code="host.eyebrow" /></p>
						<h1 class="page-heading__title"><spring:message code="host.title" /></h1>
						<p class="page-heading__description">
							<spring:message code="host.description" />
						</p>
					</header>

					<spring:message var="publishingLabel" code="host.form.submitting" />
					<spring:message var="emailPlaceholder" code="host.form.email.placeholder" />
					<spring:message var="titlePlaceholder" code="host.form.title.placeholder" />
					<spring:message var="descPlaceholder" code="host.form.description.placeholder" />
					<spring:message var="locationPlaceholder" code="host.form.location.placeholder" />
					<spring:message var="sportPadel" code="sport.padel" />
					<spring:message var="sportFootball" code="sport.football" />
					<spring:message var="sportTennis" code="sport.tennis" />
					<spring:message var="sportBasketball" code="sport.basketball" />
					<spring:message var="publishLabel" code="host.form.submit" />
					<c:url var="createMatchAction" value="/host/matches/new" />

					<form:form
						method="post"
						action="${createMatchAction}"
						modelAttribute="createEventForm"
						enctype="multipart/form-data"
						id="create-match-form"
						data-submit-guard="true"
						data-submit-loading-label="${publishingLabel}"
						cssClass="create-form"
					>
						<input
							type="hidden"
							name="timezone"
							id="match-timezone"
							value="<c:out value='${createEventForm.timezone}' />"
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
								<label class="field" for="match-email">
									<span class="field__label"><spring:message code="host.form.email" /></span>
									<form:input
										path="email"
										id="match-email"
										type="email"
										cssClass="field__control"
										required="required"
										placeholder="${emailPlaceholder}"
									/>
									<form:errors
										path="email"
										cssClass="field__error"
										element="span"
									/>
								</label>

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
										>
											<form:option value="padel" label="${sportPadel}" />
											<form:option value="football" label="${sportFootball}" />
											<form:option value="tennis" label="${sportTennis}" />
											<form:option value="basketball" label="${sportBasketball}" />
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

								<label class="field" for="match-date">
									<span class="field__label"><spring:message code="host.form.date" /></span>
									<form:input
										path="eventDate"
										id="match-date"
										type="date"
										cssClass="field__control"
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
									/>
									<form:errors
										path="eventTime"
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
									/>
									<form:errors
										path="endTime"
										cssClass="field__error"
										element="span"
									/>
								</label>
							</div>
						</article>

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
									/>
									<form:errors
										path="pricePerPlayer"
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
								label="${publishLabel}"
								type="submit"
								id="publish-match-button"
								size="lg"
								fullWidth="${true}"
								className="create-layout__submit"
							/>
						</div>
					</form:form>
				</section>
			</main>
		</div>
	</body>
</html>
