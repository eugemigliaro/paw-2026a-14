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
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<spring:message var="filtersAriaLabel" code="feed.aria.filters" />
			<spring:message var="searchAriaLabel" code="feed.aria.search" />
			<spring:message var="sortAriaLabel" code="feed.aria.sort" />

			<main class="page-shell page-shell--matches-list">

				<header class="page-heading">
					<h1 class="page-heading__title"><c:out value="${listTitle}" /></h1>
					<p class="page-heading__description"><c:out value="${listDescription}" /></p>
				</header>

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

					<section class="search-panel matches-search-panel" aria-label="${searchAriaLabel}">
						<form:form
							method="get"
							action="${listControls.searchAction}"
							modelAttribute="listSearchForm"
							cssClass="search-panel__form">
							<input type="hidden" name="email" value="<c:out value='${email}' />" />
							<c:forEach var="selectedSport" items="${selectedSports}">
								<input type="hidden" name="sport" value="<c:out value='${selectedSport}' />" />
							</c:forEach>
							<input type="hidden" name="time" value="<c:out value='${selectedTime}' />" />
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
										placeholder="${listControls.searchPlaceholder}" />
								</div>
								<ui:button label="${listControls.searchButtonLabel}" type="submit" />
							</div>
							<form:errors path="q" cssClass="search-panel__error" element="p" />
						</form:form>
					</section>
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
									<c:url var="clearSearchHref" value="${listControls.searchAction}">
										<c:param name="q" value="${listControls.searchQuery}" />
										<c:param name="sort" value="${selectedSort}" />
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
</html>
