<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Admin Dashboard | Foodie SaaS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="dashboard-page admin-dashboard">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Foodie Admin</h1>
            <p>Manage users, roles, and restaurant operations.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/dashboard">Home</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
        </div>
    </header>

    <section class="dashboard-grid">
        <article class="dashboard-panel large">
            <h2>Users & Roles</h2>
            <p>Review existing accounts and adjust access levels for admin, rider, and standard users.</p>
            <a class="button" href="${pageContext.request.contextPath}/admin/users">Open User Management</a>
        </article>

        <article class="dashboard-panel small">
            <h3>Quick actions</h3>
            <ul class="dashboard-list">
                <li>Create a new user account</li>
                <li>Update role assignments</li>
                <li>Monitor signups and access</li>
            </ul>
        </article>

        <article class="dashboard-panel small">
            <h3>Business insights</h3>
            <p>Use the admin panel to keep your restaurant SaaS site secure and current.</p>
        </article>
    </section>
</div>
</body>
</html>
