<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.foodie.model.OrderItem" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout | Foodie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=19">
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

    <div class="panel-message">
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
                    <input type="tel" name="phone" required placeholder="e.g. 03001234567" />
                </div>
                <div class="form-row">
                    <label>Payment method</label>
                    <div class="payment-options">
                        <label class="payment-option"><input type="radio" name="payment_method" value="CARD" checked onclick="togglePay()"> Card</label>
                        <label class="payment-option"><input type="radio" name="payment_method" value="UPI" onclick="togglePay()"> UPI</label>
                        <label class="payment-option"><input type="radio" name="payment_method" value="COD" onclick="togglePay()"> Cash on Delivery</label>
                    </div>
                </div>
                <div id="cardFields">
                    <div class="form-row">
                        <label>Card number (demo)</label>
                        <input type="text" name="card_number" value="4242 4242 4242 4242" />
                    </div>
                    <div class="form-row">
                        <label>Expiry / CVV (demo)</label>
                        <input type="text" name="card_extra" value="12/29  123" />
                    </div>
                    <p class="hint">This is a mock payment step — no real charge is made.</p>
                </div>
                <div id="upiHint" class="hint" style="display:none;">
                    Choosing UPI? You'll scan a QR and upload your payment screenshot within 2 minutes.
                </div>
                <!-- Screenshot chosen inside the UPI modal is carried here on submit. -->
                <input type="file" name="payment_proof" id="paymentProofInput" accept="image/*"
                       hidden style="display:none !important;" />
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

<!-- UPI mock-payment modal: QR + 120s timer + screenshot upload -->
<div class="upi-overlay" id="upiOverlay" role="dialog" aria-modal="true" aria-labelledby="upiTitle">
    <div class="upi-modal">
        <button type="button" class="upi-close" aria-label="Cancel payment" onclick="upiCancel()">&times;</button>
        <div class="upi-head">
            <h2 id="upiTitle">Pay with UPI</h2>
            <p>Foodie Restaurant Private Limited</p>
        </div>

        <div class="upi-body" id="upiPayStage">
            <div class="upi-qr" id="upiQr"><span class="upi-qr-loading">Generating QR…</span></div>
            <div class="upi-details">
                <div class="upi-amount">Amount: <strong>Rs <%= String.format("%.2f", payable) %></strong></div>
                <div class="upi-vpa">UPI ID: <strong>9064662830-1@naviaxis</strong></div>
                <div class="upi-timer" id="upiTimer">02:00</div>
                <p class="hint">Scan the QR with any UPI app, pay, then upload your payment screenshot below before the timer runs out.</p>
                <label class="upi-upload">
                    <span id="upiUploadText">Upload payment screenshot</span>
                    <input type="file" id="upiFile" accept="image/*" />
                </label>
                <button type="button" class="button" id="upiSubmitBtn" onclick="upiSubmit()" disabled>Submit payment</button>
            </div>
        </div>

        <div class="upi-body upi-result" id="upiResultStage" style="display:none;">
            <div class="upi-result-icon" id="upiResultIcon">&#10004;</div>
            <h3 id="upiResultTitle">Payment successful</h3>
            <p id="upiResultMsg">Placing your order…</p>
            <button type="button" class="button outline" id="upiRetryBtn" onclick="upiRetry()" style="display:none;">Try again</button>
        </div>
    </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
