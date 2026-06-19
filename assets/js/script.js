'use strict';



/**
 * navbar toggle
 */

const navbar = document.querySelector("[data-navbar]");
const navbarLinks = document.querySelectorAll("[data-nav-link]");
const menuToggleBtn = document.querySelector("[data-menu-toggle-btn]");

if (navbar && menuToggleBtn) {
  menuToggleBtn.addEventListener("click", function () {
    navbar.classList.toggle("active");
    this.classList.toggle("active");
  });
}

for (let i = 0; i < navbarLinks.length; i++) {
  navbarLinks[i].addEventListener("click", function () {
    if (navbar && menuToggleBtn) {
      navbar.classList.remove("active");
      menuToggleBtn.classList.remove("active");
    }
  });
}



/**
 * header sticky & back to top
 */

const header = document.querySelector("[data-header]");
const backTopBtn = document.querySelector("[data-back-top-btn]");

window.addEventListener("scroll", function () {
  if (header && backTopBtn) {
    if (window.scrollY >= 100) {
      header.classList.add("active");
      backTopBtn.classList.add("active");
    } else {
      header.classList.remove("active");
      backTopBtn.classList.remove("active");
    }
  }
});



/**
 * search box toggle
 */

const searchBtn = document.querySelector("[data-search-btn]");
const searchContainer = document.querySelector("[data-search-container]");
const searchSubmitBtn = document.querySelector("[data-search-submit-btn]");
const searchCloseBtn = document.querySelector("[data-search-close-btn]");

const searchBoxElems = [searchBtn, searchSubmitBtn, searchCloseBtn];

for (let i = 0; i < searchBoxElems.length; i++) {
  if (searchBoxElems[i] && searchContainer) {
    searchBoxElems[i].addEventListener("click", function () {
      searchContainer.classList.toggle("active");
      document.body.classList.toggle("active");
    });
  }
}



/**
 * move cycle on scroll
 */

const deliveryBoy = document.querySelector("[data-delivery-boy]");

let deliveryBoyMove = -80;
let lastScrollPos = 0;

window.addEventListener("scroll", function () {

  if (!deliveryBoy) return;

  let deliveryBoyTopPos = deliveryBoy.getBoundingClientRect().top;

  if (deliveryBoyTopPos < 500 && deliveryBoyTopPos > -250) {
    let activeScrollPos = window.scrollY;

    if (lastScrollPos < activeScrollPos) {
      deliveryBoyMove += 1;
    } else {
      deliveryBoyMove -= 1;
    }

    lastScrollPos = activeScrollPos;
    deliveryBoy.style.transform = `translateX(${deliveryBoyMove}px)`;
  }

});



/**
 * password visibility toggle
 */

const passwordToggleBtns = document.querySelectorAll("[data-password-toggle]");

for (let i = 0; i < passwordToggleBtns.length; i++) {
  passwordToggleBtns[i].addEventListener("click", function () {
    const target = document.querySelector(this.dataset.passwordToggle);

    if (!target) return;

    const isHidden = target.type === "password";
    target.type = isHidden ? "text" : "password";
    this.setAttribute("aria-label", isHidden ? "Hide password" : "Show password");

    const icon = this.querySelector("ion-icon");
    if (icon) {
      icon.setAttribute("name", isHidden ? "eye-off-outline" : "eye-outline");
    }
  });
}
