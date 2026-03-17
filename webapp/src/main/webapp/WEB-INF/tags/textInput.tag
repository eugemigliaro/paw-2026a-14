<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="rounded" required="false" %>
<%@ attribute name="borderColor" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>
<%@ attribute name="required" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="inputType" value="${not empty type ? type : 'text'}" />
<c:set var="inputSize" value="${not empty size ? size : 'md'}" />
<c:set var="inputRounded" value="${not empty rounded ? rounded : 'md'}" />
<c:set var="inputBorderColor" value="${not empty borderColor ? borderColor : 'gray'}" />
<c:set var="inputCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="inputDisabled" value="${disabled ne null ? disabled : false}" />
<c:set var="inputRequired" value="${required ne null ? required : false}" />
<c:set var="classes" value="text-input text-input-size-${inputSize} text-input-rounded-${inputRounded} text-input-border-${inputBorderColor} ${inputCssClass}" />
<c:set var="escapedName" value="${fn:escapeXml(name)}" />
<c:set var="escapedInputType" value="${fn:escapeXml(inputType)}" />
<c:set var="escapedClasses" value="${fn:escapeXml(classes)}" />
<c:set var="escapedPlaceholder" value="${fn:escapeXml(not empty placeholder ? placeholder : '')}" />
<c:set var="escapedValue" value="${fn:escapeXml(not empty value ? value : '')}" />

<div class="text-input-wrapper">
    <c:if test="${not empty label}">
        <label class="text-input-label" for="${escapedName}">
            <c:out value="${label}" />
            <c:if test="${inputRequired}">
                <span class="text-input-required">*</span>
            </c:if>
        </label>
    </c:if>
    <input
        type="${escapedInputType}"
        id="${escapedName}"
        name="${escapedName}"
        class="${escapedClasses}"
        placeholder="${escapedPlaceholder}"
        value="${escapedValue}"
        <c:if test="${inputDisabled}">disabled</c:if>
        <c:if test="${inputRequired}">required</c:if>
    />
</div>