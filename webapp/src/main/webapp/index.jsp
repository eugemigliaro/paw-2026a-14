<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<html>
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/card.css" />
</head>
<body>
    <h2><c:out value="${message}" /></h2>

    <t:card title="Inception" imageUrl="https://placehold.co/200x300">
        <p>A thief who steals corporate secrets...</p>
    </t:card>
</body>
</html>
