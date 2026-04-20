<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="showLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="hideLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="name" required="false" rtexprvalue="true" %>
<%@ attribute name="path" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="placeholder" required="false" rtexprvalue="true" %>
<%@ attribute name="hint" required="false" rtexprvalue="true" %>
<%@ attribute name="autocomplete" required="false" rtexprvalue="true" %>
<%@ attribute name="required" required="false" rtexprvalue="true" type="java.lang.Boolean" %>

<c:set var="resolvedName" value="${empty name ? path : name}" />
<c:set var="resolvedId" value="${empty id ? resolvedName : id}" />
<c:set var="escapedValue" value="${fn:escapeXml(value)}" />
<c:set var="escapedPlaceholder" value="${fn:escapeXml(placeholder)}" />
<c:set var="escapedAutocomplete" value="${fn:escapeXml(autocomplete)}" />
<c:set var="fieldClass" value="field" />
<c:if test="${required}">
  <c:set var="fieldClass" value="field field--required" />
</c:if>

<label class="${fieldClass}" for="${resolvedId}">
  <span class="field__label"><c:out value="${label}" /></span>
  <div class="password-field" data-password-visibility="true">
    <c:choose>
      <c:when test="${not empty path}">
        <form:password
          path="${path}"
          id="${resolvedId}"
          cssClass="field__control password-field__input"
          placeholder="${placeholder}"
          autocomplete="${autocomplete}" />
      </c:when>
      <c:otherwise>
        <input
          id="${resolvedId}"
          name="${resolvedName}"
          type="password"
          class="field__control password-field__input"
          <c:if test="${not empty value}">value="${escapedValue}"</c:if>
          <c:if test="${not empty placeholder}">placeholder="${escapedPlaceholder}"</c:if>
          <c:if test="${not empty autocomplete}">autocomplete="${escapedAutocomplete}"</c:if>
          <c:if test="${required}">required="required" aria-required="true"</c:if> />
      </c:otherwise>
    </c:choose>
    <button
      type="button"
      class="password-field__toggle"
      data-password-toggle="true"
      data-label-show="${showLabel}"
      data-label-hide="${hideLabel}"
      aria-label="${showLabel}"
      aria-pressed="false">
      <span class="password-field__icon password-field__icon--show" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false">
          <path d="M1.5 12s3.8-7 10.5-7 10.5 7 10.5 7-3.8 7-10.5 7S1.5 12 1.5 12Z" />
          <circle cx="12" cy="12" r="3.25" />
        </svg>
      </span>
      <span class="password-field__icon password-field__icon--hide" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false">
          <path d="M3 4.5 19.5 21" />
          <path d="M10.6 6.2A12 12 0 0 1 12 6c6.7 0 10.5 6 10.5 6a18.7 18.7 0 0 1-4.3 4.9" />
          <path d="M6.2 7.4A18.5 18.5 0 0 0 1.5 12s3.8 7 10.5 7c1.6 0 3-.3 4.3-.8" />
          <path d="M9.9 9.6A3.3 3.3 0 0 0 12 15.2" />
          <path d="M14.1 14.4A3.3 3.3 0 0 0 12.9 9" />
        </svg>
      </span>
    </button>
  </div>
  <c:if test="${not empty path}">
    <spring:bind path="${path}">
      <c:if test="${not empty hint and empty status.errorMessages}">
        <span class="field__hint"><c:out value="${hint}" /></span>
      </c:if>
    </spring:bind>
    <form:errors path="${path}" cssClass="field__error" element="span" />
  </c:if>
  <c:if test="${empty path and not empty hint}">
    <span class="field__hint"><c:out value="${hint}" /></span>
  </c:if>
</label>
