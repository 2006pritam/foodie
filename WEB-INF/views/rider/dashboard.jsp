<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Rider Dashboard | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="dashboard-page rider-dashboard">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Rider Console</h1>
            <p>Track deliveries, manage pickup tasks, and stay synced with the restaurant.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/dashboard">Home</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
        </div>
    </header>

    <section class="dashboard-grid">
        <article class="dashboard-panel large">
            <h2>Delivery overview</h2>
            <p>Access your assigned tasks and active orders from the rider dashboard.</p>
            <div class="feature-card">
                <span class="badge accent">Live</span>
                <p>New orders will appear here in real time when the system is connected.</p>
            </div>
        </article>

        <article class="dashboard-panel small">
            <h3>Today's tasks</h3>
            <ul class="dashboard-list">
                <li>View restaurant pickup details</li>
                <li>Confirm order collection</li>
                <li>Update delivery status</li>
            </ul>
        </article>

        <article class="dashboard-panel small">
            <h3>Support</h3>
            <p>Need help? Contact the admin for rider assignments and account updates.</p>
        </article>
    </section>
</div>
</body>
</html>
