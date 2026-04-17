<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ attribute name="listTitle" required="true" rtexprvalue="true" %>
<%@ attribute name="listDescription" required="true" rtexprvalue="true" %>
<%@ attribute name="emptyMessage" required="true" rtexprvalue="true" %>
<%@ attribute name="events" required="true" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="paginationItems" required="true" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="previousPageHref" required="false" rtexprvalue="true" %>
<%@ attribute name="nextPageHref" required="false" rtexprvalue="true" %>

<spring:message var="previousLabel" code="pagination.previous" />
<spring:message var="nextLabel" code="pagination.next" />

<main class="page-shell page-shell--matches-list">
	<section class="matches-list-panel">
		<div class="section-head section-head--matches-list">
			<div>
				<h1 class="section-head__title section-head__title--matches-list"><c:out value="${listTitle}" /></h1>
				<p class="section-head__meta"><c:out value="${listDescription}" /></p>
			</div>
		</div>

		<c:choose>
			<c:when test="${empty events}">
				<div class="matches-empty-state">
					<p><c:out value="${emptyMessage}" /></p>
				</div>
			</c:when>
			<c:otherwise>
				<div class="event-grid">
					<c:forEach var="event" items="${events}">
						<c:url var="eventHref" value="${event.href}" />
						<ui:card href="${eventHref}" className="event-card" ariaLabel="${event.title}">
							<div class="event-card__media ${event.mediaClass}">
								<c:if test="${not empty event.bannerImageUrl}">
									<c:url var="eventBannerSrc" value="${event.bannerImageUrl}" />
									<img
										class="event-card__image"
										src="${eventBannerSrc}"
										alt=""
										loading="lazy"
										decoding="async" />
								</c:if>
								<span class="event-card__badge"><c:out value="${event.badge}" /></span>
							</div>

							<div class="event-card__body">
								<span class="event-card__sport"><c:out value="${event.sport}" /></span>
								<h2 class="event-card__title"><c:out value="${event.title}" /></h2>
								<div class="event-card__meta">
									<span><c:out value="${event.venue}" /></span>
									<span><c:out value="${event.schedule}" /></span>
								</div>

								<div class="event-card__footer">
									<div class="event-card__cta">
										<span><c:out value="${event.priceLabel}" /></span>
									</div>
								</div>
							</div>
						</ui:card>
					</c:forEach>
				</div>
			</c:otherwise>
		</c:choose>

		<c:if test="${not empty paginationItems}">
			<section class="feed-pagination" aria-label="Pagination">
				<nav class="feed-pagination__nav" aria-label="Event list pages">
					<c:choose>
						<c:when test="${not empty previousPageHref}">
							<c:url var="prevHref" value="${previousPageHref}" />
							<a class="feed-pagination__control" href="${prevHref}">${previousLabel}</a>
						</c:when>
						<c:otherwise>
							<span class="feed-pagination__control feed-pagination__control--disabled">${previousLabel}</span>
						</c:otherwise>
					</c:choose>

					<div class="feed-pagination__pages">
						<c:forEach var="item" items="${paginationItems}">
							<c:choose>
								<c:when test="${item.ellipsis}">
									<span class="feed-pagination__ellipsis" aria-hidden="true">${item.label}</span>
								</c:when>
								<c:when test="${item.current}">
									<span class="feed-pagination__page feed-pagination__page--current" aria-current="page">${item.label}</span>
								</c:when>
								<c:otherwise>
									<c:url var="pageHref" value="${item.href}" />
									<a class="feed-pagination__page" href="${pageHref}">${item.label}</a>
								</c:otherwise>
							</c:choose>
						</c:forEach>
					</div>

					<c:choose>
						<c:when test="${not empty nextPageHref}">
							<c:url var="nextHref" value="${nextPageHref}" />
							<a class="feed-pagination__control" href="${nextHref}">${nextLabel}</a>
						</c:when>
						<c:otherwise>
							<span class="feed-pagination__control feed-pagination__control--disabled">${nextLabel}</span>
						</c:otherwise>
					</c:choose>
				</nav>
			</section>
		</c:if>
	</section>
</main>
