<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="options" required="true" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="hint" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>

<c:set var="resolvedId" value="${empty id ? name : id}" />
<c:set var="classes" value="field" />
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<label class="${classes}" for="${resolvedId}">
  <span class="field__label"><c:out value="${label}" /></span>
  <span class="field__select-wrap">
    <select id="${resolvedId}" name="${name}" class="field__control field__control--select">
      <c:forEach var="option" items="${options}">
        <option value="${option.value}" <c:if test="${option.selected}">selected="selected"</c:if>>
          <c:out value="${option.label}" />
        </option>
      </c:forEach>
    </select>
  </span>
  <c:if test="${not empty hint}">
    <span class="field__hint"><c:out value="${hint}" /></span>
  </c:if>
</label>
