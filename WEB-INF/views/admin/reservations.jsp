<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Reservation" %>
<%!
    String badgeClass(String status) { return status == null ? "pending" : status.toLowerCase(); }
    String label(String status) {
        if (status == null) return "Pending";
        return status.charAt(0) + status.substring(1).toLowerCase();
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reservations | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=16">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</head>
<body class="dashboard-page admin-reservations-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Reservations</h1>
            <p>Accept or reject table bookings. Newest first.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button outline" href="${pageContext.request.contextPath}/admin/tables">Manage Tables</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("adminReservationMessage") != null ? request.getAttribute("adminReservationMessage") : "" %>
    </div>

    <section class="panel">
        <div class="panel-header">
            <h2>All reservations</h2>
        </div>
        <div class="table-scroll">
        <table class="data-table">
            <thead>
            <tr>
                <th>Code</th><th>Customer</th><th>Table</th><th>Date</th>
                <th>Time</th><th>Party</th><th>Purpose</th><th>Food</th><th>Status</th><th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Reservation> reservations = (List<Reservation>) request.getAttribute("reservations");
                String ctx = request.getContextPath();
                if (reservations == null || reservations.isEmpty()) {
            %>
                <tr><td colspan="10" class="empty-state">No reservations yet.</td></tr>
            <%
                } else {
                    for (Reservation r : reservations) {
            %>
                <tr>
                    <td><%= r.getReservationCode() %></td>
                    <td><%= r.getCustomerName() == null ? "" : r.getCustomerName() %></td>
                    <td><%= r.getTableName() == null ? "" : r.getTableName() %></td>
                    <td><%= r.getReserveDate() %></td>
                    <td><%= r.getTimeIn() %> &ndash; <%= r.getTimeOut() %></td>
                    <td><%= r.getPartySize() %></td>
                    <td><%= r.getPurpose() == null ? "" : r.getPurpose() %></td>
                    <td><%= r.getOrderId() != null ? "Yes" : "&mdash;" %></td>
                    <td><span class="order-badge <%= badgeClass(r.getStatus()) %>"><%= label(r.getStatus()) %></span></td>
                    <td class="item-actions">
                        <% if ("PENDING".equals(r.getStatus())) { %>
                            <form class="inline-form" method="post" action="<%= ctx %>/admin/reservations">
                                <input type="hidden" name="action" value="accept" />
                                <input type="hidden" name="id" value="<%= r.getId() %>" />
                                <button type="submit" class="button small">Accept</button>
                            </form>
                            <form class="inline-form" method="post" action="<%= ctx %>/admin/reservations" onsubmit="return confirm('Reject this reservation?');">
                                <input type="hidden" name="action" value="reject" />
                                <input type="hidden" name="id" value="<%= r.getId() %>" />
                                <button type="submit" class="button danger small">Reject</button>
                            </form>
                        <% } else { %>
                            <span class="muted">No action</span>
                        <% } %>
                    </td>
                </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>
        </div>
    </section>
</div>
</body>
</html>
