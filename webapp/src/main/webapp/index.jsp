<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<html>
<head>
    <link rel="stylesheet" href="<c:url value="/css/card.css"/>" />
</head>
<body>
    <h2><c:out value="${message}" /></h2>

    <t:card title="Inception" imageUrl="https://placehold.co/200x300">
        <p>A thief who steals corporate secrets...</p>
    </t:card>
</body>
</html>
