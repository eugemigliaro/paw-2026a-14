<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<%@ taglib prefix="tf" uri="http://paw.itba.edu.ar/tags/time-functions" %>
<spring:message var="pageTitle" code="page.title.eventDetail" arguments="${event.title}" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>
			<spring:message var="participantsAria" code="event.detail.participantsAria" />

			<main class="page-shell page-shell--detail">
				<section class="detail-top ${hostViewer ? 'detail-top--host-view' : ''}">
					<div class="detail-top__main">
						<section
							class="event-hero ${eventMediaClass} ${not empty eventBannerImageUrl ? 'event-hero--with-image' : ''}"
							>
								<c:if test="${not empty eventBannerImageUrl}">
									<c:url var="eventHeroBannerSrc" value="${eventBannerImageUrl}" />
									<img
										class="event-hero__image"
										src="${eventHeroBannerSrc}"
										alt=""
									loading="eager"
									decoding="async"
								/>
							</c:if>
							<div class="event-heading">
								<span class="event-heading__badge"><spring:message code="sport.${event.sport.dbValue}" /></span>
								<h1 class="event-heading__title"><c:out value="${event.title}" /></h1>
							</div>
						</section>

						<section
							class="detail-section detail-section--about"
							aria-labelledby="about-event-title"
						>
							<div class="section-head section-head--detail-compact">
								<div>
									<h2 id="about-event-title" class="detail-section__title">
										<spring:message code="event.detail.aboutEvent" />
									</h2>
								</div>
							</div>
							<div class="detail-stack">
								<c:forEach var="paragraph" items="${aboutParagraphs}">
									<p class="body-copy detail-stack__paragraph"><c:out value="${paragraph}" /></p>
								</c:forEach>
							</div>
						</section>

						<c:if test="${hostViewer && isApprovalRequired}">
							<section
								class="detail-section detail-section--host-collapsible"
								aria-labelledby="pending-requests-title"
							>
								<details
									id="pending-requests"
									class="host-detail-accordion"
									<c:if test="${hostPendingRequestsOpen}">open="open"</c:if>>
									<summary class="host-detail-accordion__summary">
										<h2 id="pending-requests-title" class="detail-section__title">
											<spring:message code="event.host.requests.title" arguments="${hostPendingRequestCount}" />
										</h2>
										<span class="host-detail-accordion__chevron" aria-hidden="true">
											<icon:chevron fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
										</span>
									</summary>
									<c:if test="${hostActionTarget eq 'requests' && not empty hostActionErrorNotice}">
										<p class="booking-panel__notice booking-panel__notice--error host-detail-accordion__notice">
											<c:out value="${hostActionErrorNotice}" />
										</p>
									</c:if>
									<c:if test="${not empty hostPendingRequests}">
										<ul class="participant-list participant-list--managed" aria-labelledby="pending-requests-title">
											<c:forEach var="req" items="${hostPendingRequests}">
												<li class="participant-list__item participant-list__item--managed">
													<c:url var="requestProfileImageSrc" value="${userProfileImageUrls[req.id]}" />
													<img
														class="participant-list__avatar"
														src="${requestProfileImageSrc}"
														alt=""
														aria-hidden="true"
														loading="lazy"
														decoding="async" />
													<div class="participant-list__copy">
														<c:choose>
															<c:when test="${not empty req.username}">
																<c:url var="requestProfileHref" value="/users/${req.username}" />
																<a class="participant-list__name" href="${requestProfileHref}"><c:out value="${req.username}" /></a>
															</c:when>
															<c:otherwise>
																<span class="participant-list__name"><c:out value="${req.username}" /></span>
															</c:otherwise>
														</c:choose>
													</div>
													<div class="participant-list__actions">
														<c:url var="approveAction" value="/host/matches/${event.id}/requests/${req.id}/approve" />
														<spring:message var="approvingLabel" code="host.requests.approving" />
														<form
															method="post"
															action="${approveAction}"
															data-submit-guard="true"
															data-submit-loading-label="${approvingLabel}"
															class="participant-list__action-form">
															<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
															<spring:message var="approveLabel" code="host.requests.approve" />
															<ui:button label="${approveLabel}" type="submit" size="sm" className="participant-list__action-button" />
														</form>
														<c:url var="rejectAction" value="/host/matches/${event.id}/requests/${req.id}/reject" />
														<spring:message var="rejectingLabel" code="host.requests.rejecting" />
														<form
															method="post"
															action="${rejectAction}"
															data-submit-guard="true"
															data-submit-loading-label="${rejectingLabel}"
															class="participant-list__action-form">
															<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
															<spring:message var="rejectLabel" code="host.requests.reject" />
															<ui:button label="${rejectLabel}" type="submit" variant="secondary" size="sm" className="participant-list__action-button" />
														</form>
													</div>
												</li>
											</c:forEach>
										</ul>
									</c:if>
								</details>
							</section>
						</c:if>

						<c:if test="${hostViewer && isInviteOnly}">
							<section
								class="detail-section detail-section--host-collapsible"
								aria-labelledby="pending-invitations-title"
							>
								<details
									id="pending-invitations"
									class="host-detail-accordion"
									<c:if test="${hostPendingInvitesOpen}">open="open"</c:if>>
									<summary class="host-detail-accordion__summary">
										<h2 id="pending-invitations-title" class="detail-section__title">
											<spring:message code="event.host.invites.title" arguments="${hostPendingInviteCount}" />
										</h2>
										<span class="host-detail-accordion__chevron" aria-hidden="true">
											<icon:chevron fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
										</span>
									</summary>
									<c:if test="${hostActionTarget eq 'invites' && not empty hostActionErrorNotice}">
										<p class="booking-panel__notice booking-panel__notice--error host-detail-accordion__notice">
											<c:out value="${hostActionErrorNotice}" />
										</p>
									</c:if>
									<c:url var="hostInviteAction" value="${hostInviteActionPath}" />
									<spring:message var="invitingLabel" code="host.invites.inviting" />
									<form
										method="post"
										action="${hostInviteAction}"
										data-submit-guard="true"
										data-submit-loading-label="${invitingLabel}"
										class="host-invite-form"
										novalidate="novalidate">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<div class="host-invite-form__row">
											<spring:message var="emailLabel" code="form.email.label" />
											<spring:message var="emailPlaceholder" code="form.email.placeholder" />
											<ui:textInput
												label="${emailLabel}"
												name="email"
												id="host-invite-email"
												type="email"
												value="${hostInviteEmail}"
												placeholder="${emailPlaceholder}"
												className="host-invite-form__field" />
											<spring:message var="inviteSubmitLabel" code="host.invites.invite" />
											<ui:button label="${inviteSubmitLabel}" type="submit" className="host-invite-form__submit" disabled="${not matchActionCapabilities.canManageParticipants}" />
										</div>
										<c:if test="${hostSeriesInviteAvailable}">
											<label class="series-invite-option host-invite-form__series-option" for="host-invite-series">
												<input
													type="checkbox"
													name="inviteSeries"
													id="host-invite-series"
													value="true"
													class="series-invite-option__input" />
												<span class="series-invite-option__copy">
													<span class="series-invite-option__title">
														<spring:message code="host.invites.inviteSeries" />
													</span>
													<span class="series-invite-option__hint">
														<spring:message code="host.invites.inviteSeries.hint" />
													</span>
												</span>
											</label>
										</c:if>
									</form>
									<c:if test="${not empty hostPendingInvites or not empty hostDeclinedInvites}">
										<ul class="participant-list participant-list--managed participant-list--invitations" aria-labelledby="pending-invitations-title">
											<c:forEach var="invite" items="${hostPendingInvites}">
												<li class="participant-list__item participant-list__item--managed">
													<c:url var="pendingInviteProfileImageSrc" value="${userProfileImageUrls[invite.id]}" />
													<img class="participant-list__avatar" src="${pendingInviteProfileImageSrc}" alt="" aria-hidden="true" loading="lazy" decoding="async" />
													<div class="participant-list__copy">
														<c:choose>
															<c:when test="${not empty invite.username}">
																<c:url var="pendingInviteProfileHref" value="/users/${invite.username}" />
																<a class="participant-list__name" href="${pendingInviteProfileHref}"><c:out value="${invite.username}" /></a>
															</c:when>
															<c:otherwise>
																<span class="participant-list__name"><c:out value="${invite.username}" /></span>
															</c:otherwise>
														</c:choose>
													</div>
													<span class="participant-list__status participant-list__status--pending">
														<spring:message code="host.invites.status.pending" />
													</span>
												</li>
											</c:forEach>
											<c:forEach var="invite" items="${hostDeclinedInvites}">
												<li class="participant-list__item participant-list__item--managed">
													<c:url var="declinedInviteProfileImageSrc" value="${userProfileImageUrls[invite.id]}" />
													<img class="participant-list__avatar" src="${declinedInviteProfileImageSrc}" alt="" aria-hidden="true" loading="lazy" decoding="async" />
													<div class="participant-list__copy">
														<c:choose>
															<c:when test="${not empty invite.username}">
																<c:url var="declinedInviteProfileHref" value="/users/${invite.username}" />
																<a class="participant-list__name" href="${declinedInviteProfileHref}"><c:out value="${invite.username}" /></a>
															</c:when>
															<c:otherwise>
																<span class="participant-list__name"><c:out value="${invite.username}" /></span>
															</c:otherwise>
														</c:choose>
													</div>
													<span class="participant-list__status participant-list__status--declined">
														<spring:message code="host.invites.status.declined" />
													</span>
												</li>
											</c:forEach>
										</ul>
									</c:if>
								</details>
							</section>
						</c:if>

						<section
							id="participants"
							class="detail-section detail-section--participants"
							aria-labelledby="participant-section-title"
						>
							<div class="section-head section-head--detail-compact">
								<div>
									<h2 id="participant-section-title" class="detail-section__title">
										<spring:message code="event.detail.whosJoining" />
									</h2>
								</div>
								<span class="detail-section__meta"><c:out value="${participantCountLabel}" /></span>
							</div>
							<c:if test="${hostActionTarget eq 'participants' && not empty hostActionErrorNotice}">
								<p class="booking-panel__notice booking-panel__notice--error">
									<c:out value="${hostActionErrorNotice}" />
								</p>
							</c:if>

							<c:choose>
								<c:when test="${empty participants}">
									<div class="panel participant-empty-state">
										<span class="participant-empty-state__icon" aria-hidden="true">
											<icon:noParticipants fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
										</span>
										<p class="participant-empty-state__title">
											<spring:message code="event.detail.noPlayers" />
										</p>
										<p class="participant-empty-state__copy">
											<c:out value="${participantsEmptyState}" />
										</p>
									</div>
								</c:when>
								<c:otherwise>
									<ul class="participant-list" aria-label="${participantsAria}">
										<c:forEach var="participant" items="${participants}">
											<li class="participant-list__item ${not empty participantRemovePaths[participant.id] ? 'participant-list__item--managed' : ''}">
												<c:url var="participantProfileImageSrc" value="${userProfileImageUrls[participant.id]}" />
												<img
													class="participant-list__avatar"
													src="${participantProfileImageSrc}"
													alt=""
													aria-hidden="true"
													loading="lazy"
													decoding="async" />
												<div class="participant-list__copy">
													<c:url var="participantProfileHref" value="/users/${participant.username}" />
													<a class="participant-list__name" href="${participantProfileHref}"><c:out value="${participant.username}" /></a>
													<c:if test="${not empty participantReviewHrefs[participant.id]}">
														<c:url var="participantReviewHref" value="${participantReviewHrefs[participant.id]}" />
														<a class="participant-list__review-link" href="${participantReviewHref}">
															<spring:message code="event.participants.review" />
														</a>
													</c:if>
												</div>
												<c:if test="${not empty participantRemovePaths[participant.id]}">
													<c:url var="participantRemoveAction" value="${participantRemovePaths[participant.id]}" />
													<spring:message var="removingLabel" code="event.host.participants.removing" />
													<form
														method="post"
														action="${participantRemoveAction}"
														data-submit-guard="true"
														data-submit-loading-label="${removingLabel}"
														class="participant-list__action-form">
														<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
														<spring:message var="removeLabel" code="event.host.participants.remove" />
														<ui:button label="${removeLabel}" type="submit" variant="danger" size="sm" className="participant-list__action-button participant-list__action-button--danger" />
													</form>
												</c:if>
											</li>
										</c:forEach>
									</ul>
								</c:otherwise>
							</c:choose>
						</section>
					</div>

					<aside class="detail-top__sidebar ${hostViewer ? 'detail-top__sidebar--host-view' : ''}">
						<c:choose>
							<c:when test="${hostViewer}">
								<article class="panel host-panel">
									<c:if test="${not empty hostActionNotice}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<c:out value="${hostActionNotice}" />
										</p>
									</c:if>

									<spring:message var="hostCancellingLabel" code="event.host.action.cancelling" />
									<c:url var="hostEditHref" value="${hostEditPath}" />
									<c:url var="hostSeriesEditHref" value="${hostSeriesEditPath}" />
									<c:url var="hostCancelAction" value="${hostCancelPath}" />
									<c:url var="hostSeriesCancelAction" value="${hostSeriesCancelPath}" />
									<div class="host-action-card ${not empty occurrences ? 'host-action-card--recurring' : ''} ${isInviteOnly && empty occurrences ? 'host-action-card--three' : ''}">
										<c:choose>
											<c:when test="${not empty occurrences}">
												<spring:message var="hostEditOccurrenceLabel" code="event.host.action.editOccurrence" />
												<spring:message var="hostEditSeriesLabel" code="event.host.action.editSeries" />
												<spring:message var="hostCancelOccurrenceLabel" code="event.host.action.cancelOccurrence" />
												<spring:message var="hostCancelSeriesLabel" code="event.host.action.cancelSeries" />
												<c:choose>
													<c:when test="${matchActionCapabilities.canEdit}">
														<a class="host-action-card__button" href="${hostEditHref}">
															<span class="host-action-card__icon" aria-hidden="true">
																<icon:pencil fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
															</span>
															<span><c:out value="${hostEditOccurrenceLabel}" /></span>
														</a>
													</c:when>
													<c:otherwise>
														<span class="host-action-card__button is-disabled" aria-disabled="true">
															<span class="host-action-card__icon" aria-hidden="true">
																<icon:pencil fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
															</span>
															<span><c:out value="${hostEditOccurrenceLabel}" /></span>
														</span>
													</c:otherwise>
												</c:choose>
												<c:choose>
													<c:when test="${matchActionCapabilities.canEditSeries}">
														<a class="host-action-card__button" href="${hostSeriesEditHref}">
															<span class="host-action-card__icon" aria-hidden="true">
																<icon:pencil fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
															</span>
															<span><c:out value="${hostEditSeriesLabel}" /></span>
														</a>
													</c:when>
													<c:otherwise>
														<span class="host-action-card__button is-disabled" aria-disabled="true">
															<span class="host-action-card__icon" aria-hidden="true">
																<icon:pencil fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
															</span>
															<span><c:out value="${hostEditSeriesLabel}" /></span>
														</span>
													</c:otherwise>
												</c:choose>
												<form method="post" action="${hostCancelAction}" data-submit-guard="true" data-submit-loading-label="${hostCancellingLabel}" class="host-action-card__form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<button class="host-action-card__button host-action-card__button--danger" type="submit" <c:if test="${not matchActionCapabilities.canCancel}">disabled="disabled" aria-disabled="true"</c:if>>
														<span class="host-action-card__icon" aria-hidden="true">
															<icon:calendar fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
														</span>
														<span><c:out value="${hostCancelOccurrenceLabel}" /></span>
													</button>
												</form>
												<form method="post" action="${hostSeriesCancelAction}" data-submit-guard="true" data-submit-loading-label="${hostCancellingLabel}" class="host-action-card__form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<button class="host-action-card__button host-action-card__button--danger" type="submit" <c:if test="${not matchActionCapabilities.canCancelSeries}">disabled="disabled" aria-disabled="true"</c:if>>
														<span class="host-action-card__icon" aria-hidden="true">
															<icon:calendar fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
														</span>
														<span><c:out value="${hostCancelSeriesLabel}" /></span>
													</button>
												</form>
											</c:when>
											<c:otherwise>
												<spring:message var="hostEditLabel" code="event.host.action.edit" />
												<spring:message var="hostCancelLabel" code="event.host.action.cancel" />
												<c:choose>
													<c:when test="${matchActionCapabilities.canEdit}">
														<a class="host-action-card__button" href="${hostEditHref}">
															<span class="host-action-card__icon" aria-hidden="true">
																<icon:pencil fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
															</span>
															<span><c:out value="${hostEditLabel}" /></span>
														</a>
													</c:when>
													<c:otherwise>
														<span class="host-action-card__button is-disabled" aria-disabled="true">
															<span class="host-action-card__icon" aria-hidden="true">
																<icon:pencil fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
															</span>
															<span><c:out value="${hostEditLabel}" /></span>
														</span>
													</c:otherwise>
												</c:choose>
												<form method="post" action="${hostCancelAction}" data-submit-guard="true" data-submit-loading-label="${hostCancellingLabel}" class="host-action-card__form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<button class="host-action-card__button host-action-card__button--danger" type="submit" <c:if test="${not matchActionCapabilities.canCancel}">disabled="disabled" aria-disabled="true"</c:if>>
														<span class="host-action-card__icon" aria-hidden="true">
															<icon:calendar fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
														</span>
														<span><c:out value="${hostCancelLabel}" /></span>
													</button>
												</form>
												<c:if test="${isInviteOnly}">
													<spring:message var="hostInviteLabel" code="event.host.action.invite" />
													<c:choose>
														<c:when test="${matchActionCapabilities.canManageParticipants}">
															<a class="host-action-card__button" href="#pending-invitations" data-host-invite-trigger="true">
																<span class="host-action-card__icon" aria-hidden="true">
																	<icon:profilePlus fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
																</span>
																<span><c:out value="${hostInviteLabel}" /></span>
															</a>
														</c:when>
														<c:otherwise>
															<span class="host-action-card__button is-disabled" aria-disabled="true">
																<span class="host-action-card__icon" aria-hidden="true">
																	<icon:profilePlus fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
																</span>
																<span><c:out value="${hostInviteLabel}" /></span>
															</span>
														</c:otherwise>
													</c:choose>
												</c:if>
											</c:otherwise>
										</c:choose>
									</div>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:moneySign fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.reserveSpot" />
											</dt>
											<dd><c:out value="${bookingPrice}" /></dd>
										</div>
									</dl>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:calendar fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.date" />
											</dt>
											<dd><c:out value="${tf:date(event.startsAtDateTime)}" /></dd>
										</div>
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:clock fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.time" />
											</dt>
											<dd>
												<c:out value="${tf:time(event.startsAtDateTime)}" />
												<c:if test="${not empty event.endsAtDateTime}">
													<c:out value=" - ${tf:time(event.endsAtDateTime)}" />
												</c:if>
											</dd>
										</div>
									</dl>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:locationPin fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.venue" />
											</dt>
											<dd class="event-info-panel__value--truncate"><c:out value="${event.address}" /></dd>
										</div>
									</dl>
								</article>
								<c:if test="${mapAvailable}">
									<spring:message var="eventMapAria" code="event.detail.locationMap.aria" />
									<c:url var="appRootUrl" value="/" />
									<c:set var="contextAwareMapTileUrlTemplate"
										value="${appRootUrl}${fn:substring(mapTileUrlTemplate, 1, fn:length(mapTileUrlTemplate))}" />
									<div
										class="event-detail-map"
										data-event-map="true"
										data-tile-url-template="${contextAwareMapTileUrlTemplate}"
										data-attribution="${mapAttribution}"
										data-latitude="${mapLatitude}"
										data-longitude="${mapLongitude}"
										data-zoom="${mapZoom}"
										role="img"
										aria-label="${eventMapAria}">
										<c:if test="${not empty mapAttribution}">
											<p class="event-detail-map__attribution"><c:out value="${mapAttribution}" /></p>
										</c:if>
									</div>
								</c:if>

								<article class="panel event-info-panel event-info-panel--hosted-by">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:profile fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
												</span>
												<spring:message code="event.detail.hostedBy" />
											</dt>
											<dd>
												<c:url var="hostProfileImageSrc" value="${hostProfileImageUrl}" />
												<span class="event-info-panel__host">
													<img class="event-info-panel__host-avatar" src="${hostProfileImageSrc}" alt="" aria-hidden="true" loading="lazy" decoding="async" />
													<c:choose>
														<c:when test="${not empty hostProfileHref}">
															<c:url var="hostProfileHref" value="${hostProfileHref}" />
															<a class="event-info-panel__host-name" href="${hostProfileHref}"><c:out value="${hostLabel}" /></a>
														</c:when>
														<c:otherwise>
															<span class="event-info-panel__host-name"><c:out value="${hostLabel}" /></span>
														</c:otherwise>
													</c:choose>
												</span>
											</dd>
										</div>
									</dl>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:doubleProfile fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.availability" />
											</dt>
											<dd>
												<c:out value="${availabilityLabel}" /><br />
												<c:out value="${participantCountLabel}" />
											</dd>
										</div>
									</dl>
								</article>

								<article class="panel player-actions-panel">
									<c:if test="${reservationCancelled}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<c:choose>
												<c:when test="${not empty occurrences}">
													<spring:message code="event.booking.occurrenceCancelled" />
												</c:when>
												<c:otherwise>
													<spring:message code="event.booking.cancelled" />
												</c:otherwise>
											</c:choose>
										</p>
									</c:if>
									<c:if test="${not empty reservationError}">
										<p class="booking-panel__notice booking-panel__notice--error">
											<c:out value="${reservationError}" />
										</p>
									</c:if>
									<c:if test="${not empty eventStateNotice}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<c:out value="${eventStateNotice}" />
										</p>
									</c:if>
									<c:if test="${joinRequested}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.joinRequest.requested" />
										</p>
									</c:if>
									<c:if test="${joinCancelled}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.joinRequest.cancelled" />
										</p>
									</c:if>
									<c:if test="${not empty joinError}">
										<p class="booking-panel__notice booking-panel__notice--error">
											<c:out value="${joinError}" />
										</p>
									</c:if>

									<spring:message var="joiningLabel" code="event.booking.joining" />
									<c:if test="${not hostViewer or isConfirmedParticipant or matchActionCapabilities.canReserve or seriesReservationEnabled or seriesCancellationEnabled}">
										<c:choose>
											<c:when test="${isConfirmedParticipant}">
												<c:if test="${reservationConfirmed}">
													<p class="booking-panel__notice booking-panel__notice--success">
														<spring:message code="event.booking.confirmed" />
													</p>
												</c:if>
												<c:if test="${matchActionCapabilities.canCancelReservation}">
													<c:url var="reservationCancelAction" value="${reservationCancelPath}" />
													<c:choose>
														<c:when test="${not empty occurrences}">
															<spring:message var="leavingReservationLabel" code="event.booking.leavingOccurrence" />
															<spring:message var="leaveReservationLabel" code="event.booking.leaveOccurrence" />
														</c:when>
														<c:otherwise>
															<spring:message var="leavingReservationLabel" code="event.booking.leaving" />
															<spring:message var="leaveReservationLabel" code="event.booking.leave" />
														</c:otherwise>
													</c:choose>
													<form method="post" action="${reservationCancelAction}" data-submit-guard="true" data-submit-loading-label="${leavingReservationLabel}" class="booking-panel__request-form">
														<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
														<ui:button label="${leaveReservationLabel}" type="submit" fullWidth="${true}" variant="danger" />
													</form>
												</c:if>
											</c:when>
											<c:when test="${reservationRequiresLogin}">
												<c:choose>
													<c:when test="${matchActionCapabilities.canReserve}">
														<spring:message var="signInToReserveLabel" code="event.booking.signIn" />
														<c:url var="loginHref" value="/login" />
														<ui:button label="${signInToReserveLabel}" href="${loginHref}" fullWidth="${true}" />
														<p class="booking-panel__note"><spring:message code="event.booking.signInNote" /></p>
													</c:when>
													<c:when test="${matchActionCapabilities.canRequestToJoin and not seriesJoinRequestPending}">
														<spring:message var="signInToRequestLabel" code="event.joinRequest.signIn" />
														<c:url var="loginHref" value="/login" />
														<ui:button label="${signInToRequestLabel}" href="${loginHref}" fullWidth="${true}" />
														<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
													</c:when>
													<c:otherwise>
														<ui:button label="${ctaLabel}" type="button" fullWidth="${true}" disabled="${true}" />
														<p class="booking-panel__note"><spring:message code="event.booking.note" /></p>
													</c:otherwise>
												</c:choose>
											</c:when>
											<c:when test="${hasPendingJoinRequest}">
												<c:url var="cancelJoinAction" value="${cancelJoinRequestPath}" />
												<spring:message var="cancellingLabel" code="event.joinRequest.cancelling" />
												<p class="booking-panel__notice booking-panel__notice--info">
													<spring:message code="event.joinRequest.pendingLabel" />
												</p>
												<form method="post" action="${cancelJoinAction}" data-submit-guard="true" data-submit-loading-label="${cancellingLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="cancelRequestLabel" code="event.joinRequest.cancelRequest" />
													<ui:button label="${cancelRequestLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
											</c:when>
											<c:when test="${matchActionCapabilities.canRequestToJoin and not seriesJoinRequestPending}">
												<c:url var="joinRequestAction" value="${joinRequestPath}" />
												<spring:message var="requestingLabel" code="event.joinRequest.requesting" />
												<form method="post" action="${joinRequestAction}" data-submit-guard="true" data-submit-loading-label="${requestingLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<c:choose>
														<c:when test="${not empty occurrences}">
															<spring:message var="requestToJoinLabel" code="event.joinRequest.requestThisOccurrence" />
														</c:when>
														<c:otherwise>
															<spring:message var="requestToJoinLabel" code="event.joinRequest.requestToJoin" />
														</c:otherwise>
													</c:choose>
													<ui:button label="${requestToJoinLabel}" type="submit" fullWidth="${true}" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
											</c:when>
											<c:when test="${matchActionCapabilities.canReserve}">
												<c:url var="reservationRequestAction" value="${reservationRequestPath}" />
												<form method="post" action="${reservationRequestAction}" data-submit-guard="true" data-submit-loading-label="${joiningLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<ui:button label="${ctaLabel}" type="submit" fullWidth="${true}" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.booking.note" /></p>
											</c:when>
											<c:when test="${isInvitedPlayer}">
												<c:if test="${inviteAccepted}">
													<p class="booking-panel__notice booking-panel__notice--success">
														<spring:message code="event.invite.accepted" />
													</p>
												</c:if>
												<c:if test="${not empty inviteError}">
													<p class="booking-panel__notice booking-panel__notice--error">
														<c:out value="${inviteError}" />
													</p>
												</c:if>
												<p class="booking-panel__notice booking-panel__notice--info" data-invite-refresh-notice="true">
													<spring:message code="event.invite.pendingLabel" />
												</p>
												<c:url var="acceptInviteAction" value="${acceptInvitePath}" />
												<spring:message var="acceptingInviteLabel" code="event.invite.accepting" />
												<form method="post" action="${acceptInviteAction}" data-submit-guard="true" data-submit-loading-label="${acceptingInviteLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="acceptInviteLabel" code="event.invite.accept" />
													<ui:button label="${acceptInviteLabel}" type="submit" fullWidth="${true}" />
												</form>
												<c:url var="declineInviteAction" value="${declineInvitePath}" />
												<spring:message var="decliningInviteLabel" code="event.invite.declining" />
												<form method="post" action="${declineInviteAction}" data-submit-guard="true" data-submit-loading-label="${decliningInviteLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="declineInviteLabel" code="event.invite.decline" />
													<ui:button label="${declineInviteLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.invite.note" /></p>
											</c:when>
											<c:otherwise>
												<ui:button label="${ctaLabel}" type="button" fullWidth="${true}" disabled="${true}" />
												<p class="booking-panel__note"><spring:message code="event.booking.note" /></p>
											</c:otherwise>
										</c:choose>
									</c:if>

									<c:if test="${seriesJoinRequested}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.recurringJoinRequest.requested" />
										</p>
									</c:if>
									<c:if test="${seriesJoinRequestPending and not seriesJoinRequested}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<spring:message code="event.recurringJoinRequest.pending" />
										</p>
									</c:if>
									<c:if test="${seriesJoinRequestEnabled and not seriesJoinRequestPending}">
										<c:choose>
											<c:when test="${seriesJoinRequestRequiresLogin}">
												<spring:message var="signInToRequestRecurringLabel" code="event.recurringJoinRequest.signIn" />
												<c:url var="recurringJoinRequestLoginHref" value="/login" />
												<ui:button label="${signInToRequestRecurringLabel}" href="${recurringJoinRequestLoginHref}" fullWidth="${true}" variant="secondary" />
											</c:when>
											<c:otherwise>
												<c:url var="recurringJoinRequestAction" value="${seriesJoinRequestPath}" />
												<spring:message var="requestingRecurringJoinLabel" code="event.recurringJoinRequest.requesting" />
												<form method="post" action="${recurringJoinRequestAction}" data-submit-guard="true" data-submit-loading-label="${requestingRecurringJoinLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="requestRecurringJoinLabel" code="event.recurringJoinRequest.cta" />
													<ui:button label="${requestRecurringJoinLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
											</c:otherwise>
										</c:choose>
										<p class="booking-panel__note"><spring:message code="event.recurringJoinRequest.note" /></p>
									</c:if>

									<c:if test="${seriesReservationConfirmed}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.recurringReservation.confirmed" />
										</p>
									</c:if>
									<c:if test="${seriesReservationCancelled}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<spring:message code="event.recurringReservation.cancelled" />
										</p>
									</c:if>
									<c:if test="${seriesReservationJoined and not seriesReservationConfirmed}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.recurringReservation.joined" />
										</p>
									</c:if>
									<c:if test="${not empty seriesReservationError}">
										<p class="booking-panel__notice booking-panel__notice--error">
											<c:out value="${seriesReservationError}" />
										</p>
									</c:if>
									<c:if test="${seriesReservationEnabled and not seriesReservationJoined}">
										<c:choose>
											<c:when test="${seriesReservationRequiresLogin}">
												<spring:message var="signInToJoinRecurringLabel" code="event.recurringReservation.signIn" />
												<c:url var="recurringLoginHref" value="/login" />
												<ui:button label="${signInToJoinRecurringLabel}" href="${recurringLoginHref}" fullWidth="${true}" variant="secondary" />
											</c:when>
											<c:otherwise>
												<c:url var="recurringReservationAction" value="${seriesReservationPath}" />
												<spring:message var="joiningRecurringLabel" code="event.recurringReservation.joining" />
												<form method="post" action="${recurringReservationAction}" data-submit-guard="true" data-submit-loading-label="${joiningRecurringLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="joinRecurringLabel" code="event.recurringReservation.cta" />
													<ui:button label="${joinRecurringLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
											</c:otherwise>
										</c:choose>
										<p class="booking-panel__note"><spring:message code="event.recurringReservation.note" /></p>
									</c:if>
									<c:if test="${seriesCancellationEnabled}">
										<c:url var="recurringReservationCancelAction" value="${seriesReservationCancelPath}" />
										<spring:message var="leavingRecurringLabel" code="event.recurringReservation.leaving" />
										<form method="post" action="${recurringReservationCancelAction}" data-submit-guard="true" data-submit-loading-label="${leavingRecurringLabel}" class="booking-panel__request-form">
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<spring:message var="leaveRecurringLabel" code="event.recurringReservation.leave" />
											<ui:button label="${leaveRecurringLabel}" type="submit" fullWidth="${true}" variant="secondary" />
										</form>
									</c:if>
									<hr class="booking-panel__divider" />
									<c:url var="hostReportMatchHref" value="/reports/matches/${event.id}" />
									<spring:message var="hostReportMatchLabel" code="moderation.report.match.menu" />
									<ui:button label="${hostReportMatchLabel}" href="${hostReportMatchHref}" variant="danger" fullWidth="${true}" className="booking-panel__report-button" />
								</article>
							</c:when>
							<c:otherwise>
								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:moneySign fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.reserveSpot" />
											</dt>
											<dd><c:out value="${bookingPrice}" /></dd>
										</div>
									</dl>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:calendar fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.date" />
											</dt>
											<dd><c:out value="${tf:date(event.startsAtDateTime)}" /></dd>
										</div>
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:clock fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.time" />
											</dt>
											<dd>
												<c:out value="${tf:time(event.startsAtDateTime)}" />
												<c:if test="${not empty event.endsAtDateTime}">
													<c:out value=" - ${tf:time(event.endsAtDateTime)}" />
												</c:if>
											</dd>
										</div>
									</dl>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:locationPin fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.venue" />
											</dt>
											<dd class="event-info-panel__value--truncate"><c:out value="${event.address}" /></dd>
										</div>
									</dl>
								</article>
								<c:if test="${mapAvailable}">
									<spring:message var="eventMapAria" code="event.detail.locationMap.aria" />
									<c:url var="appRootUrl" value="/" />
									<c:set var="contextAwareMapTileUrlTemplate"
										value="${appRootUrl}${fn:substring(mapTileUrlTemplate, 1, fn:length(mapTileUrlTemplate))}" />
									<div
										class="event-detail-map"
										data-event-map="true"
										data-tile-url-template="${contextAwareMapTileUrlTemplate}"
										data-attribution="${mapAttribution}"
										data-latitude="${mapLatitude}"
										data-longitude="${mapLongitude}"
										data-zoom="${mapZoom}"
										role="img"
										aria-label="${eventMapAria}">
										<c:if test="${not empty mapAttribution}">
											<p class="event-detail-map__attribution"><c:out value="${mapAttribution}" /></p>
										</c:if>
									</div>
								</c:if>

								<article class="panel event-info-panel event-info-panel--hosted-by">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:profile fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
												</span>
												<spring:message code="event.detail.hostedBy" />
											</dt>
											<dd>
												<c:url var="hostProfileImageSrc" value="${hostProfileImageUrl}" />
												<span class="event-info-panel__host">
													<img class="event-info-panel__host-avatar" src="${hostProfileImageSrc}" alt="" aria-hidden="true" loading="lazy" decoding="async" />
													<c:choose>
														<c:when test="${not empty hostProfileHref}">
															<c:url var="hostProfileHref" value="${hostProfileHref}" />
															<a class="event-info-panel__host-name" href="${hostProfileHref}"><c:out value="${hostLabel}" /></a>
														</c:when>
														<c:otherwise>
															<span class="event-info-panel__host-name"><c:out value="${hostLabel}" /></span>
														</c:otherwise>
													</c:choose>
												</span>
											</dd>
										</div>
									</dl>
								</article>

								<article class="panel event-info-panel">
									<dl class="event-info-panel__list">
										<div class="booking-panel__detail-row event-info-panel__row">
											<dt>
												<span class="event-info-panel__icon" aria-hidden="true">
													<icon:doubleProfile fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
												</span>
												<spring:message code="event.booking.availability" />
											</dt>
											<dd>
												<c:out value="${availabilityLabel}" /><br />
												<c:out value="${participantCountLabel}" />
											</dd>
										</div>
									</dl>
								</article>

								<article class="panel booking-panel">
									<c:if test="${reservationCancelled}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<c:choose>
												<c:when test="${not empty occurrences}">
													<spring:message code="event.booking.occurrenceCancelled" />
												</c:when>
												<c:otherwise>
													<spring:message code="event.booking.cancelled" />
												</c:otherwise>
											</c:choose>
										</p>
									</c:if>
									<c:if test="${not empty reservationError}">
										<p class="booking-panel__notice booking-panel__notice--error">
											<c:out value="${reservationError}" />
										</p>
									</c:if>
									<c:if test="${not empty eventStateNotice}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<c:out value="${eventStateNotice}" />
										</p>
									</c:if>

									<c:if test="${joinRequested}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.joinRequest.requested" />
										</p>
									</c:if>
									<c:if test="${joinCancelled}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.joinRequest.cancelled" />
										</p>
									</c:if>
									<c:if test="${not empty joinError}">
										<p class="booking-panel__notice booking-panel__notice--error">
											<c:out value="${joinError}" />
										</p>
									</c:if>

									<spring:message var="joiningLabel" code="event.booking.joining" />
									<c:if test="${not hostViewer or isConfirmedParticipant or matchActionCapabilities.canReserve or seriesReservationEnabled or seriesCancellationEnabled}">
										<c:choose>
											<c:when test="${isConfirmedParticipant}">
												<c:if test="${reservationConfirmed}">
													<p class="booking-panel__notice booking-panel__notice--success">
														<spring:message code="event.booking.confirmed" />
													</p>
												</c:if>
												<c:if test="${matchActionCapabilities.canCancelReservation}">
													<c:url var="reservationCancelAction" value="${reservationCancelPath}" />
													<c:choose>
														<c:when test="${not empty occurrences}">
															<spring:message var="leavingReservationLabel" code="event.booking.leavingOccurrence" />
															<spring:message var="leaveReservationLabel" code="event.booking.leaveOccurrence" />
														</c:when>
														<c:otherwise>
															<spring:message var="leavingReservationLabel" code="event.booking.leaving" />
															<spring:message var="leaveReservationLabel" code="event.booking.leave" />
														</c:otherwise>
													</c:choose>
													<form method="post" action="${reservationCancelAction}" data-submit-guard="true" data-submit-loading-label="${leavingReservationLabel}" class="booking-panel__request-form">
														<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
														<ui:button label="${leaveReservationLabel}" type="submit" fullWidth="${true}" variant="danger" />
													</form>
												</c:if>
											</c:when>
											<c:when test="${reservationRequiresLogin}">
												<c:choose>
													<c:when test="${matchActionCapabilities.canReserve}">
														<spring:message var="signInToReserveLabel" code="event.booking.signIn" />
														<c:url var="loginHref" value="/login" />
														<ui:button label="${signInToReserveLabel}" href="${loginHref}" fullWidth="${true}" />
														<p class="booking-panel__note"><spring:message code="event.booking.signInNote" /></p>
													</c:when>
													<c:when test="${matchActionCapabilities.canRequestToJoin and not seriesJoinRequestPending}">
														<spring:message var="signInToRequestLabel" code="event.joinRequest.signIn" />
														<c:url var="loginHref" value="/login" />
														<ui:button label="${signInToRequestLabel}" href="${loginHref}" fullWidth="${true}" />
														<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
													</c:when>
													<c:otherwise>
														<ui:button label="${ctaLabel}" type="button" fullWidth="${true}" disabled="${true}" />
														<p class="booking-panel__note"><spring:message code="event.booking.note" /></p>
													</c:otherwise>
												</c:choose>
											</c:when>
											<c:when test="${hasPendingJoinRequest}">
												<c:url var="cancelJoinAction" value="${cancelJoinRequestPath}" />
												<spring:message var="cancellingLabel" code="event.joinRequest.cancelling" />
												<p class="booking-panel__notice booking-panel__notice--info">
													<spring:message code="event.joinRequest.pendingLabel" />
												</p>
												<form method="post" action="${cancelJoinAction}" data-submit-guard="true" data-submit-loading-label="${cancellingLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="cancelRequestLabel" code="event.joinRequest.cancelRequest" />
													<ui:button label="${cancelRequestLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
											</c:when>
											<c:when test="${matchActionCapabilities.canRequestToJoin and not seriesJoinRequestPending}">
												<c:url var="joinRequestAction" value="${joinRequestPath}" />
												<spring:message var="requestingLabel" code="event.joinRequest.requesting" />
												<form method="post" action="${joinRequestAction}" data-submit-guard="true" data-submit-loading-label="${requestingLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<c:choose>
														<c:when test="${not empty occurrences}">
															<spring:message var="requestToJoinLabel" code="event.joinRequest.requestThisOccurrence" />
														</c:when>
														<c:otherwise>
															<spring:message var="requestToJoinLabel" code="event.joinRequest.requestToJoin" />
														</c:otherwise>
													</c:choose>
													<ui:button label="${requestToJoinLabel}" type="submit" fullWidth="${true}" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
											</c:when>
											<c:when test="${matchActionCapabilities.canReserve}">
												<c:url var="reservationRequestAction" value="${reservationRequestPath}" />
												<form method="post" action="${reservationRequestAction}" data-submit-guard="true" data-submit-loading-label="${joiningLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<ui:button label="${ctaLabel}" type="submit" fullWidth="${true}" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.booking.note" /></p>
											</c:when>
											<c:when test="${isInvitedPlayer}">
												<c:if test="${inviteAccepted}">
													<p class="booking-panel__notice booking-panel__notice--success">
														<spring:message code="event.invite.accepted" />
													</p>
												</c:if>
												<c:if test="${not empty inviteError}">
													<p class="booking-panel__notice booking-panel__notice--error">
														<c:out value="${inviteError}" />
													</p>
												</c:if>
												<p class="booking-panel__notice booking-panel__notice--info" data-invite-refresh-notice="true">
													<spring:message code="event.invite.pendingLabel" />
												</p>
												<c:url var="acceptInviteAction" value="${acceptInvitePath}" />
												<spring:message var="acceptingInviteLabel" code="event.invite.accepting" />
												<form method="post" action="${acceptInviteAction}" data-submit-guard="true" data-submit-loading-label="${acceptingInviteLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="acceptInviteLabel" code="event.invite.accept" />
													<ui:button label="${acceptInviteLabel}" type="submit" fullWidth="${true}" />
												</form>
												<c:url var="declineInviteAction" value="${declineInvitePath}" />
												<spring:message var="decliningInviteLabel" code="event.invite.declining" />
												<form method="post" action="${declineInviteAction}" data-submit-guard="true" data-submit-loading-label="${decliningInviteLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="declineInviteLabel" code="event.invite.decline" />
													<ui:button label="${declineInviteLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
												<p class="booking-panel__note"><spring:message code="event.invite.note" /></p>
											</c:when>
											<c:otherwise>
												<ui:button label="${ctaLabel}" type="button" fullWidth="${true}" disabled="${true}" />
												<p class="booking-panel__note"><spring:message code="event.booking.note" /></p>
											</c:otherwise>
										</c:choose>
									</c:if>

									<c:if test="${seriesJoinRequested}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.recurringJoinRequest.requested" />
										</p>
									</c:if>
									<c:if test="${seriesJoinRequestPending and not seriesJoinRequested}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<spring:message code="event.recurringJoinRequest.pending" />
										</p>
									</c:if>
									<c:if test="${seriesJoinRequestEnabled and not seriesJoinRequestPending}">
										<c:choose>
											<c:when test="${seriesJoinRequestRequiresLogin}">
												<spring:message var="signInToRequestRecurringLabel" code="event.recurringJoinRequest.signIn" />
												<c:url var="recurringJoinRequestLoginHref" value="/login" />
												<ui:button label="${signInToRequestRecurringLabel}" href="${recurringJoinRequestLoginHref}" fullWidth="${true}" variant="secondary" />
											</c:when>
											<c:otherwise>
												<c:url var="recurringJoinRequestAction" value="${seriesJoinRequestPath}" />
												<spring:message var="requestingRecurringJoinLabel" code="event.recurringJoinRequest.requesting" />
												<form method="post" action="${recurringJoinRequestAction}" data-submit-guard="true" data-submit-loading-label="${requestingRecurringJoinLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="requestRecurringJoinLabel" code="event.recurringJoinRequest.cta" />
													<ui:button label="${requestRecurringJoinLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
											</c:otherwise>
										</c:choose>
										<p class="booking-panel__note"><spring:message code="event.recurringJoinRequest.note" /></p>
									</c:if>

									<c:if test="${seriesReservationConfirmed}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.recurringReservation.confirmed" />
										</p>
									</c:if>
									<c:if test="${seriesReservationCancelled}">
										<p class="booking-panel__notice booking-panel__notice--info">
											<spring:message code="event.recurringReservation.cancelled" />
										</p>
									</c:if>
									<c:if test="${seriesReservationJoined and not seriesReservationConfirmed}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.recurringReservation.joined" />
										</p>
									</c:if>
									<c:if test="${not empty seriesReservationError}">
										<p class="booking-panel__notice booking-panel__notice--error">
											<c:out value="${seriesReservationError}" />
										</p>
									</c:if>
									<c:if test="${seriesReservationEnabled and not seriesReservationJoined}">
										<c:choose>
											<c:when test="${seriesReservationRequiresLogin}">
												<spring:message var="signInToJoinRecurringLabel" code="event.recurringReservation.signIn" />
												<c:url var="recurringLoginHref" value="/login" />
												<ui:button label="${signInToJoinRecurringLabel}" href="${recurringLoginHref}" fullWidth="${true}" variant="secondary" />
											</c:when>
											<c:otherwise>
												<c:url var="recurringReservationAction" value="${seriesReservationPath}" />
												<spring:message var="joiningRecurringLabel" code="event.recurringReservation.joining" />
												<form method="post" action="${recurringReservationAction}" data-submit-guard="true" data-submit-loading-label="${joiningRecurringLabel}" class="booking-panel__request-form">
													<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
													<spring:message var="joinRecurringLabel" code="event.recurringReservation.cta" />
													<ui:button label="${joinRecurringLabel}" type="submit" fullWidth="${true}" variant="secondary" />
												</form>
											</c:otherwise>
										</c:choose>
										<p class="booking-panel__note"><spring:message code="event.recurringReservation.note" /></p>
									</c:if>
									<c:if test="${seriesCancellationEnabled}">
										<c:url var="recurringReservationCancelAction" value="${seriesReservationCancelPath}" />
										<spring:message var="leavingRecurringLabel" code="event.recurringReservation.leaving" />
										<form method="post" action="${recurringReservationCancelAction}" data-submit-guard="true" data-submit-loading-label="${leavingRecurringLabel}" class="booking-panel__request-form">
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<spring:message var="leaveRecurringLabel" code="event.recurringReservation.leave" />
											<ui:button label="${leaveRecurringLabel}" type="submit" fullWidth="${true}" variant="secondary" />
										</form>
									</c:if>
									<c:if test="${not empty pageContext.request.userPrincipal}">
										<hr class="booking-panel__divider" />
										<c:url var="reportMatchHref" value="/reports/matches/${event.id}" />
										<spring:message var="reportMatchLabel" code="moderation.report.match.menu" />
										<ui:button label="${reportMatchLabel}" href="${reportMatchHref}" variant="danger" fullWidth="${true}" className="booking-panel__report-button" />
									</c:if>
								</article>
							</c:otherwise>
						</c:choose>

					</aside>
				</section>

				<c:if test="${not empty occurrences}">
					<section class="detail-layout">
						<div class="detail-layout__main">
							<section class="panel detail-section recurrence-schedule" aria-labelledby="recurrence-schedule-title">
								<div class="section-head section-head--detail-compact">
									<div>
										<span class="detail-label"><spring:message code="event.recurrence.label" /></span>
										<h2 id="recurrence-schedule-title" class="detail-section__title">
											<spring:message code="event.recurrence.title" />
										</h2>
									</div>
								</div>
								<ul id="recurrence-schedule-list" class="recurrence-schedule__list">
									<c:forEach var="occurrence" items="${occurrences}">
										<li class="recurrence-schedule__item ${occurrence.id == event.id ? 'recurrence-schedule__item--current' : ''}">
											<div class="recurrence-schedule__date">
												<c:choose>
													<c:when test="${not empty occurrenceVisibleHrefs[occurrence.id]}">
														<c:url var="occurrenceHref" value="${occurrenceVisibleHrefs[occurrence.id]}" />
														<a class="recurrence-schedule__link" href="${occurrenceHref}">
															<c:out value="${tf:dateTime(occurrence.startsAtDateTime)}" />
														</a>
													</c:when>
													<c:otherwise>
														<span class="recurrence-schedule__text">
															<c:out value="${tf:dateTime(occurrence.startsAtDateTime)}" />
														</span>
													</c:otherwise>
												</c:choose>
											</div>
											<div class="recurrence-schedule__badges">
												<c:if test="${not empty occurrenceSpotsLabels[occurrence.id]}">
													<span class="recurrence-schedule__spots recurrence-schedule__spots--${occurrenceSpotsTones[occurrence.id]}">
														<c:out value="${occurrenceSpotsLabels[occurrence.id]}" />
													</span>
												</c:if>
												<c:if test="${not empty occurrenceDisplayStateKeys[occurrence.id]}">
													<span class="recurrence-schedule__status recurrence-schedule__status--${occurrenceStatusTones[occurrence.id]}">
														<spring:message code="match.status.${occurrenceDisplayStateKeys[occurrence.id]}" />
													</span>
												</c:if>
											</div>
										</li>
									</c:forEach>
								</ul>
								<c:if test="${not empty recurrencePaginationItems}">
									<spring:message var="previousLabel" code="pagination.previous" />
									<spring:message var="nextLabel" code="pagination.next" />
									<spring:message var="recurrenceScheduleTitle" code="event.recurrence.pagination.aria" />
									<section class="feed-pagination" aria-label="${recurrenceScheduleTitle}">
										<nav class="feed-pagination__nav" aria-label="${recurrenceScheduleTitle}">
											<div>
												<c:choose>
													<c:when test="${recurrenceHasPreviousPage}">
														<c:url var="recurrencePrevHref" value="${recurrencePreviousPageHref}" />
														<a class="feed-pagination__control" href="${recurrencePrevHref}">
															<c:out value="${previousLabel}" />
														</a>
													</c:when>
													<c:otherwise>
														<span class="feed-pagination__control feed-pagination__control--disabled">
															<c:out value="${previousLabel}" />
														</span>
													</c:otherwise>
												</c:choose>
											</div>
											<div class="feed-pagination__pages">
												<c:forEach var="item" items="${recurrencePaginationItems}">
													<c:choose>
														<c:when test="${item.ellipsis}">
															<span class="feed-pagination__ellipsis" aria-hidden="true">${item.label}</span>
														</c:when>
														<c:when test="${item.current}">
															<span class="feed-pagination__page feed-pagination__page--current" aria-current="page">
																<c:out value="${item.label}" />
															</span>
														</c:when>
														<c:otherwise>
															<c:url var="recurrencePageHref" value="${item.href}" />
															<a class="feed-pagination__page" href="${recurrencePageHref}">
																<c:out value="${item.label}" />
															</a>
														</c:otherwise>
													</c:choose>
												</c:forEach>
											</div>
											<div>
												<c:choose>
													<c:when test="${recurrenceHasNextPage}">
														<c:url var="recurrenceNextHref" value="${recurrenceNextPageHref}" />
														<a class="feed-pagination__control" href="${recurrenceNextHref}">
															<c:out value="${nextLabel}" />
														</a>
													</c:when>
													<c:otherwise>
														<span class="feed-pagination__control feed-pagination__control--disabled">
															<c:out value="${nextLabel}" />
														</span>
													</c:otherwise>
												</c:choose>
											</div>
										</nav>
									</section>
								</c:if>
							</section>
						</div>
					</section>
				</c:if>

				<section class="detail-recommendations">
					<div class="section-head">
						<div>
							<h2
								class="section-head__title section-head__title--detail"
							>
								<spring:message code="event.nearby.title" />
							</h2>
						</div>
						<c:url var="nearbyViewAllHref" value="/" />
						<a
							class="section-link"
							href="${nearbyViewAllHref}"
							><spring:message code="event.nearby.viewAll" /></a
						>
					</div>

					<div class="event-grid">
						<c:forEach
							var="event"
							items="${nearbyEvents}"
						>
							<ui:eventCard match="${event}" headingLevel="h3" />
						</c:forEach>
					</div>
				</section>
			</main>
		</div>
	</body>
</html>
