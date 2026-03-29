<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="href" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>

<jsp:doBody var="cardBody" />

<c:set var="classes" value="card" />
<c:if test="${not empty href}">
  <c:set var="classes" value="${classes} card--interactive" />
</c:if>
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<c:choose>
  <c:when test="${not empty href}">
    <a href="${href}" class="${classes}" <c:if test="${not empty ariaLabel}">aria-label="${ariaLabel}"</c:if>>
      ${cardBody}
    </a>
  </c:when>
  <c:otherwise>
    <article class="${classes}" <c:if test="${not empty ariaLabel}">aria-label="${ariaLabel}"</c:if>>
      ${cardBody}
    </article>
  </c:otherwise>
</c:choose>
