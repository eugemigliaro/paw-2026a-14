<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>Text Input Preview</title>
    <link rel="stylesheet" href="<c:url value='/css/textInput.css' />" />
  </head>
  <body>
    <h2><c:out value="${message}" /></h2>

    <div style="max-width: 420px; margin-top: 1rem;">
      <paw:textInput name="username" label="Username" placeholder="Enter your username" required="${true}" size="lg" rounded="full" borderColor="green" />
      <paw:textInput name="email" label="Email" type="email" placeholder="you@example.com" size="md" rounded="md" borderColor="blue" />
      <paw:textInput name="notes" label="Notes" placeholder="Write your thoughts here" size="sm" rounded="lg" borderColor="orange" />
    </div>
  </body>
</html>
