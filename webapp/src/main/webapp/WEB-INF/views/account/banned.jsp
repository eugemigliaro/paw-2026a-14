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

			<main class="page-shell account-shell">
				<section class="panel account-panel">
					<h1 class="page-heading__title account-panel__title"><c:out value="${banTitle}" /></h1>
					<p class="page-heading__description account-panel__description">
						<c:out value="${banDescription}" />
					</p>

					<c:if test="${param.action eq 'appealed'}">
						<p class="auth-notice auth-notice--success">
							<spring:message code="account.ban.appeal.sent" />
						</p>
					</c:if>
					<c:if test="${not empty param.error}">
						<p class="auth-notice auth-notice--error">
							<c:choose>
								<c:when test="${param.error eq 'appeal_limit'}">
									<spring:message code="account.ban.appeal.limit" />
								</c:when>
								<c:when test="${param.error eq 'appeal_rejected'}">
									<spring:message code="account.ban.appeal.rejected" />
								</c:when>
								<c:otherwise>
									<spring:message code="account.ban.appeal.error" />
								</c:otherwise>
							</c:choose>
						</p>
					</c:if>

					<div class="account-summary">
						<label class="field" for="account-ban-until">
							<span class="field__label"><spring:message code="account.ban.until" /></span>
							<input
								id="account-ban-until"
								type="text"
								class="field__control account-readonly-control"
								value="<c:out value='${banUntilLabel}' />"
								readonly="readonly"
								aria-readonly="true" />
						</label>
						<label class="field" for="account-ban-reason">
							<span class="field__label"><spring:message code="account.ban.reason" /></span>
							<input
								id="account-ban-reason"
								type="text"
								class="field__control account-readonly-control"
								value="<c:out value='${banReason}' />"
								readonly="readonly"
								aria-readonly="true" />
						</label>
						<c:if test="${not empty appealReason}">
							<label class="field" for="account-ban-appeal-reason-sent">
								<span class="field__label"><spring:message code="account.ban.appeal.reason" /></span>
								<textarea
									id="account-ban-appeal-reason-sent"
									class="field__control account-readonly-control"
									rows="4"
									readonly="readonly"
									aria-readonly="true"><c:out value="${appealReason}" /></textarea>
							</label>
						</c:if>
					</div>

					<c:if test="${appealAllowed}">
						<c:url var="appealAction" value="/account/ban/appeal" />
						<form method="post" action="${appealAction}" class="account-form" novalidate="novalidate">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<label class="field" for="ban-appeal-reason">
								<span class="field__label"><spring:message code="account.ban.appeal.reason" /></span>
								<textarea
									id="ban-appeal-reason"
									name="appealReason"
									rows="4"
									maxlength="2000"
									class="field__control"></textarea>
							</label>
							<div class="account-actions">
								<spring:message var="submitAppealLabel" code="account.ban.appeal.submit" />
								<ui:button label="${submitAppealLabel}" type="submit" />
							</div>
						</form>
					</c:if>

					<c:url var="logoutAction" value="/logout" />
					<spring:message var="logoutLabel" code="nav.logout" />
					<form method="post" action="${logoutAction}" class="account-logout">
						<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
						<ui:button label="${logoutLabel}" type="submit" variant="danger" />
					</form>
				</section>
			</main>
		</div>
	</body>
</html>
