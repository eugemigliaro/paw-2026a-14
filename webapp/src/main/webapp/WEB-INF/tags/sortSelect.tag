<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
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
		<div class="sort-panel" aria-label="<c:out value='${ariaLabel}' />" data-sort-select="true">
		<div class="field sort-panel__field">
				<span class="field__label" id="<c:out value='${resolvedId}' />-label"><c:out value="${label}" /></span>
			<div
				class="filter-dropdown sort-panel__dropdown"
					data-filter-name="<c:out value='${resolvedId}' />"
				data-close-on-select="true">
				<button
					type="button"
					class="filter-dropdown__toggle sort-panel__toggle"
						id="<c:out value='${resolvedId}' />"
					aria-expanded="false"
						aria-labelledby="<c:out value='${resolvedId}' />-label <c:out value='${resolvedId}' />">
					<span class="filter-dropdown__icon sort-panel__icon" aria-hidden="true">
						<icon:invertedPyramid />
					</span>
					<span class="sort-panel__toggle-label"><c:out value="${selectedLabel}" /></span>
				</button>
					<div class="filter-dropdown__panel sort-panel__panel" aria-labelledby="<c:out value='${resolvedId}' />-label">
					<c:forEach var="option" items="${options}">
						<c:choose>
							<c:when test="${not empty option.href}">
								<c:url var="optionHref" value="${option.href}" />
							</c:when>
							<c:otherwise>
								<c:url var="optionHref" value="/">
									<c:forEach var="p" items="${option.params}">
										<c:param name="${p.key}" value="${p.value}" />
									</c:forEach>
								</c:url>
							</c:otherwise>
						</c:choose>
						<a
							href="${optionHref}"
							class="filter-dropdown__item sort-panel__item ${option.selected ? 'filter-dropdown__item--active sort-panel__item--active' : ''}"
							aria-current="${option.selected ? 'true' : 'false'}">
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
