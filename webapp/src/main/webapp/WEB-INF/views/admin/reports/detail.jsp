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
				<ui:returnButton />
				<header class="page-heading">
					<h1 class="page-heading__title"><c:out value="${pageTitleLabel}" /></h1>
					<p class="page-heading__description"><c:out value="${pageDescription}" /></p>
				</header>

				<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetTypeCode}" />
				<spring:message var="reasonLabel"     code="admin.reports.reason.${report.reasonCode}" />
				<spring:message var="statusLabel"     code="admin.reports.status.${report.statusCode}" />

				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="admin.reports.section.original" /></h2>
						<div class="report-section__badges">
							<span class="report-type-badge report-type-badge--${report.targetTypeCode}">
								<c:out value="${targetTypeLabel}" />
							</span>
							<span class="report-status-badge report-status-badge--${report.statusCode}">
								<c:out value="${statusLabel}" />
							</span>
						</div>
					</div>

					<dl class="stack">
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.reporter" /></dt>
							<dd><c:out value="${report.reporterUserId}" /></dd>
						</div>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.target" /></dt>
							<dd><c:out value="${report.targetKey}" /></dd>
						</div>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="moderation.report.reason" /></dt>
							<dd><span class="report-card__reason-chip"><c:out value="${reasonLabel}" /></span></dd>
						</div>
						<c:if test="${not empty report.details}">
							<div class="report-section-field">
								<dt class="detail-label"><spring:message code="admin.reports.details" /></dt>
								<dd class="body-copy"><c:out value="${report.details}" /></dd>
							</div>
						</c:if>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.createdAt" /></dt>
							<dd><c:out value="${report.createdAtLabel}" /></dd>
						</div>
					</dl>
				</ui:card>

				<c:if test="${showResolution}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="admin.reports.section.resolution" /></h2>
						</div>

						<c:if test="${not empty report.resolutionCode}">
							<spring:message var="resolutionLabel" code="reports.mine.resolution.${report.resolutionCode}" />
							<dl class="stack report-section__top">
								<div class="report-section-field report-section-field__row">
									<dt class="detail-label"><spring:message code="admin.reports.resolution" /></dt>
									<dd><c:out value="${resolutionLabel}" /></dd>
								</div>
								<c:if test="${not empty report.resolutionDetails}">
									<div class="report-section-field">
										<dt class="detail-label"><spring:message code="admin.reports.resolutionDetails" /></dt>
										<dd class="body-copy"><c:out value="${report.resolutionDetails}" /></dd>
									</div>
								</c:if>
								<c:if test="${not empty report.reviewedByUserId}">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.reviewedBy" /></dt>
										<dd><c:out value="${report.reviewedByUserId}" /></dd>
									</div>
								</c:if>
								<c:if test="${not empty report.reviewedAtLabel}">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.reviewedAt" /></dt>
										<dd><c:out value="${report.reviewedAtLabel}" /></dd>
									</div>
								</c:if>
								<c:if test="${not empty userBan}">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.ban.until" /></dt>
										<dd><c:out value="${userBan.bannedUntilLabel}" /></dd>
									</div>
								</c:if>
							</dl>
						</c:if>

						<c:if test="${report.statusCode eq 'pending' or report.statusCode eq 'under_review'}">
							<div class="stack">
								<form method="post" class="stack">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<div class="report-section-field">
										<label class="field">
											<span class="detail-label"><spring:message code="admin.reports.resolutionDetails" /></span>
											<textarea name="resolutionDetails" maxlength="4000" class="field__control field__control--textarea" rows="3"></textarea>
										</label>
									</div>

									<div class="report-resolution__actions">
										<c:if test="${report.targetTypeCode eq 'match' or report.targetTypeCode eq 'review'}">
											<c:url var="deleteContentHref" value="/admin/reports/${report.id}/delete-content" />
											<spring:message var="deleteContentLabel" code="admin.reports.action.deleteContent" />
											<ui:button label="${deleteContentLabel}" type="submit" variant="danger" formAction="${deleteContentHref}" />
										</c:if>

										<c:if test="${report.targetTypeCode eq 'user'}">
											<div class="report-section-field__row">
												<c:url var="banUserHref" value="/admin/reports/${report.id}/ban-user" />
												<spring:message var="banUserLabel" code="admin.reports.action.banUser" />
												<ui:button label="${banUserLabel}" type="submit" variant="danger" formAction="${banUserHref}" />
												<label class="field">
													<span class="detail-label"><spring:message code="admin.reports.ban.days" /></span>
													<input type="number" name="banDays" min="0" value="7" class="field__control field__control--ban-days" />
												</label>
											</div>
										</c:if>

										<c:url var="dismissHref" value="/admin/reports/${report.id}/dismiss" />
										<spring:message var="dismissLabel" code="admin.reports.action.dismiss" />
										<ui:button label="${dismissLabel}" type="submit" variant="secondary" formAction="${dismissHref}" />
									</div>
								</form>
							</div>
						</c:if>
					</ui:card>
				</c:if>

				<c:if test="${showAppeal}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="admin.reports.section.appeal" /></h2>
						</div>

						<c:choose>
							<c:when test="${not empty report.appealReason}">
								<dl class="stack report-section__top">
									<div class="report-section-field">
										<dt class="detail-label"><spring:message code="admin.reports.appeal" /></dt>
										<dd class="body-copy"><c:out value="${report.appealReason}" /></dd>
									</div>
									<c:if test="${not empty report.appealedAtLabel}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="admin.reports.appeal.appealedAt" /></dt>
											<dd><c:out value="${report.appealedAtLabel}" /></dd>
										</div>
									</c:if>
								</dl>
							</c:when>
							<c:otherwise>
								<p class="report-section__top body-copy"><spring:message code="admin.reports.section.empty" /></p>
							</c:otherwise>
						</c:choose>
					</ui:card>
				</c:if>

				<c:if test="${showAppealResolution}">
					<ui:card className="report-section">
						<div class="section-head">
							<h2 class="field__label"><spring:message code="admin.reports.section.appealResolution" /></h2>
						</div>

						<c:choose>
							<c:when test="${report.appealed}">
								<c:url var="finalizeAppealHref" value="/admin/reports/${report.id}/finalize-appeal" />
								<form method="post" action="${finalizeAppealHref}" class="stack report-section__top">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<spring:message var="appealResolutionLabel" code="admin.reports.appeal.resolution.label" />
									<div class="report-section-field">
										<label for="appeal-decision-${report.id}" class="detail-label"><c:out value="${appealResolutionLabel}" /></label>
										<div class="report-dropdown">
											<button type="button" class="report-dropdown__toggle">
												<span id="selected-appeal-decision-label"><spring:message code="admin.reports.appealDecision.upheld" /></span>
											</button>
											<div class="report-dropdown__panel">
												<button type="button" class="report-dropdown__item report-dropdown__item--active" data-value="upheld">
													<spring:message code="admin.reports.appealDecision.upheld" />
												</button>
												<button type="button" class="report-dropdown__item" data-value="lifted">
													<spring:message code="admin.reports.appealDecision.lifted" />
												</button>
											</div>
											<select id="appeal-decision-${report.id}" name="appealDecision" style="display: none;">
												<option value="upheld"><spring:message code="admin.reports.appealDecision.upheld" /></option>
												<option value="lifted"><spring:message code="admin.reports.appealDecision.lifted" /></option>
											</select>
										</div>
									</div>
									<div class="report-section-actions">
										<spring:message var="finalizeAppealLabel" code="admin.reports.appeal.finalize" />
										<ui:button label="${finalizeAppealLabel}" type="submit" />
									</div>
								</form>
							</c:when>
							<c:when test="${not empty report.appealDecisionCode}">
								<spring:message var="appealDecisionLabel" code="admin.reports.appealDecision.${report.appealDecisionCode}" />
								<dl class="stack report-section__top">
									<div class="report-section-field report-section-field__row">
										<dt class="detail-label"><spring:message code="admin.reports.appeal.resolution.label" /></dt>
										<dd><c:out value="${appealDecisionLabel}" /></dd>
									</div>
									<c:if test="${not empty report.appealResolvedByUserId}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="admin.reports.appealResolvedBy" /></dt>
											<dd><c:out value="${report.appealResolvedByUserId}" /></dd>
										</div>
									</c:if>
									<c:if test="${not empty report.appealResolvedAtLabel}">
										<div class="report-section-field report-section-field__row">
											<dt class="detail-label"><spring:message code="admin.reports.appealResolvedAt" /></dt>
											<dd><c:out value="${report.appealResolvedAtLabel}" /></dd>
										</div>
									</c:if>
								</dl>
							</c:when>
							<c:otherwise>
								<p class="report-section__top body-copy"><spring:message code="admin.reports.section.empty" /></p>
							</c:otherwise>
						</c:choose>
					</ui:card>
				</c:if>
			</main>
		</div>
	</body>
</html>
