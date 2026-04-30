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

			<main class="page-shell page-shell--detail">
				<section class="panel">
					<ui:returnButton />
					<header class="page-heading">
						<h1 class="page-heading__title">
							<c:out value="${pageTitleLabel}" />
						</h1>
						<p class="page-heading__description">
							<c:out value="${pageDescription}" />
						</p>
					</header>

					<c:if test="${reportSent}">
						<div class="notice notice--success">
							<spring:message code="moderation.report.sent" />
						</div>
					</c:if>
					<c:if test="${not empty reportErrorMessage}">
						<div class="notice notice--error">
							<c:out value="${reportErrorMessage}" />
						</div>
					</c:if>

					<section class="panel" aria-labelledby="reported-item-title">
						<header class="page-heading">
							<h2 id="reported-item-title" class="page-heading__title page-heading__title--compact">
								<spring:message code="report.page.summary.title" />
							</h2>
						</header>

						<c:choose>
							<c:when test="${reportPage.targetTypeCode eq 'user'}">
								<article class="report-summary report-summary--user">
									<c:url var="reportUserImageSrc" value="${reportPage.user.profileImageUrl}" />
									<img
										class="public-profile-avatar-panel__image"
										src="${reportUserImageSrc}"
										alt="${reportPage.user.profileImageAlt}"
										loading="eager"
										decoding="async" />
									<div class="report-summary__body">
										<span class="field__label"><spring:message code="moderation.report.user.title" /></span>
										<strong><c:out value="${reportPage.user.username}" /></strong>
									</div>
								</article>
							</c:when>
							<c:when test="${reportPage.targetTypeCode eq 'review'}">
								<article class="report-summary report-summary--review">
									<dl class="stack">
										<div>
											<dt class="field__label"><spring:message code="report.page.review.author" /></dt>
											<dd>
												<c:choose>
													<c:when test="${not empty reportPage.review.authorProfileHref}">
														<c:url var="reportReviewAuthorHref" value="${reportPage.review.authorProfileHref}" />
														<a href="${reportReviewAuthorHref}"><c:out value="${reportPage.review.authorUsername}" /></a>
													</c:when>
													<c:otherwise>
														<c:out value="${reportPage.review.authorUsername}" />
													</c:otherwise>
												</c:choose>
											</dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.review.content" /></dt>
											<dd class="body-copy">
												<c:choose>
													<c:when test="${not empty reportPage.review.content}">
														<c:out value="${reportPage.review.content}" />
													</c:when>
													<c:otherwise>
														<spring:message code="report.page.review.emptyContent" />
													</c:otherwise>
												</c:choose>
											</dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.review.date" /></dt>
											<dd><c:out value="${reportPage.review.dateLabel}" /></dd>
										</div>
										<c:if test="${not empty reportPage.review.reviewedUsername}">
											<div>
												<dt class="field__label"><spring:message code="report.page.review.target" /></dt>
												<dd>
													<c:choose>
														<c:when test="${not empty reportPage.review.reviewedProfileHref}">
															<c:url var="reportReviewedUserHref" value="${reportPage.review.reviewedProfileHref}" />
																<a href="${reportReviewedUserHref}"><c:out value="${reportPage.review.reviewedUsername}" /></a>
															</c:when>
															<c:otherwise>
																<c:out value="${reportPage.review.reviewedUsername}" />
															</c:otherwise>
														</c:choose>
												</dd>
											</div>
										</c:if>
									</dl>
								</article>
							</c:when>
							<c:when test="${reportPage.targetTypeCode eq 'match'}">
								<article class="report-summary report-summary--match">
									<dl class="stack">
										<div>
											<dt class="field__label"><spring:message code="report.page.match.title" /></dt>
											<dd><c:out value="${reportPage.match.title}" /></dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.match.description" /></dt>
											<dd class="body-copy"><c:out value="${reportPage.match.description}" /></dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.match.host" /></dt>
											<dd>
												<c:choose>
													<c:when test="${not empty reportPage.match.hostProfileHref}">
														<c:url var="reportMatchHostHref" value="${reportPage.match.hostProfileHref}" />
														<a href="${reportMatchHostHref}"><c:out value="${reportPage.match.hostUsername}" /></a>
													</c:when>
													<c:otherwise>
														<c:out value="${reportPage.match.hostUsername}" />
													</c:otherwise>
												</c:choose>
											</dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.match.date" /></dt>
											<dd><c:out value="${reportPage.match.dateLabel}" /></dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.match.address" /></dt>
											<dd><c:out value="${reportPage.match.address}" /></dd>
										</div>
										<div>
											<dt class="field__label"><spring:message code="report.page.match.price" /></dt>
											<dd><c:out value="${reportPage.match.priceLabel}" /></dd>
										</div>
									</dl>
								</article>
							</c:when>
						</c:choose>
					</section>

					<section class="panel" aria-labelledby="report-form-title">
						<header class="page-heading">
							<h2 id="report-form-title" class="page-heading__title page-heading__title--compact">
								<spring:message code="report.page.form.title" />
							</h2>
						</header>
						<c:url var="reportAction" value="${reportActionPath}" />
						<form method="post" action="${reportAction}" class="stack">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<label class="field" for="report-reason">
								<span class="field__label"><spring:message code="moderation.report.reason" /></span>
								<select id="report-reason" name="reason" class="field__control field__control--select">
									<option value="inappropriate_content"><spring:message code="moderation.reason.inappropriate_content" /></option>
									<option value="aggressive_language"><spring:message code="moderation.reason.aggressive_language" /></option>
									<option value="harassment"><spring:message code="moderation.reason.harassment" /></option>
									<option value="cheating"><spring:message code="moderation.reason.cheating" /></option>
									<option value="other"><spring:message code="moderation.reason.other" /></option>
								</select>
							</label>
							<label class="field" for="report-details">
								<span class="field__label"><spring:message code="moderation.report.details" /></span>
								<textarea
									id="report-details"
									name="details"
									rows="4"
									maxlength="4000"
									class="field__control"></textarea>
							</label>
							<spring:message var="submitReportLabel" code="moderation.report.submit" />
							<ui:button label="${submitReportLabel}" type="submit" />
						</form>
					</section>
				</section>
			</main>
		</div>
	</body>
</html>
