<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.OrderItem" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=21">
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=2"></script>
</head>
<body class="dashboard-page checkout-page">
<div class="dashboard-shell">
    <header class="dashboard-header">
        <div class="dashboard-brand">
            <h1>Checkout</h1>
            <p>Confirm delivery details and place your order.</p>
        </div>
        <div class="dashboard-actions">
            <a class="button outline" href="${pageContext.request.contextPath}/cart">Back to Cart</a>
            <a class="button danger" href="${pageContext.request.contextPath}/logout">Sign Out</a>
            <button type="button" class="theme-toggle" data-theme-toggle aria-label="Toggle theme"><span data-theme-glyph>&#9790;</span></button>
        </div>
    </header>

    <div class="panel-message" id="checkoutFlash">
        <%= request.getAttribute("checkoutMessage") != null ? request.getAttribute("checkoutMessage") : "" %>
    </div>

    <%
        List<OrderItem> lines = (List<OrderItem>) request.getAttribute("cartLines");
        Object totalObj = request.getAttribute("cartTotal");
        String ctx = request.getContextPath();
        String dineInTable = (String) session.getAttribute("resvTableName");
        boolean dineIn = dineInTable != null;
        double subtotal = totalObj == null ? 0.0 : (Double) totalObj;
        String couponCode = (String) request.getAttribute("couponCode");
        Object discObj = request.getAttribute("couponDiscount");
        double couponDiscount = discObj == null ? 0.0 : (Double) discObj;
        Object payObj = request.getAttribute("payableTotal");
        double payable = payObj == null ? subtotal : (Double) payObj;
    %>

    <section class="checkout-layout">
        <div class="panel form-panel">
            <div class="panel-header">
                <h2><%= dineIn ? "Dine-in details" : "Delivery details" %></h2>
                <p><%= dineIn ? "Your order will be served to your reserved table." : "Where should we send your order?" %></p>
            </div>
            <% if (dineIn) { %>
                <div class="dine-in-banner">Serving to <strong>Table <%= dineInTable %></strong> (dine-in).</div>
            <% } %>
            <form method="post" action="<%= ctx %>/checkout" class="admin-form" id="checkoutForm" enctype="multipart/form-data">
                <div class="form-row">
                    <label><%= dineIn ? "Delivery address (optional for dine-in)" : "Delivery address" %></label>
                    <input type="text" name="address" <%= dineIn ? "" : "required" %>
                           placeholder="<%= dineIn ? "Table " + dineInTable + " (dine-in)" : "House no, street, area, city" %>" />
                </div>
                <div class="form-row">
                    <label>Phone number</label>
                    <input type="tel" name="phone" required placeholder="e.g. 9876543210" />
                </div>
                <div class="form-row">
                    <label>Email address</label>
                    <input type="email" name="email" required placeholder="e.g. you@example.com" />
                </div>
                <div class="form-row">
                    <label>Payment method</label>
                    <div class="payment-options">
                        <label class="payment-option"><input type="radio" name="payment_method" value="UPI" checked onclick="togglePay()"> UPI (Instant Auto-Verify)</label>
                        <label class="payment-option"><input type="radio" name="payment_method" value="CARD" onclick="togglePay()"> Card</label>
                        <label class="payment-option"><input type="radio" name="payment_method" value="COD" onclick="togglePay()"> Cash on Delivery</label>
                    </div>
                </div>
                <div id="cardFields" style="display:none;">
                    <div class="form-row">
                        <label>Card number (demo)</label>
                        <input type="text" name="card_number" value="4242 4242 4242 4242" />
                    </div>
                    <div class="form-row">
                        <label>Expiry / CVV (demo)</label>
                        <input type="text" name="card_extra" value="12/29  123" />
                    </div>
                    <p class="hint">This is a mock payment step – no real charge is made.</p>
                </div>
                <div id="upiHint" class="hint">
                    Pay securely via UPI Gateway. Scan the live QR code or open the gateway checkout page—your order confirms automatically once payment is verified.
                </div>
                <button type="submit" class="button" id="placeOrderBtn">Place order</button>
            </form>
        </div>

        <aside class="panel order-summary-panel">
            <div class="panel-header">
                <h2>Order summary</h2>
            </div>
            <ul class="summary-list">
                <% if (lines != null) { for (OrderItem line : lines) { %>
                    <li>
                        <span><%= line.getItemName() %> &times; <%= line.getQuantity() %></span>
                        <strong>Rs <%= String.format("%.2f", line.getLineTotal()) %></strong>
                    </li>
                <% } } %>
            </ul>

            <div class="coupon-box">
                <% if (couponCode != null) { %>
                    <div class="coupon-applied">
                        <span>Coupon <strong><%= couponCode %></strong> applied</span>
                        <form method="post" action="<%= ctx %>/checkout" class="inline-form">
                            <input type="hidden" name="action" value="remove_coupon" />
                            <button type="submit" class="coupon-remove" title="Remove coupon">&times;</button>
                        </form>
                    </div>
                <% } else { %>
                    <form method="post" action="<%= ctx %>/checkout" class="coupon-form">
                        <input type="hidden" name="action" value="apply_coupon" />
                        <input type="text" name="coupon_code" placeholder="Have a coupon? Enter code"
                               autocomplete="off" />
                        <button type="submit" class="button small">Apply</button>
                    </form>
                <% } %>
            </div>

            <div class="order-summary-row">
                <span>Subtotal</span>
                <span>Rs <%= String.format("%.2f", subtotal) %></span>
            </div>
            <% if (couponDiscount > 0) { %>
                <div class="order-summary-row discount-row">
                    <span>Discount<%= couponCode != null ? " (" + couponCode + ")" : "" %></span>
                    <span>&minus; Rs <%= String.format("%.2f", couponDiscount) %></span>
                </div>
            <% } %>
            <div class="order-summary-row total">
                <span>Total</span>
                <strong>Rs <%= String.format("%.2f", payable) %></strong>
            </div>
        </aside>
    </section>
