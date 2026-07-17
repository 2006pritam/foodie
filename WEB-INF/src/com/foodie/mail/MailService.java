package com.foodie.mail;

import com.foodie.model.Order;
import com.foodie.model.OrderItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin server-side client for the Resend transactional-email API
 * ({@code POST https://api.resend.com/emails}). The API key never leaves the
 * server. Mirrors the design of {@link com.foodie.chat.ChatService}: a lazy
 * singleton, configured from environment variables (preferred) with a
 * {@code db.properties} fallback, that never throws — every failure is logged
 * and swallowed so a mail hiccup can never break order placement.
 *
 * <p>Sends are fired asynchronously ({@link HttpClient#sendAsync}) so the HTTP
 * request handler that triggered them returns immediately.</p>
 *
 * <p>Config keys (env first, then db.properties):</p>
 * <ul>
 *   <li>{@code RESEND_API_KEY} / {@code resend.api.key} — required.</li>
 *   <li>{@code RESEND_FROM} / {@code resend.from} — sender, defaults to
 *       {@code Foodie <onboarding@resend.dev>} (Resend's test sender, which only
 *       delivers to the Resend account owner's address).</li>
 * </ul>
 */
public final class MailService {

    private static final Logger LOGGER = Logger.getLogger(MailService.class.getName());

    private static final String ENDPOINT = "https://api.resend.com/emails";
    private static final String DEFAULT_FROM = "Foodie <onboarding@resend.dev>";

    private final String apiKey;
    private final String from;
    private final boolean ready;
    private final HttpClient http;

    private static final MailService INSTANCE = new MailService();

    public static MailService getInstance() {
        return INSTANCE;
    }

    private MailService() {
        String key = null, sender = null;
        try {
            Properties props = new Properties();
            try (InputStream is = MailService.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) props.load(is);
            }
            key    = firstNonBlank(System.getenv("RESEND_API_KEY"), props.getProperty("resend.api.key"));
            sender = firstNonBlank(System.getenv("RESEND_FROM"),    props.getProperty("resend.from"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load mail configuration", e);
        }
        this.apiKey = key;
        this.from   = sender == null ? DEFAULT_FROM : sender;
        this.ready  = apiKey != null && !apiKey.startsWith("YOUR_");
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        if (ready) {
            LOGGER.info("MailService ready (from=" + from + ").");
        } else {
            LOGGER.warning("MailService disabled — set RESEND_API_KEY (env) or resend.api.key in db.properties.");
        }
    }

    public boolean isReady() {
        return ready;
    }

    // ---------------------------------------------------------------
    // Public send methods
    // ---------------------------------------------------------------

    /**
     * Email the customer a confirmation of a freshly placed order, listing each
     * item, quantity, line price and the payable total. No-op when mail isn't
     * configured or the order carries no email address.
     */
    public void sendOrderConfirmation(Order order) {
        if (order == null) return;
        String to = order.getEmail();
        if (blank(to)) return;
        String subject = "Your Foodie order " + order.getOrderCode() + " is placed";
        sendAsync(to, subject, orderConfirmationHtml(order));
    }

    /**
     * Email the customer the 4-digit delivery PIN once an admin accepts their
     * order. The customer hands this PIN to the rider to confirm delivery. No-op
     * when mail isn't configured, or the order has no email / no PIN yet.
     */
    public void sendDeliveryPin(Order order) {
        if (order == null) return;
        String to = order.getEmail();
        if (blank(to) || blank(order.getDeliveryPin())) return;
        String subject = "Your Foodie order " + order.getOrderCode() + " is accepted — delivery PIN inside";
        sendAsync(to, subject, deliveryPinHtml(order));
    }

    // ---------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------

    /** Fire-and-forget POST to Resend. Logs the outcome; never throws. */
    private void sendAsync(String to, String subject, String html) {
        if (!ready) {
            LOGGER.fine("MailService not ready; skipping email to " + to);
            return;
        }
        String body = "{"
                + "\"from\":\"" + jsonEscape(from) + "\","
                + "\"to\":[\"" + jsonEscape(to) + "\"],"
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"html\":\"" + jsonEscape(html) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    int status = res.statusCode();
                    if (status < 200 || status >= 300) {
                        LOGGER.warning("Resend returned HTTP " + status + ": " + truncate(res.body(), 300));
                    } else {
                        LOGGER.info("Email sent to " + to + " (\"" + subject + "\").");
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Resend request failed for " + to, ex);
                    return null;
                });
    }

    // ---------------------------------------------------------------
    // HTML templates
    // ---------------------------------------------------------------

    private String orderConfirmationHtml(Order order) {
        StringBuilder rows = new StringBuilder();
        double subtotal = 0;
        List<OrderItem> items = order.getItems();
        if (items != null) {
            for (OrderItem line : items) {
                subtotal += line.getLineTotal();
                rows.append("<tr>")
                    .append(td(escape(line.getItemName()), "left"))
                    .append(td("&times; " + line.getQuantity(), "center"))
                    .append(td("Rs " + money(line.getLineTotal()), "right"))
                    .append("</tr>");
            }
        }

        StringBuilder totals = new StringBuilder();
        totals.append(totalRow("Subtotal", "Rs " + money(subtotal), false));
        if (order.getDiscount() > 0) {
            String label = order.getCouponCode() != null
                    ? "Discount (" + escape(order.getCouponCode()) + ")" : "Discount";
            totals.append(totalRow(label, "&minus; Rs " + money(order.getDiscount()), false));
        }
        totals.append(totalRow("Total paid", "Rs " + money(order.getTotal()), true));

        String where = order.getTableName() != null && !order.getTableName().isEmpty()
                ? "Dine-in — Table " + escape(order.getTableName())
                : escape(order.getAddress());

        return wrap(
            "<h1 style=\"margin:0 0 4px;font-size:22px;color:#0b1224;\">Thanks for your order!</h1>"
          + "<p style=\"margin:0 0 20px;color:#5b6577;\">Hi " + escape(order.getCustomerName())
          + ", we've received your order and it's now awaiting confirmation.</p>"
          + card(
                "<p style=\"margin:0 0 12px;\"><strong>Order code:</strong> " + escape(order.getOrderCode()) + "</p>"
              + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
              + "<thead><tr>"
              + th("Item", "left") + th("Qty", "center") + th("Amount", "right")
              + "</tr></thead><tbody>" + rows + "</tbody></table>"
              + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;margin-top:12px;\">"
              + totals + "</table>")
          + "<p style=\"margin:20px 0 4px;color:#5b6577;font-size:14px;\"><strong>Deliver to:</strong> " + where + "</p>"
          + "<p style=\"margin:0 0 4px;color:#5b6577;font-size:14px;\"><strong>Phone:</strong> " + escape(order.getPhone()) + "</p>"
          + "<p style=\"margin:0;color:#5b6577;font-size:14px;\"><strong>Payment:</strong> " + escape(order.getPaymentMethod()) + "</p>"
          + "<p style=\"margin:24px 0 0;color:#8a93a6;font-size:13px;\">We'll email your 4-digit delivery PIN as soon as the restaurant accepts your order.</p>");
    }

    private String deliveryPinHtml(Order order) {
        return wrap(
            "<h1 style=\"margin:0 0 4px;font-size:22px;color:#0b1224;\">Your order is on its way to being prepared!</h1>"
          + "<p style=\"margin:0 0 20px;color:#5b6577;\">Hi " + escape(order.getCustomerName())
          + ", great news — order <strong>" + escape(order.getOrderCode())
          + "</strong> has been accepted.</p>"
          + card(
                "<p style=\"margin:0 0 8px;color:#5b6577;font-size:14px;\">Share this 4-digit PIN with your delivery rider to confirm hand-off:</p>"
              + "<div style=\"font-size:40px;font-weight:800;letter-spacing:10px;color:#0b1224;"
              + "text-align:center;padding:14px 0;\">" + escape(order.getDeliveryPin()) + "</div>"
              + "<p style=\"margin:0;color:#c0392b;font-size:13px;text-align:center;\">"
              + "Never share this PIN until the rider hands you your order.</p>")
          + "<p style=\"margin:20px 0 0;color:#5b6577;font-size:14px;\"><strong>Total:</strong> Rs "
          + money(order.getTotal()) + "</p>");
    }

    /** Common outer shell (background + centered card) shared by every email. */
    private String wrap(String inner) {
        return "<div style=\"margin:0;padding:24px;background:#f2f4f8;"
             + "font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;\">"
             + "<div style=\"max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;"
             + "padding:28px;box-shadow:0 6px 24px rgba(11,18,36,.06);\">"
             + "<div style=\"font-size:20px;font-weight:800;color:#e23744;margin-bottom:18px;\">Foodie</div>"
             + inner
             + "<hr style=\"border:none;border-top:1px solid #eef1f6;margin:24px 0 12px;\"/>"
             + "<p style=\"margin:0;color:#aab2c2;font-size:12px;\">Foodie Restaurant Private Limited · "
             + "This is an automated message, please do not reply.</p>"
             + "</div></div>";
    }

    private String card(String inner) {
        return "<div style=\"background:#f7f9fc;border:1px solid #eef1f6;border-radius:10px;padding:18px;\">"
             + inner + "</div>";
    }

    private String th(String label, String align) {
        return "<th style=\"text-align:" + align + ";padding:6px 4px;border-bottom:2px solid #e3e8f0;"
             + "color:#8a93a6;font-size:12px;text-transform:uppercase;\">" + label + "</th>";
    }

    private String td(String value, String align) {
        return "<td style=\"text-align:" + align + ";padding:8px 4px;border-bottom:1px solid #eef1f6;"
             + "color:#0b1224;\">" + value + "</td>";
    }

    private String totalRow(String label, String value, boolean strong) {
        String weight = strong ? "800" : "500";
        String size   = strong ? "16px" : "14px";
        String top    = strong ? "border-top:2px solid #e3e8f0;" : "";
        return "<tr>"
             + "<td style=\"text-align:left;padding:6px 4px;" + top + "color:#5b6577;\">" + label + "</td>"
             + "<td style=\"text-align:right;padding:6px 4px;" + top + "font-weight:" + weight
             + ";font-size:" + size + ";color:#0b1224;\">" + value + "</td>"
             + "</tr>";
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String money(double v) {
        return String.format("%.2f", v);
    }

    /** JSON string-escaping (no external JSON library on the classpath). */
    static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** HTML-escaping for user-supplied text embedded in the templates. */
    private static String escape(String v) {
        if (v == null) return "";
        return v.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    private static boolean blank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return null;
    }

    private static String truncate(String v, int max) {
        if (v == null) return "";
        return v.length() <= max ? v : v.substring(0, max) + "...";
    }
}
