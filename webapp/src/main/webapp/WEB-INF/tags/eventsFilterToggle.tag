<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<%@ attribute name="currentValue" required="false" rtexprvalue="true" %>
<%@ attribute name="currentFilter" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="inputName" required="false" rtexprvalue="true" %>
<%@ attribute name="leftValue" required="false" rtexprvalue="true" %>
<%@ attribute name="rightValue" required="false" rtexprvalue="true" %>
<%@ attribute name="thirdValue" required="false" rtexprvalue="true" %>
<%@ attribute name="leftLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="rightLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="thirdLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="leftHref" required="false" rtexprvalue="true" %>
<%@ attribute name="rightHref" required="false" rtexprvalue="true" %>
<%@ attribute name="thirdHref" required="false" rtexprvalue="true" %>
<%@ attribute name="forceLeftOnEmpty" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="iconOnly" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="leftIcon" required="false" rtexprvalue="true" %>
<%@ attribute name="rightIcon" required="false" rtexprvalue="true" %>
<%@ attribute name="thirdIcon" required="false" rtexprvalue="true" %>
<%@ attribute name="upcomingValue" required="false" rtexprvalue="true" %>
<%@ attribute name="pastValue" required="false" rtexprvalue="true" %>

<spring:message var="defaultLeftLabel" code="filter.upcoming" text="Upcoming" />
<spring:message var="defaultRightLabel" code="filter.past" text="Past" />

<c:set var="resolvedLeftValue" value="${empty leftValue ? (empty upcomingValue ? 'upcoming' : upcomingValue) : leftValue}" />
<c:set var="resolvedRightValue" value="${empty rightValue ? (empty pastValue ? 'past' : pastValue) : rightValue}" />
<c:set var="resolvedThirdValue" value="${thirdValue}" />
<c:set var="resolvedCurrentValue" value="${empty currentValue ? currentFilter : currentValue}" />
<c:set var="resolvedLeftLabel" value="${empty leftLabel ? defaultLeftLabel : leftLabel}" />
<c:set var="resolvedRightLabel" value="${empty rightLabel ? defaultRightLabel : rightLabel}" />
<c:set var="resolvedThirdLabel" value="${thirdLabel}" />
<c:set var="resolvedForceLeftOnEmpty" value="${forceLeftOnEmpty == null ? true : forceLeftOnEmpty}" />
<c:set var="resolvedIconOnly" value="${iconOnly == null ? false : iconOnly}" />
<c:set var="hasThirdOption" value="${not empty resolvedThirdValue and not empty resolvedThirdLabel}" />

<c:set var="isRightSelected" value="${resolvedCurrentValue eq resolvedRightValue}" />
<c:set var="isThirdSelected" value="${hasThirdOption and resolvedCurrentValue eq resolvedThirdValue}" />
<c:set var="isLeftSelected" value="${resolvedCurrentValue eq resolvedLeftValue or (resolvedForceLeftOnEmpty and empty resolvedCurrentValue and not isRightSelected and not isThirdSelected)}" />
<c:set var="selectedIndex" value="0" />
<c:if test="${isRightSelected}">
    <c:set var="selectedIndex" value="1" />
</c:if>
<c:if test="${isThirdSelected}">
    <c:set var="selectedIndex" value="2" />
</c:if>

<c:set var="finalInputValue" value="${resolvedCurrentValue}" />
<c:if test="${empty resolvedCurrentValue and isLeftSelected}">
    <c:set var="finalInputValue" value="${resolvedLeftValue}" />
</c:if>

<c:set var="wrapperClasses" value="events-toggle-wrapper" />
<c:if test="${not empty className}">
    <c:set var="wrapperClasses" value="${wrapperClasses} ${className}" />
</c:if>

