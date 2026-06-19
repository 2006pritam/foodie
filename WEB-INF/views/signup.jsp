<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="UTF-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Sign Up - Foodie</title>

  <link rel="shortcut icon" href="./favicon.svg" type="image/svg+xml">
  <link rel="stylesheet" href="./assets/css/style.css">

  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link
    href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&family=Rubik:wght@400;500;600;700&family=Shadows+Into+Light&display=swap"
    rel="stylesheet">
</head>

<body id="top">
<%
    String _sessionUser = (String) session.getAttribute("userName");
    boolean _loggedIn   = (_sessionUser != null);
%>

  <header class="header" data-header>
    <div class="container">

      <h1>
        <a href="home" class="logo">Foodie<span class="span">.</span></a>
      </h1>

      <nav class="navbar" data-navbar>
        <ul class="navbar-list">
          <li class="nav-item"><a href="home#home" class="navbar-link" data-nav-link>Home</a></li>
          <li class="nav-item"><a href="home#about" class="navbar-link" data-nav-link>About Us</a></li>
          <li class="nav-item"><a href="home#food-menu" class="navbar-link" data-nav-link>Shop</a></li>
          <li class="nav-item"><a href="home#blog" class="navbar-link" data-nav-link>Blog</a></li>
          <li class="nav-item"><a href="home#contact" class="navbar-link" data-nav-link>Contact Us</a></li>
        </ul>
      </nav>

      <div class="header-btn-group">
        <a href="home#reservation" class="btn btn-hover">Reservation</a>
        <% if (_loggedIn) { %>
          <a href="logout" class="btn btn-hover auth-header-btn">Logout (<%= _sessionUser %>)</a>
        <% } else { %>
          <a href="login" class="btn btn-hover auth-header-btn">Login / Sign Up</a>
        <% } %>

        <button class="nav-toggle-btn" aria-label="Toggle Menu" data-menu-toggle-btn>
          <span class="line top"></span>
          <span class="line middle"></span>
          <span class="line bottom"></span>
        </button>
      </div>

    </div>
  </header>

  <main>
    <section class="auth-page" style="background-image: url('./assets/images/hero-bg.jpg')">
      <div class="container">

        <div class="auth-copy">
          <p class="section-subtitle">Join Foodie</p>
          <h2 class="h1 section-title">Create Account</h2>
          <p class="section-text">Start saving your details for quick reservations, fresh offers, and easy food ordering.</p>
        </div>

        <div class="auth-card">
          <p class="footer-list-title">Sign Up</p>
          <%= request.getAttribute("authMessage") == null ? "" : request.getAttribute("authMessage") %>

          <form action="signup" method="post" class="auth-form">
            <div>
              <label class="field-label" for="signup-name">Name</label>
              <input type="text" id="signup-name" name="name" required placeholder="Enter your name" class="input-field">
            </div>

            <div>
              <label class="field-label" for="signup-email">Email</label>
              <input type="email" id="signup-email" name="email" required placeholder="Enter your email" class="input-field">
            </div>

            <div>
              <label class="field-label" for="signup-role">Account Type</label>
              <select id="signup-role" name="role" class="input-field">
                <option value="USER" selected>Customer</option>
                <option value="RIDER">Rider</option>
                <option value="ADMIN">Admin (restricted)</option>
              </select>
              <p class="field-note">Choose Rider for delivery access. Admin is reserved for admin@gmail.com.</p>
            </div>

            <div>
              <label class="field-label" for="signup-password">Password</label>
              <div class="password-field">
                <input type="password" id="signup-password" name="password" required
                       placeholder="Create a password (min 8 chars)" class="input-field" minlength="8">
                <button type="button" class="password-toggle" aria-label="Show password" data-password-toggle="#signup-password">
                  <ion-icon name="eye-outline"></ion-icon>
                </button>
              </div>
            </div>

            <button type="submit" class="btn btn-hover">Sign Up</button>

            <div class="auth-links">
              <span>Already registered? <a href="login">Login</a></span>
            </div>
          </form>
        </div>

      </div>
    </section>
  </main>

  <a href="#top" class="back-top-btn" aria-label="Back to top" data-back-top-btn>
    <ion-icon name="chevron-up"></ion-icon>
  </a>

  <script src="./assets/js/script.js" defer></script>
  <script type="module" src="https://unpkg.com/ionicons@5.5.2/dist/ionicons/ionicons.esm.js"></script>
  <script nomodule src="https://unpkg.com/ionicons@5.5.2/dist/ionicons/ionicons.js"></script>

</body>

</html>
