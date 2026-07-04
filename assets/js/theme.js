/**
 * Site-wide dark / light theme.
 *
 * Runs synchronously in <head> (no `defer`) so the `data-theme` attribute is
 * set on <html> before the first paint — this avoids a white flash when a
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
      if (glyph) glyph.textContent = isDark ? '☀' : '☾'; // ☀ / ☾
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