<div class="${wrapperClasses}" data-events-toggle="true" data-events-toggle-right-value="${resolvedRightValue}"
    data-events-toggle-options="${hasThirdOption ? '3' : '2'}" style="--events-toggle-index: ${selectedIndex};"
    <c:if test="${not empty ariaLabel}">aria-label="${ariaLabel}"</c:if>
    <c:if test="${not empty id}">id="${id}"</c:if>>
    <c:if test="${not empty inputName}">
        <input type="hidden" name="${inputName}" value="${finalInputValue}" data-events-toggle-input="true" />
    </c:if>
    <div class="events-toggle-slider ${isRightSelected ? 'right' : ''}" data-events-toggle-slider="true"></div>
    <c:choose>
        <c:when test="${not empty leftHref}">
            <c:url var="resolvedLeftHref" value="${leftHref}" />
            <a href="${resolvedLeftHref}" class="events-toggle-btn ${isLeftSelected ? 'active' : ''}" data-value="${resolvedLeftValue}" aria-current="${isLeftSelected ? 'true' : 'false'}"
                <c:if test="${resolvedIconOnly}">aria-label="${resolvedLeftLabel}" title="${resolvedLeftLabel}"</c:if>>
                <c:choose>
                    <c:when test="${resolvedIconOnly}">
                        <span class="events-toggle-icon" aria-hidden="true">
                            <c:set var="resolvedIconName" value="${leftIcon}" />
                            <c:choose>
                                <c:when test="${resolvedIconName eq 'trophy'}">
                                    <icon:trophy />
                                </c:when>
                                <c:otherwise>
                                    <icon:football />
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${resolvedLeftLabel}" />
                    </c:otherwise>
                </c:choose>
            </a>
        </c:when>
        <c:otherwise>
            <button type="button" class="events-toggle-btn ${isLeftSelected ? 'active' : ''}" data-value="${resolvedLeftValue}"
                <c:if test="${resolvedIconOnly}">aria-label="${resolvedLeftLabel}" title="${resolvedLeftLabel}"</c:if>>
                <c:choose>
                    <c:when test="${resolvedIconOnly}">
                        <span class="events-toggle-icon" aria-hidden="true">
                            <c:set var="resolvedIconName" value="${leftIcon}" />
                            <c:choose>
                                <c:when test="${resolvedIconName eq 'trophy'}">
                                    <icon:trophy />
                                </c:when>
                                <c:otherwise>
                                    <icon:football />
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${resolvedLeftLabel}" />
                    </c:otherwise>
                </c:choose>
            </button>
        </c:otherwise>
    </c:choose>
    <c:choose>
        <c:when test="${not empty rightHref}">
            <c:url var="resolvedRightHref" value="${rightHref}" />
            <a href="${resolvedRightHref}" class="events-toggle-btn ${isRightSelected ? 'active' : ''}" data-value="${resolvedRightValue}" aria-current="${isRightSelected ? 'true' : 'false'}"
                <c:if test="${resolvedIconOnly}">aria-label="${resolvedRightLabel}" title="${resolvedRightLabel}"</c:if>>
                <c:choose>
                    <c:when test="${resolvedIconOnly}">
                        <span class="events-toggle-icon" aria-hidden="true">
                            <c:set var="resolvedIconName" value="${rightIcon}" />
                            <c:choose>
                                <c:when test="${resolvedIconName eq 'trophy'}">
                                    <icon:trophy />
                                </c:when>
                                <c:otherwise>
                                    <icon:football />
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${resolvedRightLabel}" />
                    </c:otherwise>
                </c:choose>
            </a>
        </c:when>
        <c:otherwise>
            <button type="button" class="events-toggle-btn ${isRightSelected ? 'active' : ''}" data-value="${resolvedRightValue}"
                <c:if test="${resolvedIconOnly}">aria-label="${resolvedRightLabel}" title="${resolvedRightLabel}"</c:if>>
                <c:choose>
                    <c:when test="${resolvedIconOnly}">
                        <span class="events-toggle-icon" aria-hidden="true">
                            <c:set var="resolvedIconName" value="${rightIcon}" />
                            <c:choose>
                                <c:when test="${resolvedIconName eq 'trophy'}">
                                    <icon:trophy />
                                </c:when>
                                <c:otherwise>
                                    <icon:football />
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${resolvedRightLabel}" />
                    </c:otherwise>
                </c:choose>
            </button>
        </c:otherwise>
    </c:choose>
    <c:if test="${hasThirdOption}">
        <c:choose>
            <c:when test="${not empty thirdHref}">
                <c:url var="resolvedThirdHref" value="${thirdHref}" />
                <a href="${resolvedThirdHref}" class="events-toggle-btn ${isThirdSelected ? 'active' : ''}" data-value="${resolvedThirdValue}" aria-current="${isThirdSelected ? 'true' : 'false'}"
                    <c:if test="${resolvedIconOnly}">aria-label="${resolvedThirdLabel}" title="${resolvedThirdLabel}"</c:if>>
                    <c:choose>
                        <c:when test="${resolvedIconOnly}">
                            <span class="events-toggle-icon" aria-hidden="true">
                                <c:set var="resolvedIconName" value="${thirdIcon}" />
                                <c:choose>
                                <c:when test="${resolvedIconName eq 'trophy'}">
                                    <icon:trophy />
                                </c:when>
                                <c:otherwise>
                                    <icon:football />
                                </c:otherwise>
                            </c:choose>
                            </span>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${resolvedThirdLabel}" />
                        </c:otherwise>
                    </c:choose>
                </a>
            </c:when>
            <c:otherwise>
                <button type="button" class="events-toggle-btn ${isThirdSelected ? 'active' : ''}" data-value="${resolvedThirdValue}"
                    <c:if test="${resolvedIconOnly}">aria-label="${resolvedThirdLabel}" title="${resolvedThirdLabel}"</c:if>>
                    <c:choose>
                        <c:when test="${resolvedIconOnly}">
                            <span class="events-toggle-icon" aria-hidden="true">
                                <c:set var="resolvedIconName" value="${thirdIcon}" />
                                <c:choose>
                                <c:when test="${resolvedIconName eq 'trophy'}">
                                    <icon:trophy />
                                </c:when>
                                <c:otherwise>
                                    <icon:football />
                                </c:otherwise>
                            </c:choose>
                            </span>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${resolvedThirdLabel}" />
                        </c:otherwise>
                    </c:choose>
                </button>
            </c:otherwise>
        </c:choose>
    </c:if>
</div>
