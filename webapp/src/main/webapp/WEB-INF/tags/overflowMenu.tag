<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="ariaLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="menuAriaLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>

<c:set var="classes" value="overflow-menu" />
<c:if test="${not empty className}">
  <c:set var="classes" value="${classes} ${className}" />
</c:if>

<div class="${classes}" data-overflow-menu="true">
  <button
    type="button"
    class="overflow-menu__trigger"
    aria-label="${ariaLabel}"
    aria-haspopup="true"
    aria-expanded="false"
    data-overflow-menu-trigger="true">
    <span class="overflow-menu__dots" aria-hidden="true">
      <span class="overflow-menu__dot"></span>
      <span class="overflow-menu__dot"></span>
      <span class="overflow-menu__dot"></span>
    </span>
  </button>
  <div
    class="overflow-menu__panel"
    role="menu"
    <c:if test="${not empty menuAriaLabel}">aria-label="${menuAriaLabel}"</c:if>
    hidden="hidden"
    data-overflow-menu-panel="true">
    <jsp:doBody />
  </div>
</div>