</div>

<!-- Live UPI Gateway payment modal: dynamic QR + hosted checkout link + auto-polling -->
<div class="upi-overlay" id="upiOverlay" role="dialog" aria-modal="true" aria-labelledby="upiTitle">
    <div class="upi-modal">
        <button type="button" class="upi-close" aria-label="Cancel payment" onclick="upiCancel()">&times;</button>
        <div class="upi-head">
            <h2 id="upiTitle">Pay with UPI</h2>
            <p id="upiSubhead">Foodie Restaurant Private Limited</p>
            <div class="upi-powered-by" aria-label="Foodie payments powered by eMudra">
                <span>Powered by</span>
                <img src="${pageContext.request.contextPath}/assets/images/payments/emudra-secure-payment-logo.png" alt="eMudra payment gateway" width="100" height="50" loading="lazy" decoding="async" />
            </div>
        </div>

        <div class="upi-body" id="upiPayStage">
            <div class="upi-qr" id="upiQr"><span class="upi-qr-loading">Creating payment order...</span></div>
            <div class="upi-details">
                <div class="upi-amount">Amount: <strong id="upiAmountText">Rs <%= String.format("%.2f", payable) %></strong></div>
                <div class="upi-vpa">Order Ref: <strong id="upiRefText">—</strong></div>
                <div class="upi-timer" id="upiTimer">--:--</div>
                <p class="hint" id="upiStatusText">Connecting to UPI Gateway...</p>
                <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:10px;">
                    <a class="button" id="upiAppLink" href="#" style="display:none;">Open UPI App</a>
                    <a class="button outline" id="upiCheckoutLink" href="#" target="_blank" rel="noopener noreferrer" style="display:none;">Open Gateway Page</a>
                    <button type="button" class="button outline" id="upiCheckNowBtn" onclick="upiCheckNow()" style="display:none;">Check Status</button>
                </div>
            </div>
        </div>

        <div class="upi-body upi-result" id="upiResultStage" style="display:none;">
            <div class="upi-result-icon" id="upiResultIcon">&#10004;</div>
            <h3 id="upiResultTitle">Payment verified</h3>
            <p id="upiResultMsg">Placing your order...</p>
            <button type="button" class="button outline" id="upiRetryBtn" onclick="upiRetry()" style="display:none;">Try again</button>
        </div>
    </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
