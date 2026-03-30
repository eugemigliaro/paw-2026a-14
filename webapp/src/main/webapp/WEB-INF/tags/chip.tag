<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="href" required="false" rtexprvalue="true" %>
<%@ attribute name="tone" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="active" required="false" rtexprvalue="true" type="java.lang.Boolean" %>

<c:set var="resolvedTone" value="${empty tone ? 'default' : tone}" />
<c:set var="classes" value="chip chip--${resolvedTone}" />
<c:if test="${active}">
  <c:set var="classes" value="${classes} chip--active" />
</c:if>
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<c:choose>
  <c:when test="${not empty href}">
    <a href="${href}" class="${classes}"><c:out value="${label}" /></a>
  </c:when>
  <c:otherwise>
    <button type="button" class="${classes}" aria-pressed="${active ? 'true' : 'false'}"><c:out value="${label}" /></button>
  </c:otherwise>
</c:choose>
