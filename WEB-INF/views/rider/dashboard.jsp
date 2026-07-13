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
    <title>Rider Dashboard | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=18">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page rider-dashboard">
<%
    String riderName = (String) session.getAttribute("userName");
    List<Order> availableOrders = (List<Order>) request.getAttribute("availableOrders");
    List<Order> myOrders = (List<Order>) request.getAttribute("myOrders");
    String ctx = request.getContextPath();
%>
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Rider Console</h1>
            <p>Welcome, <%= riderName == null ? "Rider" : riderName %>. Pick up accepted orders and confirm deliveries.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="<%= ctx %>/dashboard">Home</a>
            <a class="button danger" href="<%= ctx %>/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("riderMessage") != null ? request.getAttribute("riderMessage") : "" %>
    </div>

    <section class="panel">
        <div class="panel-header">
            <h2>Available for pickup</h2>
            <p>Accepted orders waiting for a rider. Claim one to start delivering.</p>
        </div>
        <table class="data-table">
            <thead>
            <tr><th>Code</th><th>Customer</th><th>Items</th><th>Total</th><th>Address</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                if (availableOrders == null || availableOrders.isEmpty()) {
            %>
                <tr><td colspan="6" class="empty-state">No orders are waiting for pickup right now.</td></tr>
            <%
                } else {
                    for (Order o : availableOrders) {
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
                    <td>
                        <form class="inline-form" method="post" action="<%= ctx %>/rider/orders">
                            <input type="hidden" name="action" value="pickup" />
                            <input type="hidden" name="id" value="<%= o.getId() %>" />
                            <button type="submit" class="button small">Pick Up</button>
                        </form>
                    </td>
                </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2>My deliveries</h2>
            <p>Orders you have picked up. Confirm delivery once handed over.</p>
        </div>
        <table class="data-table">
            <thead>
            <tr><th>Code</th><th>Customer</th><th>Address</th><th>Total</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
            <%
                if (myOrders == null || myOrders.isEmpty()) {
            %>
                <tr><td colspan="6" class="empty-state">You have no active deliveries.</td></tr>
            <%
                } else {
                    for (Order o : myOrders) {
            %>
                <tr>
                    <td><%= o.getOrderCode() %></td>
                    <td><%= o.getCustomerName() == null ? "" : o.getCustomerName() %></td>
                    <td><%= o.getAddress() == null ? "" : o.getAddress() %><br><small><%= o.getPhone() == null ? "" : o.getPhone() %></small></td>
                    <td>Rs <%= String.format("%.2f", o.getTotal()) %></td>
                    <td><span class="order-badge <%= badgeClass(o.getStatus()) %>"><%= label(o.getStatus()) %></span></td>
                    <td>
                        <% if ("PICKED_UP".equals(o.getStatus())) { %>
                            <button type="button" class="button small"
                                    onclick="openPinModal('<%= o.getId() %>', '<%= o.getOrderCode() %>')">Mark Delivered</button>
                        <% } else { %>
                            <span class="muted">Delivered</span>
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

<!-- Delivery PIN prompt: rider enters the customer's 4-digit PIN to confirm delivery. -->
<div class="pin-overlay" id="pinOverlay" role="dialog" aria-modal="true" aria-labelledby="pinTitle" hidden>
    <div class="pin-modal">
        <button type="button" class="pin-close" aria-label="Close" onclick="closePinModal()">&times;</button>
        <h2 id="pinTitle">Confirm delivery</h2>
        <p class="pin-sub">Enter the 4-digit PIN for order <strong id="pinOrderCode"></strong>.</p>
        <form method="post" action="<%= ctx %>/rider/orders" id="pinForm">
            <input type="hidden" name="action" value="deliver" />
            <input type="hidden" name="id" id="pinOrderId" />
            <input type="text" name="pin" id="pinInput" class="pin-input"
                   inputmode="numeric" pattern="[0-9]{4}" maxlength="4" autocomplete="off"
                   placeholder="0000" aria-label="4-digit delivery PIN" required />
            <button type="submit" class="button">Confirm delivery</button>
        </form>
    </div>
</div>

<script>
    (function () {
        var overlay = document.getElementById('pinOverlay');
        var input   = document.getElementById('pinInput');
        var orderId = document.getElementById('pinOrderId');
        var codeEl  = document.getElementById('pinOrderCode');
        var form    = document.getElementById('pinForm');

        window.openPinModal = function (id, code) {
            orderId.value = id;
            codeEl.textContent = code;
            input.value = '';
            overlay.hidden = false;
            input.focus();
        };
        window.closePinModal = function () { overlay.hidden = true; };

        // Restrict typing to digits only.
        input.addEventListener('input', function () {
            input.value = input.value.replace(/\D/g, '').slice(0, 4);
        });

        form.addEventListener('submit', function (e) {
            if (!/^[0-9]{4}$/.test(input.value)) {
                e.preventDefault();
                input.focus();
            }
        });

        // Close on backdrop click or Escape.
        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) closePinModal();
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !overlay.hidden) closePinModal();
        });
    })();
</script>
</body>
</html>
