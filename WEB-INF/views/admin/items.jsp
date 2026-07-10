<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.Item" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Items | Foodie Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=16">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page admin-items-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Manage Items</h1>
            <p>Add, update, and delete food items shown in the menu.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/admin/dashboard">Back</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Logout</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <section class="panel">
        <div class="panel-header">
            <h2>Menu Items</h2>
            <p>Upload an image, set price, category and save each entry.</p>
        </div>
        <div class="table-scroll">
        <table class="data-table item-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Image</th>
                <th>Name</th>
                <th>Category</th>
                <th>Price</th>
                <th>Discount</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Item> items = (List<Item>) request.getAttribute("items");
                if (items != null) {
                    for (Item item : items) {
            %>
                <tr>
                    <%
                        String rawImg = item.getImagePath();
                        boolean hasImg = rawImg != null && !rawImg.isEmpty();
                        boolean remoteImg = hasImg && (rawImg.startsWith("http://") || rawImg.startsWith("https://"));
                        String thumbSrc = remoteImg ? rawImg
                                : request.getContextPath() + "/" + (hasImg ? rawImg : "assets/images/food-menu-1.png");
                    %>
                    <td><%= item.getId() %></td>
                    <td>
                        <img class="item-thumb" src="<%= thumbSrc %>" alt="Item image" />
                    </td>
                    <td><%= item.getName() %></td>
                    <td><%= item.getCategory() %></td>
                    <td>Rs <%= String.format("%.2f", item.getPrice()) %></td>
                    <td><%= String.format("%.0f", item.getDiscount()) %>%</td>
                    <td class="item-actions">
                        <form class="inline-form" method="post" enctype="multipart/form-data" action="<%= request.getContextPath() %>/admin/items">
                            <input type="hidden" name="action" value="update" />
                            <input type="hidden" name="id" value="<%= item.getId() %>" />
                            <input type="hidden" name="existingImagePath" value="<%= item.getImagePath() != null ? item.getImagePath() : "" %>" />
                            <div class="item-row-grid">
                                <input type="text" name="name" value="<%= item.getName() %>" required />
                                <input type="text" name="category" value="<%= item.getCategory() %>" required />
                                <input type="number" step="0.01" name="price" value="<%= item.getPrice() %>" required />
                                <input type="number" step="0.01" name="discount" value="<%= item.getDiscount() %>" />
                                <input type="url" name="imageUrl" placeholder="Paste image URL (optional)" value="<%= remoteImg ? rawImg : "" %>" />
                                <input type="file" name="image" accept="image/*" />
                                <button type="submit" class="button small">Save</button>
                            </div>
                        </form>
                        <form class="inline-form" method="post" action="<%= request.getContextPath() %>/admin/items" onsubmit="return confirm('Delete this item?');">
                            <input type="hidden" name="action" value="delete" />
                            <input type="hidden" name="id" value="<%= item.getId() %>" />
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
            <h2>Add New Item</h2>
            <p>Create a new menu entry with price and a photo.</p>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/items" enctype="multipart/form-data" class="admin-form">
            <input type="hidden" name="action" value="create" />
            <div class="form-row">
                <label>Name</label>
                <input type="text" name="name" required />
            </div>
            <div class="form-row">
                <label>Category</label>
                <input type="text" name="category" required />
            </div>
            <div class="form-row">
                <label>Price</label>
                <input type="number" step="0.01" name="price" required />
            </div>
            <div class="form-row">
                <label>Discount %</label>
                <input type="number" step="0.01" name="discount" value="0" />
            </div>
            <div class="form-row">
                <label>Image URL</label>
                <input type="url" name="imageUrl" placeholder="https://example.com/photo.jpg" />
                <small class="hint">Paste a web image link to fetch it directly, or upload a file below.</small>
            </div>
            <div class="form-row">
                <label>Or upload picture</label>
                <input type="file" name="image" accept="image/*" />
            </div>
            <button type="submit" class="button">Add item</button>
        </form>
    </section>

    <div class="panel-message">
        <%= request.getAttribute("adminItemMessage") != null ? request.getAttribute("adminItemMessage") : "" %>
    </div>
</div>
</body>
</html>
