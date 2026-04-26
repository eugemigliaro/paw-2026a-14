<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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

			<main class="page-shell public-profile-shell">
				<section class="panel public-profile-panel">
					<ui:returnButton />
					<header class="page-heading">
						<h1 class="page-heading__title public-profile-panel__title"><c:out value="${profileTitle}" /></h1>
						<p class="page-heading__description public-profile-panel__description">
							<c:out value="${profileDescription}" />
						</p>
					</header>

					<span class="field__label"><c:out value="${profileEyebrow}" /></span>
					<article class="panel public-profile-avatar-panel">
						<div class="public-profile-avatar-panel__content">
							<c:url var="profileImageSrc" value="${profilePage.profileImageUrl}" />
							<img
								class="public-profile-avatar-panel__image"
								src="${profileImageSrc}"
								alt="${profileImageAlt}"
								loading="eager"
								decoding="async" />
						</div>
					</article>

					<div class="public-profile-summary">
						<label class="field" for="public-profile-username">
							<span class="field__label"><c:out value="${profileUsernameLabel}" /></span>
							<input
								id="public-profile-username"
								type="text"
								class="field__control public-profile-summary__control"
								value="<c:out value='${profilePage.username}' />"
								readonly="readonly"
								aria-readonly="true" />
						</label>

						<c:if test="${not empty profilePage.name}">
							<label class="field" for="public-profile-name">
								<span class="field__label"><c:out value="${profileNameLabel}" /></span>
								<input
									id="public-profile-name"
									type="text"
									class="field__control public-profile-summary__control"
									value="<c:out value='${profilePage.name}' />"
									readonly="readonly"
									aria-readonly="true" />
							</label>
						</c:if>

						<c:if test="${not empty profilePage.lastName}">
							<label class="field" for="public-profile-last-name">
								<span class="field__label"><c:out value="${profileLastNameLabel}" /></span>
								<input
									id="public-profile-last-name"
									type="text"
									class="field__control public-profile-summary__control"
									value="<c:out value='${profilePage.lastName}' />"
									readonly="readonly"
									aria-readonly="true" />
							</label>
						</c:if>

						<c:if test="${not empty profilePage.phone}">
							<label class="field" for="public-profile-phone">
								<span class="field__label"><c:out value="${profilePhoneLabel}" /></span>
								<input
									id="public-profile-phone"
									type="tel"
									class="field__control public-profile-summary__control"
									value="<c:out value='${profilePage.phone}' />"
									readonly="readonly"
									aria-readonly="true" />
							</label>
						</c:if>
					</div>

					<c:if test="${not empty profileEditHref}">
						<div class="public-profile-actions">
							<c:url var="profileEditAction" value="${profileEditHref}" />
							<ui:button label="${profileEditLabel}" href="${profileEditAction}" variant="secondary" />
						</div>
					</c:if>
				</section>

				<section id="reviews" class="panel public-profile-panel public-profile-reviews">
					<header class="page-heading public-profile-reviews__header">
						<h2 class="public-profile-reviews__title"><spring:message code="profile.reviews.title" /></h2>
						<p class="page-heading__description public-profile-panel__description">
							<spring:message code="profile.reviews.description" />
						</p>
					</header>

					<c:if test="${param.review eq 'saved'}">
						<div class="notice notice--success">
							<spring:message code="profile.reviews.saved" />
						</div>
					</c:if>
					<c:if test="${param.review eq 'deleted'}">
						<div class="notice notice--info">
							<spring:message code="profile.reviews.deleted" />
						</div>
					</c:if>
					<c:if test="${not empty param.reviewError}">
						<div class="notice notice--error">
							<c:choose>
								<c:when test="${param.reviewError eq 'not_eligible'}">
									<spring:message code="profile.reviews.error.notEligible" />
								</c:when>
								<c:when test="${param.reviewError eq 'self_review'}">
									<spring:message code="profile.reviews.error.selfReview" />
								</c:when>
								<c:when test="${param.reviewError eq 'comment_too_long'}">
									<spring:message code="profile.reviews.error.commentTooLong" />
								</c:when>
								<c:when test="${param.reviewError eq 'not_found'}">
									<spring:message code="profile.reviews.error.notFound" />
								</c:when>
								<c:otherwise>
									<spring:message code="profile.reviews.error.invalid" />
								</c:otherwise>
							</c:choose>
						</div>
					</c:if>

					<spring:message var="reviewSummaryAria" code="profile.reviews.summaryAria" />
					<div class="public-profile-review-stats" aria-label="${reviewSummaryAria}">
						<div class="public-profile-review-stat public-profile-review-stat--like">
							<span class="public-profile-review-stat__value">
								<span class="public-profile-review-icon public-profile-review-icon--like" aria-hidden="true">
									<svg viewBox="0 0 24 24" focusable="false">
										<path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.8l4.2-8.2a1.7 1.7 0 0 1 3.1 1.3L15.4 9H20a2 2 0 0 1 2 2.3l-1.1 7A4.4 4.4 0 0 1 16.5 22H9Z" />
									</svg>
								</span>
								<c:out value="${reviewSummary.likeCount}" />
							</span>
							<span class="public-profile-review-stat__label"><spring:message code="profile.reviews.likes" /></span>
						</div>
						<div class="public-profile-review-stat public-profile-review-stat--dislike">
							<span class="public-profile-review-stat__value">
								<span class="public-profile-review-icon public-profile-review-icon--dislike" aria-hidden="true">
									<svg viewBox="0 0 24 24" focusable="false">
										<path d="M17 2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3V2Zm-2 0v11.2l-4.2 8.2a1.7 1.7 0 0 1-3.1-1.3L8.6 15H4a2 2 0 0 1-2-2.3l1.1-7A4.4 4.4 0 0 1 7.5 2H15Z" />
									</svg>
								</span>
								<c:out value="${reviewSummary.dislikeCount}" />
							</span>
							<span class="public-profile-review-stat__label"><spring:message code="profile.reviews.dislikes" /></span>
						</div>
					</div>

					<c:if test="${reviewCanSubmit}">
						<c:url var="reviewAction" value="${reviewActionPath}" />
						<c:url var="reviewFormHref" value="${reviewFormPath}" />
						<spring:message var="quickReviewAria" code="profile.reviews.quick.aria" />
						<spring:message var="quickLikeLabel" code="profile.reviews.quick.like" />
						<spring:message var="quickDislikeLabel" code="profile.reviews.quick.dislike" />
						<spring:message var="leaveReviewLabel" code="profile.reviews.form.open" />
						<spring:message var="editReviewLabel" code="profile.reviews.form.edit" />
						<spring:message var="deleteReviewLabel" code="profile.reviews.form.delete" />
						<spring:message var="reviewMenuAria" code="profile.reviews.menu.aria" />
						<spring:message var="reviewMenuLabel" code="profile.reviews.menu.label" />
						<div class="public-profile-review-actions">
							<div class="public-profile-review-quick-actions" aria-label="${quickReviewAria}">
								<form method="post" action="${reviewAction}" class="public-profile-review-quick-form">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="hidden" name="reaction" value="like" />
									<button
										type="submit"
										class="public-profile-review-quick-action public-profile-review-quick-action--like ${not empty viewerReview and viewerReview.reaction.dbValue eq 'like' ? 'is-selected' : ''}"
										aria-label="${quickLikeLabel}">
										<span class="public-profile-review-icon public-profile-review-icon--like" aria-hidden="true">
											<svg viewBox="0 0 24 24" focusable="false">
												<path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.8l4.2-8.2a1.7 1.7 0 0 1 3.1 1.3L15.4 9H20a2 2 0 0 1 2 2.3l-1.1 7A4.4 4.4 0 0 1 16.5 22H9Z" />
											</svg>
										</span>
										<span><spring:message code="profile.reviews.reaction.like" /></span>
									</button>
								</form>
								<form method="post" action="${reviewAction}" class="public-profile-review-quick-form">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="hidden" name="reaction" value="dislike" />
									<button
										type="submit"
										class="public-profile-review-quick-action public-profile-review-quick-action--dislike ${not empty viewerReview and viewerReview.reaction.dbValue eq 'dislike' ? 'is-selected' : ''}"
										aria-label="${quickDislikeLabel}">
										<span class="public-profile-review-icon public-profile-review-icon--dislike" aria-hidden="true">
											<svg viewBox="0 0 24 24" focusable="false">
												<path d="M17 2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3V2Zm-2 0v11.2l-4.2 8.2a1.7 1.7 0 0 1-3.1-1.3L8.6 15H4a2 2 0 0 1-2-2.3l1.1-7A4.4 4.4 0 0 1 7.5 2H15Z" />
											</svg>
										</span>
										<span><spring:message code="profile.reviews.reaction.dislike" /></span>
									</button>
								</form>
							</div>
							<c:choose>
								<c:when test="${not empty viewerReview}">
									<ui:overflowMenu
										ariaLabel="${reviewMenuAria}"
										menuAriaLabel="${reviewMenuLabel}"
										className="public-profile-review-menu">
										<a class="overflow-menu__item" href="${reviewFormHref}" role="menuitem">
											<c:out value="${editReviewLabel}" />
										</a>
										<c:url var="reviewDeleteAction" value="${reviewDeletePath}" />
										<form method="post" action="${reviewDeleteAction}">
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<button class="overflow-menu__item overflow-menu__item--danger" type="submit" role="menuitem">
												<c:out value="${deleteReviewLabel}" />
											</button>
										</form>
									</ui:overflowMenu>
								</c:when>
								<c:otherwise>
									<ui:button label="${leaveReviewLabel}" href="${reviewFormHref}" variant="secondary" />
								</c:otherwise>
							</c:choose>
						</div>
					</c:if>

					<c:if test="${reviewFormVisible}">
						<section class="public-profile-review-form-panel" aria-labelledby="review-form-title">
							<h3 id="review-form-title" class="public-profile-review-form-panel__title">
								<c:choose>
									<c:when test="${not empty viewerReview}">
										<spring:message code="profile.reviews.form.editTitle" />
									</c:when>
									<c:otherwise>
										<spring:message code="profile.reviews.form.title" />
									</c:otherwise>
								</c:choose>
							</h3>
							<form method="post" action="${reviewAction}" class="public-profile-review-form">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<fieldset class="public-profile-review-form__fieldset">
									<legend class="field__label"><spring:message code="profile.reviews.form.reaction" /></legend>
									<label class="public-profile-review-option">
										<input
											type="radio"
											name="reaction"
											value="like"
											required="required"
											${empty viewerReview or viewerReview.reaction.dbValue eq 'like' ? 'checked="checked"' : ''} />
										<span class="public-profile-review-icon public-profile-review-icon--like" aria-hidden="true">
											<svg viewBox="0 0 24 24" focusable="false">
												<path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.8l4.2-8.2a1.7 1.7 0 0 1 3.1 1.3L15.4 9H20a2 2 0 0 1 2 2.3l-1.1 7A4.4 4.4 0 0 1 16.5 22H9Z" />
											</svg>
										</span>
										<span><spring:message code="profile.reviews.reaction.like" /></span>
									</label>
									<label class="public-profile-review-option">
										<input
											type="radio"
											name="reaction"
											value="dislike"
											required="required"
											${not empty viewerReview and viewerReview.reaction.dbValue eq 'dislike' ? 'checked="checked"' : ''} />
										<span class="public-profile-review-icon public-profile-review-icon--dislike" aria-hidden="true">
											<svg viewBox="0 0 24 24" focusable="false">
												<path d="M17 2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3V2Zm-2 0v11.2l-4.2 8.2a1.7 1.7 0 0 1-3.1-1.3L8.6 15H4a2 2 0 0 1-2-2.3l1.1-7A4.4 4.4 0 0 1 7.5 2H15Z" />
											</svg>
										</span>
										<span><spring:message code="profile.reviews.reaction.dislike" /></span>
									</label>
								</fieldset>
								<label class="field" for="player-review-comment">
									<span class="field__label"><spring:message code="profile.reviews.form.comment" /></span>
									<textarea
										id="player-review-comment"
										name="comment"
										class="field__control public-profile-review-form__comment"
										maxlength="1000"
										rows="4"><c:out value="${viewerReview.comment}" /></textarea>
								</label>
								<div class="public-profile-review-form__actions">
									<spring:message var="saveReviewLabel" code="profile.reviews.form.save" />
									<ui:button label="${saveReviewLabel}" type="submit" />
								</div>
							</form>
						</section>
					</c:if>

					<c:choose>
						<c:when test="${empty profileReviews}">
							<p class="public-profile-reviews__empty"><spring:message code="profile.reviews.empty" /></p>
						</c:when>
						<c:otherwise>
							<ul class="public-profile-review-list">
								<c:forEach var="review" items="${profileReviews}">
									<li class="public-profile-review-list__item">
										<div class="public-profile-review-list__meta">
											<c:choose>
												<c:when test="${not empty review.reviewerProfileHref}">
													<c:url var="reviewerProfileHref" value="${review.reviewerProfileHref}" />
													<a class="public-profile-review-list__reviewer" href="${reviewerProfileHref}">
														<c:out value="${review.reviewerUsername}" />
													</a>
												</c:when>
												<c:otherwise>
													<strong class="public-profile-review-list__reviewer"><c:out value="${review.reviewerUsername}" /></strong>
												</c:otherwise>
											</c:choose>
											<span class="public-profile-review-list__reaction public-profile-review-list__reaction--${review.reaction}">
												<span class="public-profile-review-icon public-profile-review-icon--${review.reaction}" aria-hidden="true">
													<c:choose>
														<c:when test="${review.reaction eq 'like'}">
															<svg viewBox="0 0 24 24" focusable="false">
																<path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.8l4.2-8.2a1.7 1.7 0 0 1 3.1 1.3L15.4 9H20a2 2 0 0 1 2 2.3l-1.1 7A4.4 4.4 0 0 1 16.5 22H9Z" />
															</svg>
														</c:when>
														<c:otherwise>
															<svg viewBox="0 0 24 24" focusable="false">
																<path d="M17 2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3V2Zm-2 0v11.2l-4.2 8.2a1.7 1.7 0 0 1-3.1-1.3L8.6 15H4a2 2 0 0 1-2-2.3l1.1-7A4.4 4.4 0 0 1 7.5 2H15Z" />
															</svg>
														</c:otherwise>
													</c:choose>
												</span>
												<c:out value="${review.reactionLabel}" />
											</span>
											<c:if test="${not empty review.updatedAtLabel}">
												<span class="public-profile-review-list__date"><c:out value="${review.updatedAtLabel}" /></span>
											</c:if>
										</div>
										<c:if test="${not empty review.comment}">
											<p class="public-profile-review-list__comment"><c:out value="${review.comment}" /></p>
										</c:if>
									</li>
								</c:forEach>
							</ul>
						</c:otherwise>
					</c:choose>
				</section>
			</main>
		</div>
	</body>
</html>
