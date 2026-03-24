<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="imageUrl" required="false" rtexprvalue="true" %>
<%@ attribute name="imageAlt" required="false" rtexprvalue="true" %>
<%@ attribute name="imageHeight" required="false" rtexprvalue="true" %>
<%@ attribute name="variant" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="cssClass" required="false" rtexprvalue="true" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="onClick" required="false" rtexprvalue="true" %>
<%@ attribute name="fullWidth" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="footer" fragment="true" %>

<c:set var="resolvedVariant" value="${empty variant ? 'default' : variant}" />
<c:set var="resolvedImageAlt" value="${empty imageAlt ? '' : imageAlt}" />

<c:set var="classes" value="card card-${resolvedVariant}" />
<c:if test="${fullWidth}">
<c:set var="classes" value="${classes} card--full-width" />
</c:if>
<c:if test="${not empty className}">
<c:set var="classes" value="${classes} ${className}" />
</c:if>
<c:if test="${not empty cssClass}">
<c:set var="classes" value="${classes} ${cssClass}" />
</c:if>

<jsp:doBody var="cardBody" />
<c:set var="trimmedCardBody" value="${empty cardBody ? '' : fn:trim(cardBody)}" />

<div
class="${classes}"
<c:if test="${not empty id}">id="${id}"</c:if>
<c:if test="${not empty ariaLabel}">aria-label="${ariaLabel}"</c:if>
<c:if test="${not empty onClick}">onclick="${onClick}"</c:if>>
<c:if test="${not empty imageUrl}">
<div class="card__image"<c:if test="${not empty imageHeight}"> style="height: ${imageHeight};"</c:if>>
    <c:choose>
    <c:when test="${fn:startsWith(imageUrl, 'http')}">
        <img src="${imageUrl}" alt="<c:out value='${resolvedImageAlt}' />"/>
    </c:when>
    <c:otherwise>
        <img src="<c:url value='${imageUrl}'/>" alt="<c:out value='${resolvedImageAlt}' />"/>
    </c:otherwise>
</c:choose>
</div>
</c:if>

<div class="card__body">
<c:if test="${not empty title}">
    <h3 class="card__title"><c:out value="${title}" /></h3>
</c:if>
<c:if test="${not empty trimmedCardBody}">
    ${cardBody}
</c:if>
</div>

<c:if test="${footer ne null}">
<div class="card__footer">
    <jsp:invoke fragment="footer" />
</div>
</c:if>
</div>
