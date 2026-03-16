<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Button Component</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/components.css" />
    <style>
      body {
        background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
        color: #0f172a;
        font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
        margin: 0;
        min-height: 100vh;
      }

      .page {
        box-sizing: border-box;
        margin: 0 auto;
        max-width: 960px;
        padding: 48px 20px 64px;
      }

      .hero {
        margin-bottom: 32px;
      }

      .hero h1 {
        font-size: clamp(2rem, 4vw, 3rem);
        margin: 0 0 12px;
      }

      .hero p {
        color: #475569;
        font-size: 1rem;
        line-height: 1.6;
        margin: 0;
        max-width: 720px;
      }

      .grid {
        display: grid;
        gap: 20px;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      }

      .demo-card {
        background: rgba(255, 255, 255, 0.92);
        border: 1px solid rgba(148, 163, 184, 0.2);
        border-radius: 20px;
        box-shadow: 0 20px 45px -32px rgba(15, 23, 42, 0.45);
        padding: 24px;
      }

      .demo-card h2 {
        font-size: 1.05rem;
        margin: 0 0 16px;
      }

      .stack {
        display: flex;
        flex-direction: column;
        gap: 14px;
      }

      .row {
        align-items: center;
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
      }

      .row--column {
        align-items: stretch;
        flex-direction: column;
      }
    </style>
  </head>
  <body>
    <main class="page">
      <section class="hero">
        <h1>Button Component</h1>
        <p>
          This page renders the shared JSP tag from <code>/WEB-INF/tags/button.tag</code> so
          you can inspect variants, sizes, disabled state, and full-width behavior at
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
              <ui:button variant="secondary" disabled="${true}">Disabled Secondary</ui:button>
            </div>
            <div class="row">
              <ui:button title="Save changes" ariaLabel="Save changes">With body content</ui:button>
            </div>
          </div>
        </article>

        <article class="demo-card">
          <h2>Full Width</h2>
          <div class="stack row--column">
            <ui:button label="Continue" fullWidth="${true}" />
            <ui:button label="Delete Account" variant="danger" fullWidth="${true}" />
          </div>
        </article>
      </section>
    </main>
  </body>
</html>
