<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.playerInvites" />
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
						<spring:message code="player.invites.title" />
					</h1>
					<p class="page-heading__description">
						<spring:message code="player.invites.description" />
					</p>
				</header>

				<c:if test="${param.invite eq 'declined'}">
					<div class="notice notice--info">
						<spring:message code="player.invites.declined" />
					</div>
				</c:if>

				<c:choose>
					<c:when test="${empty invitedMatches}">
						<div class="panel participation-empty-panel">
							<p class="participation-empty-state">
								<c:out value="${emptyMessage}" />
							</p>
						</div>
					</c:when>
					<c:otherwise>
						<div class="event-grid">
							<c:forEach var="item" items="${invitedMatches}">
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
											<spring:message code="player.invites.statusLabel" />
										</span>
										<div class="pending-join-card__actions">
											<c:url var="acceptAction" value="${item.acceptUrl}" />
											<spring:message var="acceptingLabel" code="player.invites.accepting" />
											<form
												method="post"
												action="${acceptAction}"
												data-submit-guard="true"
												data-submit-loading-label="${acceptingLabel}"
												class="pending-join-card__cancel-form"
											>
												<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
												<spring:message var="acceptLabel" code="player.invites.accept" />
												<ui:button label="${acceptLabel}" type="submit" />
											</form>
											<c:url var="declineAction" value="${item.declineUrl}" />
											<spring:message var="decliningLabel" code="player.invites.declining" />
											<form
												method="post"
												action="${declineAction}"
												data-submit-guard="true"
												data-submit-loading-label="${decliningLabel}"
												class="pending-join-card__cancel-form"
											>
												<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
												<spring:message var="declineLabel" code="player.invites.decline" />
												<ui:button label="${declineLabel}" type="submit" variant="secondary" />
											</form>
										</div>
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
