<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
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
			<spring:message var="eventTypeFilterTitle" code="filter.eventType" />
			<c:set var="feedPath" value="/" />
			<c:url var="feedFormAction" value="${feedPath}" />
			<main class="page-shell page-shell--feed">
				<section class="feed-main">
					<section class="feed-hero-stage">
						<div class="hero-and-search">

						<section class="hero-panel">
							<c:if test="${not empty feedEyebrow}">
								<p class="eyebrow"><c:out value="${feedEyebrow}" /></p>
							</c:if>
							<h1 class="hero-panel__title"><c:out value="${feedTitle}" /></h1>
							<p class="hero-panel__description"><c:out value="${feedDescription}" /></p>
						</section>

						<section class="search-panel" aria-label="${searchAriaLabel}">
							<form:form
								method="get"
								action="${feedFormAction}"
								modelAttribute="searchForm"
								cssClass="search-panel__form search-panel__form--floating-error"
								novalidate="novalidate">
								<c:forEach var="selectedSport" items="${selectedSports}">
									<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
								</c:forEach>
								<c:if test="${selectedType eq 'tournament'}">
									<input type="hidden" name="type" value="tournament" />
								</c:if>
								<input type="hidden" name="startDate" value="<c:out value='${selectedStartDateValue}' />" />
								<input type="hidden" name="endDate" value="<c:out value='${selectedEndDateValue}' />" />
								<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
								<input type="hidden" name="minPrice" value="<c:out value='${selectedMinPriceValue}' />" />
								<input type="hidden" name="maxPrice" value="<c:out value='${selectedMaxPriceValue}' />" />
								<div class="search-panel__row">
									<div class="search-panel__input">
										<span class="search-panel__icon" aria-hidden="true"></span>
										<form:input
											path="q"
											cssClass="search-panel__control"
											placeholder="${feedSearchPlaceholder}" />
									</div>
									<ui:button label="${feedSearchButtonLabel}" type="submit" />
								</div>
								<form:errors path="q" cssClass="search-panel__error" element="p" />
							</form:form>
						</section>
					</div>
						<div class="horizontal-filters-bar" aria-label="${filtersAriaLabel}">
							<c:forEach var="group" items="${feedFilterGroups}">
									<c:choose>
										<c:when test="${group.title eq eventTypeFilterTitle}">
											<c:forEach var="option" items="${group.options}" varStatus="optionStatus">
												<c:choose>
													<c:when test="${optionStatus.first}">
														<c:set var="eventTypeMatchLabel" value="${option.label}" />
														<c:choose>
															<c:when test="${not empty option.params}">
																<c:url var="eventTypeMatchHref" value="${feedPath}">
																	<c:forEach var="p" items="${option.params}">
																		<c:param name="${p.key}" value="${p.value}" />
																	</c:forEach>
																</c:url>
															</c:when>
															<c:otherwise>
																<c:set var="eventTypeMatchHref" value="${option.href}" />
															</c:otherwise>
														</c:choose>
													</c:when>
													<c:otherwise>
														<c:set var="eventTypeTournamentLabel" value="${option.label}" />
														<c:choose>
															<c:when test="${not empty option.params}">
																<c:url var="eventTypeTournamentHref" value="${feedPath}">
																	<c:forEach var="p" items="${option.params}">
																		<c:param name="${p.key}" value="${p.value}" />
																	</c:forEach>
																</c:url>
															</c:when>
															<c:otherwise>
																<c:set var="eventTypeTournamentHref" value="${option.href}" />
															</c:otherwise>
														</c:choose>
													</c:otherwise>
												</c:choose>
											</c:forEach>
											<ui:eventsFilterToggle
												className="feed-event-type-toggle"
												ariaLabel="${group.title}"
												currentValue="${selectedType}"
												leftValue="match"
												rightValue="tournament"
												leftLabel="${eventTypeMatchLabel}"
												rightLabel="${eventTypeTournamentLabel}"
												leftHref="${eventTypeMatchHref}"
												rightHref="${eventTypeTournamentHref}"
												iconOnly="${true}"
												leftIcon="ball"
												rightIcon="trophy"
												forceLeftOnEmpty="${true}" />
										</c:when>
										<c:otherwise>
										<div class="filter-dropdown" data-filter-name="${group.title}">
										<button type="button" class="filter-dropdown__toggle">
											<span class="filter-dropdown__icon">
												<c:choose>
													<c:when test="${fn:contains(fn:toLowerCase(group.title), 'sport') || fn:contains(fn:toLowerCase(group.title), 'deporte')}">
														<icon:football />
													</c:when>
													<c:otherwise>
														<icon:trophy />
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
														<c:choose>
															<c:when test="${not empty option.params}">
																<c:url var="clearFilterHref" value="${feedFormAction}">
																	<c:forEach var="p" items="${option.params}">
																		<c:param name="${p.key}" value="${p.value}" />
																	</c:forEach>
																</c:url>
															</c:when>
															<c:otherwise>
																<c:set var="clearFilterHref" value="${option.href}" />
															</c:otherwise>
														</c:choose>
													</c:when>
													<c:otherwise>
														<c:choose>
															<c:when test="${not empty option.params}">
																<c:url var="optionHref" value="${feedFormAction}">
																	<c:forEach var="p" items="${option.params}">
																		<c:param name="${p.key}" value="${p.value}" />
																	</c:forEach>
																</c:url>
															</c:when>
															<c:otherwise>
																<c:url var="optionHref" value="${option.href}" />
															</c:otherwise>
														</c:choose>
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
									</c:otherwise>
								</c:choose>
								</c:forEach>

							<div class="filter-dropdown" data-filter-name="Date">
								<button type="button" class="filter-dropdown__toggle">
									<span class="filter-dropdown__icon">
										<icon:calendar />
									</span>
									<spring:message code="filter.date" />
								</button>
								<div class="filter-dropdown__panel">
									<form method="get" action="${feedFormAction}" class="filter-dropdown__form" novalidate="novalidate">
										<input type="hidden" name="q" value="<c:out value='${searchForm.q}' />" />
										<c:forEach var="selectedSport" items="${selectedSports}">
											<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
										</c:forEach>
										<c:if test="${selectedType eq 'tournament'}">
											<input type="hidden" name="type" value="tournament" />
										</c:if>
										<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
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

										<c:url var="clearDateHref" value="${feedPath}">
											<c:param name="q" value="${searchForm.q}" />
											<c:forEach var="selectedSport" items="${selectedSports}">
												<c:param name="sport" value="${selectedSport}" />
											</c:forEach>
											<c:if test="${selectedType eq 'tournament'}">
												<c:param name="type" value="tournament" />
											</c:if>
											<c:param name="sort" value="${selectedSort}" />
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
										<icon:banknote />
									</span>
									<spring:message code="filter.price" />
								</button>
								<div class="filter-dropdown__panel">
									<form method="get" action="${feedFormAction}" class="filter-dropdown__form" novalidate="novalidate">
										<input type="hidden" name="q" value="<c:out value='${searchForm.q}' />" />
										<c:forEach var="selectedSport" items="${selectedSports}">
											<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
										</c:forEach>
										<c:if test="${selectedType eq 'tournament'}">
											<input type="hidden" name="type" value="tournament" />
										</c:if>
										<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
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

										<c:url var="clearPriceHref" value="${feedPath}">
											<c:param name="q" value="${searchForm.q}" />
											<c:forEach var="selectedSport" items="${selectedSports}">
												<c:param name="sport" value="${selectedSport}" />
											</c:forEach>
											<c:if test="${selectedType eq 'tournament'}">
												<c:param name="type" value="tournament" />
											</c:if>
											<c:param name="sort" value="${selectedSort}" />
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

							<c:url var="clearFiltersHref" value="${feedPath}">
								<c:param name="type" value="${selectedType}" />
								<c:param name="filter" value="${searchForm.filterName}" />
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
									<c:if test="${selectedType eq 'tournament'}">
										<input type="hidden" name="type" value="tournament" />
									</c:if>
									<input type="hidden" id="explore-location-latitude" name="latitude" data-explore-location-latitude="true" />
									<input type="hidden" id="explore-location-longitude" name="longitude" data-explore-location-longitude="true" />
								</form>
							</div>
							<c:if test="${mapPickerEnabled}">
								<spring:message var="exploreLocationTitle" code="feed.locationPicker.title" />
								<spring:message var="exploreLocationDescription" code="feed.locationPicker.description" />
								<spring:message var="exploreLocationUseLabel" code="feed.locationPicker.use" />
								<spring:message var="exploreLocationCancelLabel" code="feed.locationPicker.cancel" />
								<spring:message var="exploreLocationMapAria" code="feed.locationPicker.map.aria" />
								<c:url var="appRootUrl" value="/" />
								<c:set var="contextAwareFeedMapTileUrlTemplate"
									value="${appRootUrl}${fn:substring(mapTileUrlTemplate, 1, fn:length(mapTileUrlTemplate))}" />
								<div class="explore-location-modal" data-explore-location-modal="true" hidden="hidden">
									<section class="explore-location-modal__panel" role="dialog" aria-modal="true" aria-labelledby="explore-location-title">
										<div class="explore-location-modal__header">
											<div>
												<h2 id="explore-location-title" class="explore-location-modal__title">
													<c:out value="${exploreLocationTitle}" />
												</h2>
												<p class="explore-location-modal__copy">
													<c:out value="${exploreLocationDescription}" />
												</p>
											</div>
											<button type="button" class="btn btn--ghost btn--sm" data-explore-location-cancel="true">
												<c:out value="${exploreLocationCancelLabel}" />
											</button>
										</div>
										<section
											class="location-picker explore-location-picker"
											data-location-picker="true"
											data-location-picker-deferred="true"
											data-latitude-input="#explore-location-latitude"
											data-longitude-input="#explore-location-longitude"
											data-tile-url-template="${contextAwareFeedMapTileUrlTemplate}"
											data-attribution="${mapAttribution}"
											data-default-latitude="${mapDefaultLatitude}"
											data-default-longitude="${mapDefaultLongitude}"
											data-default-zoom="${mapDefaultZoom}">
											<div class="location-picker__header">
												<span class="field__label"><c:out value="${exploreLocationTitle}" /></span>
												<div class="location-picker__actions">
													<button type="button" class="btn btn--ghost btn--sm" data-location-zoom-out="true">
														<spring:message code="host.form.location.zoomOut" />
													</button>
													<button type="button" class="btn btn--ghost btn--sm" data-location-zoom-in="true">
														<spring:message code="host.form.location.zoomIn" />
													</button>
													<button type="button" class="btn btn--ghost btn--sm" data-location-clear="true">
														<spring:message code="host.form.location.clear" />
													</button>
												</div>
											</div>
											<div class="location-picker__map" data-location-map="true" aria-label="${exploreLocationMapAria}"></div>
											<c:if test="${not empty mapAttribution}">
												<p class="location-picker__attribution"><c:out value="${mapAttribution}" /></p>
											</c:if>
										</section>
										<div class="explore-location-modal__actions">
											<button type="button" class="btn btn--ghost" data-explore-location-cancel="true">
												<c:out value="${exploreLocationCancelLabel}" />
											</button>
											<button type="button" class="btn btn--primary" data-explore-location-confirm="true" disabled="disabled">
												<c:out value="${exploreLocationUseLabel}" />
											</button>
										</div>
									</section>
								</div>
							</c:if>

					<section>
						<div class="section-head section-head--feed-list">
							<div>
								<h2 class="section-head__title section-head__title--feed-list"><spring:message code="feed.trending.title" /></h2>
								<p class="section-head__meta"><spring:message code="feed.trending.subtitle" /></p>
							</div>
						</div>
						<c:choose>
							<c:when test="${empty featuredEvents}">
								<spring:message var="emptyFeedMessage" code="feed.empty.message" />
								<div class="matches-empty-state feed-empty-state">
									<div class="feed-empty-state__art" aria-hidden="true">
										<icon:emptyState />
									</div>
									<p class="feed-empty-state__copy"><c:out value="${emptyFeedMessage}" /></p>
								</div>
							</c:when>
							<c:otherwise>
								<div class="event-grid">
									<c:forEach var="event" items="${featuredEvents}">
										<c:choose>
											<c:when test="${featuredEventType.dbValue == 'tournament'}">
												<ui:eventCard
													tournament="${event}"
													badgeLabel="${eventBadgeLabels[event.id]}"
													distanceLabel="${eventDistanceLabels[event.id]}"
													relationshipBadgeCodes="${eventRelationshipBadgeCodes[event.id]}"
													headingLevel="h3" />
											</c:when>
											<c:otherwise>
												<ui:eventCard
													match="${event}"
													badgeLabel="${eventBadgeLabels[event.id]}"
													distanceLabel="${eventDistanceLabels[event.id]}"
													relationshipBadgeCodes="${eventRelationshipBadgeCodes[event.id]}"
													headingLevel="h3" />
											</c:otherwise>
										</c:choose>
									</c:forEach>
								</div>

								<c:if test="${feedTotalPages > 1}">
									<spring:message var="paginationAriaLabel" code="pagination.aria" />
									<spring:message var="feedPaginationPagesLabel" code="feed.pagination.pages" />
									<spring:message var="previousLabel" code="pagination.previous" />
									<spring:message var="nextLabel" code="pagination.next" />
									<section class="feed-pagination" aria-label="${paginationAriaLabel}">
										<nav class="feed-pagination__nav" aria-label="${feedPaginationPagesLabel}">
											<c:choose>
												<c:when test="${not empty feedPreviousPageHref}">
													<c:url var="feedPrevHref" value="${feedPreviousPageHref}" />
														<a class="feed-pagination__control" href="${feedPrevHref}"><c:out value="${previousLabel}" /></a>
												</c:when>
												<c:otherwise>
														<span class="feed-pagination__control feed-pagination__control--disabled"><c:out value="${previousLabel}" /></span>
												</c:otherwise>
											</c:choose>

											<div class="feed-pagination__pages">
												<c:forEach var="item" items="${feedPaginationItems}">
													<c:choose>
														<c:when test="${item.ellipsis}">
																<span class="feed-pagination__ellipsis" aria-hidden="true"><c:out value="${item.label}" /></span>
														</c:when>
														<c:when test="${item.current}">
																<span class="feed-pagination__page feed-pagination__page--current" aria-current="page"><c:out value="${item.label}" /></span>
														</c:when>
														<c:otherwise>
															<c:url var="feedPageItemHref" value="${item.href}" />
																<a class="feed-pagination__page" href="${feedPageItemHref}"><c:out value="${item.label}" /></a>
														</c:otherwise>
													</c:choose>
												</c:forEach>
											</div>

											<c:choose>
												<c:when test="${not empty feedNextPageHref}">
													<c:url var="feedNextHref" value="${feedNextPageHref}" />
														<a class="feed-pagination__control" href="${feedNextHref}"><c:out value="${nextLabel}" /></a>
												</c:when>
												<c:otherwise>
														<span class="feed-pagination__control feed-pagination__control--disabled"><c:out value="${nextLabel}" /></span>
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
