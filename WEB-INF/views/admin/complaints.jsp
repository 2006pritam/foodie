<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Complaint" %>
<%!
    String badgeClass(String status) { return "RESOLVED".equals(status) ? "delivered" : "pending"; }
    String label(String status) {
        if (status == null) return "Open";
        return status.charAt(0) + status.substring(1).toLowerCase();
    }
    String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Complaints | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=14">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</head>
<body class="dashboard-page admin-complaints-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Complaints</h1>
            <p>Customer complaints against orders. Newest first.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button outline" href="${pageContext.request.contextPath}/admin/orders">Orders</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("adminComplaintMessage") != null ? request.getAttribute("adminComplaintMessage") : "" %>
    </div>

    <section class="panel">
        <div class="panel-header">
            <h2>All complaints</h2>
        </div>
        <table class="data-table">
            <thead>
            <tr>
                <th>Code</th><th>Customer</th><th>Order</th><th>Problem</th>
                <th>Status</th><th>Date</th><th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Complaint> complaints = (List<Complaint>) request.getAttribute("complaints");
                String ctx = request.getContextPath();
                if (complaints == null || complaints.isEmpty()) {
            %>
                <tr><td colspan="7" class="empty-state">No complaints yet.</td></tr>
            <%
                } else {
                    for (Complaint c : complaints) {
            %>
                <tr>
                    <td><%= esc(c.getComplaintCode()) %></td>
                    <td><%= esc(c.getCustomerName()) %></td>
                    <td><%= esc(c.getOrderCode()) %></td>
                    <td><%= esc(c.getMessage()) %></td>
                    <td><span class="order-badge <%= badgeClass(c.getStatus()) %>"><%= label(c.getStatus()) %></span></td>
                    <td><%= esc(c.getCreatedAt()) %></td>
                    <td class="item-actions">
                        <% if (!"RESOLVED".equals(c.getStatus())) { %>
                            <form class="inline-form" method="post" action="<%= ctx %>/admin/complaints">
                                <input type="hidden" name="action" value="resolve" />
                                <input type="hidden" name="id" value="<%= c.getId() %>" />
                                <button type="submit" class="button small">Mark resolved</button>
                            </form>
                        <% } else { %>
                            <span class="muted">Resolved</span>
                        <% } %>
                    </td>
                </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>
    </section>
</div>
</body>
</html>
