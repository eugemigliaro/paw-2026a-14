<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %> <%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Match Point | ${eventPage.event.title}" />
<!DOCTYPE html>
<html lang="en">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--detail">
				<section
					class="event-hero ${eventPage.event.mediaClass} ${not empty eventPage.event.bannerImageUrl ? 'event-hero--with-image' : ''}"
				>
					<c:if test="${not empty eventPage.event.bannerImageUrl}">
						<img
							class="event-hero__image"
							src="${pageContext.request.contextPath}${eventPage.event.bannerImageUrl}"
							alt=""
							loading="eager"
							decoding="async"
						/>
					</c:if>
					<div class="event-hero__content">
						<span class="event-hero__badge"
							><c:out value="${eventPage.event.sport}"
						/></span>
						<div class="event-hero__copy">
							<c:if test="${not empty eventPage.heroSubtitle}">
								<p class="event-hero__eyebrow">
									<c:out value="${eventPage.heroSubtitle}" />
								</p>
							</c:if>
							<h1 class="event-hero__title">
								<c:out value="${eventPage.event.title}" />
							</h1>
							<c:if test="${not empty eventPage.heroMeta}">
								<p class="event-hero__meta">
									<c:out value="${eventPage.heroMeta}" />
								</p>
							</c:if>
						</div>
						<ul
							class="event-hero__status-list"
							aria-label="Event summary"
						>
							<li class="event-hero__status-item">
								<span class="detail-label">When</span>
								<strong
									><c:out value="${eventPage.event.schedule}"
								/></strong>
							</li>
							<li class="event-hero__status-item">
								<span class="detail-label">Where</span>
								<strong
									><c:out value="${eventPage.event.venue}"
								/></strong>
							</li>
							<li class="event-hero__status-item">
								<span class="detail-label">Group</span>
								<strong
									><c:out
										value="${eventPage.participantCountLabel}"
								/></strong>
							</li>
							<li class="event-hero__status-item">
								<span class="detail-label">Entry</span>
								<strong
									><c:out value="${eventPage.bookingPrice}"
								/></strong>
							</li>
						</ul>
					</div>
				</section>

				<section class="detail-layout">
					<div class="detail-layout__main">
						<article class="panel host-card">
							<div class="host-card__main">
								<div
									class="host-card__avatar"
									aria-hidden="true"
								></div>
								<div class="host-card__copy">
									<span class="detail-label">Hosted by</span>
									<strong class="host-card__name"
										><c:out value="${eventPage.hostLabel}"
									/></strong>
								</div>
							</div>
						</article>

						<section
							class="detail-section detail-section--participants"
							aria-labelledby="participant-section-title"
						>
							<div
								class="section-head section-head--detail-compact"
							>
								<div>
									<span class="detail-label">Roster</span>
									<h2
										id="participant-section-title"
										class="detail-section__title"
									>
										Who's joining
									</h2>
								</div>
								<span class="detail-section__meta"
									><c:out
										value="${eventPage.participantCountLabel}"
								/></span>
							</div>

							<c:choose>
								<c:when test="${empty eventPage.participants}">
									<div class="panel participant-empty-state">
										<p
											class="participant-empty-state__title"
										>
											No confirmed players yet
										</p>
										<p
											class="participant-empty-state__copy"
										>
											<c:out
												value="${eventPage.participantsEmptyState}"
											/>
										</p>
									</div>
								</c:when>
								<c:otherwise>
									<ul
										class="participant-list"
										aria-label="Confirmed participants"
									>
										<c:forEach
											var="participant"
											items="${eventPage.participants}"
										>
											<li class="participant-list__item">
												<span
													class="participant-list__avatar"
													aria-hidden="true"
													><c:out
														value="${participant.avatarLabel}"
												/></span>
												<div
													class="participant-list__copy"
												>
													<strong
														class="participant-list__name"
														><c:out
															value="${participant.username}"
													/></strong>
												</div>
											</li>
										</c:forEach>
									</ul>
								</c:otherwise>
							</c:choose>
						</section>

						<section
							class="detail-section detail-section--about"
							aria-labelledby="about-event-title"
						>
							<div
								class="section-head section-head--detail-compact"
							>
								<div>
									<span class="detail-label">Overview</span>
									<h2
										id="about-event-title"
										class="detail-section__title"
									>
										About this event
									</h2>
								</div>
							</div>
							<div class="detail-stack">
								<c:forEach
									var="paragraph"
									items="${eventPage.aboutParagraphs}"
								>
									<p class="body-copy detail-stack__paragraph"><c:out value="${paragraph}" /></p>
								</c:forEach>
							</div>
						</section>
					</div>

					<aside class="detail-layout__sidebar">
						<article class="panel booking-panel">
							<div class="booking-panel__header">
								<div class="booking-panel__header-copy">
									<span class="detail-label"
										>Reserve this spot</span
									>
									<h2 class="booking-panel__title">
										<c:out
											value="${eventPage.bookingPrice}"
										/>
									</h2>
								</div>
							</div>

							<c:if test="${reservationConfirmed}">
								<p
									class="booking-panel__notice booking-panel__notice--success"
								>
									Your reservation is confirmed.
								</p>
							</c:if>
							<c:if test="${not empty reservationError}">
								<p
									class="booking-panel__notice booking-panel__notice--error"
								>
									<c:out value="${reservationError}" />
								</p>
							</c:if>

							<div class="booking-panel__availability">
								<div>
									<span class="detail-label"
										>Availability</span
									>
									<strong
										><c:out
											value="${eventPage.availabilityLabel}"
									/></strong>
								</div>
								<span class="booking-panel__availability-meta"
									><c:out
										value="${eventPage.participantCountLabel}"
								/></span>
							</div>

							<dl class="booking-panel__details">
								<c:forEach
									var="detail"
									items="${eventPage.bookingDetails}"
								>
									<div class="booking-panel__detail-row">
										<dt>
											<c:out value="${detail.label}" />
										</dt>
										<dd>
											<c:out value="${detail.value}" />
										</dd>
									</div>
								</c:forEach>
							</dl>

							<form
								method="post"
								action="${pageContext.request.contextPath}${reservationRequestPath}"
								data-submit-guard="true"
								data-submit-loading-label="Joining..."
								class="booking-panel__request-form"
							>
								<ui:textInput
									label="Email"
									name="email"
									type="email"
									value="${reservationRequestForm.email}"
									placeholder="you@example.com"
									required="${true}"
									autocomplete="email"
								/>
								<ui:button
									label="${eventPage.ctaLabel}"
									type="submit"
									fullWidth="${true}"
									disabled="${not reservationEnabled}"
								/>
							</form>

							<p class="booking-panel__note">
								We send a one-time confirmation link to
								finish the reservation.
							</p>
						</article>
					</aside>
				</section>

				<section class="detail-recommendations">
					<div class="section-head">
						<div>
							<h2
								class="section-head__title section-head__title--detail"
							>
								You may also like
							</h2>
						</div>
						<a
							class="section-link"
							href="${pageContext.request.contextPath}/"
							>View all matches</a
						>
					</div>

					<div class="event-grid event-grid--detail">
						<c:forEach
							var="event"
							items="${eventPage.nearbyEvents}"
						>
							<ui:card
								href="${pageContext.request.contextPath}${event.href}"
								className="event-card"
								ariaLabel="${event.title}"
							>
								<div
									class="event-card__media ${event.mediaClass}"
								>
									<c:if
										test="${not empty event.bannerImageUrl}"
									>
										<img
											class="event-card__image"
											src="${pageContext.request.contextPath}${event.bannerImageUrl}"
											alt=""
											loading="lazy"
											decoding="async"
										/>
									</c:if>
									<span class="event-card__badge"
										><c:out value="${event.sport}"
									/></span>
								</div>

								<div class="event-card__body">
									<span class="event-card__sport"
										><c:out value="${event.sport}"
									/></span>
									<h3 class="event-card__title">
										<c:out value="${event.title}" />
									</h3>
									<div class="event-card__meta">
										<span
											><c:out value="${event.venue}"
										/></span>
										<span
											><c:out value="${event.schedule}"
										/></span>
									</div>

									<div class="event-card__footer">
										<strong class="event-card__price"
											><c:out
												value="${event.priceLabel}"
											/>
											/ person</strong
										>
										<span class="event-card__spots"
											><c:out value="${event.badge}"
										/></span>
									</div>
								</div>
							</ui:card>
						</c:forEach>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
