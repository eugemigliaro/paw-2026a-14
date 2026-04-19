<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
						<p class="eyebrow"><c:out value="${profileEyebrow}" /></p>
						<h1 class="page-heading__title public-profile-panel__title"><c:out value="${profileTitle}" /></h1>
						<p class="page-heading__description public-profile-panel__description">
							<c:out value="${profileDescription}" />
						</p>
					</header>

					<article class="panel public-profile-avatar-panel">
						<span class="detail-label"><c:out value="${profileEyebrow}" /></span>
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

						<label class="field" for="public-profile-email">
							<span class="field__label"><c:out value="${profileEmailLabel}" /></span>
							<input
								id="public-profile-email"
								type="text"
								class="field__control public-profile-summary__control"
								value="<c:out value='${profilePage.email}' />"
								readonly="readonly"
								aria-readonly="true" />
						</label>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
