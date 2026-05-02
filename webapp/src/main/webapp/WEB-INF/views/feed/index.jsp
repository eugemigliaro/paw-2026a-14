<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
				<c:url var="feedFormAction" value="/" />
				<main class="page-shell page-shell--feed">


				<section class="feed-main">
					<section class="feed-hero-stage">
						<div class="hero-and-search">


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
								action="${feedFormAction}"
								modelAttribute="feedSearchForm"
								cssClass="search-panel__form"
								novalidate="novalidate">
								<c:forEach var="selectedSport" items="${selectedSports}">
									<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
								</c:forEach>
								<input type="hidden" name="startDate" value="<c:out value='${selectedStartDateValue}' />" />
								<input type="hidden" name="endDate" value="<c:out value='${selectedEndDateValue}' />" />
								<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
								<input
									type="hidden"
									name="tz"
									value="<c:out value='${selectedTimezone}' />"
									data-browser-timezone-field="true" />
								<input type="hidden" name="minPrice" value="<c:out value='${selectedMinPriceValue}' />" />
								<input type="hidden" name="maxPrice" value="<c:out value='${selectedMaxPriceValue}' />" />
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
					</div>
						<div class="horizontal-filters-bar" aria-label="${filtersAriaLabel}">
							<c:forEach var="group" items="${feedPage.filterGroups}">
								<div class="filter-dropdown" data-filter-name="${group.title}">
									<button type="button" class="filter-dropdown__toggle">
										<span class="filter-dropdown__icon">
											<c:choose>
												<c:when test="${fn:contains(fn:toLowerCase(group.title), 'sport') || fn:contains(fn:toLowerCase(group.title), 'deporte')}">
													<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
														<circle cx="12" cy="12" r="10" />
														<path d="M5.5 18.5c3-3 3-10 13-13" />
														<path d="M5.5 5.5c3 3 3 10 13 13" />
													</svg>
												</c:when>
												<c:when test="${fn:contains(fn:toLowerCase(group.title), 'status') || fn:contains(fn:toLowerCase(group.title), 'estado')}">
													<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
												</c:when>
												<c:when test="${fn:contains(fn:toLowerCase(group.title), 'visibilit')}">
													<svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
												</c:when>
												<c:otherwise>
													<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/></svg>
												</c:otherwise>
											</c:choose>
										</span>
										<c:out value="${group.title}" />
									</button>
									<div class="filter-dropdown__panel">
										<c:forEach var="option" items="${group.options}">
											<c:url var="optionHref" value="${option.href}" />
											<a href="${optionHref}" class="filter-dropdown__item ${option.active ? 'filter-dropdown__item--active' : ''}">
												<c:out value="${option.label}" />
											</a>
										</c:forEach>
									</div>
								</div>
							</c:forEach>

							<div class="filter-dropdown" data-filter-name="Date">
								<button type="button" class="filter-dropdown__toggle">
									<span class="filter-dropdown__icon">
										<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
									</span>
									<spring:message code="filter.date" />
								</button>
								<div class="filter-dropdown__panel">
									<form method="get" action="${feedFormAction}" class="filter-dropdown__form" novalidate="novalidate">
										<input type="hidden" name="q" value="<c:out value='${feedSearchForm.q}' />" />
										<c:forEach var="selectedSport" items="${selectedSports}">
											<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
										</c:forEach>
										<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
										<input type="hidden" name="tz" value="<c:out value='${selectedTimezone}' />" data-browser-timezone-field="true" />
										<input type="hidden" name="minPrice" value="<c:out value='${selectedMinPriceValue}' />" />
										<input type="hidden" name="maxPrice" value="<c:out value='${selectedMaxPriceValue}' />" />

										<div class="field filter-rail__field">
											<label class="field__label" for="start-date"><spring:message code="filter.date.from" /></label>
											<input id="start-date" name="startDate" type="date" class="field__control" min="<c:out value='${selectedDateMinValue}' />" value="<c:out value='${selectedStartDateValue}' />" />
										</div>
										<div class="field filter-rail__field">
											<label class="field__label" for="end-date"><spring:message code="filter.date.to" /></label>
											<input id="end-date" name="endDate" type="date" class="field__control" min="<c:out value='${selectedDateMinValue}' />" value="<c:out value='${selectedEndDateValue}' />" />
										</div>

										<spring:message var="applyDateLabel" code="filter.date.apply" text="Apply" />
										<ui:button label="${applyDateLabel}" type="submit" fullWidth="${true}" />
									</form>
								</div>
							</div>

							<div class="filter-dropdown" data-filter-name="Price">
								<button type="button" class="filter-dropdown__toggle">
									<span class="filter-dropdown__icon">
										<svg viewBox="0 0 24 24"><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2"/><path d="M6 12h.01M18 12h.01"/></svg>
									</span>
									<spring:message code="filter.price" />
								</button>
								<div class="filter-dropdown__panel">
									<form method="get" action="${feedFormAction}" class="filter-dropdown__form" novalidate="novalidate">
										<input type="hidden" name="q" value="<c:out value='${feedSearchForm.q}' />" />
										<c:forEach var="selectedSport" items="${selectedSports}">
											<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
										</c:forEach>
										<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
										<input type="hidden" name="tz" value="<c:out value='${selectedTimezone}' />" data-browser-timezone-field="true" />
										<input type="hidden" name="startDate" value="<c:out value='${selectedStartDateValue}' />" />
										<input type="hidden" name="endDate" value="<c:out value='${selectedEndDateValue}' />" />

										<div class="field filter-rail__field filter-rail__price-field">
											<label class="field__label" for="min-price"><spring:message code="filter.price.from" /></label>
											<div class="filter-rail__price-input-wrap">
												<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
												<input id="min-price" name="minPrice" type="number" min="0" step="0.01" inputmode="decimal" class="field__control filter-rail__price-input" value="<c:out value='${selectedMinPriceValue}' />" placeholder="0" />
											</div>
										</div>
										<div class="field filter-rail__field filter-rail__price-field">
											<label class="field__label" for="max-price"><spring:message code="filter.price.to" /></label>
											<div class="filter-rail__price-input-wrap">
												<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
												<input id="max-price" name="maxPrice" type="number" min="0" step="0.01" inputmode="decimal" class="field__control filter-rail__price-input" value="<c:out value='${selectedMaxPriceValue}' />" placeholder="12" />
											</div>
										</div>

										<spring:message var="applyPriceLabel" code="filter.price.apply" />
										<ui:button label="${applyPriceLabel}" type="submit" fullWidth="${true}" />
									</form>
								</div>
							</div>

							<c:url var="clearFiltersHref" value="${feedFormAction}">
								<c:param name="q" value="${feedSearchForm.q}" />
								<c:param name="sort" value="${selectedSort}" />
								<c:param name="tz" value="${selectedTimezone}" />
								<c:param name="startDate" value="" />
								<c:param name="endDate" value="" />
								<c:param name="minPrice" value="" />
								<c:param name="maxPrice" value="" />
							</c:url>
							<spring:message var="clearAllLabel" code="filter.clearAll" />
							<ui:button
								label="${clearAllLabel}"
								href="${clearFiltersHref}"
								variant="primary"
								size="sm"
								className="filter-rail__clear" />

							<form method="get" action="${feedFormAction}" class="sort-panel" aria-label="${sortAriaLabel}">
								<input type="hidden" name="q" value="<c:out value='${feedSearchForm.q}' />" />
								<c:forEach var="selectedSport" items="${selectedSports}">
									<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
								</c:forEach>
								<input type="hidden" name="startDate" value="<c:out value='${selectedStartDateValue}' />" />
								<input type="hidden" name="endDate" value="<c:out value='${selectedEndDateValue}' />" />
								<input
									type="hidden"
									name="tz"
									value="<c:out value='${selectedTimezone}' />"
									data-browser-timezone-field="true" />
								<input type="hidden" name="minPrice" value="<c:out value='${selectedMinPriceValue}' />" />
								<input type="hidden" name="maxPrice" value="<c:out value='${selectedMaxPriceValue}' />" />
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
						</div>

					<section>
						<div class="section-head section-head--feed-list">
							<div>
								<h2 class="section-head__title section-head__title--feed-list"><spring:message code="feed.trending.title" /></h2>
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
											<c:url var="eventBannerSrc" value="${event.bannerImageUrl}" />
											<img
												class="event-card__image"
												src="${eventBannerSrc}"
												alt=""
												loading="lazy"
												decoding="async" />
										</c:if>
										<div class="event-card__media-badges">
											<span class="event-card__badge"><c:out value="${event.badge}" /></span>
											<c:forEach var="relationshipBadge" items="${event.relationshipBadges}">
												<span class="event-badge event-badge--${relationshipBadge.type}">
													<c:out value="${relationshipBadge.label}" />
												</span>
											</c:forEach>
										</div>
									</div>

									<div class="event-card__body">
										<div class="event-card__sport-row">
											<span class="event-card__sport"><c:out value="${event.sport}" /></span>
											<c:if test="${event.recurring}">
												<span class="event-card__recurring"><c:out value="${event.recurringLabel}" /></span>
											</c:if>
										</div>
										<h3 class="event-card__title"><c:out value="${event.title}" /></h3>
										<div class="event-card__meta">
											<span class="event-card__meta-item">
												<span class="event-card__meta-icon" aria-hidden="true">
													<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
														<path d="M12 22s8-4.35 8-11a8 8 0 1 0-16 0c0 6.65 8 11 8 11z" />
														<circle cx="12" cy="11" r="3" />
													</svg>
												</span>
												<c:out value="${event.venue}" />
											</span>
											<span class="event-card__meta-item">
												<span class="event-card__meta-icon" aria-hidden="true">
													<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
														<rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
														<line x1="16" y1="2" x2="16" y2="6" />
														<line x1="8" y1="2" x2="8" y2="6" />
														<line x1="3" y1="10" x2="21" y2="10" />
													</svg>
												</span>
												<c:out value="${empty event.dateLabel ? event.schedule : event.dateLabel}" />
											</span>
											<c:if test="${not empty event.timeLabel}">
												<span class="event-card__meta-item">
													<span class="event-card__meta-icon" aria-hidden="true">
														<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
															<circle cx="12" cy="12" r="10" />
															<polyline points="12 6 12 12 16 14" />
														</svg>
													</span>
													<c:out value="${event.timeLabel}" />
												</span>
											</c:if>
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

						<c:if test="${feedPage.totalPages > 1}">
							<spring:message var="previousLabel" code="pagination.previous" />
							<spring:message var="nextLabel" code="pagination.next" />
							<section class="feed-pagination" aria-label="Pagination">
								<nav class="feed-pagination__nav" aria-label="Feed pages">
									<c:choose>
										<c:when test="${not empty feedPage.previousPageHref}">
											<c:url var="feedPrevHref" value="${feedPage.previousPageHref}" />
											<a
												class="feed-pagination__control"
												href="${feedPrevHref}">
												${previousLabel}
											</a>
										</c:when>
										<c:otherwise>
											<span class="feed-pagination__control feed-pagination__control--disabled">${previousLabel}</span>
										</c:otherwise>
									</c:choose>

									<div class="feed-pagination__pages">
										<c:forEach var="item" items="${feedPage.paginationItems}">
											<c:choose>
												<c:when test="${item.ellipsis}">
													<span class="feed-pagination__ellipsis" aria-hidden="true">${item.label}</span>
												</c:when>
												<c:when test="${item.current}">
													<span class="feed-pagination__page feed-pagination__page--current" aria-current="page">${item.label}</span>
												</c:when>
												<c:otherwise>
													<c:url var="feedPageItemHref" value="${item.href}" />
													<a
														class="feed-pagination__page"
														href="${feedPageItemHref}">
														${item.label}
													</a>
												</c:otherwise>
											</c:choose>
										</c:forEach>
									</div>

									<c:choose>
										<c:when test="${not empty feedPage.nextPageHref}">
											<c:url var="feedNextHref" value="${feedPage.nextPageHref}" />
											<a
												class="feed-pagination__control"
												href="${feedNextHref}">
												${nextLabel}
											</a>
										</c:when>
										<c:otherwise>
											<span class="feed-pagination__control feed-pagination__control--disabled">${nextLabel}</span>
										</c:otherwise>
									</c:choose>
								</nav>
							</section>
						</c:if>
					</section>
				</section>
			</main>
		</div>
	</body>
</html>
