<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.OrderItem" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=2">
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
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("checkoutMessage") != null ? request.getAttribute("checkoutMessage") : "" %>
    </div>

    <%
        List<OrderItem> lines = (List<OrderItem>) request.getAttribute("cartLines");
        Object totalObj = request.getAttribute("cartTotal");
        String ctx = request.getContextPath();
    %>

    <section class="checkout-layout">
        <div class="panel form-panel">
            <div class="panel-header">
                <h2>Delivery details</h2>
                <p>Where should we send your order?</p>
            </div>
            <form method="post" action="<%= ctx %>/checkout" class="admin-form" id="checkoutForm">
                <div class="form-row">
                    <label>Delivery address</label>
                    <input type="text" name="address" required placeholder="House no, street, area, city" />
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
            <div class="order-summary-row total">
                <span>Total</span>
                <strong>Rs <%= String.format("%.2f", totalObj == null ? 0.0 : (Double) totalObj) %></strong>
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
</body>
</html>
