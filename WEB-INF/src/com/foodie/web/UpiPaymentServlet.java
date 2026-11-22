package com.foodie.web;

import com.foodie.db.CouponDao;
import com.foodie.db.ItemDao;
import com.foodie.db.OrderDao;
import com.foodie.db.ReservationDao;
import com.foodie.mail.MailService;
import com.foodie.model.Coupon;
import com.foodie.model.Item;
import com.foodie.model.Order;
import com.foodie.model.OrderItem;
import com.foodie.payment.UpiGatewayService;
import com.foodie.payment.UpiGatewayService.GatewayOrder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet endpoints integrating {@code 2006pritam/upi_gateway} into Foodie's
 * checkout flow without modifying existing CARD/COD order placement:
 * <ul>
 *   <li>{@code POST /payment/upi/create}  — creates a gateway order for the
 *       current cart and stores the pending checkout snapshot in session.</li>
 *   <li>{@code GET  /payment/upi/status}  — polls the gateway, finalizes the
 *       Foodie order in PostgreSQL once {@code status == "paid"}, and returns
 *       the redirect URL to {@code /orders?placed=...}.</li>
 *   <li>{@code GET  /payment/upi/return}  — handles redirect back from the
 *       gateway's hosted checkout page.</li>
 *   <li>{@code POST /payment/upi/webhook} — verifies HMAC-SHA256
 *       {@code X-Signature} webhooks from {@code upi_gateway}.</li>
 * </ul>
 */
