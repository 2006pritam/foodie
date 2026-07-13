<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Complaint" %>
<%!
    String badgeClass(String status) { return "RESOLVED".equals(status) ? "delivered" : "pending"; }
    String label(String status) {
        if (status == null) return "Open";
        return status.charAt(0) + status.substring(1).toLowerCase();
    }
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
    <title>Complaints | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=19">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page admin-complaints-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Complaints</h1>
            <p>Customer complaints against orders. Newest first.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button outline" href="${pageContext.request.contextPath}/admin/orders">Orders</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("adminComplaintMessage") != null ? request.getAttribute("adminComplaintMessage") : "" %>
    </div>

    <section class="panel">
        <div class="panel-header">
            <h2>All complaints</h2>
        </div>
        <div class="table-scroll">
        <table class="data-table">
            <thead>
            <tr>
                <th>Code</th><th>Customer</th><th>Order</th><th>Problem</th>
                <th>Status</th><th>Reply</th><th>Date</th><th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Complaint> complaints = (List<Complaint>) request.getAttribute("complaints");
                String ctx = request.getContextPath();
                if (complaints == null || complaints.isEmpty()) {
            %>
                <tr><td colspan="8" class="empty-state">No complaints yet.</td></tr>
            <%
                } else {
                    for (Complaint c : complaints) {
            %>
                <tr>
                    <td><%= esc(c.getComplaintCode()) %></td>
                    <td><%= esc(c.getCustomerName()) %></td>
                    <td><%= esc(c.getOrderCode()) %></td>
                    <td><%= esc(c.getMessage()) %></td>
                    <td><span class="order-badge <%= badgeClass(c.getStatus()) %>"><%= label(c.getStatus()) %></span></td>
                    <td><%= c.getAdminReply() == null || c.getAdminReply().isEmpty() ? "&mdash;" : esc(c.getAdminReply()) %></td>
                    <td><%= esc(c.getCreatedAt()) %></td>
                    <td class="item-actions">
                        <% if (!"RESOLVED".equals(c.getStatus())) { %>
                            <button type="button" class="button small"
                                    onclick="openResolve('<%= c.getId() %>', '<%= esc(c.getComplaintCode()) %>')">Resolve</button>
                        <% } else { %>
                            <span class="muted">Resolved</span>
                        <% } %>
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
</div>

<!-- Resolve modal: admin types a reply the customer will see. -->
<div class="pin-overlay" id="resolveOverlay" role="dialog" aria-modal="true" aria-labelledby="resolveTitle" hidden>
    <div class="pin-modal resv-modal">
        <button type="button" class="pin-close" aria-label="Close" onclick="closeResolve()">&times;</button>
        <h2 id="resolveTitle">Resolve complaint</h2>
        <p class="pin-sub">Reply to complaint <strong id="resolveCode"></strong>. The customer will see your message.</p>
        <form method="post" action="${pageContext.request.contextPath}/admin/complaints" id="resolveForm">
            <input type="hidden" name="action" value="resolve" />
            <input type="hidden" name="id" id="resolveId" />
            <div class="form-row">
                <label>Reply to customer</label>
                <textarea name="reply" id="resolveReply" rows="4" required maxlength="1000"
                          placeholder="e.g. Sorry for the trouble — we've refunded your order."></textarea>
            </div>
            <button type="submit" class="button">Send reply &amp; resolve</button>
        </form>
    </div>
</div>

<script>
    (function () {
        var overlay = document.getElementById('resolveOverlay');
        var idField = document.getElementById('resolveId');
        var codeEl  = document.getElementById('resolveCode');
        var reply   = document.getElementById('resolveReply');

        window.openResolve = function (id, code) {
            idField.value = id;
            codeEl.textContent = code;
            reply.value = '';
            overlay.hidden = false;
            reply.focus();
        };
        window.closeResolve = function () { overlay.hidden = true; };
        overlay.addEventListener('click', function (e) { if (e.target === overlay) closeResolve(); });
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !overlay.hidden) closeResolve(); });
    })();
</script>
</body>
</html>
