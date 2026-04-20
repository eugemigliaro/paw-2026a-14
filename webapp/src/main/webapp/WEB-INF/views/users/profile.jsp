<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
			</main>
		</div>
	</body>
</html>
