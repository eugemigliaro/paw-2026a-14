<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>

<%@ attribute name="href" required="false" rtexprvalue="true" %>

<spring:message var="backLabel" code="common.back" />
<c:url var="homeHref" value="/" />
<c:set var="backOnClick" value="if (window.history.length > 1) { window.history.back(); return false; }" />
<c:set var="buttonHref" value="${not empty href ? href : homeHref}" />
<c:set var="buttonOnClick" value="${not empty href ? '' : backOnClick}" />

<div class="page-back-nav">
    <ui:button
        href="${buttonHref}"
        variant="ghost"
        className="page-back-nav__button"
        ariaLabel="${backLabel}"
        onClick="${buttonOnClick}">
        <span class="page-back-nav__icon" aria-hidden="true">&larr;</span>
        <span><c:out value="${backLabel}" /></span>
    </ui:button>
</div>
