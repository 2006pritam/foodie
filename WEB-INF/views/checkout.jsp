<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.OrderItem" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=17">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page checkout-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Checkout</h1>
            <p>Confirm delivery details and place your order.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/cart">Back to Cart</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("checkoutMessage") != null ? request.getAttribute("checkoutMessage") : "" %>
    </div>

    <%
        List<OrderItem> lines = (List<OrderItem>) request.getAttribute("cartLines");
        Object totalObj = request.getAttribute("cartTotal");
        String ctx = request.getContextPath();
        String dineInTable = (String) session.getAttribute("resvTableName");
        boolean dineIn = dineInTable != null;
        double subtotal = totalObj == null ? 0.0 : (Double) totalObj;
        String couponCode = (String) request.getAttribute("couponCode");
        Object discObj = request.getAttribute("couponDiscount");
        double couponDiscount = discObj == null ? 0.0 : (Double) discObj;
        Object payObj = request.getAttribute("payableTotal");
        double payable = payObj == null ? subtotal : (Double) payObj;
    %>

    <section class="checkout-layout">
        <div class="panel form-panel">
            <div class="panel-header">
                <h2><%= dineIn ? "Dine-in details" : "Delivery details" %></h2>
                <p><%= dineIn ? "Your order will be served to your reserved table." : "Where should we send your order?" %></p>
            </div>
            <% if (dineIn) { %>
                <div class="dine-in-banner">Serving to <strong>Table <%= dineInTable %></strong> (dine-in).</div>
            <% } %>
            <form method="post" action="<%= ctx %>/checkout" class="admin-form" id="checkoutForm">
                <div class="form-row">
                    <label><%= dineIn ? "Delivery address (optional for dine-in)" : "Delivery address" %></label>
                    <input type="text" name="address" <%= dineIn ? "" : "required" %>
                           placeholder="<%= dineIn ? "Table " + dineInTable + " (dine-in)" : "House no, street, area, city" %>" />
                </div>
                <div class="form-row">
                    <label>Phone number</label>
                    <input type="tel" name="phone" required placeholder="e.g. 03001234567" />
                </div>
                <div class="form-row">
                    <label>Payment method</label>
                    <div class="payment-options">
                        <label class="payment-option"><input type="radio" name="payment_method" value="CARD" checked onclick="togglePay()"> Card</label>
                        <label class="payment-option"><input type="radio" name="payment_method" value="UPI" onclick="togglePay()"> UPI</label>
                        <label class="payment-option"><input type="radio" name="payment_method" value="COD" onclick="togglePay()"> Cash on Delivery</label>
                    </div>
                </div>
                <div id="cardFields">
                    <div class="form-row">
                        <label>Card number (demo)</label>
                        <input type="text" name="card_number" value="4242 4242 4242 4242" />
                    </div>
                    <div class="form-row">
                        <label>Expiry / CVV (demo)</label>
                        <input type="text" name="card_extra" value="12/29  123" />
                    </div>
                    <p class="hint">This is a mock payment step — no real charge is made.</p>
                </div>
                <button type="submit" class="button">Place order</button>
            </form>
        </div>

        <aside class="panel order-summary-panel">
            <div class="panel-header">
                <h2>Order summary</h2>
            </div>
            <ul class="summary-list">
                <% if (lines != null) { for (OrderItem line : lines) { %>
                    <li>
                        <span><%= line.getItemName() %> &times; <%= line.getQuantity() %></span>
                        <strong>Rs <%= String.format("%.2f", line.getLineTotal()) %></strong>
                    </li>
                <% } } %>
            </ul>

            <div class="coupon-box">
                <% if (couponCode != null) { %>
                    <div class="coupon-applied">
                        <span>Coupon <strong><%= couponCode %></strong> applied</span>
                        <form method="post" action="<%= ctx %>/checkout" class="inline-form">
                            <input type="hidden" name="action" value="remove_coupon" />
                            <button type="submit" class="coupon-remove" title="Remove coupon">&times;</button>
                        </form>
                    </div>
                <% } else { %>
                    <form method="post" action="<%= ctx %>/checkout" class="coupon-form">
                        <input type="hidden" name="action" value="apply_coupon" />
                        <input type="text" name="coupon_code" placeholder="Have a coupon? Enter code"
                               autocomplete="off" />
                        <button type="submit" class="button small">Apply</button>
                    </form>
                <% } %>
            </div>

            <div class="order-summary-row">
                <span>Subtotal</span>
                <span>Rs <%= String.format("%.2f", subtotal) %></span>
            </div>
            <% if (couponDiscount > 0) { %>
                <div class="order-summary-row discount-row">
                    <span>Discount<%= couponCode != null ? " (" + couponCode + ")" : "" %></span>
                    <span>&minus; Rs <%= String.format("%.2f", couponDiscount) %></span>
                </div>
            <% } %>
            <div class="order-summary-row total">
                <span>Total</span>
                <strong>Rs <%= String.format("%.2f", payable) %></strong>
            </div>
        </aside>
    </section>
</div>
<script>
    function togglePay() {
        var method = document.querySelector('input[name="payment_method"]:checked').value;
        document.getElementById('cardFields').style.display = (method === 'CARD') ? 'block' : 'none';
    }
    togglePay();
</script>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
