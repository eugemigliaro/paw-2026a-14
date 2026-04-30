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
												<div class="events-toggle-wrapper" id="eventsToggle">
													<div class="events-toggle-slider ${param.filter eq 'past' ? 'right' : ''}"
														id="eventsSlider"></div>
													<button
														class="events-toggle-btn ${empty param.filter or param.filter ne 'past' ? 'active' : ''}"
														data-value="upcoming">
														<spring:message code="filter.upcoming" text="Upcoming" />
													</button>
													<button
														class="events-toggle-btn ${param.filter eq 'past' ? 'active' : ''}"
														data-value="past">
														<spring:message code="filter.past" text="Past" />
													</button>
												</div>
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
																				<path d="M19 21l-7-7-7 7" />
																				<polyline points="7 14 12 8 17 14" />
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
														<div class="matches-empty-state">
															<p>
																<c:out value="${emptyMessage}" />
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
																		<span class="event-card__badge">
																			<c:out value="${event.badge}" />
																		</span>
																	</div>

																	<div class="event-card__body">
																		<span class="event-card__sport">
																			<c:out value="${event.sport}" />
																		</span>

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
																				<c:out value="${event.venue}" />
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
							const eventsButtons = document.querySelectorAll(".events-toggle-btn");
							const eventsSlider = document.getElementById("eventsSlider");

							eventsButtons.forEach((btn, index) => {
								btn.addEventListener("click", () => {
									// Move slider
									if (index === 1) {
										eventsSlider.classList.add("right");
									} else {
										eventsSlider.classList.remove("right");
									}

									eventsButtons.forEach(b => b.classList.remove("active"));
									btn.classList.add("active");

									// Navigate with new filter parameter
									const value = btn.dataset.value;
									const currentUrl = new URL(window.location);
									if (value === "past") {
										currentUrl.searchParams.set("filter", "past");
									} else {
										currentUrl.searchParams.delete("filter");
									}
									currentUrl.searchParams.set("page", "1"); // Reset to first page when changing filter
									window.location.href = currentUrl.toString();
								});
							});
						</script>

						</html>
