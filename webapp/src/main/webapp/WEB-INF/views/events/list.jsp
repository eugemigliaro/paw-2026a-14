<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
	<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
		<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
			<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
				<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
					<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

						<c:set var="resolvedPageTitleCode"
							value="${empty pageTitleCode ? 'app.brand' : pageTitleCode}" />
						<spring:message var="pageTitle" code="${resolvedPageTitleCode}" />

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
									<spring:message var="searchPlaceholder" code="events.search.placeholder" />

									<main class="page-shell page-shell--matches-list">

										<!-- Header with Title, Search, and Toggle in one row -->
										<div class="events-top-section">

											<div class="events-header-container">
												<h1 class="page-heading__title events-header-title">
													<c:out value="${listTitle}" />
												</h1>

											</div>

											<div class="events-right-section">

												<!-- Search form-->
												<form:form method="get" action="${listControls.searchAction}"
													modelAttribute="listSearchForm" cssClass="filters-bar__search">
													<c:forEach var="selectedSport" items="${selectedSports}">
														<input type="hidden" name="sport"
															value="<c:out value='${selectedSport}' />" />
													</c:forEach>
													<c:forEach var="selectedStatus" items="${selectedStatuses}">
														<input type="hidden" name="status"
															value="<c:out value='${selectedStatus}' />" />
													</c:forEach>
													<c:forEach var="selectedCategory" items="${selectedCategories}">
														<input type="hidden" name="category"
															value="<c:out value='${selectedCategory}' />" />
													</c:forEach>
													<c:forEach var="selectedVisibilityItem"
														items="${selectedVisibility}">
														<input type="hidden" name="visibility"
															value="<c:out value='${selectedVisibilityItem}' />" />
													</c:forEach>
													<input type="hidden" name="startDate"
														value="<c:out value='${selectedStartDateValue}' />" />
													<input type="hidden" name="endDate"
														value="<c:out value='${selectedEndDateValue}' />" />
													<input type="hidden" name="sort"
														value="<c:out value='${selectedSort}' />" />
													<input type="hidden" name="tz"
														value="<c:out value='${selectedTimezone}' />"
														data-browser-timezone-field="true" />
													<input type="hidden" name="minPrice"
														value="<c:out value='${selectedMinPriceValue}' />" />
													<input type="hidden" name="maxPrice"
														value="<c:out value='${selectedMaxPriceValue}' />" />
													<c:if test="${param.filter eq 'past'}">
														<input type="hidden" name="filter" value="past" />
													</c:if>
													<div class="filters-bar__search-row">
														<form:input path="q" cssClass="filters-bar__search-input"
															placeholder="${searchPlaceholder}" />
														<ui:button type="submit" variant="primary"
															className="filters-bar__search-submit">
															<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
																stroke-width="3" stroke-linecap="round"
																stroke-linejoin="round">
																<circle cx="11" cy="11" r="8"></circle>
																<line x1="21" y1="21" x2="16.65" y2="16.65"></line>
															</svg>
														</ui:button>
													</div>
												</form:form>

												<!-- Toggle -->
														<ui:eventsFilterToggle currentFilter="${param.filter}" />
											</div>
										</div>

										<div class="events-filters-section">

											<c:if test="${not empty listControls}">
												<div class="horizontal-filters-bar" aria-label="${filtersAriaLabel}">

													<c:forEach var="group" items="${listControls.filterGroups}">
														<div class="filter-dropdown" data-filter-name="${group.title}">
															<button type="button" class="filter-dropdown__toggle">
																<span class="filter-dropdown__icon">
																	<c:choose>
																		<c:when
																			test="${fn:contains(fn:toLowerCase(group.title), 'category') || fn:contains(fn:toLowerCase(group.title), 'categoría')}">
																			<svg viewBox="0 0 24 24" fill="none"
																				stroke="currentColor" stroke-width="2"
																				stroke-linecap="round"
																				stroke-linejoin="round">
																				<path d="M6 21V14" />
																				<path d="M6 10V3" />
																				<path d="M12 21V12" />
																				<path d="M12 8V3" />
																				<path d="M18 21V16" />
																				<path d="M18 12V3" />
																				<rect x="4" y="10" width="4" height="4" />
																				<rect x="10" y="8" width="4" height="4" />
																				<rect x="16" y="12" width="4" height="4" />
																			</svg>
																		</c:when>
																		<c:when
																			test="${fn:contains(fn:toLowerCase(group.title), 'sport') || fn:contains(fn:toLowerCase(group.title), 'deporte')}">
																			<svg viewBox="0 0 24 24" fill="none"
																				stroke="currentColor" stroke-width="2"
																				stroke-linecap="round"
																				stroke-linejoin="round">
																				<circle cx="12" cy="12" r="10" />
																				<path d="M5.5 18.5c3-3 3-10 13-13" />
																				<path d="M5.5 5.5c3 3 3 10 13 13" />
																			</svg>
																		</c:when>
																		<c:when
																			test="${fn:contains(fn:toLowerCase(group.title), 'status') || fn:contains(fn:toLowerCase(group.title), 'estado')}">
																			<svg viewBox="0 0 24 24">
																				<circle cx="12" cy="12" r="10" />
																				<line x1="12" y1="16" x2="12" y2="12" />
																				<line x1="12" y1="8" x2="12.01"
																					y2="8" />
																			</svg>
																		</c:when>
																		<c:when
																			test="${fn:contains(fn:toLowerCase(group.title), 'visibilit')}">
																			<svg viewBox="0 0 24 24">
																				<path
																					d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
																				<circle cx="12" cy="12" r="3" />
																			</svg>
																		</c:when>
																		<c:otherwise>
																			<svg viewBox="0 0 24 24">
																				<circle cx="12" cy="12" r="10" />
																				<path d="M12 8v8M8 12h8" />
																			</svg>
																		</c:otherwise>
																	</c:choose>
																</span>
																<c:out value="${group.title}" />
															</button>
															<div class="filter-dropdown__panel">
																<c:forEach var="option" items="${group.options}">
																	<c:url var="optionHref" value="${option.href}" />
																	<a href="${optionHref}"
																		class="filter-dropdown__item ${option.active ? 'filter-dropdown__item--active' : ''}">
																		<c:out value="${option.label}" />
																	</a>
																</c:forEach>
															</div>
														</div>
													</c:forEach>

													<div class="filter-dropdown" data-filter-name="Date">
														<button type="button" class="filter-dropdown__toggle">
															<span class="filter-dropdown__icon">
																<svg viewBox="0 0 24 24">
																	<rect x="3" y="4" width="18" height="18" rx="2"
																		ry="2" />
																	<line x1="16" y1="2" x2="16" y2="6" />
																	<line x1="8" y1="2" x2="8" y2="6" />
																	<line x1="3" y1="10" x2="21" y2="10" />
																</svg>
															</span>
															<spring:message code="filter.date" />
														</button>
														<div class="filter-dropdown__panel">
															<form method="get" action="${listControls.searchAction}"
																class="filter-dropdown__form">
																<input type="hidden" name="q"
																	value="<c:out value='${listControls.searchQuery}' />" />
																<input type="hidden" name="sort"
																	value="<c:out value='${selectedSort}' />" />
																<input type="hidden" name="tz"
																	value="<c:out value='${selectedTimezone}' />"
																	data-browser-timezone-field="true" />
																<c:forEach var="selectedSport"
																	items="${selectedSports}">
																	<input type="hidden" name="sport"
																		value="<c:out value='${selectedSport}' />" />
																</c:forEach>
																<c:forEach var="selectedStatus"
																	items="${selectedStatuses}">
																	<input type="hidden" name="status"
																		value="<c:out value='${selectedStatus}' />" />
																</c:forEach>
																<c:forEach var="selectedCategory"
																	items="${selectedCategories}">
																	<input type="hidden" name="category"
																		value="<c:out value='${selectedCategory}' />" />
																</c:forEach>
																<c:forEach var="selectedVisibilityItem"
																	items="${selectedVisibility}">
																	<input type="hidden" name="visibility"
																		value="<c:out value='${selectedVisibilityItem}' />" />
																</c:forEach>
																<input type="hidden" name="minPrice"
																	value="<c:out value='${selectedMinPriceValue}' />" />
																<input type="hidden" name="maxPrice"
																	value="<c:out value='${selectedMaxPriceValue}' />" />
																<c:if test="${param.filter eq 'past'}">
																	<input type="hidden" name="filter" value="past" />
																</c:if>

																<div class="field filter-rail__field">
																	<label class="field__label" for="list-start-date">
																		<spring:message code="filter.date.from" />
																	</label>
																	<input id="list-start-date" name="startDate"
																		type="date" class="field__control"
																		min="<c:out value='${selectedDateMinValue}' />"
																		max="<c:out value='${selectedDateMaxValue}' />"
																		value="<c:out value='${selectedStartDateValue}' />" />
																</div>
																<div class="field filter-rail__field">
																	<label class="field__label" for="list-end-date">
																		<spring:message code="filter.date.to" />
																	</label>
																	<input id="list-end-date" name="endDate" type="date"
																		class="field__control"
																		min="<c:out value='${selectedDateMinValue}' />"
																		max="<c:out value='${selectedDateMaxValue}' />"
																		value="<c:out value='${selectedEndDateValue}' />" />
																</div>

																<spring:message var="applyDateLabel"
																	code="filter.date.apply" text="Apply" />
																<ui:button label="${applyDateLabel}" type="submit"
																	fullWidth="${true}" />
															</form>
														</div>
													</div>

													<div class="filter-dropdown" data-filter-name="Price">
														<button type="button" class="filter-dropdown__toggle">
															<span class="filter-dropdown__icon">
																<svg viewBox="0 0 24 24">
																	<rect x="2" y="6" width="20" height="12" rx="2" />
																	<circle cx="12" cy="12" r="2" />
																	<path d="M6 12h.01M18 12h.01" />
																</svg>
															</span>
															<spring:message code="filter.price" />
														</button>
														<div class="filter-dropdown__panel">
															<form method="get" action="${listControls.searchAction}"
																class="filter-dropdown__form">
																<input type="hidden" name="q"
																	value="<c:out value='${listControls.searchQuery}' />" />
																<input type="hidden" name="sort"
																	value="<c:out value='${selectedSort}' />" />
																<input type="hidden" name="tz"
																	value="<c:out value='${selectedTimezone}' />"
																	data-browser-timezone-field="true" />
																<c:forEach var="selectedSport"
																	items="${selectedSports}">
																	<input type="hidden" name="sport"
																		value="<c:out value='${selectedSport}' />" />
																</c:forEach>
																<c:forEach var="selectedStatus"
																	items="${selectedStatuses}">
																	<input type="hidden" name="status"
																		value="<c:out value='${selectedStatus}' />" />
																</c:forEach>
																<c:forEach var="selectedCategory"
																	items="${selectedCategories}">
																	<input type="hidden" name="category"
																		value="<c:out value='${selectedCategory}' />" />
																</c:forEach>
																<c:forEach var="selectedVisibilityItem"
																	items="${selectedVisibility}">
																	<input type="hidden" name="visibility"
																		value="<c:out value='${selectedVisibilityItem}' />" />
																</c:forEach>
																<input type="hidden" name="startDate"
																	value="<c:out value='${selectedStartDateValue}' />" />
																<input type="hidden" name="endDate"
																	value="<c:out value='${selectedEndDateValue}' />" />
																<c:if test="${param.filter eq 'past'}">
																	<input type="hidden" name="filter" value="past" />
																</c:if>

																<div
																	class="field filter-rail__field filter-rail__price-field">
																	<label class="field__label" for="list-min-price">
																		<spring:message code="filter.price.from" />
																	</label>
																	<div class="filter-rail__price-input-wrap">
																		<span class="filter-rail__price-prefix"
																			aria-hidden="true">$</span>
																		<input id="list-min-price" name="minPrice"
																			type="number" min="0" step="0.01"
																			inputmode="decimal"
																			class="field__control filter-rail__price-input"
																			value="<c:out value='${selectedMinPriceValue}' />"
																			placeholder="0" />
																	</div>
																</div>
																<div
																	class="field filter-rail__field filter-rail__price-field">
																	<label class="field__label" for="list-max-price">
																		<spring:message code="filter.price.to" />
																	</label>
																	<div class="filter-rail__price-input-wrap">
																		<span class="filter-rail__price-prefix"
																			aria-hidden="true">$</span>
																		<input id="list-max-price" name="maxPrice"
																			type="number" min="0" step="0.01"
																			inputmode="decimal"
																			class="field__control filter-rail__price-input"
																			value="<c:out value='${selectedMaxPriceValue}' />"
																			placeholder="12" />
																	</div>
																</div>

																<spring:message var="applyPriceLabel"
																	code="filter.price.apply" />
																<ui:button label="${applyPriceLabel}" type="submit"
																	fullWidth="${true}" />
															</form>
														</div>
													</div>

													<c:url var="clearSearchHref"
														value="${listControls.cleanSearchAction}">
														<c:param name="q" value="${listControls.searchQuery}" />
														<c:param name="sort" value="${selectedSort}" />
														<c:param name="tz" value="${selectedTimezone}" />
														<c:if test="${param.filter eq 'past'}">
															<c:param name="filter" value="past" />
														</c:if>
													</c:url>
													<spring:message var="clearAllLabel" code="filter.clearAll" />
													<ui:button label="${clearAllLabel}" href="${clearSearchHref}"
														variant="primary" size="sm" className="filter-rail__clear" />
												</div>
												<!-- termina horizintal filters bar -->


												<!-- Sort -->
												<section class="matches-search-sort-panel">
													<c:if test="${not empty listControls.sortOptions}">
														<form method="get" action="${listControls.searchAction}"
															class="sort-panel" aria-label="${sortAriaLabel}">
															<label class="field sort-panel__field" for="sort-select">
																<span class="field__label">
																	<c:out value="${listControls.sortLabel}" />
																</span>
																<select id="sort-select" name="sort"
																	class="field__control field__control--select sort-panel__select"
																	onchange="if(this.value){window.location.href=this.value;}">
																	<c:forEach var="option"
																		items="${listControls.sortOptions}">
																		<c:url var="optionHref"
																			value="${option.href}" />
																		<option value="${optionHref}" ${option.selected
																			? 'selected="selected"' : '' }>
																			<c:out value="${option.label}" />
																		</option>
																	</c:forEach>
																</select>
															</label>
														</form>
													</c:if>
												</section>

										</div>

										<!-- Matches -->

										<section class="matches-list-layout">
											<section class="matches-list-content">
												<c:choose>
													<c:when test="${empty events}">
														<spring:message var="emptyResultsMessage" code="feed.empty.message" />
														<div class="matches-empty-state">
															<div class="matches-empty-state__art" aria-hidden="true">
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
															<p class="matches-empty-state__copy">
																<c:out value="${emptyResultsMessage}" />
															</p>
														</div>
													</c:when>

													<c:otherwise>
														<div class="event-grid">
															<c:forEach var="event" items="${events}">
																<c:url var="eventHref" value="${event.href}" />

																<ui:card href="${eventHref}" className="event-card"
																	ariaLabel="${event.title}">

																	<div class="event-card__media ${event.mediaClass}">
																		<c:if test="${not empty event.bannerImageUrl}">
																			<c:url var="eventBannerSrc"
																				value="${event.bannerImageUrl}" />
																			<img class="event-card__image"
																				src="${eventBannerSrc}" alt=""
																				loading="lazy" decoding="async" />
																		</c:if>
																		<div class="event-card__media-badges">
																			<span class="event-card__badge">
																				<c:out value="${event.badge}" />
																			</span>
																			<c:forEach var="relationshipBadge" items="${event.relationshipBadges}">
																				<span class="event-badge event-badge--${relationshipBadge.type}">
																					<c:out value="${relationshipBadge.label}" />
																				</span>
																			</c:forEach>
																		</div>
																	</div>

																	<div class="event-card__body">
																		<div class="event-card__sport-row">
																			<span class="event-card__sport">
																				<c:out value="${event.sport}" />
																			</span>
																			<c:if test="${event.recurring}">
																				<span class="event-card__recurring">
																					<c:out value="${event.recurringLabel}" />
																				</span>
																			</c:if>
																		</div>

																		<h2 class="event-card__title">
																			<c:out value="${event.title}" />
																		</h2>

																		<div class="event-card__meta">
																			<span class="event-card__meta-item">
																				<span class="event-card__meta-icon"
																					aria-hidden="true">
																					<svg viewBox="0 0 24 24" fill="none"
																						stroke="currentColor" stroke-width="2"
																						stroke-linecap="round"
																						stroke-linejoin="round">
																						<path
																							d="M12 22s8-4.35 8-11a8 8 0 1 0-16 0c0 6.65 8 11 8 11z" />
																						<circle cx="12" cy="11" r="3" />
																					</svg>
																				</span>
																				<span class="event-card__meta-text">
																					<c:out value="${event.venue}" />
																				</span>

																			</span>
																			<span class="event-card__meta-item">
																				<span class="event-card__meta-icon"
																					aria-hidden="true">
																					<svg viewBox="0 0 24 24" fill="none"
																						stroke="currentColor" stroke-width="2"
																						stroke-linecap="round"
																						stroke-linejoin="round">
																						<rect x="3" y="4" width="18" height="18"
																							rx="2" ry="2" />
																						<line x1="16" y1="2" x2="16" y2="6" />
																						<line x1="8" y1="2" x2="8" y2="6" />
																						<line x1="3" y1="10" x2="21" y2="10" />
																					</svg>
																				</span>
																				<c:out
																					value="${empty event.dateLabel ? event.schedule : event.dateLabel}" />
																			</span>
																			<c:if test="${not empty event.timeLabel}">
																				<span class="event-card__meta-item">
																					<span class="event-card__meta-icon"
																						aria-hidden="true">
																						<svg viewBox="0 0 24 24" fill="none"
																							stroke="currentColor" stroke-width="2"
																							stroke-linecap="round"
																							stroke-linejoin="round">
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
																				<span>
																					<c:out
																						value="${event.priceLabel}" />
																				</span>
																			</div>
																		</div>
																	</div>

																</ui:card>
															</c:forEach>
														</div>
													</c:otherwise>

												</c:choose>

												<c:if test="${not empty paginationItems}">
													<spring:message var="previousLabel" code="pagination.previous" />
													<spring:message var="nextLabel" code="pagination.next" />
													<section class="feed-pagination" aria-label="${listTitle}">
														<nav class="feed-pagination__nav" aria-label="${listTitle}">

															<c:choose>
																<c:when test="${not empty previousPageHref}">
																	<c:url var="prevHref" value="${previousPageHref}" />
																	<a class="feed-pagination__control"
																		href="${prevHref}">
																		${previousLabel}
																	</a>
																</c:when>
																<c:otherwise>
																	<span
																		class="feed-pagination__control feed-pagination__control--disabled">
																		${previousLabel}
																	</span>
																</c:otherwise>
															</c:choose>

															<div class="feed-pagination__pages">
																<c:forEach var="item" items="${paginationItems}">
																	<c:choose>
																		<c:when test="${item.ellipsis}">
																			<span
																				class="feed-pagination__ellipsis">${item.label}</span>
																		</c:when>
																		<c:when test="${item.current}">
																			<span
																				class="feed-pagination__page feed-pagination__page--current"
																				aria-current="page">
																				${item.label}
																			</span>
																		</c:when>
																		<c:otherwise>
																			<c:url var="pageHref"
																				value="${item.href}" />
																			<a class="feed-pagination__page"
																				href="${pageHref}">
																				${item.label}
																			</a>
																		</c:otherwise>
																	</c:choose>
																</c:forEach>
															</div>

															<c:choose>
																<c:when test="${not empty nextPageHref}">
																	<c:url var="nextHref" value="${nextPageHref}" />
																	<a class="feed-pagination__control"
																		href="${nextHref}">
																		${nextLabel}
																	</a>
																</c:when>
																<c:otherwise>
																	<span
																		class="feed-pagination__control feed-pagination__control--disabled">
																		${nextLabel}
																	</span>
																</c:otherwise>
															</c:choose>
														</nav>
													</section>
												</c:if>
											</section>
										</section>
										</c:if>




									</main>
							</div>
						</body>
						<script>
							document.querySelectorAll("[data-events-toggle='true']").forEach(toggleRoot => {
								const eventsButtons = toggleRoot.querySelectorAll(".events-toggle-btn");
								const eventsSlider = toggleRoot.querySelector("[data-events-toggle-slider='true']");

								if (!eventsButtons.length || !eventsSlider) {
									return;
								}

								eventsButtons.forEach(btn => {
									btn.addEventListener("click", () => {
										const value = btn.dataset.value;
										const isPast = value === "past";

										eventsSlider.classList.toggle("right", isPast);
										eventsButtons.forEach(b => b.classList.remove("active"));
										btn.classList.add("active");

										const currentUrl = new URL(window.location);
										if (isPast) {
											currentUrl.searchParams.set("filter", "past");
										} else {
											currentUrl.searchParams.delete("filter");
										}
										currentUrl.searchParams.set("page", "1");
										/* Clear date and price filters – they don't carry across
										the upcoming/past boundary and cause empty results. */
										currentUrl.searchParams.delete("startDate");
										currentUrl.searchParams.delete("endDate");
										currentUrl.searchParams.delete("minPrice");
										currentUrl.searchParams.delete("maxPrice");
										window.location.href = currentUrl.toString();
									});
								});
							});
						</script>

						</html>
