<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.OrderItem" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Your Cart | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=10">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</head>
<body class="dashboard-page cart-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Your Cart</h1>
            <p>Review your items before checkout.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/menu">Back to Menu</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("cartMessage") != null ? request.getAttribute("cartMessage") : "" %>
    </div>

    <%
        List<OrderItem> lines = (List<OrderItem>) request.getAttribute("cartLines");
        Object totalObj = request.getAttribute("cartTotal");
        String ctx = request.getContextPath();
        if (lines == null || lines.isEmpty()) {
    %>
        <section class="panel">
            <p class="empty-state">Your cart is empty. <a href="<%= ctx %>/menu">Browse the menu</a> to add items.</p>
        </section>
    <%
        } else {
    %>
        <section class="panel">
            <table class="data-table">
                <thead>
                <tr>
                    <th>Item</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Line Total</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <% for (OrderItem line : lines) { %>
                    <tr>
                        <td><%= line.getItemName() %></td>
                        <td>Rs <%= String.format("%.2f", line.getPrice()) %></td>
                        <td>
                            <form class="inline-form" method="post" action="<%= ctx %>/cart">
                                <input type="hidden" name="action" value="update" />
                                <input type="hidden" name="itemId" value="<%= line.getItemId() %>" />
                                <input type="number" name="quantity" value="<%= line.getQuantity() %>" min="0" class="qty-input" />
                                <button type="submit" class="button small">Update</button>
                            </form>
                        </td>
                        <td>Rs <%= String.format("%.2f", line.getLineTotal()) %></td>
                        <td>
                            <form class="inline-form" method="post" action="<%= ctx %>/cart">
                                <input type="hidden" name="action" value="remove" />
                                <input type="hidden" name="itemId" value="<%= line.getItemId() %>" />
                                <button type="submit" class="button danger small">Remove</button>
                            </form>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>

            <div class="order-summary">
                <div class="order-summary-row total">
                    <span>Total</span>
                    <strong>Rs <%= String.format("%.2f", totalObj == null ? 0.0 : (Double) totalObj) %></strong>
                </div>
                <div class="order-summary-actions">
                    <form class="inline-form" method="post" action="<%= ctx %>/cart" onsubmit="return confirm('Clear the whole cart?');">
                        <input type="hidden" name="action" value="clear" />
                        <button type="submit" class="button outline">Clear cart</button>
                    </form>
                    <a class="button" href="<%= ctx %>/checkout">Proceed to checkout</a>
                </div>
            </div>
        </section>
    <% } %>
</div>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
