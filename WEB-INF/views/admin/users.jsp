<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Management | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=19">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page admin-users-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>User Management</h1>
            <p>Create, update, and remove users for your restaurant SaaS platform.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <section class="panel">
        <div class="panel-header">
            <h2>Existing accounts</h2>
            <p>Manage active users and roles across your app.</p>
        </div>
        <div class="table-scroll">
        <table class="data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Tenant</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <%
                List<User> adminUsers = (List<User>) request.getAttribute("adminUsers");
                if (adminUsers != null) {
                    for (User user : adminUsers) {
            %>
                <tr>
                    <td><%= user.getId() %></td>
                    <td><%= user.getName() %></td>
                    <td><%= user.getEmail() %></td>
                    <td><%= user.getRole() %></td>
                    <td><%= user.getTenantId() %></td>
                    <td>
                        <form class="inline-form" method="post" action="<%= request.getContextPath() %>/admin/users">
                            <input type="hidden" name="action" value="update" />
                            <input type="hidden" name="id" value="<%= user.getId() %>" />
                            <select name="role" class="select-inline">
                                <option value="USER" <%= "USER".equals(user.getRole()) ? "selected" : "" %>>USER</option>
                                <option value="RIDER" <%= "RIDER".equals(user.getRole()) ? "selected" : "" %>>RIDER</option>
                                <option value="ADMIN" <%= "ADMIN".equals(user.getRole()) ? "selected" : "" %>>ADMIN</option>
                            </select>
                            <input type="text" name="tenantId" value="<%= user.getTenantId() %>" class="input-inline" />
                            <button type="submit" class="button small">Save</button>
                        </form>
                        <form class="inline-form" method="post" action="<%= request.getContextPath() %>/admin/users" onsubmit="return confirm('Delete this user?');">
                            <input type="hidden" name="action" value="delete" />
                            <input type="hidden" name="id" value="<%= user.getId() %>" />
                            <button type="submit" class="button danger small">Delete</button>
                        </form>
                    </td>
                </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>
        </div>
    </section>

    <section class="panel form-panel">
        <div class="panel-header">
            <h2>Create new user</h2>
            <p>Add a new account with a role and tenant assignment.</p>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/users" class="admin-form">
            <input type="hidden" name="action" value="create" />
            <div class="form-row">
                <label>Name</label>
                <input type="text" name="name" required />
            </div>
            <div class="form-row">
                <label>Email</label>
                <input type="email" name="email" required />
            </div>
            <div class="form-row">
                <label>Role</label>
                <select name="role" required>
                    <option value="USER">USER</option>
                    <option value="RIDER">RIDER</option>
                    <option value="ADMIN">ADMIN</option>
                </select>
            </div>
            <div class="form-row">
                <label>Password</label>
                <input type="password" name="password" required minlength="8" />
            </div>
            <div class="form-row">
                <label>Tenant ID</label>
                <input type="number" name="tenantId" value="1" required />
            </div>
            <button type="submit" class="button">Create user</button>
        </form>
    </section>

    <div class="panel-message">
        <%= request.getAttribute("adminMessage") != null ? request.getAttribute("adminMessage") : "" %>
    </div>
</div>
</body>
</html>
