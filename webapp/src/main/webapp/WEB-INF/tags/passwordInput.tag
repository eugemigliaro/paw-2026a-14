<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
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
<c:set var="escapedPlaceholder" value="${fn:escapeXml(placeholder)}" />
<c:set var="escapedAutocomplete" value="${fn:escapeXml(autocomplete)}" />
<c:set var="fieldClass" value="field" />
<c:if test="${required}">
  <c:set var="fieldClass" value="field field--required" />
</c:if>
<label class="<c:out value='${fieldClass}' />" for="<c:out value='${resolvedId}' />">
  <span class="field__label"><c:out value="${label}" /></span>
  <div class="password-field" data-password-visibility="true">
    <c:choose>
      <c:when test="${not empty path}">
        <form:password
          path="${path}"
          id="${resolvedId}"
          cssClass="field__control password-field__input"
          placeholder="${escapedPlaceholder}"
          autocomplete="${escapedAutocomplete}" />
      </c:when>
      <c:otherwise>
        <input
          id="<c:out value='${resolvedId}' />"
          name="<c:out value='${resolvedName}' />"
          type="password"
          class="field__control password-field__input"
          <c:if test="${not empty value}">value="<c:out value='${value}' />"</c:if>
          <c:if test="${not empty placeholder}">placeholder="<c:out value='${placeholder}' />"</c:if>
          <c:if test="${not empty autocomplete}">autocomplete="<c:out value='${autocomplete}' />"</c:if>
          <c:if test="${required}">required="required" aria-required="true"</c:if> />
      </c:otherwise>
    </c:choose>
    <button
      type="button"
      class="password-field__toggle"
      data-password-toggle="true"
      data-label-show="<c:out value='${showLabel}' />"
      data-label-hide="<c:out value='${hideLabel}' />"
      aria-label="<c:out value='${showLabel}' />"
      aria-pressed="false">
      <span class="password-field__icon password-field__icon--show" aria-hidden="true">
        <icon:openEye />
      </span>
      <span class="password-field__icon password-field__icon--hide" aria-hidden="true">
        <icon:closedEye />
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
