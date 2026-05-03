<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="options" required="true" rtexprvalue="true" type="java.util.List" %>

<c:set var="resolvedId" value="${empty id ? 'sort-select' : id}" />

<c:if test="${not empty options}">
	<form class="sort-panel" aria-label="${ariaLabel}">
		<label class="field sort-panel__field" for="${resolvedId}">
			<span class="field__label"><c:out value="${label}" /></span>
			<select
				id="${resolvedId}"
				class="field__control field__control--select sort-panel__select"
				data-browser-timezone-url-options="true"
				onchange="if(this.value){window.location.href=this.value;}">
			<c:forEach var="option" items="${options}">
				<c:url var="optionHref" value="${option.href}" />
				<c:choose>
					<c:when test="${option.selected}">
						<option value="${optionHref}" selected="selected">
							<c:out value="${option.label}" />
						</option>
					</c:when>
					<c:otherwise>
						<option value="${optionHref}">
							<c:out value="${option.label}" />
						</option>
					</c:otherwise>
				</c:choose>
			</c:forEach>
			</select>
		</label>
	</form>
</c:if>
