<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="placeholder" required="false" rtexprvalue="true" %>
<%@ attribute name="rows" required="false" rtexprvalue="true" %>
<%@ attribute name="hint" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>

<c:set var="resolvedId" value="${empty id ? name : id}" />
<c:set var="resolvedRows" value="${empty rows ? '4' : rows}" />
<c:set var="classes" value="field" />
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<label class="${classes}" for="${resolvedId}">
  <span class="field__label"><c:out value="${label}" /></span>
  <textarea
    id="${resolvedId}"
    name="${name}"
    rows="${resolvedRows}"
    class="field__control field__control--textarea"
    <c:if test="${not empty placeholder}">placeholder="${placeholder}"</c:if>><c:out value="${value}" /></textarea>
  <c:if test="${not empty hint}">
    <span class="field__hint"><c:out value="${hint}" /></span>
  </c:if>
</label>
