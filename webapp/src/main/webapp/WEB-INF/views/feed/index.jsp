<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | Explore" />
<!DOCTYPE html>
<html lang="en">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--feed">
				<aside class="feed-sidebar" aria-label="Match filters">
					<div class="panel filter-rail">
						<div class="filter-rail__header">
							<h2 class="filter-rail__main-title">Filters</h2>
							<c:url var="clearFiltersHref" value="/">
								<c:param name="q" value="${param.q}" />
								<c:param name="sort" value="${selectedSort}" />
							</c:url>
							<ui:button
								label="Clear all"
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
						<section class="filter-rail__group">
							<h2 class="filter-rail__title">Price</h2>
							<form
								method="get"
								action="${pageContext.request.contextPath}/"
								class="filter-rail__form">
								<input type="hidden" name="q" value="<c:out value='${feedSearchForm.q}' />" />
								<c:forEach var="selectedSport" items="${selectedSports}">
									<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
								</c:forEach>
								<input type="hidden" name="time" value="<c:out value='${selectedTime}' />" />
								<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
								<input type="hidden" name="tz" value="<c:out value='${selectedTimezone}' />" />
								<div class="filter-rail__field-group">
									<div class="filter-rail__field-row">
										<div class="field filter-rail__field filter-rail__price-field">
											<label class="field__label" for="min-price">From</label>
											<div class="filter-rail__price-input-wrap">
												<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
												<input
													id="min-price"
													name="minPrice"
													type="number"
													min="0"
													step="0.01"
													inputmode="decimal"
													class="field__control filter-rail__price-input"
													value="<c:out value='${selectedMinPriceValue}' />"
													placeholder="0" />
											</div>
										</div>
										<div class="field filter-rail__field filter-rail__price-field">
											<label class="field__label" for="max-price">To</label>
											<div class="filter-rail__price-input-wrap">
												<span class="filter-rail__price-prefix" aria-hidden="true">$</span>
												<input
													id="max-price"
													name="maxPrice"
													type="number"
													min="0"
													step="0.01"
													inputmode="decimal"
													class="field__control filter-rail__price-input"
													value="<c:out value='${selectedMaxPriceValue}' />"
													placeholder="12" />
											</div>
										</div>
									</div>
								</div>
								<ui:button
									label="Apply price filters"
									type="submit"
									fullWidth="${true}"
									className="filter-rail__submit" />
							</form>
						</section>
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

						<section class="search-panel" aria-label="Search matches">
							<form:form
								method="get"
								action="${pageContext.request.contextPath}/"
								modelAttribute="feedSearchForm"
								cssClass="search-panel__form">
								<c:forEach var="selectedSport" items="${selectedSports}">
									<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
								</c:forEach>
								<input type="hidden" name="time" value="<c:out value='${selectedTime}' />" />
								<input type="hidden" name="sort" value="<c:out value='${selectedSort}' />" />
								<input type="hidden" name="tz" value="<c:out value='${selectedTimezone}' />" />
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

						<form method="get" action="${pageContext.request.contextPath}/" class="sort-panel" aria-label="Sort matches">
							<input type="hidden" name="q" value="<c:out value='${feedSearchForm.q}' />" />
							<c:forEach var="selectedSport" items="${selectedSports}">
								<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
							</c:forEach>
							<input type="hidden" name="time" value="<c:out value='${selectedTime}' />" />
							<input type="hidden" name="tz" value="<c:out value='${selectedTimezone}' />" />
							<input type="hidden" name="minPrice" value="<c:out value='${selectedMinPriceValue}' />" />
							<input type="hidden" name="maxPrice" value="<c:out value='${selectedMaxPriceValue}' />" />
							<input type="hidden" name="page" value="1" />
							<label class="field sort-panel__field" for="sort-select">
								<span class="field__label">Sort by</span>
								<select
									id="sort-select"
									name="sort"
									class="field__control field__control--select sort-panel__select"
									onchange="this.form.submit()">
									<option value="soonest" ${selectedSort == 'soonest' ? 'selected="selected"' : ''}>Soonest</option>
									<option value="price" ${selectedSort == 'price' ? 'selected="selected"' : ''}>Price: Low to high</option>
									<option value="spots" ${selectedSort == 'spots' ? 'selected="selected"' : ''}>Most spots left</option>
								</select>
							</label>
						</form>
					</section>

					<section>
						<div class="section-head section-head--feed-list">
							<div>
								<h2 class="section-head__title section-head__title--feed-list">All matches</h2>
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

						<c:if test="${feedPage.totalPages > 1}">
							<section class="feed-pagination" aria-label="Pagination">
								<nav class="feed-pagination__nav" aria-label="Feed pages">
									<c:choose>
										<c:when test="${not empty feedPage.previousPageHref}">
											<a
												class="feed-pagination__control"
												href="${pageContext.request.contextPath}${feedPage.previousPageHref}">
												Previous
											</a>
										</c:when>
										<c:otherwise>
											<span class="feed-pagination__control feed-pagination__control--disabled">Previous</span>
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
													<a
														class="feed-pagination__page"
														href="${pageContext.request.contextPath}${item.href}">
														${item.label}
													</a>
												</c:otherwise>
											</c:choose>
										</c:forEach>
									</div>

									<c:choose>
										<c:when test="${not empty feedPage.nextPageHref}">
											<a
												class="feed-pagination__control"
												href="${pageContext.request.contextPath}${feedPage.nextPageHref}">
												Next
											</a>
										</c:when>
										<c:otherwise>
											<span class="feed-pagination__control feed-pagination__control--disabled">Next</span>
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
