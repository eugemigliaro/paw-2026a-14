<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="label" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="type" required="false" rtexprvalue="true" %>
<%@ attribute name="placeholder" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="size" required="false" rtexprvalue="true" %>
<%@ attribute name="rounded" required="false" rtexprvalue="true" %>
<%@ attribute name="borderColor" required="false" rtexprvalue="true" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="cssClass" required="false" rtexprvalue="true" %>
<%@ attribute name="ariaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="autocomplete" required="false" rtexprvalue="true" %>
<%@ attribute name="inputMode" required="false" rtexprvalue="true" %>
<%@ attribute name="maxLength" required="false" rtexprvalue="true" %>
<%@ attribute name="minLength" required="false" rtexprvalue="true" %>
<%@ attribute name="pattern" required="false" rtexprvalue="true" %>
<%@ attribute name="disabled" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="required" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="readonly" required="false" rtexprvalue="true" type="java.lang.Boolean" %>

<c:set var="resolvedType" value="${empty type ? 'text' : type}" />
<c:set var="resolvedSize" value="${empty size ? 'md' : size}" />
<c:set var="resolvedRounded" value="${empty rounded ? 'md' : rounded}" />
<c:set var="resolvedBorderColor" value="${empty borderColor ? 'gray' : borderColor}" />
<c:set var="resolvedId" value="${empty id ? name : id}" />
<c:set var="resolvedDisabled" value="${disabled ne null ? disabled : false}" />
<c:set var="resolvedRequired" value="${required ne null ? required : false}" />
<c:set var="resolvedReadonly" value="${readonly ne null ? readonly : false}" />

<c:set var="classes" value="text-input text-input-size-${resolvedSize} text-input-rounded-${resolvedRounded} text-input-border-${resolvedBorderColor}" />
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>
<c:if test="${not empty cssClass}">
  <c:set var="classes" value="${classes} ${cssClass}" />
</c:if>

<c:set var="escapedName" value="${fn:escapeXml(name)}" />
<c:set var="escapedId" value="${fn:escapeXml(resolvedId)}" />
<c:set var="escapedType" value="${fn:escapeXml(resolvedType)}" />
<c:set var="escapedClasses" value="${fn:escapeXml(classes)}" />
<c:set var="escapedPlaceholder" value="${fn:escapeXml(empty placeholder ? '' : placeholder)}" />
<c:set var="escapedValue" value="${fn:escapeXml(empty value ? '' : value)}" />

<div class="text-input-wrapper">
  <c:if test="${not empty label}">
    <label class="text-input-label" for="${escapedId}">
      <c:out value="${label}" />
      <c:if test="${resolvedRequired}">
        <span class="text-input-required">*</span>
      </c:if>
    </label>
  </c:if>

  <input
    type="${escapedType}"
    id="${escapedId}"
    name="${escapedName}"
    class="${escapedClasses}"
    placeholder="${escapedPlaceholder}"
    value="${escapedValue}"
    <c:if test="${not empty title}">title="${fn:escapeXml(title)}"</c:if>
    <c:if test="${not empty ariaLabel}">aria-label="${fn:escapeXml(ariaLabel)}"</c:if>
    <c:if test="${not empty autocomplete}">autocomplete="${fn:escapeXml(autocomplete)}"</c:if>
    <c:if test="${not empty inputMode}">inputmode="${fn:escapeXml(inputMode)}"</c:if>
    <c:if test="${not empty maxLength}">maxlength="${fn:escapeXml(maxLength)}"</c:if>
    <c:if test="${not empty minLength}">minlength="${fn:escapeXml(minLength)}"</c:if>
    <c:if test="${not empty pattern}">pattern="${fn:escapeXml(pattern)}"</c:if>
    <c:if test="${resolvedDisabled}">disabled="disabled" aria-disabled="true"</c:if>
    <c:if test="${resolvedRequired}">required="required"</c:if>
    <c:if test="${resolvedReadonly}">readonly="readonly"</c:if>
  />
</div>
