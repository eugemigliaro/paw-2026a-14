<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="formId" required="true" %>
<%@ attribute name="pageNumber" required="true" type="java.lang.Integer" %>
<%@ attribute name="pageHasPrevious" required="true" type="java.lang.Boolean" %>
<%@ attribute name="pageHasNext" required="true" type="java.lang.Boolean" %>
<%@ attribute name="paginationItems" required="true" type="java.util.List" %>
<%@ attribute name="listTitle" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:if test="${not empty paginationItems}">
    <spring:message var="previousLabel" code="pagination.previous" />
    <spring:message var="nextLabel" code="pagination.next" />
    <section class="feed-pagination" aria-label="${listTitle}">
        <nav class="feed-pagination__nav" aria-label="${listTitle}">

            <c:choose>
                <c:when test="${pageHasPrevious}">
                    <c:set var="prevHref" value="" />
                    <c:forEach var="_item" items="${paginationItems}">
                        <c:if test="${not _item.ellipsis and _item.label == (pageNumber - 1)}">
                            <c:set var="prevHref" value="${_item.href}" />
                        </c:if>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${not empty prevHref}">
                            <a href="${prevHref}" class="feed-pagination__control"> 
                                <c:out value="${previousLabel}" />
                            </a>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" form="${formId}" name="page" value="${pageNumber - 1}" data-page-submit="true" class="feed-pagination__control">
                                <c:out value="${previousLabel}" />
                            </button>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <span class="feed-pagination__control feed-pagination__control--disabled">
                        <c:out value="${previousLabel}" />
                    </span>
                </c:otherwise>
            </c:choose>

            <div class="feed-pagination__pages">
                <c:forEach var="item" items="${paginationItems}">
                    <c:choose>
                        <c:when test="${item.ellipsis}">
                            <span class="feed-pagination__ellipsis"><c:out value="${item.label}" /></span>
                        </c:when>
                        <c:when test="${item.current}">
                            <span class="feed-pagination__page feed-pagination__page--current" aria-current="page">
                                <c:out value="${item.label}" />
                            </span>
                        </c:when>
                        <c:otherwise>
                            <c:choose>
                                <c:when test="${not empty item.href}">
                                    <a href="${item.href}" class="feed-pagination__page">
                                        <c:out value="${item.label}" />
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <button type="submit" form="${formId}" name="page" value="${item.label}" data-page-submit="true" class="feed-pagination__page">
                                        <c:out value="${item.label}" />
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>

            <c:choose>
                <c:when test="${pageHasNext}">
                    <c:set var="nextHref" value="" />
                    <c:forEach var="_item" items="${paginationItems}">
                        <c:if test="${not _item.ellipsis and _item.label == (pageNumber + 1)}">
                            <c:set var="nextHref" value="${_item.href}" />
                        </c:if>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${not empty nextHref}">
                            <a href="${nextHref}" class="feed-pagination__control">
                                <c:out value="${nextLabel}" />
                            </a>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" form="${formId}" name="page" value="${pageNumber + 1}" data-page-submit="true" class="feed-pagination__control">
                                <c:out value="${nextLabel}" />
                            </button>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <span class="feed-pagination__control feed-pagination__control--disabled">
                        <c:out value="${nextLabel}" />
                    </span>
                </c:otherwise>
            </c:choose>

        </nav>
    </section>
</c:if>
