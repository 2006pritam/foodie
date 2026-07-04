<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your Dashboard - Foodie</title>
  <link rel="stylesheet" href="./assets/css/style.css?v=5">
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
<!-- Customer AI chat-support widget (backed by NVIDIA via /chat) -->
<div id="chatWidget" class="chat-widget">
  <button id="chatToggle" class="chat-fab" type="button" aria-label="Open chat support">
    <span class="chat-fab-icon">&#128172;</span>
    <span class="chat-fab-text">Chat Support</span>
  </button>

  <section id="chatPanel" class="chat-panel" hidden>
    <header class="chat-panel-head">
      <div>
        <strong>Foodie Support</strong>
        <span class="chat-status">AI assistant &bull; online</span>
      </div>
      <button id="chatClose" class="chat-close" type="button" aria-label="Close chat">&times;</button>
    </header>
    <div id="chatLog" class="chat-log">
      <div class="chat-msg bot">Hi <%= name == null ? "there" : name %>! I'm your Foodie assistant. Ask me about the menu, your cart, checkout or tracking an order.</div>
    </div>
    <form id="chatForm" class="chat-input-row" autocomplete="off">
      <input id="chatInput" type="text" name="message" placeholder="Type your question..." maxlength="1000" required />
      <button type="submit" class="button small" id="chatSend">Send</button>
    </form>
  </section>
</div>

<script>
  (function () {
    var toggle = document.getElementById('chatToggle');
    var panel  = document.getElementById('chatPanel');
    var closeB = document.getElementById('chatClose');
    var form   = document.getElementById('chatForm');
    var input  = document.getElementById('chatInput');
    var sendB  = document.getElementById('chatSend');
    var log    = document.getElementById('chatLog');

    function openPanel()  { panel.hidden = false; toggle.classList.add('is-open'); input.focus(); }
    function closePanel() { panel.hidden = true;  toggle.classList.remove('is-open'); }

    toggle.addEventListener('click', function () { panel.hidden ? openPanel() : closePanel(); });
    closeB.addEventListener('click', closePanel);

    function addMsg(text, who) {
      var el = document.createElement('div');
      el.className = 'chat-msg ' + who;
      el.textContent = text;
      log.appendChild(el);
      log.scrollTop = log.scrollHeight;
      return el;
    }

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var text = input.value.trim();
      if (!text) return;
      addMsg(text, 'user');
      input.value = '';
      input.disabled = true; sendB.disabled = true;
      var typing = addMsg('...', 'bot typing');

      var body = 'message=' + encodeURIComponent(text);
      fetch('chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body
      })
        .then(function (r) { return r.json().catch(function () { return { reply: 'Sorry, something went wrong.' }; }); })
        .then(function (data) {
          typing.remove();
          addMsg(data.reply || 'Sorry, I had no answer for that.', 'bot');
        })
        .catch(function () {
          typing.remove();
          addMsg('Sorry, I could not reach support. Please try again.', 'bot');
        })
        .finally(function () {
          input.disabled = false; sendB.disabled = false; input.focus();
        });
    });
  })();
</script>
<% } %>
</body>
</html>
