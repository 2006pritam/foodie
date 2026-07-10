<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Order" %>
<%@ page import="com.foodie.model.OrderItem" %>
<%!
    String badgeClass(String status) { return status == null ? "pending" : status.toLowerCase(); }
    String label(String status) {
        if (status == null) return "Pending";
        if ("PICKED_UP".equals(status)) return "Picked up";
        return status.charAt(0) + status.substring(1).toLowerCase();
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Orders | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=16">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page admin-orders-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Manage Orders</h1>
            <p>Accept or reject incoming orders. Accepted orders become available to riders.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("adminOrderMessage") != null ? request.getAttribute("adminOrderMessage") : "" %>
    </div>

    <section class="panel">
        <div class="panel-header">
            <h2>All orders</h2>
            <p>Newest first.</p>
        </div>
        <div class="table-scroll">
        <table class="data-table">
            <thead>
            <tr>
                <th>Code</th>
                <th>Customer</th>
                <th>Items</th>
                <th>Total</th>
                <th>Address</th>
                <th>Status</th>
                <th>Rider</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Order> orders = (List<Order>) request.getAttribute("orders");
                String ctx = request.getContextPath();
                if (orders == null || orders.isEmpty()) {
            %>
                <tr><td colspan="8" class="empty-state">No orders yet.</td></tr>
            <%
                } else {
                    for (Order o : orders) {
            %>
                <tr>
                    <td><%= o.getOrderCode() %></td>
                    <td><%= o.getCustomerName() == null ? "" : o.getCustomerName() %></td>
                    <td>
                        <ul class="mini-item-list">
                            <% for (OrderItem line : o.getItems()) { %>
                                <li><%= line.getItemName() %> &times; <%= line.getQuantity() %></li>
                            <% } %>
                        </ul>
                    </td>
                    <td>Rs <%= String.format("%.2f", o.getTotal()) %></td>
                    <td><%= o.getAddress() == null ? "" : o.getAddress() %><br><small><%= o.getPhone() == null ? "" : o.getPhone() %></small></td>
                    <td><span class="order-badge <%= badgeClass(o.getStatus()) %>"><%= label(o.getStatus()) %></span></td>
                    <td><%= o.getRiderName() == null ? "&mdash;" : o.getRiderName() %></td>
                    <td class="item-actions">
                        <% if ("PENDING".equals(o.getStatus())) { %>
                            <form class="inline-form" method="post" action="<%= ctx %>/admin/orders">
                                <input type="hidden" name="action" value="accept" />
                                <input type="hidden" name="id" value="<%= o.getId() %>" />
                                <button type="submit" class="button small">Accept</button>
                            </form>
                            <form class="inline-form" method="post" action="<%= ctx %>/admin/orders" onsubmit="return confirm('Reject this order?');">
                                <input type="hidden" name="action" value="reject" />
                                <input type="hidden" name="id" value="<%= o.getId() %>" />
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
