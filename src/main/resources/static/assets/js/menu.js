/* Client-side search + category filter for the menu grid, and the light/dark toggle.
   Filtering runs entirely in the browser against the already-rendered DOM - no
   server round-trip - since a restaurant menu is small enough (tens to a couple
   hundred items) that this is instant and works even on a flaky connection. */
(function () {
    var searchInput = document.getElementById('menuSearch');
    var noResults = document.getElementById('noResults');
    var categoryLinks = document.querySelectorAll('.category-nav .nav-link');
    var itemCols = document.querySelectorAll('.food-item-col');
    var currentCategory = 'all';

    function applyFilters() {
        var searchText = searchInput.value.trim().toLowerCase();
        var anyVisible = false;

        itemCols.forEach(function (col) {
            var name = (col.dataset.name || '').toLowerCase();
            var categoryId = col.dataset.categoryId;
            var matchesCategory = currentCategory === 'all' || currentCategory === categoryId;
            var matchesSearch = !searchText || name.indexOf(searchText) !== -1;
            var visible = matchesCategory && matchesSearch;
            col.classList.toggle('d-none', !visible);
            if (visible) {
                anyVisible = true;
            }
        });

        document.querySelectorAll('.menu-category').forEach(function (section) {
            var hasVisible = section.querySelector('.food-item-col:not(.d-none)');
            section.classList.toggle('d-none', !hasVisible);
        });

        noResults.classList.toggle('d-none', anyVisible);
    }

    if (searchInput) {
        searchInput.addEventListener('input', applyFilters);
    }

    categoryLinks.forEach(function (link) {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            currentCategory = link.dataset.filter;
            categoryLinks.forEach(function (l) {
                l.classList.remove('active');
            });
            link.classList.add('active');
            searchInput.value = '';
            applyFilters();
            if (currentCategory !== 'all') {
                var section = document.getElementById('cat-' + currentCategory);
                if (section) {
                    section.scrollIntoView({behavior: 'smooth'});
                }
            }
        });
    });

    var darkModeToggle = document.getElementById('darkModeToggle');
    var htmlEl = document.documentElement;
    var storedTheme = localStorage.getItem('theme');
    if (storedTheme) {
        htmlEl.setAttribute('data-bs-theme', storedTheme);
    }
    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function () {
            var next = htmlEl.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            htmlEl.setAttribute('data-bs-theme', next);
            localStorage.setItem('theme', next);
        });
    }
})();
