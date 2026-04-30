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
									<ui:returnButton />

									<%-- MOVED UP: image variables declared earlier so image can appear first --%>
										<spring:message code="account.phone.empty" var="accountPhoneEmptyLabel" />
										<c:set var="accountPhoneValue"
											value="${empty accountProfile.phone ? accountPhoneEmptyLabel : accountProfile.phone}" />
										<c:url var="accountProfileImageSrc" value="${accountProfileImageUrl}" />

										<div class="account-profile-header">
											<div class="field">
												<span class="field__label">
													<spring:message code="account.profileImage.field" />
												</span>
												<section class="account-profile-media"
													aria-label="<c:out value='${accountProfileImageAlt}' />">
													<img class="account-profile-media__image"
														src="${accountProfileImageSrc}"
														alt="<c:out value='${accountProfileImageAlt}' />"
														loading="eager" decoding="async" />
												</section>
											</div>

											<div class="account-actions account-actions--top">
												<c:url var="accountEditHref" value="/account/edit" />
												<ui:button label="${accountEditLabel}" href="${accountEditHref}"
													variant="secondary" />
											</div>
										</div>

										<c:if test="${not empty accountUpdated}">
											<p class="auth-notice auth-notice--success">
												<c:out value="${accountUpdated}" />
											</p>
										</c:if>


										<div class="account-summary">
											<label class="field" for="account-email">
												<span class="field__label">
													<spring:message code="form.email.label" />
												</span>
												<input id="account-email" type="email"
													class="field__control account-readonly-control account-readonly-control"
													value="<c:out value='${accountProfile.email}' />"
													readonly="readonly" aria-readonly="true" />
											</label>

											<div class="account-summary__name-row">
												<label class="field" for="account-name">
													<span class="field__label">
														<spring:message code="form.name.label" />
													</span>
													<input id="account-name" type="text"
														class="field__control account-readonly-control"
														value="<c:out value='${accountProfile.name}' />"
														readonly="readonly" aria-readonly="true" />
												</label>
												<label class="field" for="account-last-name">
													<span class="field__label">
														<spring:message code="form.lastName.label" />
													</span>
													<input id="account-last-name" type="text"
														class="field__control account-readonly-control"
														value="<c:out value='${accountProfile.lastName}' />"
														readonly="readonly" aria-readonly="true" />
												</label>
											</div>


											<label class="field" for="account-username">
												<span class="field__label">
													<spring:message code="form.username.label" />
												</span>
												<input id="account-username" type="text"
													class="field__control account-readonly-control"
													value="<c:out value='${accountProfile.username}' />"
													readonly="readonly" aria-readonly="true" />
											</label>

											<label class="field" for="account-phone">
												<span class="field__label">
													<spring:message code="form.phone.label" />
												</span>
												<input id="account-phone" type="tel"
													class="field__control account-readonly-control"
													value="<c:out value='${accountPhoneValue}' />" readonly="readonly"
													aria-readonly="true" />
											</label>
										</div>

										<c:url var="logoutAction" value="/logout" />
										<form method="post" action="${logoutAction}" class="account-logout">
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<ui:button label="${logoutLabel}" type="submit" variant="danger" />
										</form>
								</section>
							</main>
					</div>
				</body>

				</html>
