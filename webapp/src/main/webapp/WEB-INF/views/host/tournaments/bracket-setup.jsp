<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
	<head>
		<%@ include file="/WEB-INF/views/includes/head.jspf" %>
	</head>
	<body>
		<div class="app-shell">
			<%@ include file="/WEB-INF/views/includes/site-header.jspf" %>
			<c:url var="detailHref" value="${tournamentDetailPath}" />
			<c:url var="generateAction" value="${generateBracketPath}" />

			<main class="page-shell tournament-bracket-page">
				<ui:returnButton href="${detailHref}" />
				<header class="page-heading tournament-bracket-page__heading">
					<div>
						<span class="tournament-status tournament-status--${bracketPage.statusTone}">
							<c:out value="${bracketPage.statusLabel}" />
						</span>
						<h1 class="page-heading__title"><c:out value="${bracketPage.title}" /></h1>
						<p class="page-heading__description">
							<spring:message code="tournament.bracket.setup.description" />
						</p>
					</div>
					<c:if test="${not bracketPage.generated}">
						<spring:message var="generateLabel" code="tournament.bracket.generate" />
						<spring:message var="generatingLabel" code="tournament.bracket.generating" />
						<form method="post" action="${generateAction}" data-submit-guard="true" data-submit-loading-label="${generatingLabel}">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<ui:button label="${generateLabel}" type="submit" />
						</form>
					</c:if>
				</header>

				<c:if test="${not empty tournamentNoticeCode}">
					<p class="booking-panel__notice booking-panel__notice--success">
						<spring:message code="${tournamentNoticeCode}" />
					</p>
				</c:if>
				<c:if test="${not empty tournamentErrorCode}">
					<p class="booking-panel__notice booking-panel__notice--error">
						<spring:message code="${tournamentErrorCode}" />
					</p>
				</c:if>

				<c:choose>
					<c:when test="${bracketPage.generated}">
						<spring:message var="bracketGridLabel" code="tournament.bracket.grid.label" />
						<section class="tournament-bracket-grid" aria-label="${bracketGridLabel}">
							<c:forEach var="round" items="${bracketPage.rounds}">
								<section class="tournament-bracket-round">
									<h2 class="tournament-bracket-round__title"><c:out value="${round.label}" /></h2>
									<div class="tournament-bracket-round__matches">
										<c:forEach var="match" items="${round.matches}">
											<article class="tournament-bracket-match ${match.focused ? 'tournament-bracket-match--focused' : ''}">
												<p class="tournament-bracket-match__label"><c:out value="${match.label}" /></p>
												<div class="tournament-bracket-match__teams">
													<span><c:out value="${match.teamA}" /></span>
													<span><c:out value="${match.teamB}" /></span>
												</div>
												<p class="tournament-bracket-match__status"><c:out value="${match.statusLabel}" /></p>
											</article>
										</c:forEach>
									</div>
								</section>
							</c:forEach>
						</section>
					</c:when>
					<c:otherwise>
						<section class="panel tournament-bracket-empty" aria-labelledby="bracket-empty-title">
							<h2 id="bracket-empty-title" class="form-card__title">
								<spring:message code="tournament.bracket.empty.title" />
							</h2>
							<p class="body-copy">
								<spring:message code="tournament.bracket.empty.description" />
							</p>
						</section>
					</c:otherwise>
				</c:choose>
			</main>
		</div>
	</body>
</html>
