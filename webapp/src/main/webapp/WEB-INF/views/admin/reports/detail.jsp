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

			<%-- Report summary card --%>
			<spring:message var="targetTypeLabel" code="admin.reports.targetType.${report.targetTypeCode}" />
			<spring:message var="reasonLabel"     code="admin.reports.reason.${report.reasonCode}" />
			<spring:message var="statusLabel"     code="admin.reports.status.${report.statusCode}" />

			<ui:card className="report-section">
				<div class="section-head">
					<h2 class="field__label"><spring:message code="report.page.summary.title" /></h2>
					<div style="display: flex; gap: 0.5rem; align-items: center; flex-wrap: wrap;">
						<span class="report-type-badge report-type-badge--${report.targetTypeCode}">
							<c:out value="${targetTypeLabel}" />
						</span>
						<span class="report-status-badge report-status-badge--${report.statusCode}">
							<c:out value="${statusLabel}" />
						</span>
					</div>
				</div>

				<dl class="stack" style="margin-top: 1.25rem;">
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
						<dd><span class="chip chip--muted"><c:out value="${reasonLabel}" /></span></dd>
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

			<%-- Appeal card (report-level appeal reason) --%>
			<c:if test="${not empty report.appealReason}">
				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="reports.mine.appealReason" /></h2>
					</div>
					<dl class="stack" style="margin-top: 1.25rem;">
						<div class="report-section-field">
							<dt class="detail-label"><spring:message code="reports.mine.appealReason" /></dt>
							<dd class="body-copy"><c:out value="${report.appealReason}" /></dd>
						</div>
					</dl>
				</ui:card>
			</c:if>

			<%-- Ban appeal card --%>
			<c:if test="${not empty banAppeal}">
				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="admin.reports.banAppeal.reason" /></h2>
					</div>
					<dl class="stack" style="margin-top: 1.25rem;">
						<div class="report-section-field">
							<dt class="detail-label"><spring:message code="admin.reports.banAppeal.reason" /></dt>
							<dd class="body-copy"><c:out value="${banAppeal.appealReason}" /></dd>
						</div>
						<div class="report-section-field report-section-field__row">
							<dt class="detail-label"><spring:message code="admin.reports.banAppeal.appealedAt" /></dt>
							<dd><c:out value="${banAppeal.appealedAtLabel}" /></dd>
						</div>
						<c:if test="${not empty banAppeal.appealDecisionCode}">
							<spring:message var="banAppealDecisionLabel" code="admin.reports.banAppeal.decision.${banAppeal.appealDecisionCode}" />
							<div class="report-section-field report-section-field__row">
								<dt class="detail-label"><spring:message code="admin.reports.banAppeal.decision" /></dt>
								<dd><c:out value="${banAppealDecisionLabel}" /></dd>
							</div>
						</c:if>
					</dl>
				</ui:card>
			</c:if>

			<%-- Moderation actions card (pending / under_review only) --%>
			<c:if test="${report.statusCode eq 'pending' or report.statusCode eq 'under_review'}">
				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="admin.reports.actions.title" /></h2>
					</div>

					<div class="report-section-actions" style="margin-top: 1.25rem;">

						<c:url var="reviewHref" value="/admin/reports/${report.id}/under-review" />
						<form method="post" action="${reviewHref}" class="participant-manage-list__action-form">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<spring:message var="reviewLabel" code="admin.reports.action.review" />
							<ui:button label="${reviewLabel}" type="submit" variant="secondary" />
						</form>

						<c:url var="dismissHref" value="/admin/reports/${report.id}/dismiss" />
						<form method="post" action="${dismissHref}" class="participant-manage-list__action-form">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<spring:message var="dismissLabel" code="admin.reports.action.dismiss" />
							<ui:button label="${dismissLabel}" type="submit" variant="secondary" />
						</form>

						<c:if test="${report.targetTypeCode eq 'match' or report.targetTypeCode eq 'review'}">
							<c:url var="deleteContentHref" value="/admin/reports/${report.id}/delete-content" />
							<form method="post" action="${deleteContentHref}" class="participant-manage-list__action-form">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<spring:message var="deleteContentLabel" code="admin.reports.action.deleteContent" />
								<ui:button label="${deleteContentLabel}" type="submit" variant="danger" />
							</form>
						</c:if>

						<c:if test="${report.targetTypeCode eq 'user'}">
							<c:url var="banUserHref" value="/admin/reports/${report.id}/ban-user" />
							<form method="post" action="${banUserHref}" class="participant-manage-list__action-form report-ban-form">
								<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
								<input type="number" name="banDays" min="1" max="365" value="7" class="field__control report-ban-form__days" />
								<spring:message var="banReasonPlaceholder" code="admin.reports.ban.reasonPlaceholder" />
								<input type="text" name="banReason" maxlength="2000" class="field__control report-ban-form__reason" placeholder="${banReasonPlaceholder}" />
								<spring:message var="banUserLabel" code="admin.reports.action.banUser" />
								<ui:button label="${banUserLabel}" type="submit" variant="danger" />
							</form>
						</c:if>

					</div>
				</ui:card>
			</c:if>

			<%-- Appeal resolution card --%>
			<c:if test="${report.appealed}">
				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="admin.reports.appeal.pending" /></h2>
					</div>
					<c:url var="finalizeAppealHref" value="/admin/reports/${report.id}/finalize-appeal" />
					<form method="post" action="${finalizeAppealHref}" class="stack" style="margin-top: 1.25rem;">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<spring:message var="appealResolutionLabel" code="admin.reports.appeal.resolution.label" />
						<label class="field" for="appeal-resolution-${report.id}">
							<span class="field__label"><c:out value="${appealResolutionLabel}" /></span>
							<select id="appeal-resolution-${report.id}" name="appealResolution" class="field__control field__control--select">
								<option value="dismissed"><spring:message code="admin.reports.appeal.resolution.dismissed" /></option>
								<option value="warning"><spring:message code="admin.reports.appeal.resolution.warning" /></option>
								<option value="content_deleted"><spring:message code="admin.reports.appeal.resolution.content_deleted" /></option>
								<option value="user_banned"><spring:message code="admin.reports.appeal.resolution.user_banned" /></option>
							</select>
						</label>
						<div class="report-section-actions">
							<spring:message var="finalizeAppealLabel" code="admin.reports.appeal.finalize" />
							<ui:button label="${finalizeAppealLabel}" type="submit" />
						</div>
					</form>
				</ui:card>
			</c:if>

			<%-- Ban appeal resolution card --%>
			<c:if test="${not empty banAppeal and banAppeal.pendingResolution}">
				<ui:card className="report-section">
					<div class="section-head">
						<h2 class="field__label"><spring:message code="admin.reports.banAppeal.resolve" /></h2>
					</div>
					<c:url var="resolveBanAppealHref" value="/admin/reports/${report.id}/resolve-ban-appeal" />
					<form method="post" action="${resolveBanAppealHref}" class="stack" style="margin-top: 1.25rem;">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<spring:message var="banAppealDecisionLabel" code="admin.reports.banAppeal.decision" />
						<label class="field" for="ban-appeal-decision-${report.id}">
							<span class="field__label"><c:out value="${banAppealDecisionLabel}" /></span>
							<select id="ban-appeal-decision-${report.id}" name="decision" class="field__control field__control--select">
								<option value="upheld"><spring:message code="admin.reports.banAppeal.decision.upheld" /></option>
								<option value="lifted"><spring:message code="admin.reports.banAppeal.decision.lifted" /></option>
							</select>
						</label>
						<div class="report-section-actions">
							<spring:message var="resolveBanAppealLabel" code="admin.reports.banAppeal.resolve" />
							<ui:button label="${resolveBanAppealLabel}" type="submit" />
						</div>
					</form>
				</ui:card>
			</c:if>

		</main>
	</div>
</body>
</html>
