<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.foodie.model.DiningTable" %>
<%!
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
    String shapeClass(String s) {
        if (s == null) return "square";
        switch (s) {
            case "RECTANGLE": return "rectangle";
            case "CIRCLE":    return "circle";
            case "FAMILY":    return "family";
            default:          return "square";
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Table Booking | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=11">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</head>
<body class="dashboard-page reservations-page">
<%
    String ctx = request.getContextPath();
    List<DiningTable> tables = (List<DiningTable>) request.getAttribute("tables");
    Map<Integer, String>  statusByTable = (Map<Integer, String>) request.getAttribute("statusByTable");
    Map<Integer, Integer> seatsByTable  = (Map<Integer, Integer>) request.getAttribute("seatsByTable");
    String resvDate    = (String) request.getAttribute("resvDate");
    String resvTimeIn  = (String) request.getAttribute("resvTimeIn");
    String resvTimeOut = (String) request.getAttribute("resvTimeOut");
    String[] floors = {"GROUND", "FIRST", "SECOND", "ROOF"};
%>
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Table Booking</h1>
            <p>Pick a date and time, then reserve a free table.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="<%= ctx %>/menu">Browse Menu</a>
            <a class="button outline" href="<%= ctx %>/reservations/my">My Reservations</a>
            <a class="button danger" href="<%= ctx %>/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message">
        <%= request.getAttribute("reservationMessage") != null ? request.getAttribute("reservationMessage") : "" %>
    </div>

    <!-- Date / time window selector: reloads the grid with availability for the chosen slot. -->
    <form class="resv-filter" method="get" action="<%= ctx %>/reservations">
        <div class="resv-filter-field">
            <label>Date</label>
            <input type="date" name="date" value="<%= resvDate %>" required />
        </div>
        <div class="resv-filter-field">
            <label>Time in</label>
            <input type="time" name="timeIn" value="<%= resvTimeIn %>" required />
        </div>
        <div class="resv-filter-field">
            <label>Time out</label>
            <input type="time" name="timeOut" value="<%= resvTimeOut %>" required />
        </div>
        <button type="submit" class="button">Check availability</button>
    </form>

    <div class="resv-legend">
        <span><i class="dot free"></i> Free</span>
        <span><i class="dot booked"></i> Booked</span>
        <span><i class="dot occupied"></i> Occupied</span>
    </div>

    <%
        if (tables == null || tables.isEmpty()) {
    %>
        <section class="panel"><p class="empty-state">No tables are available yet. Please check back soon.</p></section>
    <%
        } else {
    %>
        <div class="floor-tabs" id="floorTabs">
            <% for (int i = 0; i < floors.length; i++) { %>
                <button type="button" class="floor-tab <%= i == 0 ? "active" : "" %>" data-floor="<%= floors[i] %>"><%= floorLabel(floors[i]) %></button>
            <% } %>
        </div>

        <div class="floor-plan" id="floorPlan">
            <%
                for (DiningTable t : tables) {
                    String status = statusByTable.get(t.getId());
                    if (status == null) status = "FREE";
                    int reserved = seatsByTable.get(t.getId()) == null ? 0 : seatsByTable.get(t.getId());
                    boolean free = "FREE".equals(status);
                    String statusLower = status.toLowerCase();
                    String seatText = free ? (t.getCapacity() + " seats")
                            : (reserved > 0 ? reserved + " of " + t.getCapacity() + " seats" : "Reserved");
            %>
                <div class="rtable-card" data-floor="<%= t.getFloor() %>" style="<%= "GROUND".equals(t.getFloor()) ? "" : "display:none" %>">
                    <div class="rtable <%= shapeClass(t.getShape()) %> <%= statusLower %>" data-seats="<%= t.getCapacity() %>">
                        <span class="rtable-name"><%= t.getTableName() %></span>
                    </div>
                    <div class="rtable-meta">
                        <span class="rtable-status <%= statusLower %>"><%= status.charAt(0) + status.substring(1).toLowerCase() %></span>
                        <span class="rtable-seats"><%= seatText %></span>
                        <% if (t.getZone() != null && !t.getZone().isEmpty()) { %>
                            <span class="rtable-zone"><%= t.getZone() %></span>
                        <% } %>
                    </div>
                    <% if (free) { %>
                        <button type="button" class="button small rtable-reserve"
                                onclick="openReserve('<%= t.getId() %>', '<%= t.getTableName() %>', '<%= t.getCapacity() %>')">Reserve</button>
                    <% } else { %>
                        <button type="button" class="button small outline" disabled>Full</button>
                    <% } %>
                </div>
            <%
                }
            %>
        </div>
    <% } %>
</div>

<!-- Reserve modal: capture party size + purpose, and choose table-only or table+food. -->
<div class="pin-overlay" id="reserveOverlay" role="dialog" aria-modal="true" aria-labelledby="reserveTitle" hidden>
    <div class="pin-modal resv-modal">
        <button type="button" class="pin-close" aria-label="Close" onclick="closeReserve()">&times;</button>
        <h2 id="reserveTitle">Reserve table</h2>
        <p class="pin-sub">Table <strong id="reserveTableName"></strong> &middot; <span id="reserveWindow"></span></p>
        <form method="post" action="<%= ctx %>/reservations" id="reserveForm">
            <input type="hidden" name="action" value="create" />
            <input type="hidden" name="tableId" id="reserveTableId" />
            <input type="hidden" name="date" value="<%= resvDate %>" />
            <input type="hidden" name="timeIn" value="<%= resvTimeIn %>" />
            <input type="hidden" name="timeOut" value="<%= resvTimeOut %>" />
            <input type="hidden" name="withFood" id="reserveWithFood" value="0" />
            <div class="form-row">
                <label>Party size</label>
                <input type="number" name="partySize" id="reservePartySize" min="1" value="2" required />
            </div>
            <div class="form-row">
                <label>Purpose (optional)</label>
                <input type="text" name="purpose" placeholder="e.g. Birthday, Business dinner" maxlength="120" />
            </div>
            <div class="resv-modal-actions">
                <button type="submit" class="button outline" onclick="document.getElementById('reserveWithFood').value='0'">Reserve only</button>
                <button type="submit" class="button" onclick="document.getElementById('reserveWithFood').value='1'">Reserve + order food</button>
            </div>
        </form>
    </div>
</div>

<script>
    (function () {
        // Floor tabs: show only the selected floor's tables.
        var tabs = document.querySelectorAll('.floor-tab');
        function selectFloor(floor) {
            for (var i = 0; i < tabs.length; i++) {
                tabs[i].classList.toggle('active', tabs[i].getAttribute('data-floor') === floor);
            }
            var cards = document.querySelectorAll('.rtable-card');
            for (var j = 0; j < cards.length; j++) {
                cards[j].style.display = (cards[j].getAttribute('data-floor') === floor) ? '' : 'none';
            }
        }
        for (var i = 0; i < tabs.length; i++) {
            (function (tab) {
                tab.addEventListener('click', function () { selectFloor(tab.getAttribute('data-floor')); });
            })(tabs[i]);
        }
        if (tabs.length) selectFloor(tabs[0].getAttribute('data-floor'));

        // Reserve modal.
        var overlay = document.getElementById('reserveOverlay');
        var win = '<%= resvTimeIn %> – <%= resvTimeOut %> on <%= resvDate %>';
        window.openReserve = function (id, name, capacity) {
            document.getElementById('reserveTableId').value = id;
            document.getElementById('reserveTableName').textContent = name;
            document.getElementById('reserveWindow').textContent = win;
            var party = document.getElementById('reservePartySize');
            party.max = capacity;
            if (parseInt(party.value, 10) > parseInt(capacity, 10)) party.value = capacity;
            overlay.hidden = false;
            party.focus();
        };
        window.closeReserve = function () { overlay.hidden = true; };
        overlay.addEventListener('click', function (e) { if (e.target === overlay) closeReserve(); });
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !overlay.hidden) closeReserve(); });
    })();
</script>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
