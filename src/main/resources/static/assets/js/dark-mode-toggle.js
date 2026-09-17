/* Shared light/dark toggle wiring for pages that render #darkModeToggle (owner pages;
   the customer menu page wires its own copy inside menu.js alongside search/filter logic).
   Also wires the Open/Closed pill in the owner nav bar to POST /owner/settings/open-toggle
   and reload, since that endpoint existed but nothing in the UI called it. */
(function () {
    var toggle = document.getElementById('darkModeToggle');
    if (toggle) {
        var htmlEl = document.documentElement;
        toggle.addEventListener('click', function () {
            var next = htmlEl.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            htmlEl.setAttribute('data-bs-theme', next);
            localStorage.setItem('theme', next);
        });
    }

    var openToggle = document.getElementById('restaurantOpenToggle');
    if (openToggle) {
        openToggle.addEventListener('click', function () {
            openToggle.disabled = true;
            fetch('/owner/settings/open-toggle', {method: 'POST', headers: window.getCsrfHeaders()})
                .then(function () { window.location.reload(); })
                .catch(function () { openToggle.disabled = false; });
        });
    }
})();