public class UpiPaymentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpiPaymentServlet.class.getName());
    private static final String SESSION_PENDING_KEY = "pendingUpiOrders";

    private ItemDao itemDao;
    private CouponDao couponDao;
    private OrderDao orderDao;
    private ReservationDao reservationDao;

    @Override
    public void init() throws ServletException {
        this.itemDao = new ItemDao();
        this.couponDao = new CouponDao();
        this.orderDao = new OrderDao();
        this.reservationDao = new ReservationDao();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String path = req.getServletPath();
        if ("/payment/upi/create".equals(path)) {
            handleCreate(req, res);
        } else if ("/payment/upi/webhook".equals(path)) {
            handleWebhook(req, res);
        } else {
            sendJsonError(res, HttpServletResponse.SC_NOT_FOUND, "Unknown payment endpoint.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/payment/upi/status".equals(path)) {
            handleStatus(req, res);
        } else if ("/payment/upi/return".equals(path)) {
            handleReturn(req, res);
        } else {
            sendJsonError(res, HttpServletResponse.SC_NOT_FOUND, "Unknown payment endpoint.");
        }
    }

    // ------------------------------------------------------------------
    // POST /payment/upi/create
    // ------------------------------------------------------------------

    private void handleCreate(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || intAttr(session, "userId") <= 0) {
            sendJsonError(res, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in to place an order.");
            return;
        }

        UpiGatewayService gateway = UpiGatewayService.getInstance();
        if (!gateway.isReady()) {
            sendJsonError(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "UPI payment gateway is not configured yet. Set UPI_GATEWAY_BASE_URL, UPI_GATEWAY_API_KEY, and UPI_GATEWAY_API_SECRET.");
            return;
        }

        Map<Integer, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            sendJsonError(res, HttpServletResponse.SC_BAD_REQUEST, "Your cart is empty.");
            return;
        }

        String address = trimParam(req, "address");
        String phone   = trimParam(req, "phone");
        String email   = trimParam(req, "email");

        int resvTableId      = intAttr(session, "resvTableId");
        String resvTableName = (String) session.getAttribute("resvTableName");
        int resvId           = intAttr(session, "resvId");
        boolean dineIn       = resvTableId > 0 && resvTableName != null;
        if (dineIn && blank(address)) {
            address = "Dine-in - Table " + resvTableName;
        }

        if ((!dineIn && blank(address)) || blank(phone) || blank(email)) {
            sendJsonError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Address, phone, and email are required before paying with UPI.");
            return;
        }

        try {
            List<OrderItem> lines = buildCartLines(cart);
            if (lines.isEmpty()) {
                cart.clear();
                sendJsonError(res, HttpServletResponse.SC_BAD_REQUEST, "Your cart items are no longer available.");
                return;
            }

            double total = 0;
            for (OrderItem line : lines) {
                total += line.getLineTotal();
            }
            double subtotal = round2(total);

            double discount = 0;
            String couponCode = null;
            String sessionCoupon = (String) session.getAttribute("checkoutCoupon");
            if (sessionCoupon != null) {
                Coupon c = couponDao.findUsable(sessionCoupon, subtotal);
                if (c != null) {
                    discount = round2(c.discountFor(subtotal));
                    couponCode = c.getCode();
                }
            }
            double payable = round2(Math.max(0, subtotal - discount));
            if (payable < 1.0) {
                sendJsonError(res, HttpServletResponse.SC_BAD_REQUEST,
                        "UPI payments require a payable amount of at least Rs 1.00.");
                return;
            }

            String clientOrderId = "ORD" + System.currentTimeMillis();
            String customerName  = String.valueOf(session.getAttribute("userName"));
            String redirectUrl   = gateway.buildRedirectUrl(req.getContextPath(), clientOrderId);

            GatewayOrder created = gateway.createOrder(
                    clientOrderId,
                    payable,
                    customerName,
                    email,
                    phone,
                    "Foodie " + clientOrderId,
                    redirectUrl
            );

            if (!created.ok) {
                sendJsonError(res, HttpServletResponse.SC_BAD_GATEWAY,
                        created.errorMessage != null ? created.errorMessage : "Could not initiate UPI payment.");
                return;
            }

            PendingUpiCheckout pending = new PendingUpiCheckout();
            pending.clientOrderId  = clientOrderId;
            pending.gatewayOrderId = created.orderId;
            pending.userId         = intAttr(session, "userId");
            pending.customerName   = customerName;
            pending.tenantId       = intAttr(session, "tenantId") > 0 ? intAttr(session, "tenantId") : 1;
            pending.address        = address;
            pending.phone          = phone;
            pending.email          = email;
            pending.couponCode     = couponCode;
            pending.discount       = discount;
            pending.payable        = payable;
            pending.dineIn         = dineIn;
            pending.resvTableId    = resvTableId;
            pending.resvTableName  = resvTableName;
            pending.resvId         = resvId;
            pending.lines          = lines;

            getPendingMap(session).put(clientOrderId, pending);

            StringBuilder json = new StringBuilder(384);
            json.append("{\"ok\":true");
            json.append(",\"client_order_id\":\"").append(UpiGatewayService.jsonEscape(clientOrderId)).append("\"");
            json.append(",\"gateway_order_id\":\"").append(UpiGatewayService.jsonEscape(created.orderId)).append("\"");
            json.append(",\"amount\":\"").append(String.format(Locale.US, "%.2f", payable)).append("\"");
            json.append(",\"status\":\"").append(UpiGatewayService.jsonEscape(nullToEmpty(created.status))).append("\"");
            json.append(",\"payment_url\":").append(jsonNullableString(created.paymentUrl));
            json.append(",\"upi_intent\":").append(jsonNullableString(created.upiIntent));
            json.append(",\"expires_at\":").append(jsonNullableString(created.expiresAt));
            json.append(",\"test\":").append(created.test);
            json.append("}");
            sendJson(res, HttpServletResponse.SC_OK, json.toString());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error preparing UPI checkout", e);
            sendJsonError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "A server error occurred while preparing your order.");
        }
    }

    // ------------------------------------------------------------------
    // GET /payment/upi/status?order=<clientOrderId>
    // ------------------------------------------------------------------

    private void handleStatus(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || intAttr(session, "userId") <= 0) {
            sendJsonError(res, HttpServletResponse.SC_UNAUTHORIZED, "Session expired. Please sign in again.");
            return;
        }

        String clientOrderId = trimParam(req, "order");
        if (blank(clientOrderId)) {
            sendJsonError(res, HttpServletResponse.SC_BAD_REQUEST, "Missing order parameter.");
            return;
        }

        PendingUpiCheckout pending = getPendingMap(session).get(clientOrderId);
        if (pending == null) {
            sendJsonError(res, HttpServletResponse.SC_NOT_FOUND, "No pending UPI checkout found for this order.");
            return;
        }

        if (pending.finalized) {
            sendJson(res, HttpServletResponse.SC_OK, buildStatusJson(
                    "paid", pending.utr, null, buildPlacedRedirect(req, pending.clientOrderId)));
            return;
        }

        GatewayOrder remote = UpiGatewayService.getInstance().getOrderStatus(pending.gatewayOrderId);
        if (!remote.ok) {
            sendJsonError(res, HttpServletResponse.SC_BAD_GATEWAY,
                    remote.errorMessage != null ? remote.errorMessage : "Unable to check UPI payment status.");
            return;
        }

        if (remote.isPaid()) {
            try {
                boolean created = finalizeOrder(session, pending, remote);
                if (!created) {
                    sendJsonError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Payment received, but we couldn't save your order. Please contact support with reference "
                                    + pending.gatewayOrderId + ".");
                    return;
                }
                sendJson(res, HttpServletResponse.SC_OK, buildStatusJson(
                        "paid", pending.utr, remote.paymentUrl, buildPlacedRedirect(req, pending.clientOrderId)));
                return;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to finalize paid UPI order " + pending.clientOrderId, e);
                sendJsonError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Payment verified, but saving the order failed. Reference: " + pending.gatewayOrderId);
                return;
            }
        }

        sendJson(res, HttpServletResponse.SC_OK,
                buildStatusJson(nullToEmpty(remote.status), remote.utr, remote.paymentUrl, null));
    }

    // ------------------------------------------------------------------
    // GET /payment/upi/return?order=<clientOrderId>
    // ------------------------------------------------------------------

    private void handleReturn(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        String ctx = req.getContextPath();
        if (session == null || intAttr(session, "userId") <= 0) {
            res.sendRedirect(ctx + "/login");
            return;
        }

        String clientOrderId = trimParam(req, "order");
        PendingUpiCheckout pending = blank(clientOrderId) ? null : getPendingMap(session).get(clientOrderId);
        if (pending == null) {
            res.sendRedirect(ctx + "/orders");
            return;
        }

        if (pending.finalized) {
            res.sendRedirect(buildPlacedRedirect(req, pending.clientOrderId));
            return;
        }

        GatewayOrder remote = UpiGatewayService.getInstance().getOrderStatus(pending.gatewayOrderId);
        if (remote.ok && remote.isPaid()) {
            try {
                if (finalizeOrder(session, pending, remote)) {
                    res.sendRedirect(buildPlacedRedirect(req, pending.clientOrderId));
                    return;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error finalizing UPI order on return", e);
            }
        }

        res.sendRedirect(ctx + "/checkout");
    }

    // ------------------------------------------------------------------
    // POST /payment/upi/webhook
    // ------------------------------------------------------------------

    private void handleWebhook(HttpServletRequest req, HttpServletResponse res) throws IOException {
        UpiGatewayService gateway = UpiGatewayService.getInstance();
        if (!gateway.isReady()) {
            sendJsonError(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway not configured.");
            return;
        }

        String rawBody = readRequestBody(req);
        String signature = req.getHeader("X-Signature");
        if (!gateway.verifyWebhookSignature(rawBody, signature)) {
            LOGGER.warning("Rejected UPI webhook with invalid X-Signature.");
            sendJsonError(res, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature.");
            return;
        }

        String event = req.getHeader("X-Event");
        String data = UpiGatewayService.sliceObject(rawBody, "data");
        String gatewayOrderId = data != null ? UpiGatewayService.extractJsonString(data, "order_id") : null;
        String clientOrderId  = data != null ? UpiGatewayService.extractJsonString(data, "client_order_id") : null;
        String status         = data != null ? UpiGatewayService.extractJsonString(data, "status") : null;
        String utr            = data != null ? UpiGatewayService.extractJsonString(data, "utr") : null;

        LOGGER.info("Verified UPI webhook: event=" + event
                + " order_id=" + gatewayOrderId
                + " client_order_id=" + clientOrderId
                + " status=" + status
                + " utr=" + utr);

        sendJson(res, HttpServletResponse.SC_OK, "{\"received\":true}");
    }

    // ------------------------------------------------------------------
    // Order finalization & helpers
    // ------------------------------------------------------------------

    private boolean finalizeOrder(HttpSession session, PendingUpiCheckout pending, GatewayOrder remote)
            throws SQLException {
        synchronized (pending) {
            if (pending.finalized) {
                return true;
            }

            Order order = new Order();
            order.setOrderCode(pending.clientOrderId);
            order.setUserId(pending.userId);
            order.setCustomerName(pending.customerName);
            order.setTenantId(pending.tenantId);
            order.setAddress(pending.address);
            order.setPhone(pending.phone);
            order.setEmail(pending.email);
            order.setPaymentMethod("UPI");
            order.setCouponCode(pending.couponCode);
            order.setDiscount(pending.discount);
            order.setTotal(pending.payable);

            StringBuilder proof = new StringBuilder("UPI Gateway ").append(pending.gatewayOrderId);
            if (!blank(remote.utr)) {
                proof.append(" | UTR ").append(remote.utr);
            } else if (remote.test) {
                proof.append(" (SANDBOX)");
            }
            order.setPaymentProof(proof.toString());

            if (pending.dineIn) {
                order.setTableId(pending.resvTableId);
                order.setTableName(pending.resvTableName);
            }

            int newId = orderDao.createOrder(order, pending.lines);
            if (newId <= 0) {
                return false;
            }

            pending.finalized = true;
            pending.utr = remote.utr;

            Map<Integer, Integer> cart = getCart(session);
            cart.clear();
            session.removeAttribute("checkoutCoupon");

            order.setId(newId);
            order.setStatus(OrderDao.PENDING);
            order.setItems(pending.lines);
            MailService.getInstance().sendOrderConfirmation(order);

            if (pending.dineIn) {
                if (pending.resvId > 0) {
                    try {
                        reservationDao.linkOrder(pending.resvId, newId);
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Failed to link UPI order to reservation", e);
                    }
                }
                session.removeAttribute("resvTableId");
                session.removeAttribute("resvTableName");
                session.removeAttribute("resvId");
            }
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getCart(HttpSession session) {
        Object c = session.getAttribute("cart");
        if (c instanceof Map) {
            return (Map<Integer, Integer>) c;
        }
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        session.setAttribute("cart", cart);
        return cart;
    }

    @SuppressWarnings("unchecked")
    private Map<String, PendingUpiCheckout> getPendingMap(HttpSession session) {
        Object existing = session.getAttribute(SESSION_PENDING_KEY);
        if (existing instanceof Map) {
            return (Map<String, PendingUpiCheckout>) existing;
        }
        Map<String, PendingUpiCheckout> map = new ConcurrentHashMap<>();
        session.setAttribute(SESSION_PENDING_KEY, map);
        return map;
    }

    private List<OrderItem> buildCartLines(Map<Integer, Integer> cart) throws SQLException {
        List<OrderItem> lines = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            int qty = e.getValue();
            if (qty <= 0) continue;
            Item item = itemDao.findById(e.getKey());
            if (item == null) continue;
            double pct = Math.max(0, Math.min(100, item.getDiscount()));
            double unitPrice = round2(item.getPrice() * (1.0 - pct / 100.0));
            lines.add(new OrderItem(item.getId(), item.getName(), unitPrice, qty));
        }
        return lines;
    }

    private static String buildPlacedRedirect(HttpServletRequest req, String orderCode) {
        String encoded = URLEncoder.encode(orderCode, StandardCharsets.UTF_8);
        return req.getContextPath() + "/orders?placed=" + encoded;
    }

    private static String buildStatusJson(String status, String utr, String paymentUrl, String redirect) {
        return "{\"ok\":true"
                + ",\"status\":\"" + UpiGatewayService.jsonEscape(nullToEmpty(status)) + "\""
                + ",\"utr\":" + jsonNullableString(utr)
                + ",\"payment_url\":" + jsonNullableString(paymentUrl)
                + ",\"redirect\":" + jsonNullableString(redirect)
                + "}";
    }

    private static void sendJson(HttpServletResponse res, int status, String json) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Cache-Control", "no-store");
        res.getWriter().write(json);
    }

    private static void sendJsonError(HttpServletResponse res, int status, String message) throws IOException {
        sendJson(res, status, "{\"ok\":false,\"message\":\"" + UpiGatewayService.jsonEscape(message) + "\"}");
    }

    private static String jsonNullableString(String v) {
        return v == null ? "null" : "\"" + UpiGatewayService.jsonEscape(v) + "\"";
    }

    private static String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static int intAttr(HttpSession session, String name) {
        if (session == null) return 0;
        Object o = session.getAttribute(name);
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private static String trimParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    private static boolean blank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static final class PendingUpiCheckout implements Serializable {
        private static final long serialVersionUID = 1L;
        String clientOrderId;
        String gatewayOrderId;
        int userId;
        String customerName;
        int tenantId;
        String address;
        String phone;
        String email;
        String couponCode;
        double discount;
        double payable;
        boolean dineIn;
        int resvTableId;
        String resvTableName;
        int resvId;
        List<OrderItem> lines;
        boolean finalized;
        String utr;
    }
}
