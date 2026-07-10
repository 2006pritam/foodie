<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.foodie.model.User" %>
<%!
    String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Profile - Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=16">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page profile-page">
<%
    String ctx = request.getContextPath();
    User user = (User) request.getAttribute("profileUser");
    String flash = (String) request.getAttribute("profileMessage");
%>
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>My Profile</h1>
            <p>Manage your account details and security settings.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="<%= ctx %>/dashboard">Back to Dashboard</a>
            <a class="button danger" href="<%= ctx %>/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <% if (flash != null) { %>
        <div class="panel-message"><%= flash %></div>
    <% } %>

    <div class="profile-grid">
        <section class="panel profile-header-panel">
            <div class="profile-avatar">
                <%
                    String photoUrl = user != null ? user.getPhotoUrl() : null;
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                %>
                <img src="<%= esc(photoUrl) %>" alt="Profile photo" class="avatar-img">
                <%
                    } else {
                %>
                <span class="avatar-placeholder"><%= user != null && user.getName() != null ? esc(user.getName().substring(0, 1).toUpperCase()) : "?" %></span>
                <%
                    }
                %>
            </div>
            <div class="profile-info">
                <h2><%= user != null ? esc(user.getName()) : "User" %></h2>
                <p class="profile-email"><%= user != null ? esc(user.getEmail()) : "" %></p>
                <p class="profile-role">Role: <%= user != null ? esc(user.getRole()) : "USER" %></p>
            </div>
        </section>

        <section class="panel form-panel">
            <div class="panel-header">
                <h2>Personal Information</h2>
            </div>
            <form method="post" action="<%= ctx %>/profile" class="admin-form">
                <input type="hidden" name="action" value="updateProfile">
                <div class="form-row">
                    <label for="name">Full Name</label>
                    <input type="text" id="name" name="name" value="<%= user != null ? esc(user.getName()) : "" %>" required maxlength="100">
                </div>
                <div class="form-row">
                    <label for="phone">Phone Number</label>
                    <input type="tel" id="phone" name="phone" value="<%= user != null ? esc(user.getPhone()) : "" %>" placeholder="e.g. +91 98765 43210" maxlength="50">
                </div>
                <div class="form-row">
                    <label for="photoUrl">Profile Photo URL</label>
                    <input type="url" id="photoUrl" name="photoUrl" value="<%= user != null ? esc(user.getPhotoUrl()) : "" %>" placeholder="https://example.com/photo.jpg" maxlength="500">
                    <small class="form-hint">Enter a direct link to an image (jpg, png, webp).</small>
                </div>
                <button type="submit" class="button">Save Changes</button>
            </form>
        </section>

        <section class="panel form-panel">
            <div class="panel-header">
                <h2>Change Password</h2>
            </div>
            <form method="post" action="<%= ctx %>/profile" class="admin-form">
                <input type="hidden" name="action" value="changePassword">
                <div class="form-row">
                    <label for="currentPassword">Current Password</label>
                    <input type="password" id="currentPassword" name="currentPassword" required>
                </div>
                <div class="form-row">
                    <label for="newPassword">New Password</label>
                    <input type="password" id="newPassword" name="newPassword" required minlength="6">
                </div>
                <div class="form-row">
                    <label for="confirmPassword">Confirm New Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                </div>
                <button type="submit" class="button">Update Password</button>
            </form>
        </section>

        <section class="panel form-panel">
            <div class="panel-header">
                <h2>Change Email</h2>
            </div>
            <form method="post" action="<%= ctx %>/profile" class="admin-form">
                <input type="hidden" name="action" value="changeEmail">
                <div class="form-row">
                    <label for="newEmail">New Email Address</label>
                    <input type="email" id="newEmail" name="newEmail" required maxlength="255">
                </div>
                <div class="form-row">
                    <label for="password">Current Password (to confirm)</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <button type="submit" class="button outline">Update Email</button>
            </form>
        </section>
    </div>
</div>
<%@ include file="chat-widget.jsp" %>
</body>
</html>