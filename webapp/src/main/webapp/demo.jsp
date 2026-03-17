<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %> 
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Button Component</title>
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/button.css"
    />
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/demoPage.css"
    />
  </head>
  <body>
    <main class="page">
      <section class="hero">
        <h1>Button Component</h1>
        <p>
          This page renders the shared JSP tag from
          <code>/WEB-INF/tags/button.tag</code> so you can inspect variants,
          sizes, disabled state, and full-width behavior at
          <code>/button</code>.
        </p>
      </section>

      <section class="grid">
        <article class="demo-card">
          <h2>Variants</h2>
          <div class="stack">
            <div class="row">
              <ui:button label="Primary" />
              <ui:button label="Secondary" variant="secondary" />
            </div>
            <div class="row">
              <ui:button label="Ghost" variant="ghost" />
              <ui:button label="Danger" variant="danger" />
            </div>
          </div>
        </article>

        <article class="demo-card">
          <h2>Sizes</h2>
          <div class="stack">
            <div class="row">
              <ui:button label="Small" size="sm" />
              <ui:button label="Medium" size="md" />
              <ui:button label="Large" size="lg" />
            </div>
          </div>
        </article>

        <article class="demo-card">
          <h2>States</h2>
          <div class="stack">
            <div class="row">
              <ui:button label="Disabled" disabled="${true}" />
              <ui:button variant="secondary" disabled="${true}">
                Disabled Secondary
              </ui:button>
            </div>
            <div class="row">
              <ui:button title="Save changes" ariaLabel="Save changes">
                With body content
              </ui:button>
            </div>
          </div>
        </article>

        <article class="demo-card">
          <h2>Full Width</h2>
          <div class="stack row--column">
            <ui:button label="Continue" fullWidth="${true}" />
            <ui:button
              label="Delete Account"
              variant="danger"
              fullWidth="${true}"
            />
          </div>
        </article>
      </section>
    </main>
  </body>
</html>
