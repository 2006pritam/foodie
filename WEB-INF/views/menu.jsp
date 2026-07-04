<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Item" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Menu | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=5">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</head>
<body class="dashboard-page menu-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Browse Menu</h1>
            <p>Pick your favourites and add them to your cart.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/cart">Cart (<%= request.getAttribute("cartCount") != null ? request.getAttribute("cartCount") : 0 %>)</a>
            <a class="button outline" href="${pageContext.request.contextPath}/orders">My Orders</a>
            <a class="button" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("menuMessage") != null ? request.getAttribute("menuMessage") : "" %>
    </div>

    <section class="menu-grid">
        <%
            List<Item> items = (List<Item>) request.getAttribute("items");
            if (items == null || items.isEmpty()) {
        %>
            <p class="empty-state">No items are available yet. Please check back soon.</p>
        <%
            } else {
                String ctx = request.getContextPath();
                for (Item item : items) {
                    double discounted = item.getPrice() * (1 - item.getDiscount() / 100.0);
                    if (discounted < 0) discounted = 0;
                    String rawImg = item.getImagePath();
                    boolean hasImg = rawImg != null && !rawImg.isEmpty();
                    boolean remoteImg = hasImg && (rawImg.startsWith("http://") || rawImg.startsWith("https://"));
                    String imgSrc = remoteImg ? rawImg
                            : ctx + "/" + (hasImg ? rawImg : "assets/images/food-menu-1.png");
        %>
            <article class="menu-card">
                <img class="menu-thumb" src="<%= imgSrc %>" alt="<%= item.getName() %>" />
                <div class="menu-card-body">
                    <span class="menu-category"><%= item.getCategory() %></span>
                    <h3><%= item.getName() %></h3>
                    <div class="menu-price">
                        <% if (item.getDiscount() > 0) { %>
                            <span class="price-old">Rs <%= String.format("%.0f", item.getPrice()) %></span>
                            <span class="price-now">Rs <%= String.format("%.0f", discounted) %></span>
                            <span class="order-badge accepted"><%= String.format("%.0f", item.getDiscount()) %>% off</span>
                        <% } else { %>
                            <span class="price-now">Rs <%= String.format("%.0f", item.getPrice()) %></span>
                        <% } %>
                    </div>
                    <form method="post" action="<%= ctx %>/cart" class="menu-add-form">
                        <input type="hidden" name="action" value="add" />
                        <input type="hidden" name="itemId" value="<%= item.getId() %>" />
                        <input type="number" name="quantity" value="1" min="1" class="qty-input" />
                        <button type="submit" class="button small">Add to cart</button>
                    </form>
                </div>
            </article>
        <%
                }
            }
        %>
    </section>
</div>
</body>
</html>
