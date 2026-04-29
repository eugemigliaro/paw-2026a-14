<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>

<c:set var="resolvedPageTitleCode" value="${empty pageTitleCode ? 'app.brand' : pageTitleCode}" />
<spring:message var="pageTitle" code="${resolvedPageTitleCode}" />

<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
		<style>
			.events-header-container {
				display: flex;
				align-items: center;
				gap: 24px;
				margin-bottom: 24px;
			}

			.events-header-title {
				flex-shrink: 0;
			}

			.events-header-search {
				flex: 1;
				min-width: 200px;
			}

			.events-header-search .search-panel__form {
				display: flex;
				gap: 8px;
				align-items: center;
			}

			.events-header-search .search-panel__row {
				display: flex;
				gap: 8px;
				flex: 1;
			}

			.events-header-search .search-panel__input {
				flex: 1;
			}

			.events-header-search .search-panel__control {
				font-size: 13px;
				padding: 6px 10px;
				height: 36px;
			}

			.events-toggle-wrapper {
				position: relative;
				display: inline-flex;
				background: #e5e7eb;
				border-radius: 999px;
				padding: 4px;
				width: 200px;
				flex-shrink: 0;
			}

			.events-toggle-slider {
				position: absolute;
				top: 4px;
				left: 4px;
				width: 50%;
				height: calc(100% - 8px);
				background: white;
				border-radius: 999px;
				transition: transform 0.3s ease;
				box-shadow: 0 2px 6px rgba(0,0,0,0.15);
			}

			.events-toggle-slider.right {
				transform: translateX(100%);
			}

			.events-toggle-btn {
				flex: 1;
				z-index: 1;
				border: none;
				background: transparent;
				cursor: pointer;
				padding: 8px 0;
				font-weight: 400;
				border-radius: 999px;
				font-size: 13px;
				font-family: inherit;
			}

			.events-toggle-btn:hover {
				color: #333;
			}
		</style>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<spring:message var="filtersAriaLabel" code="feed.aria.filters" />
			<spring:message var="searchAriaLabel" code="feed.aria.search" />
			<spring:message var="sortAriaLabel" code="feed.aria.sort" />

			<main class="page-shell page-shell--matches-list">

				<!-- Header with Title, Search, and Toggle in one row -->
				<div class="events-header-container">
					<h1 class="page-heading__title events-header-title"><c:out value="${listTitle}" /></h1>

					<!-- Search Bar -->
					<div class="events-header-search" aria-label="${searchAriaLabel}">
						<form:form
							method="get"
							action="${listControls.searchAction}"
							modelAttribute="listSearchForm"
							cssClass="search-panel__form">
							<c:forEach var="selectedSport" items="${selectedSports}">
								<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
							</c:forEach>
							<c:forEach var="selectedStatus" items="${selectedStatuses}">
								<input type="hidden" name="status" value="<c:out value='${selectedStatus}' />" />
							</c:forEach>
							<c:forEach var="selectedVisibilityItem" items="${selectedVisibility}">
								<input type="hidden" name="visibility" value="<c:out value='${selectedVisibilityItem}' />" />
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
							<c:if test="${param.filter eq 'past'}">
								<input type="hidden" name="filter" value="past" />
							</c:if>
							<div class="search-panel__row">
								<div class="search-panel__input">
									<span class="search-panel__icon" aria-hidden="true"></span>
									<form:input
										path="q"
										cssClass="field__control search-panel__control"
										placeholder="${listControls.searchPlaceholder}" />
								</div>
								<ui:button label="${listControls.searchButtonLabel}" type="submit" />
							</div>
							<form:errors path="q" cssClass="search-panel__error" element="p" />
						</form:form>
					</div>

					<!-- Toggle -->
					<div class="events-toggle-wrapper" id="eventsToggle">
						<div class="events-toggle-slider" id="eventsSlider"></div>
						<button class="events-toggle-btn" data-value="upcoming">Upcoming</button>
						<button class="events-toggle-btn" data-value="past">Past</button>
					</div>
				</div>

				<section class="matches-search-sort-panel">
					<c:if test="${not empty listControls.sortOptions}">
						<form method="get" action="${listControls.searchAction}" class="sort-panel" aria-label="${sortAriaLabel}">
							<label class="field sort-panel__field" for="sort-select">
								<span class="field__label"><c:out value="${listControls.sortLabel}" /></span>
								<select
									id="sort-select"
									name="sort"
									class="field__control field__control--select sort-panel__select"
									onchange="if(this.value){window.location.href=this.value;}">
									<c:forEach var="option" items="${listControls.sortOptions}">
										<c:url var="optionHref" value="${option.href}" />
										<option value="${optionHref}" ${option.selected ? 'selected="selected"' : ''}>
											<c:out value="${option.label}" />
										</option>
									</c:forEach>
								</select>
							</label>
						</form>
					</c:if>
				</section>

				<c:if test="${not empty listControls}">
					<section class="matches-list-layout">
						<aside class="matches-list-sidebar" aria-label="${filtersAriaLabel}">
							<div class="panel filter-rail">
								<div class="filter-rail__header">
									<div class="filter-rail__heading">
										<h2 class="filter-rail__main-title">
											<c:out value="${listControls.filterTitle}" />
										</h2>
									</div>
									<c:url var="clearSearchHref" value="/events">
										<c:param name="q" value="${listControls.searchQuery}" />
										<c:param name="sort" value="${selectedSort}" />
										<c:param name="tz" value="${selectedTimezone}" />
										<c:param name="startDate" value="${selectedStartDateValue}" />
										<c:param name="endDate" value="${selectedEndDateValue}" />
										<c:param name="minPrice" value="${selectedMinPriceValue}" />
										<c:param name="maxPrice" value="${selectedMaxPriceValue}" />
										<c:if test="${param.filter eq 'past'}">
											<c:param name="filter" value="past" />
										</c:if>
									</c:url>
									<spring:message var="clearAllLabel" code="filter.clearAll" />
									<ui:button
										label="${clearAllLabel}"
										href="${clearSearchHref}"
										variant="primary"
										size="sm"
										className="filter-rail__clear"/>
								</div>
								<c:forEach var="group" items="${listControls.filterGroups}">
									<section class="filter-rail__group">
										<div class="filter-rail__group-header">
											<h2 class="filter-rail__title">
												<c:out value="${group.title}" />
											</h2>
										</div>

										<div class="filter-rail__options">
											<c:forEach var="option" items="${group.options}">
												<c:url var="optionHref" value="${option.href}" />
												<ui:chip
													label="${option.label}"
													href="${optionHref}"
													active="${option.active}"
													tone="default"
													className="filter-rail__chip"/>
											</c:forEach>
										</div>
									</section>
								</c:forEach>
								<section class="filter-rail__group">
									<div class="filter-rail__group-header">
										<h2 class="filter-rail__title"><spring:message code="filter.datePrice" /></h2>
									</div>
									<form method="get" action="${listControls.searchAction}" class="filter-rail__form">
										<input type="hidden" name="q" value="<c:out value='${listControls.searchQuery}' />" />
										<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
										<input type="hidden" name="tz" value="<c:out value='${selectedTimezone}' />" data-browser-timezone-field="true" />
										<c:forEach var="selectedSport" items="${selectedSports}">
											<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
										</c:forEach>
										<c:forEach var="selectedStatus" items="${selectedStatuses}">
											<input type="hidden" name="status" value="<c:out value='${selectedStatus}' />" />
										</c:forEach>
										<c:forEach var="selectedVisibilityItem" items="${selectedVisibility}">
											<input type="hidden" name="visibility" value="<c:out value='${selectedVisibilityItem}' />" />
										</c:forEach>
										<c:if test="${param.filter eq 'past'}">
											<input type="hidden" name="filter" value="past" />
										</c:if>
										<div class="filter-rail__field-group">
											<div class="field filter-rail__field">
												<label class="field__label" for="list-start-date"><spring:message code="filter.date.from" /></label>
												<input id="list-start-date" name="startDate" type="date" class="field__control" min="<c:out value='${selectedDateMinValue}' />" max="<c:out value='${selectedDateMaxValue}' />" value="<c:out value='${selectedStartDateValue}' />" />
											</div>
											<div class="field filter-rail__field">
												<label class="field__label" for="list-end-date"><spring:message code="filter.date.to" /></label>
												<input id="list-end-date" name="endDate" type="date" class="field__control" min="<c:out value='${selectedDateMinValue}' />" max="<c:out value='${selectedDateMaxValue}' />" value="<c:out value='${selectedEndDateValue}' />" />
											</div>
										</div>
										<div class="filter-rail__field-group">
											<div class="field filter-rail__field filter-rail__price-field">
												<label class="field__label" for="list-min-price"><spring:message code="filter.price.from" /></label>
												<div class="filter-rail__price-input-wrap">
													<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
													<input id="list-min-price" name="minPrice" type="number" min="0" step="0.01" inputmode="decimal" class="field__control filter-rail__price-input" value="<c:out value='${selectedMinPriceValue}' />" placeholder="0" />
												</div>
											</div>
											<div class="field filter-rail__field filter-rail__price-field">
												<label class="field__label" for="list-max-price"><spring:message code="filter.price.to" /></label>
												<div class="filter-rail__price-input-wrap">
													<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
													<input id="list-max-price" name="maxPrice" type="number" min="0" step="0.01" inputmode="decimal" class="field__control filter-rail__price-input" value="<c:out value='${selectedMaxPriceValue}' />" placeholder="12" />
												</div>
											</div>
											<p class="filter-rail__caption"><spring:message code="filter.price.perPlayer" /></p>
										</div>
										<spring:message var="applyPriceLabel" code="filter.price.apply" />
										<ui:button label="${applyPriceLabel}" type="submit" fullWidth="${true}" className="filter-rail__submit" />
									</form>
								</section>
							</div>
						</aside>

						<section class="matches-list-content">
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
															decoding="async"/>
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
								<spring:message var="previousLabel" code="pagination.previous" />
								<spring:message var="nextLabel" code="pagination.next" />
								<section class="feed-pagination" aria-label="${listTitle}">
									<nav class="feed-pagination__nav" aria-label="${listTitle}">

										<c:choose>
											<c:when test="${not empty previousPageHref}">
												<c:url var="prevHref" value="${previousPageHref}" />
												<a class="feed-pagination__control" href="${prevHref}">
													${previousLabel}
												</a>
											</c:when>
											<c:otherwise>
												<span class="feed-pagination__control feed-pagination__control--disabled">
													${previousLabel}
												</span>
											</c:otherwise>
										</c:choose>

										<div class="feed-pagination__pages">
											<c:forEach var="item" items="${paginationItems}">
												<c:choose>
													<c:when test="${item.ellipsis}">
														<span class="feed-pagination__ellipsis">${item.label}</span>
													</c:when>
													<c:when test="${item.current}">
														<span class="feed-pagination__page feed-pagination__page--current" aria-current="page">
															${item.label}
														</span>
													</c:when>
													<c:otherwise>
														<c:url var="pageHref" value="${item.href}" />
														<a class="feed-pagination__page" href="${pageHref}">
															${item.label}
														</a>
													</c:otherwise>
												</c:choose>
											</c:forEach>
										</div>

										<c:choose>
											<c:when test="${not empty nextPageHref}">
												<c:url var="nextHref" value="${nextPageHref}" />
												<a class="feed-pagination__control" href="${nextHref}">
													${nextLabel}
												</a>
											</c:when>
											<c:otherwise>
												<span class="feed-pagination__control feed-pagination__control--disabled">
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

		// Set initial slider position based on current filter parameter
		const urlParams = new URLSearchParams(window.location.search);
		const currentFilter = urlParams.get("filter") || "upcoming";
		if (currentFilter === "past") {
			document.getElementById("eventsSlider").classList.add("right");
		}
	</script>
</html>
