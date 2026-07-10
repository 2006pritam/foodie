<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Order" %>
<%@ page import="com.foodie.model.Feedback" %>
<%!
    String badgeClass(String status) { return status == null ? "pending" : status.toLowerCase(); }
    String label(String status) {
        if (status == null) return "Pending";
        if ("PICKED_UP".equals(status)) return "Picked up";
        return status.charAt(0) + status.substring(1).toLowerCase();
    }
    int num(Object o) { return o == null ? 0 : ((Number) o).intValue(); }
    String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | Foodie SaaS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=16">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page admin-dashboard">
<div class="admin-dashboard-layout">
    <aside class="admin-sidebar">
        <div class="sidebar-brand">
            <span class="sidebar-logo">Foodie Admin</span>
            <p>Welcome, admin</p>
        </div>

        <nav class="sidebar-nav">
            <a class="sidebar-link active" href="#">Dashboard</a>
            <a class="sidebar-link" href="${pageContext.request.contextPath}/admin/items">Manage Items</a>
            <a class="sidebar-link" href="${pageContext.request.contextPath}/admin/orders">Orders</a>
            <a class="sidebar-link" href="${pageContext.request.contextPath}/admin/tables">Manage Tables</a>
            <a class="sidebar-link" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="sidebar-link" href="${pageContext.request.contextPath}/admin/complaints">Complaints</a>
            <a class="sidebar-link" href="${pageContext.request.contextPath}/admin/users">Manage Users</a>
        </nav>

        <a class="sidebar-logout" href="${pageContext.request.contextPath}/logout">Logout</a>
    </aside>

    <main class="admin-main">
        <header class="admin-topbar">
            <div>
                <span class="eyebrow">Dashboard Overview</span>
                <h1>Admin Overview</h1>
                <p class="topbar-copy">Track orders, view menus, and manage restaurant operations from one unified panel.</p>
            </div>
            <div class="admin-topbar-actions">
                <a class="button outline" href="${pageContext.request.contextPath}/dashboard">Home</a>
                <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
                <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
            </div>
        </header>

        <div class="admin-overview">
            <section class="metric-section">
                <div class="section-title">
                    <h2>Overview</h2>
                    <span>Key figures at a glance</span>
                </div>
                <div class="metric-cards">
                    <article class="metric-card accent-yellow">
                        <span class="metric-label">Total Items</span>
                        <strong><%= num(request.getAttribute("totalItems")) %></strong>
                        <small>In menu</small>
                    </article>
                    <article class="metric-card accent-blue">
                        <span class="metric-label">Total Orders</span>
                        <strong><%= num(request.getAttribute("totalOrders")) %></strong>
                        <small>All time</small>
                    </article>
                    <article class="metric-card accent-purple">
                        <span class="metric-label">Feedback</span>
                        <strong><%= num(request.getAttribute("feedbackCount")) %></strong>
                        <small>Messages received</small>
                    </article>
                    <article class="metric-card accent-red">
                        <span class="metric-label">Open Complaints</span>
                        <strong><%= num(request.getAttribute("openComplaints")) %></strong>
                        <small><a href="${pageContext.request.contextPath}/admin/complaints">View all</a></small>
                    </article>
                </div>
            </section>

            <section class="metric-section">
                <div class="section-title">
                    <h2>Order Status</h2>
                    <span>Live breakdown of the order pipeline</span>
                </div>
                <div class="metric-cards status-metrics">
                    <article class="metric-card accent-yellow">
                        <span class="metric-label">Pending</span>
                        <strong><%= num(request.getAttribute("pendingCount")) %></strong>
                        <small>Awaiting review</small>
                    </article>
                    <article class="metric-card accent-blue">
                        <span class="metric-label">Accepted</span>
                        <strong><%= num(request.getAttribute("acceptedCount")) %></strong>
                        <small>In progress</small>
                    </article>
                    <article class="metric-card accent-purple">
                        <span class="metric-label">Picked up</span>
                        <strong><%= num(request.getAttribute("pickedUpCount")) %></strong>
                        <small>Out for delivery</small>
                    </article>
                    <article class="metric-card accent-green">
                        <span class="metric-label">Delivered</span>
                        <strong><%= num(request.getAttribute("deliveredCount")) %></strong>
                        <small>Completed</small>
                    </article>
                    <article class="metric-card accent-red">
                        <span class="metric-label">Rejected</span>
                        <strong><%= num(request.getAttribute("rejectedCount")) %></strong>
                        <small>Declined</small>
                    </article>
                </div>
            </section>
        </div>

        <section class="dashboard-grid">
            <article class="dashboard-panel panel-large feedback-panel">
                <div class="panel-header">
                    <h2>Customer Feedback</h2>
                </div>
                <ul class="feedback-list">
                    <%
                        List<Feedback> recentFeedback = (List<Feedback>) request.getAttribute("recentFeedback");
                        if (recentFeedback == null || recentFeedback.isEmpty()) {
                    %>
                        <li><span class="order-meta">No feedback yet.</span></li>
                    <%
                        } else {
                            for (Feedback f : recentFeedback) {
                    %>
                        <li class="feedback-item">
                            <div class="feedback-head">
                                <span class="order-title"><%= esc(f.getName()) %></span>
                                <span class="order-meta"><%= esc(f.getCreatedAt()) %></span>
                            </div>
                            <% if (f.getEmail() != null && !f.getEmail().isEmpty()) { %>
                                <span class="order-meta"><%= esc(f.getEmail()) %></span>
                            <% } %>
                            <p class="feedback-message"><%= esc(f.getMessage()) %></p>
                        </li>
                    <%
                            }
                        }
                    %>
                </ul>
            </article>

            <article class="dashboard-panel panel-large recent-orders">
                <div class="panel-header">
                    <h2>Recent Orders</h2>
                    <a class="button small outline" href="${pageContext.request.contextPath}/admin/orders">View All</a>
                </div>
                <ul class="order-list">
                    <%
                        List<Order> recentOrders = (List<Order>) request.getAttribute("recentOrders");
                        if (recentOrders == null || recentOrders.isEmpty()) {
                    %>
                        <li><span class="order-meta">No orders yet.</span></li>
                    <%
                        } else {
                            for (Order o : recentOrders) {
                    %>
                        <li>
                            <div>
                                <span class="order-title"><%= o.getCustomerName() == null ? "Customer" : o.getCustomerName() %></span>
                                <span class="order-meta"><%= o.getOrderCode() %></span>
                            </div>
                            <div class="order-state">
                                <span class="order-badge <%= badgeClass(o.getStatus()) %>"><%= label(o.getStatus()) %></span>
                                <strong>Rs <%= String.format("%.0f", o.getTotal()) %></strong>
                            </div>
                        </li>
                    <%
                            }
                        }
                    %>
                </ul>
            </article>

            <article class="dashboard-panel categories-panel">
                <div class="panel-header">
                    <h2>Items by Category</h2>
                </div>
                <ul class="category-list">
                    <li>
                        <span>Beverages & Extras</span>
                        <div class="progress-bar"><span style="width: 60%"></span></div>
                        <strong>3</strong>
                    </li>
                    <li>
                        <span>Combo Box</span>
                        <div class="progress-bar"><span style="width: 45%"></span></div>
                        <strong>2</strong>
                    </li>
                    <li>
                        <span>Crazy Doubles</span>
                        <div class="progress-bar"><span style="width: 55%"></span></div>
                        <strong>2</strong>
                    </li>
                    <li>
                        <span>Desserts</span>
                        <div class="progress-bar"><span style="width: 62%"></span></div>
                        <strong>3</strong>
                    </li>
                    <li>
                        <span>Pasta | Sandwich | Calzone</span>
                        <div class="progress-bar"><span style="width: 50%"></span></div>
                        <strong>3</strong>
                    </li>
                    <li>
                        <span>Pizza Flavors</span>
                        <div class="progress-bar"><span style="width: 58%"></span></div>
                        <strong>3</strong>
                    </li>
                    <li>
                        <span>Popular!</span>
                        <div class="progress-bar"><span style="width: 75%"></span></div>
                        <strong>4</strong>
                    </li>
                    <li>
                        <span>Starters</span>
                        <div class="progress-bar"><span style="width: 53%"></span></div>
                        <strong>3</strong>
                    </li>
                </ul>
            </article>

            <article class="dashboard-panel popular-panel">
                <div class="panel-header">
                    <h2>Popular Items</h2>
                    <a class="button small outline" href="#">Manage</a>
                </div>
                <ul class="popular-list">
                    <li>
                        <span>Supreme Pizza</span>
                        <strong>24 orders</strong>
                    </li>
                    <li>
                        <span>Cheesy Garlic Bread</span>
                        <strong>19 orders</strong>
                    </li>
                    <li>
                        <span>Spicy Chicken Wings</span>
                        <strong>16 orders</strong>
                    </li>
                </ul>
            </article>
        </section>
    </main>
</div>
</body>
</html>
