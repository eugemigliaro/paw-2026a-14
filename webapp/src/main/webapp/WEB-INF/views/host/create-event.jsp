<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | Host Mode" />
<!DOCTYPE html>
<html lang="en">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--split">
				<section class="create-layout__main">
					<header class="page-heading">
						<p class="eyebrow">
							<c:out value="${createPage.eyebrow}" />
						</p>
						<h1 class="page-heading__title">
							<c:out value="${createPage.title}" />
						</h1>
						<p class="page-heading__description">
							<c:out value="${createPage.description}" />
						</p>
					</header>

					<article class="panel form-card">
						<span class="detail-label">01 - The Basics</span>
						<h2 class="form-card__title">
							Give the event a clear point of view
						</h2>
						<div class="create-stack">
							<ui:textInput
								label="Event title"
								name="eventTitle"
								placeholder="Saturday Morning Padel Championship"
							/>

							<div>
								<span class="field__label">Category</span>
								<div class="chip-row">
									<c:forEach
										var="chip"
										items="${createPage.categoryChips}"
									>
										<ui:chip
											label="${chip.label}"
											active="${chip.active}"
											tone="${chip.tone}"
										/>
									</c:forEach>
								</div>
							</div>

							<ui:textArea
								label="Description"
								name="description"
								placeholder="Tell participants what to expect from the format, venue, and vibe."
							/>
						</div>
					</article>

					<article class="panel form-card">
						<span class="detail-label">02 - Logistics</span>
						<h2 class="form-card__title">Set the venue and time</h2>
						<div class="form-card__grid">
							<ui:textInput
								label="Location"
								name="location"
								placeholder="Enter venue address"
							/>
							<ui:textInput
								label="Date"
								name="date"
								type="date"
							/>
							<ui:textInput
								label="Start time"
								name="startTime"
								type="time"
							/>
							<ui:textInput
								label="End time"
								name="endTime"
								type="time"
							/>
						</div>
					</article>

					<article class="panel form-card">
						<span class="detail-label"
							>03 - Details & Capacity</span
						>
						<h2 class="form-card__title">
							Control who joins and how the event is priced
						</h2>
						<div class="form-card__grid form-card__grid--three">
							<ui:textInput
								label="Capacity"
								name="capacity"
								type="number"
								value="12"
								min="1"
							/>
							<ui:selectField
								label="Skill level"
								name="skillLevel"
								options="${createPage.skillLevels}"
							/>
							<ui:selectField
								label="Pricing mode"
								id="pricingMode"
								name="pricingMode"
								options="${createPage.priceModes}"
							/>
						</div>

						<div class="create-stack">
							<ui:textInput
								label="Ticket price"
								id="ticketPrice"
								className="pricing-field"
								name="ticketPrice"
								type="number"
								value="15"
								step="0.01"
								hint="Visible in the booking sidebar once backend wiring exists."
							/>
						</div>
					</article>
				</section>

				<aside class="create-layout__sidebar">
					<article class="panel upload-card">
						<span class="detail-label">Upload cover</span>
						<div class="upload-card__placeholder">
							<div class="upload-card__content">
								<strong
									><c:out value="${createPage.uploadHint}"
								/></strong>
								<span
									><c:out value="${createPage.uploadCaption}"
								/></span>
							</div>
						</div>
					</article>

					<article class="panel detail-card">
						<span class="detail-label">Event snapshot</span>
						<h2 class="detail-card__title">Host-ready defaults</h2>
						<ul class="snapshot-list">
							<c:forEach
								var="item"
								items="${createPage.snapshotItems}"
							>
								<li><c:out value="${item}" /></li>
							</c:forEach>
						</ul>
					</article>

					<article class="panel map-card">
						<span class="detail-label">Venue preview</span>
						<div class="map-placeholder">
							<span class="map-placeholder__pin">9</span>
						</div>
					</article>

					<ui:button
						label="Create Event"
						fullWidth="${true}"
						disabled="${true}"
					/>
				</aside>
			</main>
		</div>
		<script>
			(function () {
				var pricingMode = document.getElementById("pricingMode");
				var ticketPriceField = document.querySelector(".pricing-field");
				var ticketPriceInput = document.getElementById("ticketPrice");

				if (!pricingMode || !ticketPriceField || !ticketPriceInput) {
					return;
				}

				function syncTicketPriceVisibility() {
					var isFree = pricingMode.value === "free";
					ticketPriceField.classList.toggle("field--hidden", isFree);
					ticketPriceField.setAttribute(
						"aria-hidden",
						isFree ? "true" : "false",
					);
					ticketPriceInput.disabled = isFree;
				}

				pricingMode.addEventListener(
					"change",
					syncTicketPriceVisibility,
				);
				syncTicketPriceVisibility();
			})();
		</script>
	</body>
</html>
