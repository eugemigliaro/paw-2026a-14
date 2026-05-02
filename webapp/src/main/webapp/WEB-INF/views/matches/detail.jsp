<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.eventDetail" arguments="${eventPage.event.title}" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>
			<spring:message var="eventSummaryAria" code="event.detail.summaryAria" />
			<spring:message var="participantsAria" code="event.detail.participantsAria" />

			<main class="page-shell page-shell--detail">
				<section class="detail-top">
					<section
						class="event-hero ${eventPage.event.mediaClass} ${not empty eventPage.event.bannerImageUrl ? 'event-hero--with-image' : ''}"
					>
						<c:if test="${not empty eventPage.event.bannerImageUrl}">
							<c:url var="eventHeroBannerSrc" value="${eventPage.event.bannerImageUrl}" />
							<img
								class="event-hero__image"
								src="${eventHeroBannerSrc}"
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
								aria-label="${eventSummaryAria}"
							>
								<li class="event-hero__status-item">
									<span class="detail-label"><spring:message code="event.detail.when" /></span>
									<strong
										><c:out value="${eventPage.event.schedule}"
									/></strong>
								</li>
								<li class="event-hero__status-item">
									<span class="detail-label"><spring:message code="event.detail.where" /></span>
									<strong
										><c:out value="${eventPage.event.venue}"
									/></strong>
								</li>
								<li class="event-hero__status-item">
									<span class="detail-label"><spring:message code="event.detail.group" /></span>
									<strong
										><c:out
											value="${eventPage.participantCountLabel}"
									/></strong>
								</li>
								<li class="event-hero__status-item">
									<span class="detail-label"><spring:message code="event.detail.entry" /></span>
									<strong
										><c:out value="${eventPage.bookingPrice}"
									/></strong>
								</li>
							</ul>
						</div>
					</section>

					<aside class="detail-top__sidebar">
						<article class="panel booking-panel">
							<div class="booking-panel__header">
								<div class="booking-panel__header-copy">
									<span class="detail-label"
										><spring:message code="event.booking.reserveSpot" /></span
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
									<spring:message code="event.booking.confirmed" />
								</p>
							</c:if>
							<c:if test="${reservationCancelled}">
								<p
									class="booking-panel__notice booking-panel__notice--info"
								>
									<c:choose>
										<c:when test="${not empty eventPage.occurrences}">
											<spring:message code="event.booking.occurrenceCancelled" />
										</c:when>
										<c:otherwise>
											<spring:message code="event.booking.cancelled" />
										</c:otherwise>
									</c:choose>
								</p>
							</c:if>
							<c:if test="${not empty hostActionNotice}">
								<p
									class="booking-panel__notice booking-panel__notice--success"
								>
									<c:out value="${hostActionNotice}" />
								</p>
							</c:if>
							<c:if test="${not empty reservationError}">
								<p
									class="booking-panel__notice booking-panel__notice--error"
								>
									<c:out value="${reservationError}" />
								</p>
							</c:if>
							<c:if test="${not empty eventStateNotice}">
								<p class="booking-panel__notice booking-panel__notice--info">
									<c:out value="${eventStateNotice}" />
								</p>
							</c:if>

							<div class="booking-panel__availability">
								<div>
									<span class="detail-label"
										><spring:message code="event.booking.availability" /></span
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

							<c:if test="${hostCanManage}">
								<spring:message var="hostManageEditLabel" code="host.manage.edit" />
								<c:choose>
									<c:when test="${not empty eventPage.occurrences}">
										<spring:message var="hostManageEditLabel" code="host.manage.editOccurrence" />
										<spring:message var="hostManageEditSeriesLabel" code="host.manage.editSeries" />
										<spring:message var="hostManageCancelLabel" code="host.manage.cancelOccurrence" />
										<spring:message var="hostManageCancelSeriesLabel" code="host.manage.cancelSeries" />
										<spring:message var="hostManageDetailLabel" code="host.manage.detailOccurrence" />
									</c:when>
									<c:otherwise>
										<spring:message var="hostManageCancelLabel" code="host.manage.cancel" />
										<spring:message var="hostManageDetailLabel" code="host.manage.detail" />
									</c:otherwise>
								</c:choose>
								<spring:message var="hostManageCancellingLabel" code="host.manage.cancelling" />
								<spring:message var="hostManageMenuLabel" code="host.manage.menu" />
								<spring:message var="hostManageMenuTriggerLabel" code="host.manage.menu.trigger" />
								<ui:overflowMenu
									ariaLabel="${hostManageMenuTriggerLabel}"
									menuAriaLabel="${hostManageMenuLabel}"
									className="booking-panel__overflow-menu">
									<c:url var="hostEditHref" value="${hostEditPath}" />
									<c:choose>
										<c:when test="${hostCanEdit}">
											<a class="overflow-menu__item" href="${hostEditHref}" role="menuitem">
												<c:out value="${hostManageEditLabel}" />
											</a>
										</c:when>
										<c:otherwise>
											<span
												class="overflow-menu__item overflow-menu__item--disabled"
												role="menuitem"
												aria-disabled="true">
												<c:out value="${hostManageEditLabel}" />
											</span>
										</c:otherwise>
									</c:choose>
									<c:if test="${not empty eventPage.occurrences}">
										<c:url var="hostSeriesEditHref" value="${hostSeriesEditPath}" />
										<c:choose>
											<c:when test="${hostCanEditSeries}">
												<a class="overflow-menu__item" href="${hostSeriesEditHref}" role="menuitem">
													<c:out value="${hostManageEditSeriesLabel}" />
												</a>
											</c:when>
											<c:otherwise>
												<span
													class="overflow-menu__item overflow-menu__item--disabled"
													role="menuitem"
													aria-disabled="true">
													<c:out value="${hostManageEditSeriesLabel}" />
												</span>
											</c:otherwise>
										</c:choose>
									</c:if>
									<c:url var="hostCancelAction" value="${hostCancelPath}" />
									<form
										method="post"
										action="${hostCancelAction}"
										data-submit-guard="true"
										data-submit-loading-label="${hostManageCancellingLabel}">
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<c:choose>
											<c:when test="${hostCanCancel}">
												<button
													class="overflow-menu__item overflow-menu__item--danger"
													type="submit"
													role="menuitem">
													<c:out value="${hostManageCancelLabel}" />
												</button>
											</c:when>
											<c:otherwise>
												<button
													class="overflow-menu__item overflow-menu__item--danger"
													type="submit"
													role="menuitem"
													disabled="disabled"
													aria-disabled="true">
													<c:out value="${hostManageCancelLabel}" />
												</button>
											</c:otherwise>
										</c:choose>
									</form>
									<c:if test="${not empty eventPage.occurrences}">
										<c:url var="hostSeriesCancelAction" value="${hostSeriesCancelPath}" />
										<form
											method="post"
											action="${hostSeriesCancelAction}"
											data-submit-guard="true"
											data-submit-loading-label="${hostManageCancellingLabel}">
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<c:choose>
												<c:when test="${hostCanCancelSeries}">
													<button
														class="overflow-menu__item overflow-menu__item--danger"
														type="submit"
														role="menuitem">
														<c:out value="${hostManageCancelSeriesLabel}" />
													</button>
												</c:when>
												<c:otherwise>
													<button
														class="overflow-menu__item overflow-menu__item--danger"
														type="submit"
														role="menuitem"
														disabled="disabled"
														aria-disabled="true">
														<c:out value="${hostManageCancelSeriesLabel}" />
													</button>
												</c:otherwise>
											</c:choose>
										</form>
									</c:if>
								</ui:overflowMenu>
								<div class="booking-panel__host-note">
									<p class="detail-label"><spring:message code="host.manage.label" /></p>
									<p><c:out value="${hostManageDetailLabel}" /></p>
								</div>
							</c:if>

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

							<c:if test="${joinRequested}">
								<p class="booking-panel__notice booking-panel__notice--success">
									<spring:message code="event.joinRequest.requested" />
								</p>
							</c:if>
							<c:if test="${joinCancelled}">
								<p class="booking-panel__notice booking-panel__notice--info">
									<spring:message code="event.joinRequest.cancelled" />
								</p>
							</c:if>
							<c:if test="${not empty joinError}">
								<p class="booking-panel__notice booking-panel__notice--error">
									<c:out value="${joinError}" />
								</p>
							</c:if>

							<spring:message var="joiningLabel" code="event.booking.joining" />
							<c:if test="${hostViewer}">
								<c:choose>
									<c:when test="${isInviteOnly}">
										<c:url var="hostInvitesHref" value="${hostInvitesPath}" />
										<spring:message var="hostInvitesLabel" code="event.host.invites" />
										<ui:button
											label="${hostInvitesLabel}"
											href="${hostInvitesHref}"
											fullWidth="${true}"
											disabled="${not hostCanManageParticipants}" />
									</c:when>
									<c:when test="${isApprovalRequired}">
										<c:url var="hostRequestsHref" value="${hostRequestsPath}" />
										<spring:message var="hostRequestsLabel" code="event.host.requests" />
										<ui:button
											label="${hostRequestsLabel}"
											href="${hostRequestsHref}"
											fullWidth="${true}"
											disabled="${not hostCanManageParticipants}" />
									</c:when>
								</c:choose>
								<c:url var="hostParticipantsHref" value="${hostParticipantsPath}" />
								<spring:message var="hostParticipantsLabel" code="event.host.participants" />
								<ui:button
									label="${hostParticipantsLabel}"
									href="${hostParticipantsHref}"
									fullWidth="${true}"
									variant="secondary"
									disabled="${not hostCanManageParticipants}" />
							</c:if>

							<spring:message var="joiningLabel" code="event.booking.joining" />
							<c:if test="${not hostViewer or isConfirmedParticipant or reservationEnabled or seriesReservationEnabled or seriesCancellationEnabled}">
								<c:choose>
									<c:when test="${isConfirmedParticipant}">
										<p class="booking-panel__notice booking-panel__notice--success">
											<spring:message code="event.booking.confirmed" />
										</p>
										<c:if test="${reservationCancellationEnabled}">
											<c:url var="reservationCancelAction" value="${reservationCancelPath}" />
											<c:choose>
												<c:when test="${not empty eventPage.occurrences}">
													<spring:message var="leavingReservationLabel" code="event.booking.leavingOccurrence" />
													<spring:message var="leaveReservationLabel" code="event.booking.leaveOccurrence" />
												</c:when>
												<c:otherwise>
													<spring:message var="leavingReservationLabel" code="event.booking.leaving" />
													<spring:message var="leaveReservationLabel" code="event.booking.leave" />
												</c:otherwise>
											</c:choose>
											<form
												method="post"
												action="${reservationCancelAction}"
												data-submit-guard="true"
												data-submit-loading-label="${leavingReservationLabel}"
												class="booking-panel__request-form"
											>
												<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
												<ui:button label="${leaveReservationLabel}" type="submit" fullWidth="${true}" variant="danger" />
											</form>
										</c:if>
									</c:when>
									<c:when test="${reservationRequiresLogin}">
										<c:choose>
											<c:when test="${reservationEnabled}">
												<spring:message var="signInToReserveLabel" code="event.booking.signIn" />
												<c:url var="loginHref" value="/login" />
												<ui:button label="${signInToReserveLabel}" href="${loginHref}" fullWidth="${true}" />
												<p class="booking-panel__note"><spring:message code="event.booking.signInNote" /></p>
											</c:when>
											<c:when test="${joinRequestEnabled}">
												<spring:message var="signInToRequestLabel" code="event.joinRequest.signIn" />
												<c:url var="loginHref" value="/login" />
												<ui:button label="${signInToRequestLabel}" href="${loginHref}" fullWidth="${true}" />
												<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
											</c:when>
											<c:otherwise>
												<ui:button label="${eventPage.ctaLabel}" type="button" fullWidth="${true}" disabled="${true}" />
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
										<form
											method="post"
											action="${cancelJoinAction}"
											data-submit-guard="true"
											data-submit-loading-label="${cancellingLabel}"
											class="booking-panel__request-form"
										>
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<spring:message var="cancelRequestLabel" code="event.joinRequest.cancelRequest" />
											<ui:button label="${cancelRequestLabel}" type="submit" fullWidth="${true}" variant="secondary" />
										</form>
										<p class="booking-panel__note"><spring:message code="event.joinRequest.inviteOnlyNote" /></p>
									</c:when>
									<c:when test="${joinRequestEnabled}">
										<c:url var="joinRequestAction" value="${joinRequestPath}" />
										<spring:message var="requestingLabel" code="event.joinRequest.requesting" />
										<form
											method="post"
											action="${joinRequestAction}"
											data-submit-guard="true"
											data-submit-loading-label="${requestingLabel}"
											class="booking-panel__request-form"
										>
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<c:choose>
												<c:when test="${not empty eventPage.occurrences}">
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
									<c:when test="${reservationEnabled}">
										<c:url var="reservationRequestAction" value="${reservationRequestPath}" />
										<form
											method="post"
											action="${reservationRequestAction}"
											data-submit-guard="true"
											data-submit-loading-label="${joiningLabel}"
											class="booking-panel__request-form"
										>
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<ui:button label="${eventPage.ctaLabel}" type="submit" fullWidth="${true}" />
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
										<p class="booking-panel__notice booking-panel__notice--info">
											<spring:message code="event.invite.pendingLabel" />
										</p>
										<c:url var="acceptInviteAction" value="${acceptInvitePath}" />
										<spring:message var="acceptingInviteLabel" code="event.invite.accepting" />
										<form
											method="post"
											action="${acceptInviteAction}"
											data-submit-guard="true"
											data-submit-loading-label="${acceptingInviteLabel}"
											class="booking-panel__request-form"
										>
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<spring:message var="acceptInviteLabel" code="event.invite.accept" />
											<ui:button label="${acceptInviteLabel}" type="submit" fullWidth="${true}" />
										</form>
										<c:url var="declineInviteAction" value="${declineInvitePath}" />
										<spring:message var="decliningInviteLabel" code="event.invite.declining" />
										<form
											method="post"
											action="${declineInviteAction}"
											data-submit-guard="true"
											data-submit-loading-label="${decliningInviteLabel}"
											class="booking-panel__request-form"
										>
											<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
											<spring:message var="declineInviteLabel" code="event.invite.decline" />
											<ui:button label="${declineInviteLabel}" type="submit" fullWidth="${true}" variant="secondary" />
										</form>
										<p class="booking-panel__note"><spring:message code="event.invite.note" /></p>
									</c:when>
									<c:otherwise>
										<ui:button label="${eventPage.ctaLabel}" type="button" fullWidth="${true}" disabled="${true}" />
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
										<form
											method="post"
											action="${recurringJoinRequestAction}"
											data-submit-guard="true"
											data-submit-loading-label="${requestingRecurringJoinLabel}"
											class="booking-panel__request-form"
										>
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
										<form
											method="post"
											action="${recurringReservationAction}"
											data-submit-guard="true"
											data-submit-loading-label="${joiningRecurringLabel}"
											class="booking-panel__request-form"
										>
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
								<form
									method="post"
									action="${recurringReservationCancelAction}"
									data-submit-guard="true"
									data-submit-loading-label="${leavingRecurringLabel}"
									class="booking-panel__request-form"
								>
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<spring:message var="leaveRecurringLabel" code="event.recurringReservation.leave" />
									<ui:button label="${leaveRecurringLabel}" type="submit" fullWidth="${true}" variant="secondary" />
								</form>
							</c:if>
						</article>
					</aside>
				</section>

				<section class="detail-layout">
					<div class="detail-layout__main">
						<article class="panel host-card">
							<div class="host-card__main">
								<c:url var="hostProfileImageSrc" value="${eventPage.hostProfileImageUrl}" />
								<img
									class="host-card__avatar"
									src="${hostProfileImageSrc}"
									alt=""
									aria-hidden="true"
									loading="lazy"
									decoding="async" />
								<div class="host-card__copy">
									<span class="detail-label"><spring:message code="event.detail.hostedBy" /></span>
									<c:choose>
										<c:when test="${not empty eventPage.hostProfileHref}">
											<c:url var="hostProfileHref" value="${eventPage.hostProfileHref}" />
											<a class="host-card__name" href="${hostProfileHref}">
												<c:out value="${eventPage.hostLabel}" />
											</a>
										</c:when>
										<c:otherwise>
											<strong class="host-card__name"><c:out value="${eventPage.hostLabel}" /></strong>
										</c:otherwise>
									</c:choose>
								</div>
							</div>
						</article>

						<section
							class="detail-section detail-section--about"
							aria-labelledby="about-event-title"
						>
							<div
								class="section-head section-head--detail-compact"
							>
								<div>
									<span class="detail-label"><spring:message code="event.detail.overview" /></span>
									<h2
										id="about-event-title"
										class="detail-section__title"
									>
										<spring:message code="event.detail.aboutEvent" />
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

						<section
							class="detail-section detail-section--participants"
							aria-labelledby="participant-section-title"
						>
							<div
								class="section-head section-head--detail-compact"
							>
								<div>
									<h2
										id="participant-section-title"
										class="detail-section__title"
									>
										<spring:message code="event.detail.whosJoining" />
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
											<spring:message code="event.detail.noPlayers" />
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
										aria-label="${participantsAria}"
									>
										<c:forEach
											var="participant"
											items="${eventPage.participants}"
										>
											<li class="participant-list__item">
												<c:url var="participantProfileImageSrc" value="${participant.profileImageUrl}" />
												<img
													class="participant-list__avatar"
													src="${participantProfileImageSrc}"
													alt=""
													aria-hidden="true"
													loading="lazy"
													decoding="async" />
												<div
													class="participant-list__copy"
												>
													<c:url var="participantProfileHref" value="${participant.profileHref}" />
													<a
														class="participant-list__name"
														href="${participantProfileHref}"
														><c:out
															value="${participant.username}"
													/></a>
													<c:if test="${not empty participant.reviewHref}">
														<c:url var="participantReviewHref" value="${participant.reviewHref}" />
														<a class="participant-list__review-link" href="${participantReviewHref}">
															<spring:message code="event.participants.review" />
														</a>
													</c:if>
												</div>
											</li>
										</c:forEach>
									</ul>
								</c:otherwise>
							</c:choose>
						</section>

						<c:if test="${not empty eventPage.occurrences}">
							<c:set var="recurrencePreviewLimit" value="3" />
							<c:set var="recurrenceScheduleCollapsed" value="${fn:length(eventPage.occurrences) > recurrencePreviewLimit}" />
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
									<c:forEach var="occurrence" items="${eventPage.occurrences}" varStatus="occurrenceStatus">
										<li
											class="recurrence-schedule__item ${occurrence.current ? 'recurrence-schedule__item--current' : ''}"
											<c:if test="${recurrenceScheduleCollapsed and occurrenceStatus.index >= recurrencePreviewLimit and not occurrence.current}">
												hidden="hidden"
												data-recurrence-extra-date="true"
											</c:if>>
											<div class="recurrence-schedule__date">
												<c:choose>
													<c:when test="${not empty occurrence.href}">
														<c:url var="occurrenceHref" value="${occurrence.href}" />
														<a class="recurrence-schedule__link" href="${occurrenceHref}">
															<c:out value="${occurrence.schedule}" />
														</a>
													</c:when>
													<c:otherwise>
														<span class="recurrence-schedule__text">
															<c:out value="${occurrence.schedule}" />
														</span>
													</c:otherwise>
												</c:choose>
											</div>
											<div class="recurrence-schedule__badges">
												<c:if test="${not empty occurrence.statusLabel}">
													<span class="recurrence-schedule__status recurrence-schedule__status--${occurrence.statusTone}">
														<c:out value="${occurrence.statusLabel}" />
													</span>
												</c:if>
											</div>
										</li>
									</c:forEach>
								</ul>
								<c:if test="${recurrenceScheduleCollapsed}">
									<spring:message var="showMoreRecurringDatesLabel" code="event.recurrence.showMore" />
									<spring:message var="showLessRecurringDatesLabel" code="event.recurrence.showLess" />
									<button
										type="button"
										class="btn btn--secondary btn--sm recurrence-schedule__toggle"
										data-recurrence-toggle="true"
										data-show-more-label="${showMoreRecurringDatesLabel}"
										data-show-less-label="${showLessRecurringDatesLabel}"
										aria-controls="recurrence-schedule-list"
										aria-expanded="false">
										<c:out value="${showMoreRecurringDatesLabel}" />
									</button>
								</c:if>
							</section>
						</c:if>

					</div>

				</section>

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
							items="${eventPage.nearbyEvents}"
						>
							<c:url var="nearbyCardHref" value="${event.href}" />
							<ui:card
								href="${nearbyCardHref}"
								className="event-card"
								ariaLabel="${event.title}"
							>
								<div
									class="event-card__media ${event.mediaClass}"
								>
									<c:if
										test="${not empty event.bannerImageUrl}"
									>
										<c:url var="nearbyBannerSrc" value="${event.bannerImageUrl}" />
										<img
											class="event-card__image"
											src="${nearbyBannerSrc}"
											alt=""
											loading="lazy"
											decoding="async"
										/>
									</c:if>
									<div class="event-card__media-badges">
										<span class="event-card__badge"
											><c:out value="${event.badge}"
										/></span>
										<c:forEach var="relationshipBadge" items="${event.relationshipBadges}">
											<span class="event-badge event-badge--${relationshipBadge.type}">
												<c:out value="${relationshipBadge.label}" />
											</span>
										</c:forEach>
									</div>
								</div>

								<div class="event-card__body">
									<div class="event-card__sport-row">
										<span class="event-card__sport"
											><c:out value="${event.sport}"
										/></span>
										<c:if test="${event.recurring}">
											<span class="event-card__recurring">
												<c:out value="${event.recurringLabel}" />
											</span>
										</c:if>
									</div>
									<h3 class="event-card__title">
										<c:out value="${event.title}" />
									</h3>
									<div class="event-card__meta">
										<span class="event-card__meta-item">
											<span class="event-card__meta-icon" aria-hidden="true">
												<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<path d="M12 22s8-4.35 8-11a8 8 0 1 0-16 0c0 6.65 8 11 8 11z" />
													<circle cx="12" cy="11" r="3" />
												</svg>
											</span>
											<c:out value="${event.venue}" />
										</span>
										<span class="event-card__meta-item">
											<span class="event-card__meta-icon" aria-hidden="true">
												<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
													<line x1="16" y1="2" x2="16" y2="6" />
													<line x1="8" y1="2" x2="8" y2="6" />
													<line x1="3" y1="10" x2="21" y2="10" />
												</svg>
											</span>
											<c:out value="${empty event.dateLabel ? event.schedule : event.dateLabel}" />
										</span>
										<c:if test="${not empty event.timeLabel}">
											<span class="event-card__meta-item">
												<span class="event-card__meta-icon" aria-hidden="true">
													<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
														<circle cx="12" cy="12" r="10" />
														<polyline points="12 6 12 12 16 14" />
													</svg>
												</span>
												<c:out value="${event.timeLabel}" />
											</span>
										</c:if>
									</div>

									<div class="event-card__footer">
										<div class="event-card__cta">
											<span><c:out value="${event.priceLabel}" /></span>
										</div>
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
