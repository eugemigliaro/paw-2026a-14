<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
	<spring:message var="pageTitle" code="page.title.explore" />
	<!DOCTYPE html>
	<html lang="${pageContext.response.locale.language}">
		<head>
			<%@ include file="/WEB-INF/views/includes/head.jspf" %>
		</head>
		<body>
			<div class="app-shell">
				<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

				<spring:message var="filtersAriaLabel" code="feed.aria.filters" />
				<spring:message var="searchAriaLabel" code="feed.aria.search" />
				<spring:message var="sortAriaLabel" code="feed.aria.sort" />
				<main class="page-shell page-shell--feed">
					<aside class="feed-sidebar" aria-label="${filtersAriaLabel}">
					<div class="panel filter-rail">
						<div class="filter-rail__header">
							<h2 class="filter-rail__main-title"><spring:message code="filter.title" /></h2>
							<c:url var="clearFiltersHref" value="/">
								<c:param name="q" value="${param.q}" />
								<c:param name="sort" value="${selectedSort}" />
							</c:url>
							<spring:message var="clearAllLabel" code="filter.clearAll" />
							<ui:button
								label="${clearAllLabel}"
								href="${clearFiltersHref}"
								variant="ghost"
								size="sm"
								className="filter-rail__clear" />
						</div>
						<c:forEach var="group" items="${feedPage.filterGroups}">
							<section class="filter-rail__group">
								<h2 class="filter-rail__title"><c:out value="${group.title}" /></h2>
									<div class="filter-rail__options">
										<c:forEach var="option" items="${group.options}">
											<ui:chip
												label="${option.label}"
												href="${pageContext.request.contextPath}${option.href}"
												active="${option.active}"
												tone="default"
												className="filter-rail__chip" />
										</c:forEach>
								</div>
							</section>
						</c:forEach>
					</div>
				</aside>

				<section class="feed-main">
					<section class="feed-hero-stage">
						<section class="hero-panel">
							<c:if test="${not empty feedPage.eyebrow}">
								<p class="eyebrow"><c:out value="${feedPage.eyebrow}" /></p>
							</c:if>
							<h1 class="hero-panel__title"><c:out value="${feedPage.title}" /></h1>
							<p class="hero-panel__description"><c:out value="${feedPage.description}" /></p>
						</section>

						<section class="search-panel" aria-label="${searchAriaLabel}">
							<form:form
								method="get"
								action="${pageContext.request.contextPath}/"
								modelAttribute="feedSearchForm"
								cssClass="search-panel__form">
								<input type="hidden" name="sport" value="<c:out value='${param.sport}' />" />
								<input type="hidden" name="time" value="<c:out value='${param.time}' />" />
								<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
								<input type="hidden" name="tz" value="<c:out value='${param.tz}' />" />
								<div class="search-panel__row">
									<div class="search-panel__input">
										<span class="search-panel__icon" aria-hidden="true"></span>
										<form:input
											path="q"
											cssClass="field__control search-panel__control"
											placeholder="${feedPage.searchPlaceholder}" />
									</div>
									<ui:button label="${feedPage.searchButtonLabel}" type="submit" />
								</div>
								<form:errors path="q" cssClass="search-panel__error" element="p" />
							</form:form>
						</section>

						<form method="get" action="${pageContext.request.contextPath}/" class="sort-panel" aria-label="${sortAriaLabel}">
							<input type="hidden" name="q" value="<c:out value='${param.q}' />" />
							<input type="hidden" name="sport" value="<c:out value='${param.sport}' />" />
							<input type="hidden" name="time" value="<c:out value='${param.time}' />" />
							<input type="hidden" name="tz" value="<c:out value='${param.tz}' />" />
							<input type="hidden" name="page" value="1" />
							<label class="field sort-panel__field" for="sort-select">
								<span class="field__label"><spring:message code="feed.sortBy" /></span>
								<select
									id="sort-select"
									name="sort"
									class="field__control field__control--select sort-panel__select"
									onchange="this.form.submit()">
									<option value="soonest" ${selectedSort == 'soonest' ? 'selected="selected"' : ''}><spring:message code="feed.sort.soonest" /></option>
									<option value="price" ${selectedSort == 'price' ? 'selected="selected"' : ''}><spring:message code="feed.sort.price" /></option>
									<option value="spots" ${selectedSort == 'spots' ? 'selected="selected"' : ''}><spring:message code="feed.sort.spots" /></option>
								</select>
							</label>
						</form>
					</section>

					<section>
						<div class="section-head">
							<div>
								<h2 class="section-head__title"><spring:message code="feed.trending.title" /></h2>
								<p class="section-head__meta"><spring:message code="feed.trending.subtitle" /></p>
							</div>
						</div>

						<div class="event-grid">
							<c:forEach var="event" items="${feedPage.featuredEvents}">
								<c:url var="eventHref" value="${event.href}" />
								<ui:card
									href="${eventHref}"
									className="event-card"
									ariaLabel="${event.title}">
									<div class="event-card__media ${event.mediaClass}">
										<c:if test="${not empty event.bannerImageUrl}">
											<img
												class="event-card__image"
												src="${pageContext.request.contextPath}${event.bannerImageUrl}"
												alt=""
												loading="lazy"
												decoding="async" />
										</c:if>
										<span class="event-card__badge"><c:out value="${event.badge}" /></span>
									</div>

									<div class="event-card__body">
										<span class="event-card__sport"><c:out value="${event.sport}" /></span>
										<h3 class="event-card__title"><c:out value="${event.title}" /></h3>
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

						<div class="section-head">
							<div>
								<p class="section-head__meta">
									<spring:message code="feed.pagination" arguments="${feedPage.page},${feedPage.totalPages}" argumentSeparator="," />
								</p>
							</div>
							<div class="chip-row">
								<c:if test="${not empty feedPage.previousPageHref}">
									<spring:message var="previousLabel" code="pagination.previous" />
									<ui:chip
										label="${previousLabel}"
										href="${pageContext.request.contextPath}${feedPage.previousPageHref}"
										active="${false}"
										tone="default" />
								</c:if>
								<c:if test="${not empty feedPage.nextPageHref}">
									<spring:message var="nextLabel" code="pagination.next" />
									<ui:chip
										label="${nextLabel}"
										href="${pageContext.request.contextPath}${feedPage.nextPageHref}"
										active="${false}"
										tone="default" />
								</c:if>
							</div>
						</div>
					</section>
				</section>
			</main>
		</div>
	</body>
</html>