<script>
    var CTX = "<%= ctx %>";

    function togglePay() {
        var method = document.querySelector('input[name="payment_method"]:checked').value;
        document.getElementById('cardFields').style.display = (method === 'CARD') ? 'block' : 'none';
        document.getElementById('upiHint').style.display = (method === 'UPI') ? 'block' : 'none';
    }
    togglePay();

    (function () {
        var form        = document.getElementById('checkoutForm');
        var placeBtn    = document.getElementById('placeOrderBtn');
        var overlay     = document.getElementById('upiOverlay');
        var qrBox       = document.getElementById('upiQr');
        var subhead     = document.getElementById('upiSubhead');
        var amountEl    = document.getElementById('upiAmountText');
        var refEl       = document.getElementById('upiRefText');
        var timerEl     = document.getElementById('upiTimer');
        var statusEl    = document.getElementById('upiStatusText');
        var appLink     = document.getElementById('upiAppLink');
        var checkoutLnk = document.getElementById('upiCheckoutLink');
        var checkNowBtn = document.getElementById('upiCheckNowBtn');
        var payStage    = document.getElementById('upiPayStage');
        var resStage    = document.getElementById('upiResultStage');
        var resIcon     = document.getElementById('upiResultIcon');
        var resTitle    = document.getElementById('upiResultTitle');
        var resMsg      = document.getElementById('upiResultMsg');
        var retryBtn    = document.getElementById('upiRetryBtn');

        var countdown      = null;
        var pollTimer      = null;
        var currentOrderId = null;
        var pollingInFlight = false;

        form.addEventListener('submit', function (e) {
            var method = document.querySelector('input[name="payment_method"]:checked').value;
            if (method !== 'UPI') return; // CARD and COD submit normally to /checkout
            e.preventDefault();
            if (!form.reportValidity()) return;
            startUpiGatewayCheckout();
        });

        function startUpiGatewayCheckout() {
            stopTimers();
            currentOrderId = null;
            payStage.style.display = '';
            resStage.style.display = 'none';
            appLink.style.display = 'none';
            checkoutLnk.style.display = 'none';
            checkNowBtn.style.display = 'none';
            qrBox.innerHTML = '<span class="upi-qr-loading">Creating payment order...</span>';
            refEl.textContent = '—';
            timerEl.textContent = '--:--';
            timerEl.classList.remove('urgent');
            subhead.textContent = 'Foodie Restaurant Private Limited';
            statusEl.textContent = 'Connecting to UPI Gateway...';
            overlay.classList.add('open');
            placeBtn.disabled = true;

            var params = new URLSearchParams();
            params.append('address', form.elements['address'] ? form.elements['address'].value : '');
            params.append('phone',   form.elements['phone']   ? form.elements['phone'].value   : '');
            params.append('email',   form.elements['email']   ? form.elements['email'].value   : '');

            fetch(CTX + '/payment/upi/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: params.toString(),
                credentials: 'same-origin'
            })
            .then(function (r) {
                return r.json().then(function (data) { return { ok: r.ok, data: data }; });
            })
            .then(function (res) {
                placeBtn.disabled = false;
                if (!res.ok || !res.data || !res.data.ok) {
                    var err = (res.data && res.data.message) ? res.data.message : 'Could not initiate UPI payment.';
                    showResult(false, 'Payment unavailable', err);
                    return;
                }
                renderGatewayOrder(res.data);
            })
            .catch(function () {
                placeBtn.disabled = false;
                showResult(false, 'Connection error', 'Could not reach the server. Please try again.');
            });
        }

        function renderGatewayOrder(data) {
            currentOrderId = data.client_order_id;
            amountEl.textContent = 'Rs ' + data.amount;
            refEl.textContent = data.gateway_order_id || data.client_order_id;
            if (data.test) {
                subhead.textContent = 'Foodie Restaurant (Sandbox Test Mode)';
            }

            if (data.upi_intent) {
                renderQr(data.upi_intent);
                appLink.href = data.upi_intent;
                appLink.style.display = 'inline-flex';
                statusEl.textContent = 'Scan the QR with any UPI app (or open the gateway page). Waiting for automatic confirmation...';
            } else {
                qrBox.innerHTML = '<div class="upi-qr-loading" style="padding:16px;text-align:center;">'
                    + (data.test ? 'Sandbox test order ready.<br>Click <strong>Open Gateway Page</strong> to simulate payment.'
                                 : 'Complete payment on the hosted gateway page.')
                    + '</div>';
                statusEl.textContent = 'Complete payment in the gateway page. This window updates automatically.';
            }

            if (data.payment_url) {
                checkoutLnk.href = data.payment_url;
                checkoutLnk.style.display = 'inline-flex';
            }
            checkNowBtn.style.display = 'inline-flex';

            var secondsLeft = 900;
            if (data.expires_at) {
                var diff = Math.floor((new Date(data.expires_at).getTime() - Date.now()) / 1000);
                if (!isNaN(diff) && diff > 0) secondsLeft = diff;
            }
            startTimer(secondsLeft);
            startPolling();
        }

        function renderQr(uri) {
            qrBox.innerHTML = '';
            try {
                new QRCode(qrBox, {
                    text: uri,
                    width: 200,
                    height: 200,
                    colorDark: '#0b1224',
                    colorLight: '#ffffff'
                });
            } catch (err) {
                var img = document.createElement('img');
                img.width = 200;
                img.height = 200;
                img.alt = 'UPI QR';
                img.src = 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=' + encodeURIComponent(uri);
                qrBox.appendChild(img);
            }
        }

        function startTimer(seconds) {
            if (countdown) clearInterval(countdown);
            var remaining = seconds;
            paintTimer(remaining);
            countdown = setInterval(function () {
                remaining--;
                paintTimer(remaining);
                if (remaining <= 0) {
                    stopTimers();
                    showResult(false, 'Payment expired', 'This UPI payment window expired. Click Try again to generate a fresh QR.');
                }
            }, 1000);
        }

        function paintTimer(s) {
            if (s < 0) s = 0;
            var m = Math.floor(s / 60), sec = s % 60;
            timerEl.textContent = (m < 10 ? '0' : '') + m + ':' + (sec < 10 ? '0' : '') + sec;
            timerEl.classList.toggle('urgent', s <= 30);
        }

        function startPolling() {
            if (pollTimer) clearInterval(pollTimer);
            pollTimer = setInterval(pollStatus, 3000);
        }

        function pollStatus() {
            if (!currentOrderId || pollingInFlight) return;
            pollingInFlight = true;
            fetch(CTX + '/payment/upi/status?order=' + encodeURIComponent(currentOrderId), {
                credentials: 'same-origin'
            })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                pollingInFlight = false;
                if (!data || !data.ok) return;

                if (data.status === 'paid' && data.redirect) {
                    stopTimers();
                    var note = data.utr ? ('UTR ' + data.utr + ' verified. Redirecting to your receipt...') : 'Redirecting to your receipt...';
                    showResult(true, 'Payment verified!', note);
                    setTimeout(function () { window.location.href = data.redirect; }, 900);
                } else if (data.status === 'review') {
                    statusEl.textContent = 'UTR submitted — awaiting merchant confirmation...';
                } else if (data.status === 'expired' || data.status === 'failed') {
                    stopTimers();
                    showResult(false, 'Payment ' + data.status, 'Your UPI payment could not be completed. You can try again.');
                }
            })
            .catch(function () {
                pollingInFlight = false;
            });
        }

        function showResult(ok, title, message) {
            payStage.style.display = 'none';
            resStage.style.display = '';
            resStage.classList.toggle('is-ok', ok);
            resStage.classList.toggle('is-fail', !ok);
            resIcon.innerHTML = ok ? '&#10004;' : '&#10006;';
            resTitle.textContent = title;
            resMsg.textContent = message;
            retryBtn.style.display = ok ? 'none' : 'inline-flex';
        }

        function stopTimers() {
            if (countdown) { clearInterval(countdown); countdown = null; }
            if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        }

        window.upiCheckNow = function () {
            statusEl.textContent = 'Checking payment status...';
            pollStatus();
        };

        window.upiRetry = function () {
            startUpiGatewayCheckout();
        };

        window.upiCancel = function () {
            stopTimers();
            overlay.classList.remove('open');
        };

        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) window.upiCancel();
        });
    })();
</script>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
