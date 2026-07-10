/**
 * Site-wide dark / light theme.
 *
 * Runs synchronously in <head> (no `defer`) so the `data-theme` attribute is
 * set on <html> before the first paint -- this avoids a white flash when a
 * returning visitor has chosen dark mode.
 *
 * Default is light. The choice is saved in localStorage under "foodie-theme"
 * and shared across every page. Any element with [data-theme-toggle] flips it.
 */
(function () {
  'use strict';

  var STORAGE_KEY = 'foodie-theme';
  var root = document.documentElement;

  function current() {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light';
    } catch (e) {
      return 'light';
    }
  }

  function apply(theme) {
    root.setAttribute('data-theme', theme);
    // Keep every toggle button's glyph + label in sync.
    var toggles = document.querySelectorAll('[data-theme-toggle]');
    for (var i = 0; i < toggles.length; i++) {
      var isDark = theme === 'dark';
      toggles[i].setAttribute('aria-label', isDark ? 'Switch to light mode' : 'Switch to dark mode');
      toggles[i].setAttribute('title', isDark ? 'Light mode' : 'Dark mode');
      var glyph = toggles[i].querySelector('[data-theme-glyph]');
      // Use Unicode escapes (pure ASCII source bytes) so the glyph renders
      // correctly regardless of the charset Tomcat serves this .js file with.
      if (glyph) glyph.textContent = isDark ? '\u2600' : '\u263E'; // sun (U+2600) / moon (U+263E)
    }
  }

  // Set the attribute as early as possible.
  apply(current());

  function toggle() {
    var next = current() === 'dark' ? 'light' : 'dark';
    try { localStorage.setItem(STORAGE_KEY, next); } catch (e) {}
    apply(next);
  }

  // Wire the buttons once the DOM is ready (they may not exist yet at head time).
  function wire() {
    apply(current());
    var toggles = document.querySelectorAll('[data-theme-toggle]');
    for (var i = 0; i < toggles.length; i++) {
      toggles[i].addEventListener('click', function (e) {
        e.preventDefault();
        toggle();
      });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', wire);
  } else {
    wire();
  }
})();

/**
 * Page-navigation loading overlay.
 *
 * Shows a branded spinner on the page you're leaving whenever a full-page
 * navigation starts — a link click or a real form submit. Because it renders
 * on the *outgoing* page, it also covers the wait for slow destinations (the
 * admin dashboard's cold DB queries) and the extra /*.jsp redirect hop.
 *
 * It deliberately skips anything that doesn't actually navigate: AJAX forms
 * (chat, rider actions call preventDefault), cancelled confirm() dialogs,
 * new-tab / download / hash / mailto links, and modifier-clicks.
 */
(function () {
  'use strict';

  var overlay = null;

  function build() {
    if (overlay) return overlay;
    overlay = document.createElement('div');
    overlay.className = 'page-loader';
    overlay.setAttribute('aria-hidden', 'true');
    overlay.innerHTML =
      '<div class="page-loader-spinner"></div>' +
      '<div class="page-loader-label">Loading…</div>';
    document.body.appendChild(overlay);
    return overlay;
  }

  var failsafe = null;
  function show() {
    build();
    // Next frame so the opacity transition actually runs.
    window.requestAnimationFrame(function () { overlay.classList.add('is-active'); });
    // Never let it hang forever if a navigation is blocked (the admin
    // dashboard can take ~20s, so keep this comfortably above that).
    if (failsafe) window.clearTimeout(failsafe);
    failsafe = window.setTimeout(hide, 40000);
  }

  function hide() {
    if (overlay) overlay.classList.remove('is-active');
    if (failsafe) { window.clearTimeout(failsafe); failsafe = null; }
  }

  function isModifiedClick(e) {
    return e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey;
  }

  function wireLoader() {
    build();

    // Link navigations.
    document.addEventListener('click', function (e) {
      if (e.defaultPrevented || isModifiedClick(e)) return;
      var a = e.target && e.target.closest ? e.target.closest('a') : null;
      if (!a) return;
      var href = a.getAttribute('href');
      if (!href) return;
      if (a.target && a.target !== '_self') return;   // opens a new tab/window
      if (a.hasAttribute('download')) return;
      if (href.charAt(0) === '#') return;              // in-page anchor
      if (/^(javascript|mailto|tel):/i.test(href)) return;

      var url;
      try { url = new URL(a.href, window.location.href); } catch (err) { return; }
      if (url.origin !== window.location.origin) return;         // external
      // Same page, only the hash changes -> no navigation.
      if (url.pathname === window.location.pathname &&
          url.search === window.location.search && url.hash) return;

      show();
    });

    // Real form submits. AJAX forms and cancelled confirm() dialogs have
    // already called preventDefault by the time this bubbles to document.
    document.addEventListener('submit', function (e) {
      if (e.defaultPrevented) return;
      if (e.target && e.target.getAttribute('target') === '_blank') return;
      show();
    });
  }

  // Hide on restore from the back/forward cache, so a returning page never
  // shows a stuck spinner.
  window.addEventListener('pageshow', hide);
  window.addEventListener('pagehide', hide);

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', wireLoader);
  } else {
    wireLoader();
  }
})();
