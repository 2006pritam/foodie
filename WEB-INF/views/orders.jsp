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
            case "CANCELLED": return "Cancelled";
            default: return status.charAt(0) + status.substring(1).toLowerCase();
        }
    }
    // Customers may cancel their own order only before a rider picks it up.
    boolean cancellable(String status) {
        return "PENDING".equals(status) || "ACCEPTED".equals(status);
    }
    // HTML-escape for safe rendering of user-supplied order fields on the receipt.
    String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Orders | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=12">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
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
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
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
                boolean cancelled = "CANCELLED".equals(o.getStatus());
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
            <% } else if (cancelled) { %>
                <p class="order-rejected-note">You cancelled this order.</p>
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

            <%
                String pin = o.getDeliveryPin();
                boolean showPin = pin != null && !pin.isEmpty()
                        && ("ACCEPTED".equals(o.getStatus()) || "PICKED_UP".equals(o.getStatus()));
                if (showPin) {
            %>
                <div class="delivery-pin-box">
                    <span class="delivery-pin-label">Delivery PIN</span>
                    <span class="delivery-pin-code"><%= esc(pin) %></span>
                    <span class="delivery-pin-hint">Share this with your rider to confirm delivery.</span>
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
                <% if (o.getTableName() != null && !o.getTableName().isEmpty()) { %>
                    <span>Table: <strong><%= esc(o.getTableName()) %></strong></span>
                <% } %>
                <span>Deliver to: <strong><%= o.getAddress() %></strong> (<%= o.getPhone() %>)</span>
                <% if (o.getRiderName() != null && !o.getRiderName().isEmpty()) { %>
                    <span>Rider: <strong><%= o.getRiderName() %></strong></span>
                <% } %>
                <span class="order-total">Total: <strong>Rs <%= String.format("%.2f", o.getTotal()) %></strong></span>
                <button type="button" class="button small" onclick="openReceipt('<%= esc(o.getOrderCode()) %>')">Print receipt</button>
                <% if (cancellable(o.getStatus())) { %>
                    <form method="post" action="${pageContext.request.contextPath}/orders" class="order-cancel-form"
                          onsubmit="return confirm('Cancel this order? This cannot be undone.');">
                        <input type="hidden" name="action" value="cancel" />
                        <input type="hidden" name="orderId" value="<%= o.getId() %>" />
                        <button type="submit" class="button small danger">Cancel order</button>
                    </form>
                <% } %>
            </div>
        </section>

        <!-- Printable invoice for <%= esc(o.getOrderCode()) %> -->
        <div class="receipt-overlay" id="receipt-<%= esc(o.getOrderCode()) %>" role="dialog" aria-modal="true">
            <div class="receipt-modal">
                <button type="button" class="receipt-close" aria-label="Close" onclick="closeReceipt('<%= esc(o.getOrderCode()) %>')">&times;</button>
                <div class="receipt-brand">
                    <h2>FOODIE</h2>
                    <p>Restaurant SaaS &bull; Order Invoice</p>
                    <p>foodie.example.com</p>
                </div>
                <div class="r-line"></div>
                <div class="receipt-row"><span>Order #:</span><span><%= esc(o.getOrderCode()) %></span></div>
                <div class="receipt-row"><span>Date:</span><span><%= esc(o.getCreatedAt()) %></span></div>
                <% if (o.getTableName() != null && !o.getTableName().isEmpty()) { %>
                    <div class="receipt-row"><span>Table:</span><span><%= esc(o.getTableName()) %></span></div>
                <% } %>
                <div class="receipt-row"><span>Status:</span><span><%= label(o.getStatus()) %></span></div>
                <div class="r-line"></div>
                <div class="receipt-section-title">CUSTOMER</div>
                <p class="receipt-muted"><%= esc(o.getCustomerName()) %></p>
                <p class="receipt-muted"><%= esc(o.getPhone()) %></p>
                <p class="receipt-muted">Deliver to: <%= esc(o.getAddress()) %></p>
                <div class="r-line"></div>
                <table class="receipt-table">
                    <thead>
                    <tr><th>ITEM</th><th class="num">QTY</th><th class="num">PRICE</th><th class="num">TOTAL</th></tr>
                    </thead>
                    <tbody>
                    <% for (OrderItem line : o.getItems()) { %>
                        <tr>
                            <td><%= esc(line.getItemName()) %></td>
                            <td class="num"><%= line.getQuantity() %></td>
                            <td class="num"><%= String.format("%.0f", line.getPrice()) %></td>
                            <td class="num"><%= String.format("%.0f", line.getLineTotal()) %></td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
                <div class="r-line"></div>
                <div class="receipt-row"><span>Subtotal:</span><span>Rs <%= String.format("%.2f", o.getTotal()) %></span></div>
                <div class="receipt-total-row"><span>TOTAL</span><span>Rs <%= String.format("%.2f", o.getTotal()) %></span></div>
                <div class="receipt-footer">
                    Thank you for your order!<br>Please keep this receipt for reference.
                </div>
                <div class="receipt-actions">
                    <button type="button" class="button" onclick="window.print()">Print</button>
                    <button type="button" class="button outline" onclick="closeReceipt('<%= esc(o.getOrderCode()) %>')">Close</button>
                </div>
            </div>
        </div>
    <%
            }
        }
    %>
</div>

<script>
    function openReceipt(code) {
        var el = document.getElementById('receipt-' + code);
        if (el) el.classList.add('open');
    }
    function closeReceipt(code) {
        var el = document.getElementById('receipt-' + code);
        if (el) el.classList.remove('open');
    }
    // Close when clicking the dark backdrop (outside the modal card).
    document.addEventListener('click', function (e) {
        if (e.target.classList && e.target.classList.contains('receipt-overlay')) {
            e.target.classList.remove('open');
        }
    });
    // If we arrived right after placing an order (?placed=CODE), auto-open its receipt.
    (function () {
        var m = window.location.search.match(/[?&]placed=([^&]+)/);
        if (m) openReceipt(decodeURIComponent(m[1]));
    })();
</script>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
