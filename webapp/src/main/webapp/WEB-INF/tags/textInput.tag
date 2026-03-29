<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="type" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="placeholder" required="false" rtexprvalue="true" %>
<%@ attribute name="hint" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="min" required="false" rtexprvalue="true" %>
<%@ attribute name="max" required="false" rtexprvalue="true" %>
<%@ attribute name="step" required="false" rtexprvalue="true" %>
<%@ attribute name="autocomplete" required="false" rtexprvalue="true" %>
<%@ attribute name="required" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="disabled" required="false" rtexprvalue="true" type="java.lang.Boolean" %>

<c:set var="resolvedId" value="${empty id ? name : id}" />
<c:set var="resolvedType" value="${empty type ? 'text' : type}" />
<c:set var="classes" value="field" />
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<label class="${classes}" for="${resolvedId}">
  <span class="field__label"><c:out value="${label}" /></span>
  <input
    id="${resolvedId}"
    name="${name}"
    type="${resolvedType}"
    class="field__control"
    <c:if test="${not empty value}">value="${value}"</c:if>
    <c:if test="${not empty placeholder}">placeholder="${placeholder}"</c:if>
    <c:if test="${not empty min}">min="${min}"</c:if>
    <c:if test="${not empty max}">max="${max}"</c:if>
    <c:if test="${not empty step}">step="${step}"</c:if>
    <c:if test="${not empty autocomplete}">autocomplete="${autocomplete}"</c:if>
    <c:if test="${required}">required="required"</c:if>
    <c:if test="${disabled}">disabled="disabled"</c:if> />
  <c:if test="${not empty hint}">
    <span class="field__hint"><c:out value="${hint}" /></span>
  </c:if>
</label>
