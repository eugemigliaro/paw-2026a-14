<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="label" required="false" rtexprvalue="true" %>
<%@ attribute name="type" required="false" rtexprvalue="true" %>
<%@ attribute name="variant" required="false" rtexprvalue="true" %>
<%@ attribute name="size" required="false" rtexprvalue="true" %>
<%@ attribute name="href" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="name" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="onClick" required="false" rtexprvalue="true" %>
<%@ attribute name="form" required="false" rtexprvalue="true" %>
<%@ attribute name="formAction" required="false" rtexprvalue="true" %>
<%@ attribute name="target" required="false" rtexprvalue="true" %>
<%@ attribute name="rel" required="false" rtexprvalue="true" %>
<%@ attribute name="fullWidth" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="disabled" required="false" rtexprvalue="true" type="java.lang.Boolean" %>

<c:set var="resolvedType" value="${empty type ? 'button' : type}" />
<c:set var="resolvedVariant" value="${empty variant ? 'primary' : variant}" />
<c:set var="resolvedSize" value="${empty size ? 'md' : size}" />

<jsp:doBody var="buttonBody" />
<c:set var="trimmedBody" value="${empty buttonBody ? '' : fn:trim(buttonBody)}" />

<c:set var="classes" value="btn btn--${resolvedVariant} btn--${resolvedSize}" />
<c:if test="${fullWidth}">
  <c:set var="classes" value="${classes} btn--full-width" />
</c:if>
<c:if test="${disabled}">
  <c:set var="classes" value="${classes} is-disabled" />
</c:if>
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<c:choose>
  <c:when test="${not empty href}">
    <a
      href="${href}"
      class="${classes}"
      <c:if test="${not empty id}">id="${id}"</c:if>
      <c:if test="${not empty title}">title="${title}"</c:if>
      <c:if test="${not empty ariaLabel}">aria-label="${ariaLabel}"</c:if>
      <c:if test="${not empty onClick}">onclick="${onClick}"</c:if>
      <c:if test="${not empty target}">target="${target}"</c:if>
      <c:if test="${not empty rel}">rel="${rel}"</c:if>>
      <c:choose>
        <c:when test="${not empty trimmedBody}">
          ${buttonBody}
        </c:when>
        <c:otherwise>
          <c:out value="${label}" />
        </c:otherwise>
      </c:choose>
    </a>
  </c:when>
  <c:otherwise>
    <button
      type="${resolvedType}"
      class="${classes}"
      <c:if test="${not empty id}">id="${id}"</c:if>
      <c:if test="${not empty name}">name="${name}"</c:if>
      <c:if test="${not empty value}">value="${value}"</c:if>
      <c:if test="${not empty title}">title="${title}"</c:if>
      <c:if test="${not empty ariaLabel}">aria-label="${ariaLabel}"</c:if>
      <c:if test="${not empty onClick}">onclick="${onClick}"</c:if>
      <c:if test="${not empty form}">form="${form}"</c:if>
      <c:if test="${not empty formAction}">formaction="${formAction}"</c:if>
      <c:if test="${disabled}">disabled="disabled" aria-disabled="true"</c:if>>
      <c:choose>
        <c:when test="${not empty trimmedBody}">
          ${buttonBody}
        </c:when>
        <c:otherwise>
          <c:out value="${label}" />
        </c:otherwise>
      </c:choose>
    </button>
  </c:otherwise>
</c:choose>
