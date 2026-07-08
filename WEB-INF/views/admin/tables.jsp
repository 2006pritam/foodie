<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.DiningTable" %>
<%!
    String shapeOpt(String cur, String val) {
        return "<option value=\"" + val + "\"" + (val.equals(cur) ? " selected" : "") + ">"
                + val.charAt(0) + val.substring(1).toLowerCase() + "</option>";
    }
    String floorLabel(String f) {
        if (f == null) return "";
        switch (f) {
            case "GROUND": return "Ground";
            case "FIRST":  return "1st floor";
            case "SECOND": return "2nd floor";
            case "ROOF":   return "Roof";
            default:       return f;
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Table Management | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=14">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</head>
<body class="dashboard-page admin-tables-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Table Management</h1>
            <p>Create the tables customers can reserve — shape, seats, floor and zone.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button outline" href="${pageContext.request.contextPath}/admin/reservations">Reservations</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <section class="panel">
        <div class="panel-header">
            <h2>Existing tables</h2>
            <p>Edit shape, capacity, floor, zone or deactivate a table.</p>
        </div>
        <table class="data-table">
            <thead>
                <tr>
                    <th>ID</th><th>Name</th><th>Shape</th><th>Seats</th>
                    <th>Floor</th><th>Zone</th><th>Active</th><th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <%
                List<DiningTable> tables = (List<DiningTable>) request.getAttribute("tables");
                String ctx = request.getContextPath();
                if (tables == null || tables.isEmpty()) {
            %>
                <tr><td colspan="8" class="empty-state">No tables yet. Add your first below.</td></tr>
            <%
                } else {
                    for (DiningTable t : tables) {
            %>
                <tr>
                    <td><%= t.getId() %></td>
                    <td>
                        <form class="inline-form" method="post" action="<%= ctx %>/admin/tables">
                            <input type="hidden" name="action" value="update" />
                            <input type="hidden" name="id" value="<%= t.getId() %>" />
                            <input type="text" name="table_name" value="<%= t.getTableName() %>" class="input-inline" required />
                    </td>
                    <td>
                            <select name="shape" class="select-inline">
                                <%= shapeOpt(t.getShape(), "SQUARE") %>
                                <%= shapeOpt(t.getShape(), "RECTANGLE") %>
                                <%= shapeOpt(t.getShape(), "CIRCLE") %>
                                <%= shapeOpt(t.getShape(), "FAMILY") %>
                            </select>
                    </td>
                    <td>
                            <input type="number" name="capacity" value="<%= t.getCapacity() %>" min="1" class="input-inline" style="width:70px" required />
                    </td>
                    <td>
                            <select name="floor" class="select-inline">
                                <option value="GROUND" <%= "GROUND".equals(t.getFloor()) ? "selected" : "" %>>Ground</option>
                                <option value="FIRST"  <%= "FIRST".equals(t.getFloor())  ? "selected" : "" %>>1st floor</option>
                                <option value="SECOND" <%= "SECOND".equals(t.getFloor()) ? "selected" : "" %>>2nd floor</option>
                                <option value="ROOF"   <%= "ROOF".equals(t.getFloor())   ? "selected" : "" %>>Roof</option>
                            </select>
                    </td>
                    <td>
                            <input type="text" name="zone" value="<%= t.getZone() == null ? "" : t.getZone() %>" class="input-inline" style="width:90px" />
                    </td>
                    <td>
                            <input type="checkbox" name="active" <%= t.isActive() ? "checked" : "" %> />
                    </td>
                    <td>
                            <button type="submit" class="button small">Save</button>
                        </form>
                        <form class="inline-form" method="post" action="<%= ctx %>/admin/tables" onsubmit="return confirm('Delete this table?');">
                            <input type="hidden" name="action" value="delete" />
                            <input type="hidden" name="id" value="<%= t.getId() %>" />
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
    </section>

    <section class="panel form-panel">
        <div class="panel-header">
            <h2>Add a table</h2>
            <p>Define a custom table for the floor plan.</p>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/tables" class="admin-form">
            <input type="hidden" name="action" value="create" />
            <div class="form-row">
                <label>Table name / number</label>
                <input type="text" name="table_name" placeholder="e.g. T1 or VIP-2" required />
            </div>
            <div class="form-row">
                <label>Shape</label>
                <select name="shape" required>
                    <option value="SQUARE">Square</option>
                    <option value="RECTANGLE">Rectangle</option>
                    <option value="CIRCLE">Circle</option>
                    <option value="FAMILY">Family / large</option>
                </select>
            </div>
            <div class="form-row">
                <label>Capacity (chairs)</label>
                <input type="number" name="capacity" value="4" min="1" required />
            </div>
            <div class="form-row">
                <label>Floor</label>
                <select name="floor" required>
                    <option value="GROUND">Ground</option>
                    <option value="FIRST">1st floor</option>
                    <option value="SECOND">2nd floor</option>
                    <option value="ROOF">Roof</option>
                </select>
            </div>
            <div class="form-row">
                <label>Zone / purpose (optional)</label>
                <input type="text" name="zone" placeholder="e.g. VIP, Terrace, Family" />
            </div>
            <button type="submit" class="button">Add table</button>
        </form>
    </section>

    <div class="panel-message">
        <%= request.getAttribute("adminTableMessage") != null ? request.getAttribute("adminTableMessage") : "" %>
    </div>
</div>
</body>
</html>
