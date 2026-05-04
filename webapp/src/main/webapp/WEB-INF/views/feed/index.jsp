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
					<spring:message var="clearFilterLabel" code="filter.clear" text="Clear" />
					<spring:message var="seeResultsLabel" code="filter.seeResults" text="See results" />
					<spring:message var="priceRangeError" code="filter.price.rangeError" />
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
											cssClass="search-panel__control"
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
													<c:when test="${fn:contains(fn:toLowerCase(group.title), 'category') || fn:contains(fn:toLowerCase(group.title), 'categoría') || fn:contains(fn:toLowerCase(group.title), 'categoria')}">
														<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
															stroke-linecap="round" stroke-linejoin="round">
															<line x1="4" y1="6" x2="20" y2="6"/>
															<line x1="4" y1="12" x2="20" y2="12"/>
															<line x1="4" y1="18" x2="20" y2="18"/>
															<circle cx="8" cy="6" r="2" fill="white"/>
															<circle cx="15" cy="12" r="2" fill="white"/>
															<circle cx="10" cy="18" r="2" fill="white"/>
														</svg>
													</c:when>
													<c:when test="${fn:contains(fn:toLowerCase(group.title), 'sport') || fn:contains(fn:toLowerCase(group.title), 'deporte')}">
														<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
															stroke-linecap="round" stroke-linejoin="round">
															<circle cx="12" cy="12" r="10"/>
															<polygon points="12,7.4 16.37,10.58 14.7,15.72 9.3,15.72 7.63,10.58"/>
															<line x1="12" y1="7.4" x2="12" y2="2"/>
															<line x1="16.37" y1="10.58" x2="21.5" y2="8.9"/>
															<line x1="14.7" y1="15.72" x2="17.9" y2="20.1"/>
															<line x1="9.3" y1="15.72" x2="6.1" y2="20.1"/>
															<line x1="7.63" y1="10.58" x2="2.5" y2="8.9"/>
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
											<c:set var="clearFilterHref" value="" />
											<c:forEach var="option" items="${group.options}" varStatus="optionStatus">
												<c:choose>
													<c:when test="${optionStatus.first}">
														<c:set var="clearFilterHref" value="${option.href}" />
													</c:when>
													<c:otherwise>
														<c:url var="optionHref" value="${option.href}" />
														<a href="${optionHref}" class="filter-dropdown__item ${option.active ? 'filter-dropdown__item--active' : ''}">
															<c:out value="${option.label}" />
														</a>
													</c:otherwise>
												</c:choose>
											</c:forEach>
											<div class="filter-dropdown__actions">
												<c:url var="clearFilterUrl" value="${clearFilterHref}" />
												<ui:button label="${clearFilterLabel}" href="${clearFilterUrl}"
													variant="secondary" size="sm" className="filter-dropdown__action" />
												<ui:button label="${seeResultsLabel}" type="button"
													variant="primary" size="sm"
													className="filter-dropdown__action filter-dropdown__close" />
											</div>
										</div>
										<c:set var="hasSelectedFilterOptions" value="${false}" />
										<c:forEach var="option" items="${group.options}" varStatus="optionStatus">
											<c:if test="${not optionStatus.first and option.active}">
												<c:set var="hasSelectedFilterOptions" value="${true}" />
											</c:if>
										</c:forEach>
										<c:if test="${hasSelectedFilterOptions}">
											<div class="filter-dropdown__selected-list">
												<c:forEach var="option" items="${group.options}" varStatus="optionStatus">
													<c:if test="${not optionStatus.first and option.active}">
														<span class="filter-dropdown__selected-item">
															<c:out value="${option.label}" />
														</span>
													</c:if>
												</c:forEach>
											</div>
										</c:if>
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

										<c:url var="clearDateHref" value="${feedFormAction}">
											<c:param name="q" value="${feedSearchForm.q}" />
											<c:forEach var="selectedSport" items="${selectedSports}">
												<c:param name="sport" value="${selectedSport}" />
											</c:forEach>
											<c:param name="sort" value="${selectedSort}" />
											<c:param name="tz" value="${selectedTimezone}" />
											<c:param name="minPrice" value="${selectedMinPriceValue}" />
											<c:param name="maxPrice" value="${selectedMaxPriceValue}" />
										</c:url>
										<spring:message var="applyDateLabel" code="filter.date.apply" text="Apply" />
										<div class="filter-dropdown__actions">
											<ui:button label="${clearFilterLabel}" href="${clearDateHref}"
												variant="secondary" size="sm" className="filter-dropdown__action" />
											<ui:button label="${applyDateLabel}" type="submit"
												size="sm" className="filter-dropdown__action" />
										</div>
										</form>
									</div>
									<c:if test="${not empty selectedStartDateValue or not empty selectedEndDateValue}">
										<div class="filter-dropdown__selected-list">
											<c:if test="${not empty selectedStartDateValue}">
												<c:set var="formattedStartDate"
													value="${fn:substring(selectedStartDateValue, 8, 10)}/${fn:substring(selectedStartDateValue, 5, 7)}/${fn:substring(selectedStartDateValue, 2, 4)}" />
												<span class="filter-dropdown__selected-item">
													<spring:message code="filter.date.from" />:
													<c:out value="${formattedStartDate}" />
												</span>
											</c:if>
											<c:if test="${not empty selectedEndDateValue}">
												<c:set var="formattedEndDate"
													value="${fn:substring(selectedEndDateValue, 8, 10)}/${fn:substring(selectedEndDateValue, 5, 7)}/${fn:substring(selectedEndDateValue, 2, 4)}" />
												<span class="filter-dropdown__selected-item">
													<spring:message code="filter.date.to" />:
													<c:out value="${formattedEndDate}" />
												</span>
											</c:if>
										</div>
									</c:if>
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
												<input id="min-price" name="minPrice" type="number" min="0" step="0.01" inputmode="decimal" class="field__control filter-rail__price-input" data-price-from="true" value="<c:out value='${selectedMinPriceValue}' />" placeholder="0" />
											</div>
										</div>
										<div class="field filter-rail__field filter-rail__price-field">
											<label class="field__label" for="max-price"><spring:message code="filter.price.to" /></label>
											<div class="filter-rail__price-input-wrap">
												<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
												<input id="max-price" name="maxPrice" type="number" min="0" step="0.01" inputmode="decimal" class="field__control filter-rail__price-input" data-price-to="true" data-price-range-error="${priceRangeError}" value="<c:out value='${selectedMaxPriceValue}' />" placeholder="12" />
											</div>
										</div>

										<c:url var="clearPriceHref" value="${feedFormAction}">
											<c:param name="q" value="${feedSearchForm.q}" />
											<c:forEach var="selectedSport" items="${selectedSports}">
												<c:param name="sport" value="${selectedSport}" />
											</c:forEach>
											<c:param name="sort" value="${selectedSort}" />
											<c:param name="tz" value="${selectedTimezone}" />
											<c:param name="startDate" value="${selectedStartDateValue}" />
											<c:param name="endDate" value="${selectedEndDateValue}" />
											<c:param name="minPrice" value="" />
											<c:param name="maxPrice" value="" />
										</c:url>
										<spring:message var="applyPriceLabel" code="filter.price.apply" />
										<div class="filter-dropdown__actions">
											<ui:button label="${clearFilterLabel}" href="${clearPriceHref}"
												variant="secondary" size="sm" className="filter-dropdown__action" />
											<ui:button label="${applyPriceLabel}" type="submit"
												size="sm" className="filter-dropdown__action" />
										</div>
										</form>
									</div>
									<c:if test="${not empty selectedMinPriceValue or not empty selectedMaxPriceValue}">
										<div class="filter-dropdown__selected-list">
											<c:if test="${not empty selectedMinPriceValue}">
												<span class="filter-dropdown__selected-item filter-dropdown__selected-item--truncate">
													<spring:message code="filter.price.from" />:
													<c:out value="${selectedMinPriceValue}" />
												</span>
											</c:if>
											<c:if test="${not empty selectedMaxPriceValue}">
												<span class="filter-dropdown__selected-item filter-dropdown__selected-item--truncate">
													<spring:message code="filter.price.to" />:
													<c:out value="${selectedMaxPriceValue}" />
												</span>
											</c:if>
										</div>
									</c:if>
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

								<ui:sortSelect
									id="feed-sort-select"
									label="${sortLabel}"
									ariaLabel="${sortAriaLabel}"
									options="${sortOptions}" />

								<form
									method="post"
									action="<c:url value='/explore/location' />"
									class="near-me-panel near-me-panel--hidden"
									data-explore-location-form="true"
									data-location-available="${nearMeAvailable}">
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="hidden" name="latitude" data-explore-location-latitude="true" />
									<input type="hidden" name="longitude" data-explore-location-longitude="true" />
									<p class="near-me-panel__status" data-explore-location-status="true" role="status"></p>
								</form>
							</div>

					<section>
						<div class="section-head section-head--feed-list">
							<div>
								<h2 class="section-head__title section-head__title--feed-list"><spring:message code="feed.trending.title" /></h2>
								<p class="section-head__meta"><spring:message code="feed.trending.subtitle" /></p>
							</div>
						</div>
						<c:choose>
							<c:when test="${empty feedPage.featuredEvents}">
								<spring:message var="emptyFeedMessage" code="feed.empty.message" />
								<div class="matches-empty-state feed-empty-state">
									<div class="feed-empty-state__art" aria-hidden="true">
									<svg width="100%" viewBox="0 0 680 340" role="img" xmlns="http://www.w3.org/2000/svg" fill="none" stroke="#888" stroke-linecap="round" stroke-linejoin="round">
										<!-- Sun -->
										<circle cx="560" cy="80" r="34" stroke-width="1.5"/>
										<g stroke-width="1.5">
											<line x1="560" y1="30" x2="560" y2="18"/>
											<line x1="560" y1="142" x2="560" y2="130"/>
											<line x1="510" y1="80" x2="498" y2="80"/>
											<line x1="622" y1="80" x2="610" y2="80"/>
											<line x1="522" y1="42" x2="514" y2="34"/>
											<line x1="606" y1="126" x2="614" y2="134"/>
											<line x1="606" y1="42" x2="614" y2="34"/>
											<line x1="522" y1="126" x2="514" y2="134"/>
										</g>

										<!-- Birds -->
										<path d="M390 65 Q400 58 410 55 Q418 53 424 57 Q430 53 436 55 Q444 58 450 65" stroke-width="1.5"/>
										<path d="M470 45 Q478 40 484 38 Q490 37 494 41 Q498 38 504 40 Q510 43 516 48" stroke-width="1.2"/>

										<!-- Cactus trunk -->
										<path d="M179 275 L179 200 Q179 189 190 189 Q201 189 201 200 L201 275" stroke-width="1.5"/>

										<!-- Left arm: goes left, curves up, closes back into trunk -->
										<path d="M179 228 Q148 228 148 218 Q148 208 158 208 L179 208" stroke-width="1.5"/>

										<!-- Right arm: goes right, curves up, closes back into trunk -->
										<path d="M201 240 Q232 240 232 230 Q232 220 222 220 L201 220" stroke-width="1.5"/>

										<!-- Spines -->
										<g stroke-width="1">
											<line x1="179" y1="205" x2="173" y2="200"/>
											<line x1="201" y1="205" x2="207" y2="200"/>
											<line x1="179" y1="250" x2="173" y2="247"/>
											<line x1="201" y1="250" x2="207" y2="247"/>
											<line x1="148" y1="218" x2="143" y2="214"/>
											<line x1="232" y1="230" x2="237" y2="226"/>
										</g>

										<!-- Dunes -->
										<path d="M0 240 Q120 175 280 210 Q400 240 500 198 Q590 162 680 200" stroke-width="1.5"/>
										<path d="M0 270 Q80 232 200 248 Q340 268 460 232 Q560 208 680 238" stroke-width="1.5"/>
										<path d="M0 300 Q100 272 220 286 Q350 306 480 276 Q580 256 680 278" stroke-width="1.5"/>
										<line x1="0" y1="320" x2="680" y2="320" stroke-width="1"/>

										</svg>
									</div>
									<p class="feed-empty-state__copy"><c:out value="${emptyFeedMessage}" /></p>
								</div>
							</c:when>
							<c:otherwise>
								<div class="event-grid">
									<c:forEach var="event" items="${feedPage.featuredEvents}">
										<c:url var="eventHref" value="${event.href}" />
										<ui:card href="${eventHref}" className="event-card" ariaLabel="${event.title}">
											<div class="event-card__media ${event.mediaClass}">
												<c:if test="${not empty event.bannerImageUrl}">
													<c:url var="eventBannerSrc" value="${event.bannerImageUrl}" />
													<img class="event-card__image" src="${eventBannerSrc}" alt="" loading="lazy" decoding="async" />
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
														<span class="event-card__meta-text">
															<c:out value="${event.venue}" />
														</span>

													</span>
													<c:if test="${not empty event.distanceLabel}">
														<span class="event-card__meta-item">
															<span class="event-card__meta-icon" aria-hidden="true">
																<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
																	<path d="M21 6H3" />
																	<path d="M7 6v4" />
																	<path d="M11 6v3" />
																	<path d="M15 6v4" />
																	<path d="M19 6v3" />
																	<rect x="3" y="6" width="18" height="12" rx="2" />
																</svg>
															</span>
															<span class="event-card__meta-text">
																<c:out value="${event.distanceLabel}" />
															</span>
														</span>
													</c:if>
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
													<c:if test="${not empty event.hostLabel}">
														<span class="event-card__meta-item">
															<span class="event-card__meta-icon" aria-hidden="true">
																<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
																	<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
																	<circle cx="12" cy="7" r="4" />
																</svg>
															</span>
															<span class="event-card__meta-text">
																<spring:message code="event.card.hostedBy" />
																<c:out value="${event.hostLabel}" />
															</span>
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
													<a class="feed-pagination__control" href="${feedPrevHref}">${previousLabel}</a>
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
															<a class="feed-pagination__page" href="${feedPageItemHref}">${item.label}</a>
														</c:otherwise>
													</c:choose>
												</c:forEach>
											</div>

											<c:choose>
												<c:when test="${not empty feedPage.nextPageHref}">
													<c:url var="feedNextHref" value="${feedPage.nextPageHref}" />
													<a class="feed-pagination__control" href="${feedNextHref}">${nextLabel}</a>
												</c:when>
												<c:otherwise>
													<span class="feed-pagination__control feed-pagination__control--disabled">${nextLabel}</span>
												</c:otherwise>
											</c:choose>
										</nav>
									</section>
								</c:if>
							</c:otherwise>
						</c:choose>
					</section>
				</section>
			</main>
		</div>
	</body>
</html>
