<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="currentValue" required="false" rtexprvalue="true" %>
<%@ attribute name="currentFilter" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="inputName" required="false" rtexprvalue="true" %>
<%@ attribute name="leftValue" required="false" rtexprvalue="true" %>
<%@ attribute name="rightValue" required="false" rtexprvalue="true" %>
<%@ attribute name="leftLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="rightLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="forceLeftOnEmpty" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="upcomingValue" required="false" rtexprvalue="true" %>
<%@ attribute name="pastValue" required="false" rtexprvalue="true" %>

<spring:message var="defaultLeftLabel" code="filter.upcoming" text="Upcoming" />
<spring:message var="defaultRightLabel" code="filter.past" text="Past" />

<c:set var="resolvedLeftValue" value="${empty leftValue ? (empty upcomingValue ? 'upcoming' : upcomingValue) : leftValue}" />
<c:set var="resolvedRightValue" value="${empty rightValue ? (empty pastValue ? 'past' : pastValue) : rightValue}" />
<c:set var="resolvedCurrentValue" value="${empty currentValue ? currentFilter : currentValue}" />
<c:set var="resolvedLeftLabel" value="${empty leftLabel ? defaultLeftLabel : leftLabel}" />
<c:set var="resolvedRightLabel" value="${empty rightLabel ? defaultRightLabel : rightLabel}" />
<c:set var="resolvedForceLeftOnEmpty" value="${forceLeftOnEmpty == null ? true : forceLeftOnEmpty}" />

<c:set var="isRightSelected" value="${resolvedCurrentValue eq resolvedRightValue}" />
<c:set var="isLeftSelected" value="${resolvedCurrentValue eq resolvedLeftValue or (resolvedForceLeftOnEmpty and empty resolvedCurrentValue and not isRightSelected)}" />

<c:set var="finalInputValue" value="${resolvedCurrentValue}" />
<c:if test="${empty resolvedCurrentValue and isLeftSelected}">
    <c:set var="finalInputValue" value="${resolvedLeftValue}" />
</c:if>

<c:set var="wrapperClasses" value="events-toggle-wrapper" />
<c:if test="${not empty className}">
    <c:set var="wrapperClasses" value="${wrapperClasses} ${className}" />
</c:if>

<div class="${wrapperClasses}" data-events-toggle="true" data-events-toggle-right-value="${resolvedRightValue}"
    <c:if test="${not empty id}">id="${id}"</c:if>>
    <c:if test="${not empty inputName}">
        <input type="hidden" name="${inputName}" value="${finalInputValue}" data-events-toggle-input="true" />
    </c:if>
    <div class="events-toggle-slider ${isRightSelected ? 'right' : ''}" data-events-toggle-slider="true"></div>
    <button type="button" class="events-toggle-btn ${isLeftSelected ? 'active' : ''}" data-value="${resolvedLeftValue}">
        <c:out value="${resolvedLeftLabel}" />
    </button>
    <button type="button" class="events-toggle-btn ${isRightSelected ? 'active' : ''}" data-value="${resolvedRightValue}">
        <c:out value="${resolvedRightLabel}" />
    </button>
</div>
