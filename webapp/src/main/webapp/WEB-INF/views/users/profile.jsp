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

					<div class="public-profile-topbar">
						<ui:returnButton />

						<c:if test="${not empty profileEditHref or reportUserCanSubmit}">
							<div class="public-profile-actions">
								<c:if test="${not empty profileEditHref}">
									<c:url var="profileEditAction" value="${profileEditHref}" />
									<ui:button label="${profileEditLabel}" href="${profileEditAction}" variant="secondary" />
								</c:if>
								<c:if test="${reportUserCanSubmit}">
									<c:url var="reportUserHref" value="/reports/users/${profilePage.username}" />
									<spring:message var="reportUserLabel" code="moderation.report.user.submit" />
									<ui:button label="${reportUserLabel}" href="${reportUserHref}" variant="danger" />
								</c:if>
							</div>
						</c:if>
					</div>
					<div class="public-profile-hero">
						<div class="public-profile-avatar-panel">
							<div class="public-profile-avatar-panel__content">
								<c:url var="profileImageSrc" value="${profilePage.profileImageUrl}" />
								<img
									class="public-profile-avatar-panel__image"
									src="${profileImageSrc}"
									alt="${profileImageAlt}"
									loading="eager"
									decoding="async" />
							</div>
						</div>

						<div class="public-profile-summary">
							<c:if test="${profileBanned}">
								<div class="notice notice--error">
									<strong><c:out value="${profileBannedLabel}" /></strong>
									<c:if test="${not empty profileBannedUntil}">
										<span> · <c:out value="${profileBannedUntil}" /></span>
									</c:if>
									<c:if test="${not empty profileBannedReason}">
										<div><c:out value="${profileBannedReason}" /></div>
									</c:if>
								</div>
							</c:if>

							<p class="public-profile-summary__line">
								<span class="field__label">
									<spring:message code="profile.public.username" />:
								</span>
								<span class="public-profile-summary__value">
									<c:out value="${profilePage.username}" />
								</span>
							</p>

							<p class="public-profile-summary__line">
								<span class="field__label">
									<c:out value="${profileFullNameLabel}" />:
								</span>
								<span class="public-profile-summary__value">
									<c:out value="${profilePage.name}" />
									<c:if test="${not empty profilePage.lastName}">
										<c:out value=" " />
										<c:out value="${profilePage.lastName}" />
									</c:if>
								</span>
							</p>

							<div class="public-profile-summary__line public-profile-summary__line--inline">
								<span class="public-profile-summary__pair">
									<span class="field__label">
										<c:out value="${profileEmailLabel}" />:
									</span>
									<span class="public-profile-summary__value">
										<c:out value="${profilePage.email}" />
									</span>
								</span>
								<c:if test="${not empty profilePage.phone}">
									<span class="public-profile-summary__pair">
										<span class="field__label">
											<c:out value="${profilePhoneLabel}" />:
										</span>
										<span class="public-profile-summary__value">
											<c:out value="${profilePage.phone}" />
										</span>
									</span>
								</c:if>
							</div>
						</div>
					</div>

				</section>
				<section class="panel public-profile-panel public-profile-ratings">
					<header class="public-profile-ratings__header">
						<h2 class="public-profile-ratings__title">${profileRatingsTitle}</h2>
					</header>
					<c:choose>
						<c:when test="${empty profileRatings}">
							<p class="public-profile-ratings__empty">${profileRatingsEmpty}</p>
						</c:when>
						<c:otherwise>
							<ul class="public-profile-ratings__list">
								<c:forEach var="rating" items="${profileRatings}">
									<li class="public-profile-ratings__item">
										<span class="public-profile-ratings__sport">${rating.sportLabel}</span>
										<span class="public-profile-ratings__elo">${rating.elo}</span>
									</li>
								</c:forEach>
							</ul>
						</c:otherwise>
					</c:choose>
				</section>
				<section id="reviews" class="panel public-profile-panel public-profile-reviews">
					<header class="page-heading public-profile-reviews__header">
						<h2 class="public-profile-reviews__title"><spring:message code="profile.reviews.title" /></h2>
						<p class="page-heading__description public-profile-panel__description">
							<spring:message code="profile.reviews.description" />
						</p>
					</header>

						<c:if test="${reviewStatus eq 'saved'}">
							<div class="notice notice--success">
								<spring:message code="profile.reviews.saved" />
							</div>
						</c:if>
						<c:if test="${reviewStatus eq 'deleted'}">
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
							<c:if test="${reportStatus eq 'sent'}">
								<div class="notice notice--success">
									<spring:message code="moderation.report.sent" />
								</div>
							</c:if>


					<spring:message var="reviewSummaryAria" code="profile.reviews.summaryAria" />
					<c:if test="${reviewCanSubmit}">
						<c:url var="reviewAction" value="${reviewActionPath}" />
						<c:url var="reviewFormHref" value="${reviewFormPath}" />
						<c:url var="reviewSectionHref" value="${reviewSectionPath}" />
						<spring:message var="quickLikeLabel" code="profile.reviews.quick.like" />
						<spring:message var="quickDislikeLabel" code="profile.reviews.quick.dislike" />
						<spring:message var="addCommentLabel" code="profile.reviews.form.addComment" />
						<spring:message var="editCommentLabel" code="profile.reviews.form.editComment" />
						<spring:message var="deleteReviewLabel" code="profile.reviews.form.delete" />
					</c:if>
					<div class="public-profile-review-stats" aria-label="${reviewSummaryAria}">
						<c:choose>
							<c:when test="${reviewCanSubmit}">
								<form method="post" action="${reviewAction}" class="public-profile-review-stat-form">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="hidden" name="reaction" value="like" />
									<c:if test="${not empty viewerReview.comment}">
										<input type="hidden" name="comment" value="<c:out value='${viewerReview.comment}' />" />
									</c:if>
									<button
										type="submit"
										class="public-profile-review-stat public-profile-review-stat--like public-profile-review-stat--button ${not empty viewerReview and viewerReview.reaction.dbValue eq 'like' ? 'is-selected' : ''}"
										aria-label="${quickLikeLabel}">
										<span class="public-profile-review-stat__value">
											<span class="public-profile-review-icon public-profile-review-icon--like" aria-hidden="true">
												<svg viewBox="0 0 24 24" focusable="false">
													<path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.8l4.2-8.2a1.7 1.7 0 0 1 3.1 1.3L15.4 9H20a2 2 0 0 1 2 2.3l-1.1 7A4.4 4.4 0 0 1 16.5 22H9Z" />
												</svg>
											</span>
											<c:out value="${reviewSummary.likeCount}" />
										</span>
										<span class="public-profile-review-stat__label"><c:out value="${reviewLikeLabel}" /></span>
									</button>
								</form>
							</c:when>
							<c:otherwise>
								<button type="button" disabled="disabled" aria-disabled="true" aria-describedby="reviewLockedNote"
									class="public-profile-review-stat public-profile-review-stat--like public-profile-review-stat--locked">
									<span class="public-profile-review-stat__value">
										<span class="public-profile-review-icon public-profile-review-icon--like" aria-hidden="true">
											<svg viewBox="0 0 24 24" focusable="false">
												<path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.8l4.2-8.2a1.7 1.7 0 0 1 3.1 1.3L15.4 9H20a2 2 0 0 1 2 2.3l-1.1 7A4.4 4.4 0 0 1 16.5 22H9Z" />
											</svg>
										</span>
										<c:out value="${reviewSummary.likeCount}" />
									</span>
									<span class="public-profile-review-stat__label"><c:out value="${reviewLikeLabel}" /></span>
								</button>
							</c:otherwise>
						</c:choose>
						<c:choose>
							<c:when test="${reviewCanSubmit}">
								<form method="post" action="${reviewAction}" class="public-profile-review-stat-form">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="hidden" name="reaction" value="dislike" />
									<c:if test="${not empty viewerReview.comment}">
										<input type="hidden" name="comment" value="<c:out value='${viewerReview.comment}' />" />
									</c:if>
									<button
										type="submit"
										class="public-profile-review-stat public-profile-review-stat--dislike public-profile-review-stat--button ${not empty viewerReview and viewerReview.reaction.dbValue eq 'dislike' ? 'is-selected' : ''}"
										aria-label="${quickDislikeLabel}">
										<span class="public-profile-review-stat__value">
											<span class="public-profile-review-icon public-profile-review-icon--dislike" aria-hidden="true">
												<svg viewBox="0 0 24 24" focusable="false">
													<path d="M17 2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3V2Zm-2 0v11.2l-4.2 8.2a1.7 1.7 0 0 1-3.1-1.3L8.6 15H4a2 2 0 0 1-2-2.3l1.1-7A4.4 4.4 0 0 1 7.5 2H15Z" />
												</svg>
											</span>
											<c:out value="${reviewSummary.dislikeCount}" />
										</span>
										<span class="public-profile-review-stat__label"><c:out value="${reviewDislikeLabel}" /></span>
									</button>
								</form>
							</c:when>
							<c:otherwise>
								<button type="button" disabled="disabled" aria-disabled="true" aria-describedby="reviewLockedNote"
									class="public-profile-review-stat public-profile-review-stat--dislike public-profile-review-stat--locked">
									<span class="public-profile-review-stat__value">
										<span class="public-profile-review-icon public-profile-review-icon--dislike" aria-hidden="true">
											<svg viewBox="0 0 24 24" focusable="false">
												<path d="M17 2h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-3V2Zm-2 0v11.2l-4.2 8.2a1.7 1.7 0 0 1-3.1-1.3L8.6 15H4a2 2 0 0 1-2-2.3l1.1-7A4.4 4.4 0 0 1 7.5 2H15Z" />
											</svg>
										</span>
										<c:out value="${reviewSummary.dislikeCount}" />
									</span>
									<span class="public-profile-review-stat__label"><c:out value="${reviewDislikeLabel}" /></span>
								</button>
							</c:otherwise>
						</c:choose>
					</div>

					<c:if test="${not empty reviewLockedMessage}">
						<p id="reviewLockedNote" class="public-profile-review-locked">
							<span class="public-profile-review-locked__icon" aria-hidden="true">
								<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" focusable="false">
									<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
									<path d="M7 11V7a5 5 0 0 1 10 0v4"/>
								</svg>
							</span>
							<span class="public-profile-review-locked__text"><c:out value="${reviewLockedMessage}" /></span>
						</p>
					</c:if>

					<c:if test="${reviewCanSubmit and not empty viewerReview}">
						<div class="public-profile-review-mine">
							<c:choose>
								<c:when test="${empty viewerReview.comment}">
									<p class="public-profile-review-mine__prompt"><c:out value="${reviewCommentPromptLabel}" /></p>
								</c:when>
								<c:otherwise>
									<p class="public-profile-review-mine__comment"><c:out value="${viewerReview.comment}" /></p>
								</c:otherwise>
							</c:choose>
							<div class="public-profile-review-mine__actions">
								<a href="${reviewFormHref}"
									class="btn btn--ghost btn--md public-profile-review-mine__edit"
									aria-label="${empty viewerReview.comment ? addCommentLabel : editCommentLabel}">
									<span class="public-profile-review-mine__icon" aria-hidden="true">
										<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" focusable="false">
											<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
											<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
										</svg>
									</span>
									<c:out value="${empty viewerReview.comment ? addCommentLabel : editCommentLabel}" />
								</a>
								<c:url var="reviewDeleteAction" value="${reviewDeletePath}" />
								<form method="post" action="${reviewDeleteAction}" class="public-profile-review-mine__delete-form">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<button type="submit"
										class="btn btn--secondary btn--md public-profile-review-mine__delete"
										aria-label="${deleteReviewLabel}">
										<span class="public-profile-review-mine__icon" aria-hidden="true">
											<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" focusable="false">
												<polyline points="3 6 5 6 21 6"/>
												<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
												<path d="M10 11v6"/>
												<path d="M14 11v6"/>
												<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
											</svg>
										</span>
									</button>
								</form>
							</div>
						</div>
					</c:if>

					<c:if test="${reviewFormVisible and not empty viewerReview}">
						<section class="public-profile-review-form-panel" aria-labelledby="review-form-title">
							<h3 id="review-form-title" class="public-profile-review-form-panel__title">
								<c:choose>
									<c:when test="${empty viewerReview.comment}">
										<spring:message code="profile.reviews.form.addComment" />
									</c:when>
									<c:otherwise>
										<spring:message code="profile.reviews.form.editComment" />
									</c:otherwise>
								</c:choose>
							</h3>
							<form method="post" action="${reviewAction}" class="public-profile-review-form">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<input
									type="hidden"
									name="reaction"
									value="${not empty viewerReview ? viewerReview.reaction.dbValue : 'like'}" />
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
									<spring:message var="cancelReviewLabel" code="profile.reviews.form.cancel" />
									<ui:button label="${cancelReviewLabel}" href="${reviewSectionHref}" variant="secondary" />
									<ui:button label="${saveReviewLabel}" type="submit" />
								</div>
							</form>
						</section>
					</c:if>

					<c:if test="${not empty reviewFilterOptions}">
						<spring:message var="reviewFilterAria" code="profile.reviews.filter.aria" />
						<nav class="public-profile-review-filter" aria-label="${reviewFilterAria}">
							<span class="public-profile-review-filter__label"><spring:message code="profile.reviews.filter.label" /></span>
							<c:set var="reviewFilterCurrentValue" value="both" />
							<c:forEach var="option" items="${reviewFilterOptions}" varStatus="optionStatus">
								<c:choose>
									<c:when test="${optionStatus.index eq 0}">
										<c:set var="reviewFilterLeftLabel" value="${option.label}" />
										<c:set var="reviewFilterLeftHref" value="${option.href}" />
										<c:if test="${option.active}">
											<c:set var="reviewFilterCurrentValue" value="both" />
										</c:if>
									</c:when>
									<c:when test="${optionStatus.index eq 1}">
										<c:set var="reviewFilterRightLabel" value="${option.label}" />
										<c:set var="reviewFilterRightHref" value="${option.href}" />
										<c:if test="${option.active}">
											<c:set var="reviewFilterCurrentValue" value="positive" />
										</c:if>
									</c:when>
									<c:when test="${optionStatus.index eq 2}">
										<c:set var="reviewFilterThirdLabel" value="${option.label}" />
										<c:set var="reviewFilterThirdHref" value="${option.href}" />
										<c:if test="${option.active}">
											<c:set var="reviewFilterCurrentValue" value="bad" />
										</c:if>
									</c:when>
								</c:choose>
							</c:forEach>
							<ui:eventsFilterToggle
								id="profile-review-filter-toggle"
								className="public-profile-review-filter__toggle"
								currentValue="${reviewFilterCurrentValue}"
								leftValue="both"
								rightValue="positive"
								thirdValue="bad"
								leftLabel="${reviewFilterLeftLabel}"
								rightLabel="${reviewFilterRightLabel}"
								thirdLabel="${reviewFilterThirdLabel}"
								leftHref="${reviewFilterLeftHref}"
								rightHref="${reviewFilterRightHref}"
								thirdHref="${reviewFilterThirdHref}"
								forceLeftOnEmpty="${false}" />
						</nav>
					</c:if>

					<c:choose>
						<c:when test="${empty profileReviews}">
							<p class="public-profile-reviews__empty">
								<spring:message code="profile.reviews.emptyState" />
							</p>
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
										<c:if test="${not empty pageContext.request.userPrincipal}">
											<div class="public-profile-actions">
												<c:url var="reportReviewHref" value="/reports/reviews/${review.reviewId}" />
												<spring:message var="reportReviewLabel" code="moderation.report.review.submit" />
												<ui:button label="${reportReviewLabel}" href="${reportReviewHref}" variant="danger" />
											</div>
										</c:if>
									</li>
								</c:forEach>
							</ul>
							<c:if test="${reviewTotalPages > 1}">
								<spring:message var="previousLabel" code="pagination.previous" />
								<spring:message var="nextLabel" code="pagination.next" />
								<section class="feed-pagination" aria-label="${reviewFilterAria}">
									<nav class="feed-pagination__nav" aria-label="${reviewFilterAria}">
										<c:choose>
											<c:when test="${not empty reviewPreviousPageHref}">
												<c:url var="reviewPrevHref" value="${reviewPreviousPageHref}" />
												<a class="feed-pagination__control" href="${reviewPrevHref}">
													${previousLabel}
												</a>
											</c:when>
											<c:otherwise>
												<span class="feed-pagination__control feed-pagination__control--disabled">
													${previousLabel}
												</span>
											</c:otherwise>
										</c:choose>

										<div class="feed-pagination__pages">
											<c:forEach var="item" items="${reviewPaginationItems}">
												<c:choose>
													<c:when test="${item.ellipsis}">
														<span class="feed-pagination__ellipsis" aria-hidden="true">${item.label}</span>
													</c:when>
													<c:when test="${item.current}">
														<span class="feed-pagination__page feed-pagination__page--current" aria-current="page">
															${item.label}
														</span>
													</c:when>
													<c:otherwise>
														<c:url var="reviewPageHref" value="${item.href}" />
														<a class="feed-pagination__page" href="${reviewPageHref}">
															${item.label}
														</a>
													</c:otherwise>
												</c:choose>
											</c:forEach>
										</div>

										<c:choose>
											<c:when test="${not empty reviewNextPageHref}">
												<c:url var="reviewNextHref" value="${reviewNextPageHref}" />
												<a class="feed-pagination__control" href="${reviewNextHref}">
													${nextLabel}
												</a>
											</c:when>
											<c:otherwise>
												<span class="feed-pagination__control feed-pagination__control--disabled">
													${nextLabel}
												</span>
											</c:otherwise>
										</c:choose>
									</nav>
								</section>
							</c:if>
						</c:otherwise>
					</c:choose>
				</section>
			</main>
		</div>
	</body>
</html>
