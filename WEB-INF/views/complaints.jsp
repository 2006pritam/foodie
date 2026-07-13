<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Order" %>
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
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Raise a Complaint | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=18">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page complaints-page">
<%
    String ctx = request.getContextPath();
    List<Order> orders = (List<Order>) request.getAttribute("orders");
    List<Complaint> complaints = (List<Complaint>) request.getAttribute("complaints");
%>
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Raise a Complaint</h1>
            <p>Have an issue with an order? Tell us and our team will look into it.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="<%= ctx %>/orders">My Orders</a>
            <a class="button outline" href="<%= ctx %>/menu">Browse Menu</a>
            <a class="button outline" href="<%= ctx %>/profile">My Profile</a>
            <a class="button danger" href="<%= ctx %>/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("complaintMessage") != null ? request.getAttribute("complaintMessage") : "" %>
    </div>

    <section class="panel form-panel">
        <div class="panel-header">
            <h2>New complaint</h2>
            <p>Select the order you have a problem with and describe the issue.</p>
        </div>
        <%
            if (orders == null || orders.isEmpty()) {
        %>
            <p class="empty-state">You have no orders yet. <a href="<%= ctx %>/menu">Place an order</a> first.</p>
        <%
            } else {
        %>
        <form method="post" action="<%= ctx %>/complaints" class="admin-form">
            <div class="form-row">
                <label>Order</label>
                <select name="orderId" required>
                    <option value="">-- Select an order --</option>
                    <% for (Order o : orders) { %>
                        <option value="<%= o.getId() %>">
                            <%= esc(o.getOrderCode()) %> &mdash; Rs <%= String.format("%.2f", o.getTotal()) %>
                            (<%= esc(o.getStatus()) %>)
                        </option>
                    <% } %>
                </select>
            </div>
            <div class="form-row">
                <label>What is the problem?</label>
                <textarea name="message" rows="4" required maxlength="1000"
                          placeholder="Describe the issue with this order..."></textarea>
            </div>
            <button type="submit" class="button">Submit complaint</button>
        </form>
        <% } %>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2>Your complaints</h2>
        </div>
        <%
            if (complaints == null || complaints.isEmpty()) {
        %>
            <p class="empty-state">You have not raised any complaints.</p>
        <%
            } else {
        %>
        <table class="data-table">
            <thead>
            <tr><th>Code</th><th>Order</th><th>Problem</th><th>Status</th><th>Reply from us</th><th>Date</th></tr>
            </thead>
            <tbody>
            <% for (Complaint c : complaints) { %>
                <tr>
                    <td><%= esc(c.getComplaintCode()) %></td>
                    <td><%= esc(c.getOrderCode()) %></td>
                    <td><%= esc(c.getMessage()) %></td>
                    <td><span class="order-badge <%= badgeClass(c.getStatus()) %>"><%= label(c.getStatus()) %></span></td>
                    <td><%= c.getAdminReply() == null || c.getAdminReply().isEmpty() ? "<span class=\"muted\">Awaiting response</span>" : esc(c.getAdminReply()) %></td>
                    <td><%= esc(c.getCreatedAt()) %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        <% } %>
    </section>
</div>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
