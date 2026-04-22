<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.playerRequests" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--matches-list">

				<header class="page-heading">
					<h1 class="page-heading__title">
						<spring:message code="player.requests.title" />
					</h1>
					<p class="page-heading__description">
						<spring:message code="player.requests.description" />
					</p>
				</header>

				<c:choose>
					<c:when test="${empty pendingMatches}">
						<div class="panel participation-empty-panel">
							<p class="participation-empty-state">
								<c:out value="${emptyMessage}" />
							</p>
						</div>
					</c:when>
					<c:otherwise>
						<div class="event-grid">
							<c:forEach var="item" items="${pendingMatches}">
								<div class="pending-join-card">
									<c:url var="cardHref" value="${item.card.href}" />
									<ui:card href="${cardHref}" className="event-card" ariaLabel="${item.card.title}">
										<div class="event-card__media ${item.card.mediaClass}">
											<c:if test="${not empty item.card.bannerImageUrl}">
												<c:url var="bannerSrc" value="${item.card.bannerImageUrl}" />
												<img
													class="event-card__image"
													src="${bannerSrc}"
													alt=""
													loading="lazy"
													decoding="async"
												/>
											</c:if>
											<span class="event-card__badge"><c:out value="${item.card.sport}" /></span>
										</div>
										<div class="event-card__body">
											<span class="event-card__sport"><c:out value="${item.card.sport}" /></span>
											<h3 class="event-card__title"><c:out value="${item.card.title}" /></h3>
											<div class="event-card__meta">
												<span><c:out value="${item.card.venue}" /></span>
												<span><c:out value="${item.card.schedule}" /></span>
											</div>
											<div class="event-card__footer">
												<strong class="event-card__price">
													<c:out value="${item.card.priceLabel}" />
													<spring:message code="event.pricePerPerson" />
												</strong>
												<span class="event-card__spots"><c:out value="${item.card.badge}" /></span>
											</div>
										</div>
									</ui:card>

									<div class="pending-join-card__footer">
										<span class="pending-join-card__status">
											<spring:message code="event.joinRequest.pendingLabel" />
										</span>
										<c:url var="cancelAction" value="${item.cancelUrl}" />
										<spring:message var="cancellingLabel" code="player.requests.cancelling" />
										<form
											method="post"
											action="${cancelAction}"
											data-submit-guard="true"
											data-submit-loading-label="${cancellingLabel}"
											class="pending-join-card__cancel-form"
										>
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<spring:message var="cancelLabel" code="player.requests.cancel" />
											<ui:button label="${cancelLabel}" type="submit" variant="secondary" />
										</form>
									</div>
								</div>
							</c:forEach>
						</div>
					</c:otherwise>
				</c:choose>

			</main>
		</div>
	</body>
</html>
