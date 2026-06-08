<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="count" required="true" rtexprvalue="true" type="java.lang.Integer" %>
<%@ attribute name="oneCode" required="true" rtexprvalue="true" %>
<%@ attribute name="manyCode" required="true" rtexprvalue="true" %>
<c:choose>
	<c:when test="${count == 1}"><spring:message code="${oneCode}" /></c:when>
	<c:otherwise><spring:message code="${manyCode}" arguments="${count}" /></c:otherwise>
</c:choose>
