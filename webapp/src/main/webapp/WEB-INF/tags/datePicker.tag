<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<%@ attribute name="label" required="true" rtexprvalue="true" %>
<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="min" required="false" rtexprvalue="true" %>
<%@ attribute name="max" required="false" rtexprvalue="true" %>
<%@ attribute name="required" required="false" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>

<c:set var="resolvedId" value="${empty id ? name : id}" />
<c:set var="fieldClasses" value="field dpicker" />
<c:if test="${not empty className}">
  <c:set var="fieldClasses" value="${fieldClasses} ${className}" />
</c:if>
<c:set var="resolvedMin" value="${empty min ? '' : min}" />
<c:set var="resolvedMax" value="${empty max ? '' : max}" />
<c:set var="resolvedValue" value="${empty value ? '' : value}" />

<div class="<c:out value='${fieldClasses}' />" data-dpicker="true"
    data-lang="<c:out value='${pageContext.response.locale.language}' />"
    data-min="<c:out value='${resolvedMin}' />"
    data-max="<c:out value='${resolvedMax}' />">
  
  	<span class="field__label"><c:out value="${label}" /></span>
	<div class="dpicker__input-wrap">
		<input type="hidden" name="<c:out value='${name}' />"
			id="<c:out value='${resolvedId}' />"
			value="<c:out value='${resolvedValue}' />" data-dpicker-iso="true" />
		<input type="text" class="field__control dpicker__display"
			readonly="readonly" autocomplete="off" placeholder="<c:out value='${pageContext.response.locale.language == "es" ? "dd/mm/aaaa" : "MM/dd/yyyy"}' />"
			<c:if test="${not empty min}">min="<c:out value='${min}' />"</c:if>
			<c:if test="${not empty max}">max="<c:out value='${max}' />"</c:if>
			<c:if test="${required}">required="required" aria-required="true"</c:if>
			data-dpicker-display="true" />
		
		<button type="button" class="dpicker__btn" data-dpicker-btn="true"
			aria-label="<c:out value='${pageContext.response.locale.language == "es" ? "Abrir calendario" : "Open calendar"}' />">
			<icon:calendarDetail fill="none" stroke="currentColor" strokeWidth="2"
				strokeLinecap="round" strokeLinejoin="round" />
		</button>
	</div>
  
	<div class="dpicker__popup" data-dpicker-popup="true"></div>
	<span class="field__error" data-dpicker-error="true" hidden></span>
</div>
