<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="options" required="true" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="hint" required="false" rtexprvalue="true" %>

<c:set var="resolvedId" value="${empty id ? 'sort-select' : id}" />

<c:if test="${not empty options}">
	<c:set var="selectedLabel" value="" />
	<c:forEach var="option" items="${options}" varStatus="status">
		<c:if test="${option.selected or (empty selectedLabel and status.first)}">
			<c:set var="selectedLabel" value="${option.label}" />
		</c:if>
	</c:forEach>
	<div class="sort-panel" aria-label="${ariaLabel}" data-sort-select="true">
		<div class="field sort-panel__field">
			<span class="field__label" id="${resolvedId}-label"><c:out value="${label}" /></span>
			<div
				class="filter-dropdown sort-panel__dropdown"
				data-filter-name="${resolvedId}"
				data-close-on-select="true">
				<button
					type="button"
					class="filter-dropdown__toggle sort-panel__toggle"
					id="${resolvedId}"
					aria-expanded="false"
					aria-labelledby="${resolvedId}-label ${resolvedId}">
					<span class="filter-dropdown__icon sort-panel__icon" aria-hidden="true">
						<svg viewBox="0 0 24 24">
							<path d="M3 7h18" />
							<path d="M6 12h12" />
							<path d="M10 17h4" />
						</svg>
					</span>
					<span class="sort-panel__toggle-label"><c:out value="${selectedLabel}" /></span>
				</button>
				<div class="filter-dropdown__panel sort-panel__panel" aria-labelledby="${resolvedId}-label">
					<c:forEach var="option" items="${options}">
						<c:url var="optionHref" value="${option.href}" />
						<a
							href="${optionHref}"
							class="filter-dropdown__item sort-panel__item ${option.selected ? 'filter-dropdown__item--active sort-panel__item--active' : ''}"
							aria-current="${option.selected ? 'true' : 'false'}"
							data-browser-timezone-url-link="true">
							<c:out value="${option.label}" />
						</a>
					</c:forEach>
				</div>
				<c:if test="${not empty hint}">
					<span class="field__hint sort-panel__hint"><c:out value="${hint}" /></span>
				</c:if>
			</div>
		</div>
	</div>
</c:if>
