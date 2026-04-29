<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %> <%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="spring" uri="http://www.springframework.org/tags" %> <%@ taglib
prefix="ui" tagdir="/WEB-INF/tags" %>
<spring:message var="pageTitle" code="page.title.adminReports" />
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>

			<main class="page-shell page-shell--matches-list">
				<header class="page-heading">
					<h1 class="page-heading__title">
						<c:out value="${pageTitleLabel}" />
					</h1>
					<p class="page-heading__description">
						<c:out value="${pageDescription}" />
					</p>
				</header>

				<c:if test="${not empty param.action}">
					<div class="notice notice--success">
						<c:choose>
							<c:when test="${param.action eq 'reviewed'}">
								<spring:message
									code="admin.reports.action.reviewed"
								/>
							</c:when>
							<c:when test="${param.action eq 'dismissed'}">
								<spring:message
									code="admin.reports.action.dismissed"
								/>
							</c:when>
							<c:when test="${param.action eq 'warned'}">
								<spring:message
									code="admin.reports.action.warned"
								/>
							</c:when>
							<c:when test="${param.action eq 'deleted'}">
								<spring:message
									code="admin.reports.action.deleted"
								/>
							</c:when>
							<c:when test="${param.action eq 'banned'}">
								<spring:message
									code="admin.reports.action.banned"
								/>
							</c:when>
							<c:when test="${param.action eq 'ban_appeal_resolved'}">
								<spring:message
									code="admin.reports.action.ban_appeal_resolved"
								/>
							</c:when>
							<c:otherwise>
								<spring:message
									code="admin.reports.action.reviewed"
								/>
							</c:otherwise>
						</c:choose>
					</div>
				</c:if>
				<c:if test="${not empty param.error}">
					<div class="notice notice--error">
						<c:choose>
							<c:when test="${param.error eq 'report_not_found'}">
								<spring:message
									code="admin.reports.error.report_not_found"
								/>
							</c:when>
							<c:otherwise>
								<spring:message
									code="admin.reports.error.action_failed"
								/>
							</c:otherwise>
						</c:choose>
					</div>
				</c:if>

				<section class="panel participation-panel">
					<div
						class="panel__header"
						style="
							display: flex;
							justify-content: space-between;
							gap: 1rem;
							align-items: flex-start;
						"
					>
						<div>
							<strong
								><c:out value="${activeReportCountLabel}"
							/></strong>
						</div>
					</div>

					<c:choose>
						<c:when test="${empty reports}">
							<p class="participation-empty-state">
								<c:out value="${emptyMessage}" />
							</p>
						</c:when>
						<c:otherwise>
							<ul class="participant-manage-list">
								<c:forEach var="report" items="${reports}">
									<li class="participant-manage-list__item">
										<div
											class="participant-manage-list__info"
											style="
												flex-direction: column;
												align-items: flex-start;
												gap: 0.75rem;
											"
										>
											<div>
												<strong
													>#<c:out
														value="${report.id}"
												/></strong>
												<span>
													·
													<spring:message
														code="admin.reports.reporter" />
													<c:out
														value="${report.reporterUserId}"
												/></span>
											</div>
											<div>
												<spring:message
													var="targetTypeLabel"
													code="admin.reports.targetType.${report.targetTypeCode}"
												/>
												<spring:message
													var="reasonLabel"
													code="admin.reports.reason.${report.reasonCode}"
												/>
												<spring:message
													var="statusLabel"
													code="admin.reports.status.${report.statusCode}"
												/>
												<span class="chip"
													><c:out
														value="${targetTypeLabel}"
												/></span>
												<span class="chip"
													><c:out
														value="${reasonLabel}"
												/></span>
												<span class="chip"
													><c:out
														value="${statusLabel}"
												/></span>
											</div>
											<div>
												<spring:message
													code="admin.reports.target"
												/>:
												<c:out
													value="${report.targetKey}"
												/>
											</div>
											<div>
												<spring:message
													code="admin.reports.details"
												/>:
												<c:out
													value="${report.details}"
												/>
											</div>
											<div>
												<spring:message
													code="admin.reports.createdAt"
												/>:
												<c:out
													value="${report.createdAtLabel}"
												/>
											</div>
											<c:if
												test="${not empty report.appealReason}"
											>
												<div>
													<spring:message
														code="admin.reports.appeal"
													/>:
													<c:out
														value="${report.appealReason}"
													/>
												</div>
											</c:if>
										</div>
										<div
											class="participant-manage-list__actions"
										>
											<c:url
												var="viewDetailHref"
												value="/admin/reports/${report.id}"
											/>
											<spring:message
												var="viewDetailLabel"
												code="admin.reports.action.view"
											/>
											<ui:button
												label="${viewDetailLabel}"
												href="${viewDetailHref}"
												variant="secondary"
											/>
											<c:if
												test="${report.statusCode eq 'pending' or report.statusCode eq 'under_review'}"
											>
												<c:url
													var="reviewHref"
													value="/admin/reports/${report.id}/under-review"
												/>
												<form
													method="post"
													action="${reviewHref}"
													class="participant-manage-list__action-form"
												>
													<input
														type="hidden"
														name="${_csrf.parameterName}"
														value="${_csrf.token}"
													/>
													<spring:message
														var="reviewLabel"
														code="admin.reports.action.review"
													/>
													<ui:button
														label="${reviewLabel}"
														type="submit"
														variant="secondary"
													/>
												</form>
												<c:url
													var="dismissHref"
													value="/admin/reports/${report.id}/dismiss"
												/>
												<form
													method="post"
													action="${dismissHref}"
													class="participant-manage-list__action-form"
												>
													<input
														type="hidden"
														name="${_csrf.parameterName}"
														value="${_csrf.token}"
													/>
													<spring:message
														var="dismissLabel"
														code="admin.reports.action.dismiss"
													/>
													<ui:button
														label="${dismissLabel}"
														type="submit"
														variant="secondary"
													/>
												</form>
												<c:if
													test="${report.targetTypeCode eq 'match' or report.targetTypeCode eq 'review'}"
												>
													<c:url
														var="deleteContentHref"
														value="/admin/reports/${report.id}/delete-content"
													/>
													<form
														method="post"
														action="${deleteContentHref}"
														class="participant-manage-list__action-form"
													>
														<input
															type="hidden"
															name="${_csrf.parameterName}"
															value="${_csrf.token}"
														/>
														<spring:message
															var="deleteContentLabel"
															code="admin.reports.action.deleteContent"
														/>
														<ui:button
															label="${deleteContentLabel}"
															type="submit"
															variant="danger"
														/>
													</form>
												</c:if>
												<c:if
													test="${report.targetTypeCode eq 'user'}"
												>
													<c:url
														var="banUserHref"
														value="/admin/reports/${report.id}/ban-user"
													/>
														<form
															method="post"
															action="${banUserHref}"
															class="participant-manage-list__action-form"
														>
															<input
																type="hidden"
																name="${_csrf.parameterName}"
																value="${_csrf.token}"
															/>
															<input
																type="number"
																name="banDays"
																min="1"
																max="365"
																value="7"
																class="field__control"
															/>
															<spring:message
																var="banReasonPlaceholder"
																code="admin.reports.ban.reasonPlaceholder"
															/>
															<input
																type="text"
																name="banReason"
																maxlength="2000"
																class="field__control"
																placeholder="${banReasonPlaceholder}"
															/>
															<spring:message
																var="banUserLabel"
																code="admin.reports.action.banUser"
															/>
														<ui:button
															label="${banUserLabel}"
															type="submit"
															variant="danger"
														/>
													</form>
												</c:if>
											</c:if>
											<c:if test="${report.appealed}">
												<div
													class="panel"
													style="
														width: 100%;
														padding: 1rem;
														display: grid;
														gap: 0.75rem;
													"
												>
													<p
														class="participation-empty-state"
													>
														<spring:message
															code="admin.reports.appeal.pending"
														/>
													</p>
													<c:url
														var="finalizeAppealHref"
														value="/admin/reports/${report.id}/finalize-appeal"
													/>
													<form
														method="post"
														action="${finalizeAppealHref}"
														class="stack"
													>
														<input
															type="hidden"
															name="${_csrf.parameterName}"
															value="${_csrf.token}"
														/>
														<spring:message
															var="appealResolutionLabel"
															code="admin.reports.appeal.resolution.label"
														/>
														<label
															class="field"
															for="appeal-resolution-${report.id}"
														>
															<span
																class="field__label"
																><c:out
																	value="${appealResolutionLabel}"
															/></span>
															<select
																id="appeal-resolution-${report.id}"
																name="appealResolution"
																class="field__control field__control--select"
															>
																<option
																	value="dismissed"
																>
																	<spring:message
																		code="admin.reports.appeal.resolution.dismissed"
																	/>
																</option>
																<option
																	value="warning"
																>
																	<spring:message
																		code="admin.reports.appeal.resolution.warning"
																	/>
																</option>
																<option
																	value="content_deleted"
																>
																	<spring:message
																		code="admin.reports.appeal.resolution.content_deleted"
																	/>
																</option>
																<option
																	value="user_banned"
																>
																	<spring:message
																		code="admin.reports.appeal.resolution.user_banned"
																	/>
																</option>
															</select>
														</label>
														<div>
															<spring:message
																var="finalizeAppealLabel"
																code="admin.reports.appeal.finalize"
															/>
															<ui:button
																label="${finalizeAppealLabel}"
																type="submit"
															/>
														</div>
													</form>
												</div>
											</c:if>
										</div>
									</li>
								</c:forEach>
							</ul>
						</c:otherwise>
					</c:choose>
				</section>
			</main>
		</div>
	</body>
</html>
