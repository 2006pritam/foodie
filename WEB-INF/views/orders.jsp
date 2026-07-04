<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Order" %>
<%@ page import="com.foodie.model.OrderItem" %>
<%!
    // Map a status to how far along the 4-step timeline it is (Placed/Accepted/Picked up/Delivered).
    int stepFor(String status) {
        if (status == null) return 1;
        switch (status) {
            case "PENDING":   return 1;
            case "ACCEPTED":  return 2;
            case "PICKED_UP": return 3;
            case "DELIVERED": return 4;
            default:          return 0; // REJECTED or unknown
        }
    }
    String badgeClass(String status) {
        return status == null ? "pending" : status.toLowerCase();
    }
    String label(String status) {
        if (status == null) return "Pending";
        switch (status) {
            case "PICKED_UP": return "Picked up";
            default: return status.charAt(0) + status.substring(1).toLowerCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Orders | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=2">
</head>
<body class="dashboard-page orders-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>My Orders</h1>
            <p>Track the status of your orders in real time.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/menu">Order More</a>
            <a class="button" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("orderMessage") != null ? request.getAttribute("orderMessage") : "" %>
    </div>

    <%
        List<Order> orders = (List<Order>) request.getAttribute("orders");
        if (orders == null || orders.isEmpty()) {
    %>
        <section class="panel">
            <p class="empty-state">You have no orders yet. <a href="${pageContext.request.contextPath}/menu">Start ordering</a>.</p>
        </section>
    <%
        } else {
            String[] steps = {"Placed", "Accepted", "Picked up", "Delivered"};
            for (Order o : orders) {
                boolean rejected = "REJECTED".equals(o.getStatus());
                int step = stepFor(o.getStatus());
    %>
        <section class="panel order-card">
            <div class="panel-header order-card-header">
                <div>
                    <h2><%= o.getOrderCode() %></h2>
                    <p class="order-meta"><%= o.getCreatedAt() %></p>
                </div>
                <span class="order-badge <%= badgeClass(o.getStatus()) %>"><%= label(o.getStatus()) %></span>
            </div>

            <% if (rejected) { %>
                <p class="order-rejected-note">This order was rejected by the restaurant.</p>
            <% } else { %>
                <div class="status-timeline">
                    <% for (int i = 0; i < steps.length; i++) { %>
                        <div class="timeline-step <%= (i + 1) <= step ? "done" : "" %>">
                            <span class="timeline-dot"></span>
                            <span class="timeline-label"><%= steps[i] %></span>
                        </div>
                    <% } %>
                </div>
            <% } %>

            <ul class="summary-list">
                <% for (OrderItem line : o.getItems()) { %>
                    <li>
                        <span><%= line.getItemName() %> &times; <%= line.getQuantity() %></span>
                        <strong>Rs <%= String.format("%.2f", line.getLineTotal()) %></strong>
                    </li>
                <% } %>
            </ul>

            <div class="order-card-footer">
                <span>Deliver to: <strong><%= o.getAddress() %></strong> (<%= o.getPhone() %>)</span>
                <% if (o.getRiderName() != null && !o.getRiderName().isEmpty()) { %>
                    <span>Rider: <strong><%= o.getRiderName() %></strong></span>
                <% } %>
                <span class="order-total">Total: <strong>Rs <%= String.format("%.2f", o.getTotal()) %></strong></span>
            </div>
        </section>
    <%
            }
        }
    %>
</div>
</body>
</html>
