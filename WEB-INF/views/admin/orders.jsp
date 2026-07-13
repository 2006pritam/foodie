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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=19">
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
                <th>Payment</th>
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
                <tr><td colspan="9" class="empty-state">No orders yet.</td></tr>
            <%
                } else {
                    for (Order o : orders) {
                        boolean isUpi = "UPI".equalsIgnoreCase(o.getPaymentMethod());
                        boolean hasProof = o.getPaymentProof() != null && !o.getPaymentProof().isEmpty();
                        String proofUrl = hasProof ? ctx + "/" + o.getPaymentProof() : "";
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
                    <td class="payment-cell">
                        <span class="pay-method"><%= o.getPaymentMethod() == null ? "&mdash;" : o.getPaymentMethod() %></span>
                        <% if (isUpi && hasProof) { %>
                            <br>
                            <img class="pay-proof-thumb" src="<%= proofUrl %>" alt="UPI payment screenshot"
                                 onclick="openProof('<%= proofUrl %>', '<%= o.getOrderCode() %>')" />
                        <% } else if (isUpi) { %>
                            <br><span class="pay-noproof">No screenshot</span>
                        <% } %>
                    </td>
                    <td><%= o.getAddress() == null ? "" : o.getAddress() %><br><small><%= o.getPhone() == null ? "" : o.getPhone() %></small></td>
                    <td><span class="order-badge <%= badgeClass(o.getStatus()) %>"><%= label(o.getStatus()) %></span></td>
                    <td><%= o.getRiderName() == null ? "&mdash;" : o.getRiderName() %></td>
                    <td class="item-actions">
                        <% if ("PENDING".equals(o.getStatus())) { %>
                            <% if (isUpi && hasProof) { %>
                                <button type="button" class="button small outline"
                                        onclick="openProof('<%= proofUrl %>', '<%= o.getOrderCode() %>')">View payment</button>
                            <% } %>
                            <form class="inline-form" method="post" action="<%= ctx %>/admin/orders">
                                <input type="hidden" name="action" value="accept" />
                                <input type="hidden" name="id" value="<%= o.getId() %>" />
                                <button type="submit" class="button small" title="Verify payment and confirm the order">Accept &amp; verify</button>
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

<!-- Payment screenshot preview -->
<div class="proof-overlay" id="proofOverlay" role="dialog" aria-modal="true" aria-label="Payment screenshot">
    <div class="proof-modal">
        <button type="button" class="proof-close" aria-label="Close" onclick="closeProof()">&times;</button>
        <h3 id="proofTitle">Payment screenshot</h3>
        <img id="proofImg" src="" alt="UPI payment screenshot" />
        <p class="hint">Check the amount, UPI ID and status, then Accept &amp; verify or Reject the order.</p>
    </div>
</div>

<script>
    function openProof(url, code) {
        document.getElementById('proofImg').src = url;
        document.getElementById('proofTitle').textContent = 'Payment screenshot — ' + code;
        document.getElementById('proofOverlay').classList.add('open');
    }
    function closeProof() {
        document.getElementById('proofOverlay').classList.remove('open');
    }
    document.getElementById('proofOverlay').addEventListener('click', function (e) {
        if (e.target === this) closeProof();
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeProof();
    });
</script>
</body>
</html>
