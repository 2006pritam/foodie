<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your Dashboard - Foodie</title>
  <link rel="stylesheet" href="./assets/css/style.css?v=13">
  <script src="./assets/js/theme.js"></script>
</head>
<body class="dashboard-page">
<%
  String name = (String) session.getAttribute("userName");
  String role = (String) session.getAttribute("userRole");
  if (role == null) role = "USER";
%>
<div class="dashboard-shell">
  <header class="dashboard-header">
    <div class="dashboard-brand">
      <h1>Welcome, <%= name == null ? "Foodie" : name %></h1>
      <p>Your <%= role.equalsIgnoreCase("ADMIN") ? "Admin" : role.equalsIgnoreCase("RIDER") ? "Rider" : "Customer" %> dashboard for the restaurant SaaS experience.</p>
    </div>
    <div class="dashboard-actions">
      <% if ("ADMIN".equalsIgnoreCase(role)) { %>
        <a class="button outline" href="admin/dashboard">Admin Panel</a>
      <% } else if ("RIDER".equalsIgnoreCase(role)) { %>
        <a class="button outline" href="rider/dashboard">Rider Panel</a>
      <% } else { %>
        <a class="button outline" href="menu">Browse Menu</a>
        <a class="button outline" href="cart">My Cart</a>
        <a class="button outline" href="orders">My Orders</a>
        <a class="button outline" href="reservations">Table Booking</a>
      <% } %>
      <a class="button danger" href="logout">Sign Out</a>
      <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
    </div>
  </header>

  <section class="dashboard-grid">
    <article class="dashboard-panel">
      <h2>Account overview</h2>
      <p><strong>Name:</strong> <%= name == null ? "User" : name %></p>
      <p><strong>Role:</strong> <%= role %></p>
    </article>

    <article class="dashboard-panel">
      <h2>Dashboard access</h2>
      <p>Use the buttons above to open the correct dashboard for your role.</p>
      <ul class="dashboard-list">
        <% if ("ADMIN".equalsIgnoreCase(role)) { %>
          <li>Open the Admin panel to manage users.</li>
          <li>View role assignments and tenant settings.</li>
        <% } else if ("RIDER".equalsIgnoreCase(role)) { %>
          <li>Open the Rider panel for delivery tasks.</li>
          <li>Check current order assignments.</li>
        <% } else { %>
          <li><a href="menu">Browse the menu</a> and add items to your cart.</li>
          <li><a href="orders">Track your orders</a> from placed to delivered.</li>
          <li>Need help? Tap the <strong>Chat Support</strong> button, bottom-right.</li>
        <% } %>
      </ul>
    </article>
  </section>
</div>

<% if (!"ADMIN".equalsIgnoreCase(role) && !"RIDER".equalsIgnoreCase(role)) { %>
<%@ include file="chat-widget.jsp" %>
<% } %>
</body>
</html>
