package com.foodie.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side HTTP client for the {@code 2006pritam/upi_gateway} REST API
 * ({@code POST /api/v1/orders} and {@code GET /api/v1/orders/{orderNo}}),
 * plus HMAC-SHA256 verification for incoming gateway webhooks.
 *
 * <p>Mirrors {@link com.foodie.mail.MailService}: lazy singleton, env-first
 * configuration with {@code db.properties} fallback, and zero external JSON
 * dependencies.</p>
 *
 * <p>Config keys (environment variables first, then {@code db.properties}):</p>
 * <ul>
 *   <li>{@code UPI_GATEWAY_BASE_URL} / {@code upi.gateway.base.url} — required,
 *       e.g. {@code https://your-upi-gateway.example.com} (trailing
 *       {@code /api/v1} is stripped automatically if included).</li>
 *   <li>{@code UPI_GATEWAY_API_KEY} / {@code upi.gateway.api.key} — required
 *       ({@code X-Api-Key}; keys prefixed with {@code test_key_} automatically
 *       use the gateway's sandbox environment).</li>
 *   <li>{@code UPI_GATEWAY_API_SECRET} / {@code upi.gateway.api.secret} —
 *       required ({@code X-Api-Secret} and HMAC-SHA256 webhook secret).</li>
 *   <li>{@code UPI_GATEWAY_ACCOUNT_ID} / {@code upi.gateway.account.id} —
 *       optional numeric live {@code payment_account_id} (ignored in sandbox).</li>
 *   <li>{@code UPI_GATEWAY_PUBLIC_URL} / {@code upi.gateway.public.url} —
 *       optional public {@code https://} URL of this Foodie deployment used to
 *       build {@code redirect_url} for hosted-checkout return (must also be
 *       whitelisted in the merchant's allowed redirect domains on the gateway).</li>
 * </ul>
 */
public final class UpiGatewayService {

    private static final Logger LOGGER = Logger.getLogger(UpiGatewayService.class.getName());

    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final Integer paymentAccountId;
    private final String publicUrl;
    private final boolean sandbox;
    private final boolean ready;
    private final HttpClient http;

    private static final UpiGatewayService INSTANCE = new UpiGatewayService();

    public static UpiGatewayService getInstance() {
        return INSTANCE;
    }

    private UpiGatewayService() {
        String url = null, key = null, secret = null, accIdRaw = null, pubUrl = null;
        try {
            Properties props = new Properties();
            try (InputStream is = UpiGatewayService.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) props.load(is);
            }
            url      = firstNonBlank(System.getenv("UPI_GATEWAY_BASE_URL"),   props.getProperty("upi.gateway.base.url"));
            key      = firstNonBlank(System.getenv("UPI_GATEWAY_API_KEY"),    props.getProperty("upi.gateway.api.key"));
            secret   = firstNonBlank(System.getenv("UPI_GATEWAY_API_SECRET"), props.getProperty("upi.gateway.api.secret"));
            accIdRaw = firstNonBlank(System.getenv("UPI_GATEWAY_ACCOUNT_ID"), props.getProperty("upi.gateway.account.id"));
            pubUrl   = firstNonBlank(System.getenv("UPI_GATEWAY_PUBLIC_URL"), props.getProperty("upi.gateway.public.url"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load UPI gateway configuration", e);
        }

        this.baseUrl = normalizeBaseUrl(url);
        this.apiKey = key;
        this.apiSecret = secret;
        this.paymentAccountId = parsePositiveInt(accIdRaw);
        this.publicUrl = stripTrailingSlash(pubUrl);
        this.sandbox = key != null && key.startsWith("test_key_");
        this.ready = this.baseUrl != null
                && this.apiKey != null && !this.apiKey.startsWith("YOUR_")
                && this.apiSecret != null && !this.apiSecret.startsWith("YOUR_");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        if (ready) {
            LOGGER.info("UpiGatewayService ready (baseUrl=" + this.baseUrl
                    + ", env=" + (sandbox ? "sandbox" : "live") + ").");
        } else {
            LOGGER.warning("UpiGatewayService disabled — set UPI_GATEWAY_BASE_URL, "
                    + "UPI_GATEWAY_API_KEY, and UPI_GATEWAY_API_SECRET.");
        }
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    /**
     * Build the HTTPS {@code redirect_url} for a given order code if
     * {@code UPI_GATEWAY_PUBLIC_URL} is configured with {@code https://}.
     * Returns {@code null} otherwise so the gateway never rejects local HTTP URLs.
     */
    public String buildRedirectUrl(String contextPath, String clientOrderId) {
        if (publicUrl == null || !publicUrl.startsWith("https://")) {
            return null;
        }
        String ctx = (contextPath == null || "/".equals(contextPath)) ? "" : contextPath;
        String encoded = URLEncoder.encode(clientOrderId, StandardCharsets.UTF_8);
        return publicUrl + ctx + "/payment/upi/return?order=" + encoded;
    }

    /**
     * Create a new UPI payment order via {@code POST /api/v1/orders}.
     */
    public GatewayOrder createOrder(
            String clientOrderId,
            double amount,
            String customerName,
            String customerEmail,
            String customerMobile,
            String note,
            String redirectUrl) {
        if (!ready) {
            return GatewayOrder.error("UPI payment gateway is not configured on the server.");
        }

        StringBuilder json = new StringBuilder(256);
        json.append("{");
        json.append("\"amount\":").append(String.format(Locale.US, "%.2f", amount));
        json.append(",\"client_order_id\":\"").append(jsonEscape(truncate(clientOrderId, 64))).append("\"");
        // upi_gateway prohibits payment_account_id in sandbox mode.
        if (!sandbox && paymentAccountId != null) {
            json.append(",\"payment_account_id\":").append(paymentAccountId);
        }
        if (!blank(customerName)) {
            json.append(",\"customer_name\":\"").append(jsonEscape(truncate(customerName.trim(), 100))).append("\"");
        }
        if (!blank(customerEmail)) {
            json.append(",\"customer_email\":\"").append(jsonEscape(truncate(customerEmail.trim(), 255))).append("\"");
        }
        if (!blank(customerMobile)) {
            json.append(",\"customer_mobile\":\"").append(jsonEscape(truncate(customerMobile.trim(), 20))).append("\"");
        }
        if (!blank(note)) {
            json.append(",\"note\":\"").append(jsonEscape(truncate(note.trim(), 50))).append("\"");
        }
        if (!blank(redirectUrl) && redirectUrl.startsWith("https://")) {
            json.append(",\"redirect_url\":\"").append(jsonEscape(truncate(redirectUrl, 500))).append("\"");
        }
        json.append("}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/orders"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Api-Key", apiKey)
                .header("X-Api-Secret", apiSecret)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();

        return executeOrderRequest("POST /api/v1/orders", request);
    }

    /**
     * Fetch (and trigger live verification for) an existing order via
     * {@code GET /api/v1/orders/{orderNo}}.
     */
    public GatewayOrder getOrderStatus(String orderNoOrClientOrderId) {
        if (!ready) {
            return GatewayOrder.error("UPI payment gateway is not configured on the server.");
        }
        if (blank(orderNoOrClientOrderId)) {
            return GatewayOrder.error("Missing order identifier.");
        }

        String encoded = URLEncoder.encode(orderNoOrClientOrderId.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/orders/" + encoded))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("X-Api-Key", apiKey)
                .header("X-Api-Secret", apiSecret)
                .GET()
                .build();

        return executeOrderRequest("GET /api/v1/orders/" + orderNoOrClientOrderId, request);
    }

    /**
     * Verify {@code X-Signature} sent by {@code upi_gateway}'s {@code SendWebhook}
     * job: {@code hash_hmac('sha256', $rawBody, $signingSecret)}.
     */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (!ready || rawBody == null || blank(signatureHeader)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = toHex(digest);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to verify UPI webhook signature", e);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // HTTP + JSON parsing
    // ------------------------------------------------------------------

    private GatewayOrder executeOrderRequest(String label, HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();

            if (status < 200 || status >= 300) {
                String msg = extractJsonString(body, "message");
                if (blank(msg)) {
                    msg = "UPI gateway returned HTTP " + status;
                }
                LOGGER.warning(label + " failed (HTTP " + status + "): " + truncate(body, 300));
                return GatewayOrder.error(msg);
            }

            String dataSection = sliceObject(body, "data");
            if (dataSection == null) {
                LOGGER.warning(label + " missing data object: " + truncate(body, 300));
                return GatewayOrder.error("Unexpected response from UPI payment gateway.");
            }

            GatewayOrder order = new GatewayOrder(
                    true,
                    extractJsonString(dataSection, "order_id"),
                    extractJsonString(dataSection, "client_order_id"),
                    extractJsonRawValue(dataSection, "amount"),
                    extractJsonString(dataSection, "status"),
                    extractJsonString(dataSection, "environment"),
                    extractJsonBoolean(dataSection, "test"),
                    extractJsonString(dataSection, "utr"),
                    extractJsonString(dataSection, "payment_url"),
                    extractJsonString(dataSection, "upi_intent"),
                    extractJsonString(dataSection, "expires_at"),
                    null
            );

            if (blank(order.orderId)) {
                return GatewayOrder.error("UPI gateway response did not include an order_id.");
            }
            return order;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, label + " interrupted", e);
            return GatewayOrder.error("Payment gateway request was interrupted.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, label + " network error", e);
            return GatewayOrder.error("Could not reach the UPI payment gateway. Please try again.");
        }
    }

    /** DTO for {@code Order::toApiArray()} returned by {@code upi_gateway}. */
    public static final class GatewayOrder {
        public final boolean ok;
        public final String orderId;
        public final String clientOrderId;
        public final String amount;
        public final String status;
        public final String environment;
        public final boolean test;
        public final String utr;
        public final String paymentUrl;
        public final String upiIntent;
        public final String expiresAt;
        public final String errorMessage;

        private GatewayOrder(
                boolean ok, String orderId, String clientOrderId, String amount,
                String status, String environment, boolean test, String utr,
                String paymentUrl, String upiIntent, String expiresAt, String errorMessage) {
            this.ok = ok;
            this.orderId = orderId;
            this.clientOrderId = clientOrderId;
            this.amount = amount;
            this.status = status;
            this.environment = environment;
            this.test = test;
            this.utr = utr;
            this.paymentUrl = paymentUrl;
            this.upiIntent = upiIntent;
            this.expiresAt = expiresAt;
            this.errorMessage = errorMessage;
        }

        public static GatewayOrder error(String message) {
            return new GatewayOrder(false, null, null, null, null, null, false, null, null, null, null, message);
        }

        public boolean isPaid() {
            return "paid".equalsIgnoreCase(status);
        }
    }

    // ------------------------------------------------------------------
    // Minimal zero-dependency JSON helpers
    // ------------------------------------------------------------------

    /** Extract the JSON object substring for a given key (e.g. {@code "data":{...}}). */
    public static String sliceObject(String json, String key) {
        int valStart = findValueStart(json, key);
        if (valStart < 0 || json.charAt(valStart) != '{') return null;
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = valStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(valStart, i + 1);
                    }
                }
            }
        }
        return null;
    }

    /** Extract a JSON string value for {@code key}, unescaping JSON escapes. Returns null if missing or JSON null. */
    public static String extractJsonString(String json, String key) {
        int valStart = findValueStart(json, key);
        if (valStart < 0) return null;
        if (json.startsWith("null", valStart)) return null;
        if (json.charAt(valStart) != '"') return null;

        StringBuilder sb = new StringBuilder();
        for (int i = valStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ignored) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        }
                        break;
                    default:
                        sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static boolean extractJsonBoolean(String json, String key) {
        int valStart = findValueStart(json, key);
        return valStart >= 0 && json.startsWith("true", valStart);
    }

    private static String extractJsonRawValue(String json, String key) {
        int valStart = findValueStart(json, key);
        if (valStart < 0) return null;
        if (json.charAt(valStart) == '"') {
            return extractJsonString(json, key);
        }
        if (json.startsWith("null", valStart)) return null;
        int end = valStart;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            end++;
        }
        return json.substring(valStart, end);
    }

    private static int findValueStart(String json, String key) {
        if (json == null || key == null) return -1;
        String needle = "\"" + key + "\"";
        int idx = 0;
        while ((idx = json.indexOf(needle, idx)) >= 0) {
            int afterKey = idx + needle.length();
            while (afterKey < json.length() && Character.isWhitespace(json.charAt(afterKey))) {
                afterKey++;
            }
            if (afterKey < json.length() && json.charAt(afterKey) == ':') {
                int valStart = afterKey + 1;
                while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) {
                    valStart++;
                }
                if (valStart < json.length()) {
                    return valStart;
                }
            }
            idx = afterKey;
        }
        return -1;
    }

    public static String jsonEscape(String s) {
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

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean blank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return null;
    }

    private static String normalizeBaseUrl(String url) {
        String s = stripTrailingSlash(url);
        if (s == null) return null;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/api/v1/orders")) {
            s = s.substring(0, s.length() - "/api/v1/orders".length());
        } else if (lower.endsWith("/api/v1")) {
            s = s.substring(0, s.length() - "/api/v1".length());
        }
        return stripTrailingSlash(s);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) return null;
        String s = url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.isEmpty() ? null : s;
    }

    private static Integer parsePositiveInt(String raw) {
        if (blank(raw)) return null;
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncate(String v, int max) {
        if (v == null) return "";
        return v.length() <= max ? v : v.substring(0, max);
    }
}
