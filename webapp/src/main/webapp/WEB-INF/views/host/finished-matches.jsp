<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.hostFinishedMatches" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>
			<ui:matchList
				listTitle="${listTitle}"
				listDescription="${listDescription}"
				emptyMessage="${emptyMessage}"
				events="${events}"
				paginationItems="${paginationItems}"
				previousPageHref="${previousPageHref}"
				nextPageHref="${nextPageHref}" />
		</div>
	</body>
</html>