<script>
    var UPI_PAYABLE = "<%= String.format("%.2f", payable) %>";
    var UPI_URI = "upi://pay?pa=9064662830-1@naviaxis&pn=" +
        encodeURIComponent("Foodie Restaurant Private Limited") +
        "&am=" + UPI_PAYABLE + "&cu=INR&tn=" + encodeURIComponent("Foodie order payment");

    function togglePay() {
        var method = document.querySelector('input[name="payment_method"]:checked').value;
        document.getElementById('cardFields').style.display = (method === 'CARD') ? 'block' : 'none';
        document.getElementById('upiHint').style.display = (method === 'UPI') ? 'block' : 'none';
    }
    togglePay();

    (function () {
        var form      = document.getElementById('checkoutForm');
        var overlay   = document.getElementById('upiOverlay');
        var qrBox     = document.getElementById('upiQr');
        var timerEl   = document.getElementById('upiTimer');
        var fileInput = document.getElementById('upiFile');
        var submitBtn = document.getElementById('upiSubmitBtn');
        var uploadTxt = document.getElementById('upiUploadText');
        var proofInput= document.getElementById('paymentProofInput');
        var payStage  = document.getElementById('upiPayStage');
        var resStage  = document.getElementById('upiResultStage');
        var resIcon   = document.getElementById('upiResultIcon');
        var resTitle  = document.getElementById('upiResultTitle');
        var resMsg    = document.getElementById('upiResultMsg');
        var retryBtn  = document.getElementById('upiRetryBtn');
        var qrRendered= false;
        var countdown = null;
        var expired   = false;

        // Intercept submit: for UPI, open the payment modal instead of posting.
        form.addEventListener('submit', function (e) {
            var method = document.querySelector('input[name="payment_method"]:checked').value;
            if (method !== 'UPI') return;                 // CARD/COD submit normally
            if (proofInput.files && proofInput.files.length) return; // already paid, let it through
            e.preventDefault();
            openUpi();
        });

        function openUpi() {
            expired = false;
            payStage.style.display = '';
            resStage.style.display = 'none';
            fileInput.value = '';
            uploadTxt.textContent = 'Upload payment screenshot';
            submitBtn.disabled = true;
            overlay.classList.add('open');
            renderQr();
            startTimer(120);
        }

        function renderQr() {
            if (qrRendered) return;
            qrBox.innerHTML = '';
            try {
                new QRCode(qrBox, { text: UPI_URI, width: 200, height: 200,
                    colorDark: '#0b1224', colorLight: '#ffffff' });
                qrRendered = true;
            } catch (err) {
                // Fallback: render via an image QR service if the library didn't load.
                var img = document.createElement('img');
                img.width = 200; img.height = 200; img.alt = 'UPI QR';
                img.src = 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=' +
                    encodeURIComponent(UPI_URI);
                qrBox.appendChild(img);
                qrRendered = true;
            }
        }

        function startTimer(seconds) {
            if (countdown) clearInterval(countdown);
            var remaining = seconds;
            paint(remaining);
            countdown = setInterval(function () {
                remaining--;
                paint(remaining);
                if (remaining <= 0) {
                    clearInterval(countdown);
                    onExpire();
                }
            }, 1000);
        }
        function paint(s) {
            if (s < 0) s = 0;
            var m = Math.floor(s / 60), sec = s % 60;
            timerEl.textContent = (m < 10 ? '0' : '') + m + ':' + (sec < 10 ? '0' : '') + sec;
            timerEl.classList.toggle('urgent', s <= 10);
        }

        fileInput.addEventListener('change', function () {
            if (expired) return;
            if (fileInput.files && fileInput.files.length) {
                uploadTxt.textContent = fileInput.files[0].name;
                submitBtn.disabled = false;
            } else {
                submitBtn.disabled = true;
            }
        });

        window.upiSubmit = function () {
            if (expired) return;
            if (!(fileInput.files && fileInput.files.length)) return;
            if (countdown) clearInterval(countdown);
            // Move the chosen screenshot into the real form input and post.
            try {
                var dt = new DataTransfer();
                dt.items.add(fileInput.files[0]);
                proofInput.files = dt.files;
            } catch (err) { /* older browsers: server still validates presence */ }
            showResult(true, 'Payment successful', 'Placing your order…');
            setTimeout(function () { form.submit(); }, 900);
        };

        function onExpire() {
            expired = true;
            submitBtn.disabled = true;
            showResult(false, 'Payment failed',
                'You ran out of time before uploading your payment screenshot.');
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

        window.upiRetry = function () { openUpi(); };

        window.upiCancel = function () {
            if (countdown) clearInterval(countdown);
            overlay.classList.remove('open');
        };

        // Click on dark backdrop cancels.
        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) window.upiCancel();
        });
    })();
</script>

<%@ include file="chat-widget.jsp" %>
</body>
</html>
