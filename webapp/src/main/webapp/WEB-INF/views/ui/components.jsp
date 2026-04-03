<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | UI Components" />
<!DOCTYPE html>
<html lang="en">
  <head>
    <%@ include file="/WEB-INF/views/includes/head.jspf" %>
  </head>
  <body>
    <div class="app-shell">
      <%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

      <main class="page-shell">
        <header class="page-heading">
          <p class="eyebrow">Internal preview</p>
          <h1 class="page-heading__title">Shared UI primitives</h1>
          <p class="page-heading__description">
            This route previews the MVP-safe components before real functionality is wired.
          </p>
        </header>

        <section class="preview-grid">
          <article class="panel preview-card">
            <h2 class="section-head__title">Buttons</h2>
            <div class="chip-row">
              <ui:button label="Primary" />
              <ui:button label="Secondary" variant="secondary" />
              <ui:button label="Disabled" disabled="${true}" />
            </div>
          </article>

          <article class="panel preview-card">
            <h2 class="section-head__title">Chips</h2>
            <div class="chip-row">
              <c:forEach var="chip" items="${componentPage.sampleChips}">
                <ui:chip label="${chip.label}" active="${chip.active}" tone="${chip.tone}" />
              </c:forEach>
            </div>
          </article>

          <article class="panel preview-card">
            <h2 class="section-head__title">Form fields</h2>
            <div class="create-stack">
              <ui:textInput label="Event title" name="previewTitle" placeholder="Enter a headline" />
              <ui:selectField label="Skill level" name="previewSkill" options="${componentPage.skillLevels}" />
              <ui:textArea label="Description" name="previewDescription" placeholder="Describe the event flow" />
            </div>
          </article>

          <article class="panel preview-card">
            <h2 class="section-head__title">Event card</h2>
            <ui:card className="event-card" href="${pageContext.request.contextPath}${componentPage.sampleEvent.href}">
              <div class="event-card__media ${componentPage.sampleEvent.mediaClass}">
                <span class="event-card__badge"><c:out value="${componentPage.sampleEvent.sport}" /></span>
              </div>
              <div class="event-card__body">
                <h3 class="event-card__title"><c:out value="${componentPage.sampleEvent.title}" /></h3>
                <div class="event-card__meta">
                  <span><c:out value="${componentPage.sampleEvent.schedule}" /></span>
                  <span><c:out value="${componentPage.sampleEvent.venue}" /></span>
                  <span><c:out value="${componentPage.sampleEvent.badge}" /></span>
                </div>
                <div class="event-card__footer">
                  <strong class="event-card__price"><c:out value="${componentPage.sampleEvent.priceLabel}" /></strong>
                </div>
              </div>
            </ui:card>
          </article>
        </section>
      </main>
    </div>
  </body>
</html>
