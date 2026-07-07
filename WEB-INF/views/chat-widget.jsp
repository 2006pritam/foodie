<%--
  Reusable customer AI chat-support widget (backed by NVIDIA via /chat).
  Included statically with <%@ include file="chat-widget.jsp" %>, so it inherits
  the host page's directives (no <%@ page %> here — that would duplicate the
  parent's contentType and fail JSP translation). Reads the display name
  straight from the session, so any customer-facing page can drop it in.
--%>
<%
  String chatUserName = (String) session.getAttribute("userName");
%>
<div id="chatWidget" class="chat-widget">
  <button id="chatToggle" class="chat-fab" type="button" aria-label="Open chat support">
    <span class="chat-fab-icon">&#128172;</span>
    <span class="chat-fab-text">Chat Support</span>
  </button>

  <section id="chatPanel" class="chat-panel" hidden>
    <header class="chat-panel-head">
      <div>
        <strong>Foodie Support</strong>
        <span class="chat-status">AI assistant &bull; online</span>
      </div>
      <button id="chatClose" class="chat-close" type="button" aria-label="Close chat">&times;</button>
    </header>
    <div id="chatLog" class="chat-log">
      <div class="chat-msg bot">Hi <%= chatUserName == null ? "there" : chatUserName %>! I'm your Foodie assistant. Ask me about the menu, your cart, checkout or tracking an order.</div>
    </div>
    <form id="chatForm" class="chat-input-row" autocomplete="off">
      <input id="chatInput" type="text" name="message" placeholder="Type your question..." maxlength="1000" required />
      <button type="submit" class="button small" id="chatSend">Send</button>
    </form>
  </section>
</div>

<script>
  (function () {
    var toggle = document.getElementById('chatToggle');
    var panel  = document.getElementById('chatPanel');
    var closeB = document.getElementById('chatClose');
    var form   = document.getElementById('chatForm');
    var input  = document.getElementById('chatInput');
    var sendB  = document.getElementById('chatSend');
    var log    = document.getElementById('chatLog');
    var chatUrl = '<%= request.getContextPath() %>/chat';

    function openPanel()  { panel.hidden = false; toggle.classList.add('is-open'); input.focus(); }
    function closePanel() { panel.hidden = true;  toggle.classList.remove('is-open'); }

    toggle.addEventListener('click', function () { panel.hidden ? openPanel() : closePanel(); });
    closeB.addEventListener('click', closePanel);

    function addMsg(text, who) {
      var el = document.createElement('div');
      el.className = 'chat-msg ' + who;
      el.textContent = text;
      log.appendChild(el);
      log.scrollTop = log.scrollHeight;
      return el;
    }

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var text = input.value.trim();
      if (!text) return;
      addMsg(text, 'user');
      input.value = '';
      input.disabled = true; sendB.disabled = true;
      var typing = addMsg('...', 'bot typing');

      var body = 'message=' + encodeURIComponent(text);
      fetch(chatUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body
      })
        .then(function (r) { return r.json().catch(function () { return { reply: 'Sorry, something went wrong.' }; }); })
        .then(function (data) {
          typing.remove();
          addMsg(data.reply || 'Sorry, I had no answer for that.', 'bot');
        })
        .catch(function () {
          typing.remove();
          addMsg('Sorry, I could not reach support. Please try again.', 'bot');
        })
        .finally(function () {
          input.disabled = false; sendB.disabled = false; input.focus();
        });
    });
  })();
</script>
