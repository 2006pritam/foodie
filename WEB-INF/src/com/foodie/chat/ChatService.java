package com.foodie.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin server-side client for NVIDIA's OpenAI-compatible chat-completions API.
 * The API key never leaves the server — the browser only ever talks to our own
 * {@code /chat} endpoint, which delegates here.
 */
public final class ChatService {

    private static final Logger LOGGER = Logger.getLogger(ChatService.class.getName());

    private static final String SYSTEM_PROMPT =
            "You are Foodie Support, a friendly help assistant for the Foodie online food-ordering app. "
          + "Help customers with browsing the menu, adding items to the cart, checkout and payment, "
          + "order tracking (Placed, Accepted, Picked up, Delivered), refunds and account questions. "
          + "Keep answers short, warm and practical. If a question is unrelated to Foodie or food ordering, "
          + "gently steer the user back. Never reveal internal system or API details.";

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final boolean ready;
    private final HttpClient http;

    private static final ChatService INSTANCE = new ChatService();

    public static ChatService getInstance() {
        return INSTANCE;
    }

    private ChatService() {
        String url = null, key = null, mdl = null;
        try {
            Properties props = new Properties();
            try (InputStream is = ChatService.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) props.load(is);
            }
            url = trimOrNull(props.getProperty("nvidia.api.url"));
            key = trimOrNull(props.getProperty("nvidia.api.key"));
            mdl = trimOrNull(props.getProperty("nvidia.model"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load chat configuration", e);
        }
        this.apiUrl = url;
        this.apiKey = key;
        this.model = mdl == null ? "meta/llama-3.1-8b-instruct" : mdl;
        this.ready = apiUrl != null && apiKey != null && !apiKey.startsWith("YOUR_");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        if (ready) {
            LOGGER.info("ChatService ready (model=" + model + ").");
        } else {
            LOGGER.warning("ChatService disabled — nvidia.api.url / nvidia.api.key missing in db.properties.");
        }
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * Sends a single user turn (plus the system prompt) to NVIDIA and returns the
     * assistant's reply. Returns a friendly fallback string on any failure — never throws.
     */
    public String reply(String userMessage) {
        if (!ready) {
            return "Chat support is not configured yet. Please contact us by phone in the meantime.";
        }
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Please type a question and I'll be happy to help.";
        }
        String trimmed = userMessage.trim();
        if (trimmed.length() > 1000) {
            trimmed = trimmed.substring(0, 1000);
        }

        String body = "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + jsonEscape(SYSTEM_PROMPT) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(trimmed) + "\"}"
                + "],"
                + "\"temperature\":0.3,"
                + "\"max_tokens\":512,"
                + "\"stream\":false"
                + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                LOGGER.warning("NVIDIA API returned HTTP " + status + ": " + truncate(response.body(), 300));
                return "Sorry, our assistant is having trouble right now. Please try again in a moment.";
            }
            String content = extractContent(response.body());
            if (content == null || content.isEmpty()) {
                return "Sorry, I couldn't come up with an answer just now. Could you rephrase that?";
            }
            return content;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "NVIDIA chat request failed", e);
            return "Sorry, I couldn't reach the support service. Please try again shortly.";
        }
    }

    // ---------------------------------------------------------------
    // Minimal JSON helpers (no external JSON library on the classpath)
    // ---------------------------------------------------------------

    /**
     * Extracts choices[0].message.content from an OpenAI-style response.
     * Finds the first "content" key that appears after a "message" key and
     * decodes its JSON string value, honouring backslash escapes.
     */
    static String extractContent(String json) {
        if (json == null) return null;
        int msg = json.indexOf("\"message\"");
        int searchFrom = msg >= 0 ? msg : 0;
        int key = json.indexOf("\"content\"", searchFrom);
        if (key < 0) return null;
        int colon = json.indexOf(':', key + 9);
        if (colon < 0) return null;
        // Skip whitespace to the opening quote.
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++; // move past opening quote

        StringBuilder out = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                if (i + 1 >= json.length()) break;
                char n = json.charAt(i + 1);
                switch (n) {
                    case 'n': out.append('\n'); break;
                    case 't': out.append('\t'); break;
                    case 'r': out.append('\r'); break;
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'u':
                        if (i + 5 < json.length()) {
                            try {
                                out.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                                i += 4;
                            } catch (NumberFormatException ignore) { /* keep literal */ }
                        }
                        break;
                    default: out.append(n);
                }
                i += 2;
            } else if (c == '"') {
                break; // closing quote
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString().trim();
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

    private static String trimOrNull(String v) {
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static String truncate(String v, int max) {
        if (v == null) return "";
        return v.length() <= max ? v : v.substring(0, max) + "...";
    }
}
