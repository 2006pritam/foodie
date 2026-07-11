<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Reservation" %>
<%!
    String badgeClass(String status) { return status == null ? "pending" : status.toLowerCase(); }
    String label(String status) {
        if (status == null) return "Pending";
        return status.charAt(0) + status.substring(1).toLowerCase();
    }
    boolean cancellable(String status) {
        return "PENDING".equals(status) || "ACCEPTED".equals(status);
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Reservations | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=17">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page my-reservations-page">
<%
    String ctx = request.getContextPath();
    List<Reservation> reservations = (List<Reservation>) request.getAttribute("reservations");
%>
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>My Reservations</h1>
            <p>Track your table bookings and their status.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="<%= ctx %>/reservations">Book a Table</a>
            <a class="button outline" href="<%= ctx %>/menu">Browse Menu</a>
            <a class="button danger" href="<%= ctx %>/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("reservationMessage") != null ? request.getAttribute("reservationMessage") : "" %>
    </div>

    <%
        if (reservations == null || reservations.isEmpty()) {
    %>
        <section class="panel">
            <p class="empty-state">You have no reservations yet. <a href="<%= ctx %>/reservations">Book a table</a>.</p>
        </section>
    <%
        } else {
            for (Reservation r : reservations) {
    %>
        <section class="panel order-card">
            <div class="panel-header order-card-header">
                <div>
                    <h2><%= r.getReservationCode() %></h2>
                    <p class="order-meta">Table <%= r.getTableName() %> &middot; <%= r.getReserveDate() %> &middot; <%= r.getTimeIn() %>&ndash;<%= r.getTimeOut() %></p>
                </div>
                <span class="order-badge <%= badgeClass(r.getStatus()) %>"><%= label(r.getStatus()) %></span>
            </div>

            <div class="order-card-footer">
                <span>Party of <strong><%= r.getPartySize() %></strong></span>
                <% if (r.getPurpose() != null && !r.getPurpose().isEmpty()) { %>
                    <span>Purpose: <strong><%= r.getPurpose() %></strong></span>
                <% } %>
                <% if (r.getOrderId() != null) { %>
                    <span><a href="<%= ctx %>/orders">Food order attached &rarr;</a></span>
                <% } %>
                <% if (cancellable(r.getStatus())) { %>
                    <form method="post" action="<%= ctx %>/reservations" class="order-cancel-form"
                          onsubmit="return confirm('Cancel this reservation? This cannot be undone.');">
                        <input type="hidden" name="action" value="cancel" />
                        <input type="hidden" name="id" value="<%= r.getId() %>" />
                        <button type="submit" class="button small danger">Cancel reservation</button>
                    </form>
                <% } %>
            </div>
        </section>
    <%
            }
        }
    %>
</div>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
