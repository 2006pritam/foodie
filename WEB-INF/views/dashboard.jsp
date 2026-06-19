<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your Dashboard - Foodie</title>
  <link rel="stylesheet" href="./assets/css/style.css">
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
        <a class="button outline" href="home">Browse Offers</a>
      <% } %>
      <a class="button danger" href="logout">Sign Out</a>
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
          <li>Browse restaurant offerings.</li>
          <li>Return to the homepage for reservations.</li>
        <% } %>
      </ul>
    </article>
  </section>
</div>
</body>
</html>
