<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Component Demo</title>
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/button.css"
    />
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/card.css"
    />
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/textInput.css"
    />
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/demoPage.css"
    />
  </head>
  <body>
    <main class="page">
      <section class="hero">
        <h1>Component Demo</h1>
        <p>
          This page renders the shared JSP tags from
          <code>/WEB-INF/tags/button.tag</code> and
          <code>/WEB-INF/tags/card.tag</code> and
          <code>/WEB-INF/tags/textInput.tag</code> so you can inspect their main
          variants and supported content patterns.
        </p>
      </section>

      <section class="hero">
        <h2>Button Component</h2>
      </section>
      
      <section class="grid">
        <ui:card title="Variants" cssClass="demo-card">
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
        </ui:card>

        <ui:card title="Sizes" cssClass="demo-card">
          <div class="stack">
            <div class="row">
              <ui:button label="Small" size="sm" />
              <ui:button label="Medium" size="md" />
              <ui:button label="Large" size="lg" />
            </div>
          </div>
        </ui:card>

        <ui:card title="States" cssClass="demo-card">
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
        </ui:card>

        <ui:card title="Full Width" cssClass="demo-card">
          <div class="stack row--column">
            <ui:button label="Continue" fullWidth="${true}" />
            <ui:button
              label="Delete Account"
              variant="danger"
              fullWidth="${true}"
            />
          </div>
        </ui:card>
      </section>

      <section class="hero">
        <h2>Card Component</h2>
      </section>

      <section class="grid">
        <ui:card title="Card Variants" cssClass="demo-card">
          <div class="stack row--column">
            <ui:card title="Default Card">
              <p>
                Basic body content rendered through the tag body, similar to
                how <code>button.tag</code> supports custom body content.
              </p>
            </ui:card>

            <ui:card
              title="Horizontal Card"
              variant="horizontal"
              ariaLabel="Horizontal card example"
            >
              <jsp:attribute name="footer">
                <ui:button label="Read More" size="sm" variant="secondary" />
              </jsp:attribute>
              <jsp:body>
                <p>
                  The <code>variant</code> attribute maps to card classes and
                  the footer fragment lets the component render actions.
                </p>
              </jsp:body>
            </ui:card>
          </div>
        </ui:card>

        <ui:card title="Card Attributes" cssClass="demo-card">
          <div class="stack row--column">
            <ui:card
              id="featured-card"
              title="Interactive Card"
              onClick="console.log('card clicked')"
              ariaLabel="Interactive featured card"
            >
              <jsp:attribute name="footer">
                <ui:button label="Primary Action" size="sm" />
                <ui:button label="Secondary" size="sm" variant="secondary" />
              </jsp:attribute>
              <jsp:body>
                <p>
                  This example shows <code>id</code>, <code>className</code>,
                  <code>ariaLabel</code> and <code>onClick</code> working the
                  same way as in <code>button.tag</code>.
                </p>
              </jsp:body>
            </ui:card>
            <ui:card
              title="Card with Image"
              ariaLabel="Card with image example"
              imageUrl="https://picsum.photos/400/200"
              imageAlt="Example image for card component"
              imageHeight="150px"
            >
              <jsp:attribute name="footer">
                <ui:button label="View Details" size="sm" variant="secondary" />
              </jsp:attribute>
              <jsp:body>
                <p>
                  The card body can include any content, such as images, text, 
                  or even other components.
                </p>
              </jsp:body>
            </ui:card>
          </div>
        </ui:card>
      </section>

      <section class="hero">
        <h2>Text Input Component</h2>
      </section>

      <section class="grid">
        <ui:card title="Basic" cssClass="demo-card">
          <div class="stack row--column">
            <ui:textInput 
              name="firstName" 
              label="First name" 
              placeholder="Jane" 
            />
            <ui:textInput 
              name="email" 
              label="Email" 
              type="email" 
              placeholder="jane@email.com" 
              required="${true}" 
            />
          </div>
        </ui:card>

        <ui:card title="Styles" cssClass="demo-card">
          <div class="stack row--column">
            <ui:textInput 
              name="search" 
              label="Search" 
              size="sm" 
              rounded="full" 
              borderColor="blue" 
              placeholder="Search products" 
            />
            <ui:textInput 
              name="bio" 
              label="Bio" 
              size="lg" 
              rounded="lg" 
              borderColor="green" 
              placeholder="Tell us about yourself" 
            />
          </div>
        </ui:card>

        <ui:card title="States" cssClass="demo-card">
          <div class="stack row--column">
            <ui:textInput 
              id="readonlyField" 
              name="readonly"
              label="Readonly" 
              value="Locked value" 
              readonly="${true}" 
            />
            <ui:textInput
              id="disabledField" 
              name="disabled"
              label="Disabled" 
              value="Disabled value" 
              disabled="${true}" 
            />
          </div>
        </ui:card>
      </section>
    </main>
  </body>
</html>
