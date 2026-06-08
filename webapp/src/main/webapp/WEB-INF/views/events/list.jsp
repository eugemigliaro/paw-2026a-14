<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<c:set var="resolvedPageTitleCode" value="${empty pageTitleCode ? 'app.brand' : pageTitleCode}" />
<spring:message var="pageTitle" code="${resolvedPageTitleCode}" />
<spring:message var="listTitle" code="events.title" />
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
					<spring:message var="clearFilterLabel" code="filter.clear" text="Clear" />
					<spring:message var="seeResultsLabel" code="filter.seeResults" text="See results" />
					<spring:message var="priceRangeError" code="filter.price.rangeError" />
					<spring:message var="eventTypeFilterTitle" code="filter.eventType" />

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
							<c:url var="eventsSearchFormAction" value="${listControls.searchAction}" />
							<div class="events-search-container">
								<form:form id="eventsSearchForm" method="get" action="${eventsSearchFormAction}"
									modelAttribute="searchForm" cssClass="filters-bar__search"
									novalidate="novalidate">
									<c:forEach var="selectedSport" items="${searchForm.sport}">
										<input type="hidden" name="sport"
											value="<c:out value='${selectedSport}' />" />
									</c:forEach>
									<c:forEach var="selectedStatus" items="${searchForm.status}">
										<input type="hidden" name="status"
											value="<c:out value='${selectedStatus}' />" />
									</c:forEach>
									<c:forEach var="selectedCategory" items="${searchForm.category}">
										<input type="hidden" name="category"
											value="<c:out value='${selectedCategory}' />" />
									</c:forEach>
									<c:if test="${searchForm.type.getValue() eq 'tournament'}">
										<input type="hidden" name="type" value="tournament" />
									</c:if>
									<c:forEach var="selectedVisibilityItem"
										items="${searchForm.visibility}">
										<input type="hidden" name="visibility"
											value="<c:out value='${selectedVisibilityItem}' />" />
									</c:forEach>
									<c:if test="${searchForm.type.getValue() ne 'tournament'}">
										<input type="hidden" name="startDate"
											value="<c:out value='${searchForm.startDate}' />" />
										<input type="hidden" name="endDate"
											value="<c:out value='${searchForm.endDate}' />" />
									</c:if>
									<input type="hidden" name="sort"
										value="<c:out value='${searchForm.sort}' />" />
									<input type="hidden" name="minPrice"
										value="<c:out value='${searchForm.minPrice}' />" />
									<input type="hidden" name="maxPrice"
										value="<c:out value='${searchForm.maxPrice}' />" />
									<c:if test="${searchForm.filterName eq 'PAST'}">
										<input type="hidden" name="filter" value="${searchForm.filterName}" />
									</c:if>
									<input type="hidden" name="page" id="eventsSearchForm_page" value="${pageNumber}" />
									<div class="filters-bar__search-row">
										<form:input path="q" cssClass="filters-bar__search-input" placeholder="${searchPlaceholder}" />
										<ui:button type="submit" variant="primary" className="filters-bar__search-submit">
											<icon:magnifyingGlass />
										</ui:button>
									</div>
									<form:errors path="q" cssClass="search-panel__error" element="p" />
								</form:form>
							</div>

							<!-- Toggle -->
							<ui:eventsFilterToggle currentFilter="${searchForm.filterName}" />
						</div>
					</div>

					<div class="events-filters-section">

						<c:if test="${not empty listControls}">
							<div class="horizontal-filters-bar" aria-label="${filtersAriaLabel}">

								<c:forEach var="group" items="${listControls.filterGroups}">
									<c:choose>
										<c:when test="${group.titleCode eq 'filter.eventType'}">
											<c:forEach var="option" items="${group.options}" varStatus="optionStatus">
												<c:choose>
													<c:when test="${optionStatus.first}">
														<spring:message var="eventTypeMatchLabel" code="${option.labelCode}" />
														<c:choose>
															<c:when test="${not empty option.params}">
																<c:url var="eventTypeMatchHref" value="${listControls.searchAction}">
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
														<spring:message var="eventTypeTournamentLabel" code="${option.labelCode}" />
														<c:choose>
															<c:when test="${not empty option.params}">
																<c:url var="eventTypeTournamentHref" value="${listControls.searchAction}">
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
												ariaLabel="${eventTypeFilterTitle}"
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
									<spring:message var="groupTitleLabel" code="${group.titleCode}" />
									<div class="filter-dropdown" data-filter-name="${group.titleCode}">
										<button type="button" class="filter-dropdown__toggle">
											<span class="filter-dropdown__icon">
												<c:choose>
													<c:when test="${group.titleCode eq 'filter.category'}">
														<icon:category />
													</c:when>
													<c:when test="${group.titleCode eq 'filter.categories'}">
														<icon:football />
													</c:when>
													<c:when test="${group.titleCode eq 'host.filters.status'}">
														<icon:status />
													</c:when>
													<c:otherwise>
														<icon:status />
													</c:otherwise>
												</c:choose>
											</span>
											<c:out value="${groupTitleLabel}" />
										</button>
											<div class="filter-dropdown__panel">
												<c:set var="clearFilterHref" value="" />
												<c:forEach var="option" items="${group.options}" varStatus="optionStatus">
													<c:choose>
														<c:when test="${optionStatus.first}">
															<c:choose>
																<c:when test="${not empty option.params}">
																	<c:url var="clearFilterHref" value="${listControls.searchAction}">
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
																	<c:url var="optionHref" value="${listControls.searchAction}">
																		<c:forEach var="p" items="${option.params}">
																			<c:param name="${p.key}" value="${p.value}" />
																		</c:forEach>
																	</c:url>
																</c:when>
																<c:otherwise>
																	<c:url var="optionHref" value="${option.href}" />
																</c:otherwise>
															</c:choose>
															<a href="${optionHref}"
																class="filter-dropdown__item ${option.active ? 'filter-dropdown__item--active' : ''}">
																<spring:message code="${option.labelCode}" />
															</a>
														</c:otherwise>
													</c:choose>
												</c:forEach>
												<div class="filter-dropdown__actions">
													<c:url var="clearFilterUrl" value="${clearFilterHref}" />
													<ui:button label="${clearFilterLabel}" href="${clearFilterUrl}"
														variant="secondary" size="sm"
														className="filter-dropdown__action" />
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
																<spring:message code="${option.labelCode}" />
															</span>
														</c:if>
													</c:forEach>
												</div>
											</c:if>
										</div>
										</c:otherwise>
									</c:choose>
									</c:forEach>

								<c:if test="${selectedType ne 'tournament'}">
								<div class="filter-dropdown" data-filter-name="Date">
									<button type="button" class="filter-dropdown__toggle">
										<span class="filter-dropdown__icon">
											<icon:calendar />
										</span>
										<spring:message code="filter.date" />
									</button>
									<div class="filter-dropdown__panel">
										<c:url var="dateFormAction" value="${listControls.searchAction}" />
										<form method="get" action="${dateFormAction}"
											class="filter-dropdown__form">
											<input type="hidden" name="q"
												value="<c:out value='${listControls.searchQuery}' />" />
											<input type="hidden" name="sort"
												value="<c:out value='${searchForm.sort}' />" />
											<c:forEach var="selectedSport"
												items="${searchForm.sport}">
												<input type="hidden" name="sport"
													value="<c:out value='${selectedSport}' />" />
											</c:forEach>
											<c:forEach var="selectedStatus"
												items="${searchForm.status}">
												<input type="hidden" name="status"
													value="<c:out value='${selectedStatus}' />" />
											</c:forEach>
											<c:forEach var="selectedCategory"
												items="${searchForm.category}">
												<input type="hidden" name="category"
													value="<c:out value='${selectedCategory}' />" />
											</c:forEach>
											<c:if test="${searchForm.type.getValue() eq 'tournament'}">
												<input type="hidden" name="type" value="tournament" />
											</c:if>
											<c:forEach var="selectedVisibilityItem"
												items="${searchForm.visibility}">
												<input type="hidden" name="visibility"
													value="<c:out value='${selectedVisibilityItem}' />" />
											</c:forEach>
											<input type="hidden" name="minPrice"
												value="<c:out value='${searchForm.minPrice}' />" />
											<input type="hidden" name="maxPrice"
												value="<c:out value='${searchForm.maxPrice}' />" />
											<c:if test="${searchForm.filterName eq 'PAST'}">
												<input type="hidden" name="filter" value="${searchForm.filterName}" />
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

												<c:url var="clearDateHref" value="${listControls.searchAction}">
													<c:param name="q" value="${listControls.searchQuery}" />
													<c:param name="sort" value="${searchForm.sort}" />
													<c:forEach var="selectedSport" items="${searchForm.sport}">
														<c:param name="sport" value="${selectedSport}" />
													</c:forEach>
													<c:forEach var="selectedStatus" items="${searchForm.status}">
														<c:param name="status" value="${selectedStatus}" />
													</c:forEach>
													<c:forEach var="selectedCategory" items="${searchForm.category}">
														<c:param name="category" value="${selectedCategory}" />
													</c:forEach>
													<c:if test="${searchForm.type.getValue() eq 'tournament'}">
														<c:param name="type" value="tournament" />
													</c:if>
													<c:forEach var="selectedVisibilityItem" items="${searchForm.visibility}">
														<c:param name="visibility" value="${selectedVisibilityItem}" />
													</c:forEach>
													<c:param name="minPrice" value="${searchForm.minPrice}" />
													<c:param name="maxPrice" value="${searchForm.maxPrice}" />
													<c:if test="${searchForm.filterName eq 'PAST'}">
														<c:param name="filter" value="${searchForm.filterName}" />
													</c:if>
												</c:url>
												<spring:message var="applyDateLabel"
													code="filter.date.apply" text="Apply" />
												<div class="filter-dropdown__actions">
													<ui:button label="${clearFilterLabel}" href="${clearDateHref}"
														variant="secondary" size="sm"
														className="filter-dropdown__action" />
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
								</c:if>

								<div class="filter-dropdown" data-filter-name="Price">
									<button type="button" class="filter-dropdown__toggle">
										<span class="filter-dropdown__icon">
											<icon:banknote />
										</span>
										<spring:message code="filter.price" />
									</button>
									<div class="filter-dropdown__panel">
										<c:url var="priceFormAction" value="${listControls.searchAction}" />
										<form method="get" action="${priceFormAction}"
											class="filter-dropdown__form">
											<input type="hidden" name="q"
												value="<c:out value='${listControls.searchQuery}' />" />
											<input type="hidden" name="sort"
												value="<c:out value='${searchForm.sort}' />" />
											<c:forEach var="selectedSport"
												items="${searchForm.sport}">
												<input type="hidden" name="sport"
													value="<c:out value='${selectedSport}' />" />
											</c:forEach>
											<c:forEach var="selectedStatus"
												items="${searchForm.status}">
												<input type="hidden" name="status"
													value="<c:out value='${selectedStatus}' />" />
											</c:forEach>
											<c:forEach var="selectedCategory"
												items="${searchForm.category}">
												<input type="hidden" name="category"
													value="<c:out value='${selectedCategory}' />" />
											</c:forEach>
											<c:forEach var="selectedVisibilityItem"
												items="${searchForm.visibility}">
												<input type="hidden" name="visibility"
													value="<c:out value='${selectedVisibilityItem}' />" />
											</c:forEach>
											<c:if test="${searchForm.type.getValue() eq 'tournament'}">
												<input type="hidden" name="type" value="tournament" />
											</c:if>
											<c:if test="${searchForm.type.getValue() ne 'tournament'}">
												<input type="hidden" name="startDate"
													value="<c:out value='${searchForm.startDate}' />" />
												<input type="hidden" name="endDate"
													value="<c:out value='${searchForm.endDate}' />" />
											</c:if>
											<c:if test="${searchForm.filterName eq 'PAST'}">
												<input type="hidden" name="filter" value="${searchForm.filterName}" />
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
															data-price-from="true"
															value="<c:out value='${searchForm.minPrice}' />"
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
															data-price-to="true"
															data-price-range-error="${priceRangeError}"
															value="<c:out value='${searchForm.maxPrice}' />"
															placeholder="12" />
													</div>
												</div>

												<c:url var="clearPriceHref" value="${listControls.searchAction}">
													<c:param name="q" value="${listControls.searchQuery}" />
													<c:param name="sort" value="${searchForm.sort}" />
													<c:forEach var="selectedSport" items="${searchForm.sport}">
														<c:param name="sport" value="${selectedSport}" />
													</c:forEach>
													<c:forEach var="selectedStatus" items="${searchForm.status}">
														<c:param name="status" value="${selectedStatus}" />
													</c:forEach>
													<c:forEach var="selectedCategory" items="${searchForm.category}">
														<c:param name="category" value="${selectedCategory}" />
													</c:forEach>
													<c:if test="${searchForm.type.getValue() eq 'tournament'}">
														<c:param name="type" value="tournament" />
													</c:if>
													<c:forEach var="selectedVisibilityItem" items="${searchForm.visibility}">
														<c:param name="visibility" value="${selectedVisibilityItem}" />
													</c:forEach>
													<c:if test="${searchForm.type.getValue() ne 'tournament'}">
														<c:param name="startDate" value="${searchForm.startDate}" />
														<c:param name="endDate" value="${searchForm.endDate}" />
													</c:if>
													<c:param name="minPrice" value="" />
													<c:param name="maxPrice" value="" />
													<c:if test="${searchForm.filterName eq 'PAST'}">
														<c:param name="filter" value="${searchForm.filterName}" />
													</c:if>
												</c:url>
												<spring:message var="applyPriceLabel"
													code="filter.price.apply" />
												<div class="filter-dropdown__actions">
													<ui:button label="${clearFilterLabel}" href="${clearPriceHref}"
														variant="secondary" size="sm"
														className="filter-dropdown__action" />
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

								<c:url var="clearSearchHref" value="${listControls.cleanSearchAction}">
									<c:param name="type" value="${searchForm.type}" />
									<c:param name="filter" value="${searchForm.filterName}" />
								</c:url>
									<spring:message var="clearAllLabel" code="filter.clearAll" />
									<ui:button label="${clearAllLabel}" href="${clearSearchHref}"
										variant="primary" size="sm" className="filter-rail__clear" />

									<spring:message var="sortByLabel" code="feed.sortBy" />
									<ui:sortSelect
										id="events-sort-select"
										label="${sortByLabel}"
										ariaLabel="${sortAriaLabel}"
										options="${listControls.sortOptions}" />
								</div>
						</div>

					<!-- Matches -->

					<section class="matches-list-layout">
						<section class="matches-list-content">
							<c:choose>
								<c:when test="${empty events}">
									<spring:message var="emptyResultsMessage" code="feed.empty.message" />
									<div class="matches-empty-state">
										<div class="matches-empty-state__art" aria-hidden="true">
											<icon:emptyState />
										</div>
										<p class="matches-empty-state__copy">
											<c:out value="${emptyResultsMessage}" />
										</p>
									</div>
								</c:when>

								<c:otherwise>
									<div class="event-grid">
										<c:forEach var="event" items="${events}">
											<c:choose>
												<c:when test="${eventType.dbValue == 'tournament'}">
													<ui:eventCard
														tournament="${event}"
														badgeCode="${eventBadgeCodes[event.id]}"
														relationshipBadgeCodes="${eventRelationshipBadgeCodes[event.id]}"
														headingLevel="h2" />
												</c:when>
												<c:otherwise>
													<ui:eventCard
														match="${event}"
														badgeCode="${eventBadgeCodes[event.id]}"
														relationshipBadgeCodes="${eventRelationshipBadgeCodes[event.id]}"
														headingLevel="h2" />
												</c:otherwise>
											</c:choose>
										</c:forEach>
									</div>
								</c:otherwise>

							</c:choose>

							<ui:pagination formId="eventsSearchForm" pageNumber="${pageNumber}" pageHasPrevious="${pageHasPrevious}" pageHasNext="${pageHasNext}" paginationItems="${paginationItems}" listTitle="${listTitle}" />
						</section>
					</section>
					</c:if>




				</main>
		</div>
	</body>
</html>
