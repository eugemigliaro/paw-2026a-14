<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
				<ui:returnButton />
				<header class="page-heading">
					<h1 class="page-heading__title">
						<c:out value="${pageTitleLabel}" />
					</h1>
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

				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label">
							<spring:message code="report.page.summary.title" />
						</h2>
					</div>

					<c:choose>
						<c:when test="${reportPage.targetTypeCode eq 'user'}">
							<article class="report-summary report-summary--user">
								<dl class="stack">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="moderation.report.user.title" /></dt>
										<dd class="report-section-field__row">
											<c:url var="reportUserImageSrc" value="${reportPage.user.profileImageUrl}" />
											<img
												class="public-profile-avatar-panel__image report-profile__image"
												src="${reportUserImageSrc}"
												alt="${reportPage.user.profileImageAlt}"
												loading="eager"
												decoding="async" />
											<div class	="report-summary__body">
												<strong><c:out value="${reportPage.user.username}" /></strong>
											</div>
										</dd>
									</div>
								</dl>
							</article>
						</c:when>
						<c:when test="${reportPage.targetTypeCode eq 'review'}">
							<article class="report-summary report-summary--review">
								<dl class="stack">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.review.author" /></dt>
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
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.review.content" /></dt>
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
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.review.date" /></dt>
										<dd><c:out value="${reportPage.review.dateLabel}" /></dd>
									</div>
									<c:if test="${not empty reportPage.review.reviewedUsername}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="report.page.review.target" /></dt>
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
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.match.name" /></dt>
										<dd><c:out value="${reportPage.match.title}" /></dd>
									</div>
									<c:if test="${not empty reportPage.match.description}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="report.page.match.description" /></dt>
											<dd class="body-copy"><c:out value="${reportPage.match.description}" /></dd>
										</div>
									</c:if>
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.match.host" /></dt>
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
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.match.date" /></dt>
										<dd><c:out value="${reportPage.match.dateLabel}" /></dd>
									</div>
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.match.address" /></dt>
										<dd><c:out value="${reportPage.match.address}" /></dd>
									</div>
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="report.page.match.price" /></dt>
										<dd><c:out value="${reportPage.match.priceLabel}" /></dd>
									</div>
								</dl>
							</article>
						</c:when>
					</c:choose>
				</ui:card>

				<ui:card className="report-section">
					<div class="section-head">
						<h2 id="report-form-title" class="field__label">
							<spring:message code="report.page.form.title" />
						</h2>
					</div>

					<dl class="stack">
						<c:url var="reportAction" value="${reportActionPath}" />
						<form:form modelAttribute="reportForm" action="${reportAction}">
							<form:errors path="" cssClass="notice notice--error" element="div" />
							<div class="report-section-field">
								<label for="report-reason" class="detail-label"><spring:message code="moderation.report.reason" /></label>
								<div class="report-dropdown" id="reason-dropdown">
									<button type="button" class="report-dropdown__toggle" id="reason-toggle">
										<span id="selected-reason-label"><spring:message code="moderation.reason.inappropriate_content" /></span>
									</button>
									<div class="report-dropdown__panel">
										<button type="button" class="report-dropdown__item report-dropdown__item--active" data-value="inappropriate_content">
											<spring:message code="moderation.reason.inappropriate_content" />
										</button>
										<button type="button" class="report-dropdown__item" data-value="aggressive_language">
											<spring:message code="moderation.reason.aggressive_language" />
										</button>
										<button type="button" class="report-dropdown__item" data-value="harassment">
											<spring:message code="moderation.reason.harassment" />
										</button>
										<button type="button" class="report-dropdown__item" data-value="cheating">
											<spring:message code="moderation.reason.cheating" />
										</button>
										<button type="button" class="report-dropdown__item" data-value="other">
											<spring:message code="moderation.reason.other" />
										</button>
									</div>
									<form:select id="report-reason" path="reason" cssStyle="display: none;">
										<form:option value="inappropriate_content"><spring:message code="moderation.reason.inappropriate_content" /></form:option>
										<form:option value="aggressive_language"><spring:message code="moderation.reason.aggressive_language" /></form:option>
										<form:option value="harassment"><spring:message code="moderation.reason.harassment" /></form:option>
										<form:option value="cheating"><spring:message code="moderation.reason.cheating" /></form:option>
										<form:option value="other"><spring:message code="moderation.reason.other" /></form:option>
									</form:select>
								</div>
								<form:errors path="reason" cssClass="notice notice--error" element="span" />
							</div>
							<div class="report-section-field">
								<label for="report-details" class="detail-label"><spring:message code="moderation.report.details" /></label>
								<form:textarea
									id="report-details"
									path="details"
									rows="4"
									maxlength="4000"
									cssClass="field__control" />
								<form:errors path="details" cssClass="notice notice--error" element="span" />
							</div>
							<spring:message var="submitReportLabel" code="moderation.report.submit" />
							<div class="report-section-actions">
								<ui:button label="${submitReportLabel}" type="submit" />
							</div>
						</form:form>
					</dl>
				</ui:card>
			</main>
		</div>
	</body>
</html>
