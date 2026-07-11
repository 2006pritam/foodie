<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Coupon" %>
<%!
    String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    String discountText(Coupon c) {
        if (Coupon.PERCENT.equals(c.getType())) {
            return String.format("%.0f%% off", c.getValue());
        }
        return "Rs " + String.format("%.0f", c.getValue()) + " off";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Coupons | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=17">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page admin-coupons-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Coupons</h1>
            <p>Create discount codes customers can apply at checkout.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("adminCouponMessage") != null ? request.getAttribute("adminCouponMessage") : "" %>
    </div>

    <section class="panel form-panel">
        <div class="panel-header">
            <h2>Create a coupon</h2>
            <p>Pick a code, a discount (percentage or flat Rs), and optional rules.</p>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/coupons" class="admin-form">
            <input type="hidden" name="action" value="create" />
            <div class="form-row">
                <label>Coupon code</label>
                <input type="text" name="code" placeholder="e.g. SAVE20" required
                       pattern="[A-Za-z0-9_-]{3,40}" title="3–40 letters, digits, - or _" />
            </div>
            <div class="form-row">
                <label>Discount type</label>
                <select name="type" id="couponType" onchange="couponTypeHint()">
                    <option value="PERCENT">Percentage (%)</option>
                    <option value="FLAT">Flat amount (Rs)</option>
                </select>
            </div>
            <div class="form-row">
                <label>Discount value</label>
                <input type="number" name="value" step="0.01" min="0.01" required />
                <small class="hint" id="valueHint">e.g. 20 = 20% off the order subtotal.</small>
            </div>
            <div class="form-row">
                <label>Minimum order (Rs, optional)</label>
                <input type="number" name="min_order" step="0.01" min="0" value="0" />
                <small class="hint">Coupon only applies when the cart subtotal reaches this amount.</small>
            </div>
            <div class="form-row">
                <label>Expiry date (optional)</label>
                <input type="date" name="expiry_date" />
                <small class="hint">Leave blank for a coupon that never expires.</small>
            </div>
            <button type="submit" class="button">Create coupon</button>
        </form>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2>Existing coupons</h2>
            <p>Toggle a coupon on/off or delete it.</p>
        </div>
        <div class="table-scroll">
        <table class="data-table">
            <thead>
            <tr>
                <th>Code</th><th>Discount</th><th>Min order</th>
                <th>Expiry</th><th>Status</th><th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Coupon> coupons = (List<Coupon>) request.getAttribute("coupons");
                String ctx = request.getContextPath();
                if (coupons == null || coupons.isEmpty()) {
            %>
                <tr><td colspan="6" class="empty-state">No coupons yet. Create your first above.</td></tr>
            <%
                } else {
                    for (Coupon c : coupons) {
            %>
                <tr>
                    <td><strong><%= esc(c.getCode()) %></strong></td>
                    <td><%= discountText(c) %></td>
                    <td><%= c.getMinOrder() > 0 ? "Rs " + String.format("%.0f", c.getMinOrder()) : "—" %></td>
                    <td><%= c.getExpiryDate() == null ? "Never" : esc(c.getExpiryDate()) %></td>
                    <td>
                        <span class="order-badge <%= c.isActive() ? "delivered" : "pending" %>">
                            <%= c.isActive() ? "Active" : "Inactive" %>
                        </span>
                    </td>
                    <td class="item-actions">
                        <form class="inline-form" method="post" action="<%= ctx %>/admin/coupons">
                            <input type="hidden" name="action" value="toggle" />
                            <input type="hidden" name="id" value="<%= c.getId() %>" />
                            <button type="submit" class="button small outline"><%= c.isActive() ? "Disable" : "Enable" %></button>
                        </form>
                        <form class="inline-form" method="post" action="<%= ctx %>/admin/coupons"
                              onsubmit="return confirm('Delete coupon <%= esc(c.getCode()) %>?');">
                            <input type="hidden" name="action" value="delete" />
                            <input type="hidden" name="id" value="<%= c.getId() %>" />
                            <button type="submit" class="button small danger">Delete</button>
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
</div>

<script>
    function couponTypeHint() {
        var t = document.getElementById('couponType').value;
        document.getElementById('valueHint').textContent = (t === 'FLAT')
            ? 'e.g. 100 = Rs 100 off the order subtotal.'
            : 'e.g. 20 = 20% off the order subtotal.';
    }
    couponTypeHint();
</script>
</body>
</html>
