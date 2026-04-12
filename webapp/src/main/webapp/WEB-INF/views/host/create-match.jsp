<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %> <%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="form" uri="http://www.springframework.org/tags/form" %> <%@ taglib
prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | Host Mode" />
<!DOCTYPE html>
<html lang="en">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell">
				<section class="create-layout__main">
					<header class="page-heading">
						<p class="eyebrow">Hosting</p>
						<h1 class="page-heading__title">Create your match</h1>
						<p class="page-heading__description">
							Share the sport, location, schedule, and details players need
							before they reserve a spot.
						</p>
					</header>

					<form:form
						method="post"
						action="${pageContext.request.contextPath}/host/matches/new"
						modelAttribute="createEventForm"
						enctype="multipart/form-data"
						id="create-match-form"
						data-submit-guard="true"
						data-submit-loading-label="Publishing..."
						cssClass="create-form"
					>
						<form:hidden path="timezone" id="match-timezone" />
						<c:if test="${not empty formError}">
							<p class="field__error">
								<c:out value="${formError}" />
							</p>
						</c:if>
						<article class="panel form-card">
							<span class="detail-label">01 - The Basics</span>
							<h2 class="form-card__title">
								Give the match a clear point of view
							</h2>
							<div class="create-stack">
								<label class="field" for="match-email">
									<span class="field__label">Your email</span>
									<form:input
										path="email"
										id="match-email"
										type="email"
										cssClass="field__control"
										required="required"
										placeholder="you@example.com"
									/>
									<form:errors
										path="email"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-title">
									<span class="field__label"
										>Match title</span
									>
									<form:input
										path="title"
										id="match-title"
										cssClass="field__control"
										required="required"
										placeholder="Saturday Morning Padel Championship"
									/>
									<form:errors
										path="title"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-sport">
									<span class="field__label">Category</span>
									<span class="field__select-wrap">
										<form:select
											path="sport"
											id="match-sport"
											cssClass="field__control field__control--select"
										>
											<form:option value="padel"
												>Padel</form:option
											>
											<form:option value="football"
												>Football</form:option
											>
											<form:option value="tennis"
												>Tennis</form:option
											>
											<form:option value="basketball"
												>Basketball</form:option
											>
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
										>Description</span
									>
									<form:textarea
										path="description"
										id="match-description"
										cssClass="field__control field__control--textarea"
										placeholder="Tell participants what to expect from the format, venue, and vibe."
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
							<span class="detail-label">02 - Logistics</span>
							<h2 class="form-card__title">
								Set the venue and time
							</h2>
							<div class="create-stack">
								<label class="field" for="match-address">
									<span class="field__label">Location</span>
									<form:input
										path="address"
										id="match-address"
										cssClass="field__control"
										required="required"
										placeholder="Enter venue address"
									/>
									<form:errors
										path="address"
										cssClass="field__error"
										element="span"
									/>
								</label>

								<label class="field" for="match-date">
									<span class="field__label">Date</span>
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
									<span class="field__label">Start time</span>
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
									<span class="field__label">End time</span>
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
							<span class="detail-label">03 - Capacity</span>
							<h2 class="form-card__title">
								Control who joins and match price
							</h2>
							<div class="form-card__grid form-card__grid--three">
								<label class="field" for="match-capacity">
									<span class="field__label">Capacity</span>
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
										>Price per player</span
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
								>04 - Banner (optional)</span
							>
							<h2 class="form-card__title">
								Add a cover image for feed and match detail
							</h2>
							<label class="field" for="match-banner-image">
								<span class="field__label">Banner image</span>
								<form:input
									path="bannerImage"
									id="match-banner-image"
									type="file"
									accept="image/png,image/jpeg,image/webp,image/gif"
									cssClass="field__control upload-card__file-input"
								/>
								<span class="field__hint"
									>Accepted formats: JPG, PNG, WEBP, GIF. Max size 5 MB.</span
								>
							</label>
							<div class="upload-card__guidance" role="note">
								<p class="upload-card__guidance-title">Upload cover photo</p>
								<p class="upload-card__guidance-copy">
									Recommended size: 1600 x 900 px
								</p>
								<p class="upload-card__guidance-copy">
									The banner appears on the feed and match detail page.
								</p>
							</div>
						</article>

						<div class="create-layout__actions">
							<ui:button
								label="Publish Match"
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
		<script src="${pageContext.request.contextPath}/js/create-match.js"></script>
	</body>
</html>
