<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="title"    required="false" %>
<%@ attribute name="imageUrl" required="false" %>
<%@ attribute name="imageAlt" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="variant"  required="false" %>
<%@ attribute name="footer"   fragment="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="cardVariant"  value="${not empty variant  ? variant  : 'default'}" />
<c:set var="cardCssClass" value="${not empty cssClass ? cssClass : ''}" />

<div class="card card-${cardVariant} ${cardCssClass}">
    <c:if test="${not empty imageUrl}">
        <div class="card__image">
            <img src="${imageUrl}" alt="<c:out value='${imageAlt}' />" />
        </div>
    </c:if>

    <div class="card__body">
        <c:if test="${not empty title}">
            <h3 class="card__title"><c:out value="${title}" /></h3>
        </c:if>
        <jsp:doBody />
    </div>

    <c:if test="${footer ne null}">
        <div class="card__footer">
            <jsp:invoke fragment="footer" />
        </div>
    </c:if>
</div>


