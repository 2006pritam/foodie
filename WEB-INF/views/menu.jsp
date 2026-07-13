<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Item" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Menu | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=18">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
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
            <a class="button outline" href="${pageContext.request.contextPath}/reservations">Table Booking</a>
            <a class="button outline" href="${pageContext.request.contextPath}/complaints">Raise Complaint</a>
            <a class="button outline" href="${pageContext.request.contextPath}/profile">My Profile</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("menuMessage") != null ? request.getAttribute("menuMessage") : "" %>
    </div>

    <% if (session.getAttribute("resvTableName") != null) { %>
        <div class="dine-in-banner">
            Ordering for <strong>Table <%= session.getAttribute("resvTableName") %></strong> (dine-in). Add items, then check out.
        </div>
    <% } %>

    <%
        List<Item> items = (List<Item>) request.getAttribute("items");
        boolean hasItems = items != null && !items.isEmpty();
    %>
    <% if (hasItems) { %>
    <div class="menu-search-row">
        <input type="search" id="menuSearch" class="menu-search-input"
               placeholder="Search items by name or category..."
               autocomplete="off" aria-label="Search menu items" />
    </div>
    <% } %>

    <section class="menu-grid">
        <%
            if (!hasItems) {
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
            <article class="menu-card" data-search="<%= (item.getName() + " " + item.getCategory()).toLowerCase().replace("\"", "&quot;") %>">
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

    <% if (hasItems) { %>
    <p class="empty-state" id="menuNoResults" hidden>No items match your search.</p>
    <% } %>
</div>

<% if (hasItems) { %>
<script>
  (function () {
    var input = document.getElementById('menuSearch');
    if (!input) return;
    var cards = Array.prototype.slice.call(document.querySelectorAll('.menu-grid .menu-card'));
    var noResults = document.getElementById('menuNoResults');

    function applyFilter() {
      var q = input.value.trim().toLowerCase();
      var visible = 0;
      for (var i = 0; i < cards.length; i++) {
        var hay = cards[i].getAttribute('data-search') || '';
        var match = q === '' || hay.indexOf(q) !== -1;
        cards[i].style.display = match ? '' : 'none';
        if (match) visible++;
      }
      if (noResults) noResults.hidden = (visible !== 0);
    }

    input.addEventListener('input', applyFilter);
  })();
</script>
<% } %>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
