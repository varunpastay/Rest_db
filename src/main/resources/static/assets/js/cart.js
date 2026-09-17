/* Drives the menu page's cart: syncs with /cart (session-scoped, server-trusted
   prices) via fetch, re-renders the per-card Add/stepper controls, the sticky
   cart bar, and the off-canvas drawer, and submits /order/place. Uses event
   delegation on document so newly-rendered drawer rows need no re-binding. */
(function () {
    var cartState = {items: [], subtotal: 0, totalQuantity: 0};

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }

    function money(value) {
        return window.CURRENCY_SYMBOL + Number(value).toFixed(2);
    }

    function post(action, params) {
        var body = new URLSearchParams(Object.assign({action: action}, params));
        return fetch(window.APP_CONTEXT_PATH + '/cart', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body.toString()
        }).then(function (res) {
            return res.json().then(function (data) {
                return {ok: res.ok, data: data};
            });
        });
    }

    function fetchCart() {
        fetch(window.APP_CONTEXT_PATH + '/cart')
            .then(function (res) {
                return res.json();
            })
            .then(render);
    }

    function render(data) {
        cartState = data;
        renderMenuControls();
        renderCartBar();
        renderDrawer();
    }

    function renderMenuControls() {
        document.querySelectorAll('.add-control').forEach(function (el) {
            var foodItemId = el.dataset.foodItemId;
            var item = cartState.items.find(function (i) {
                return String(i.foodItemId) === foodItemId;
            });
            var addBtn = el.querySelector('.add-btn');
            var stepper = el.querySelector('.qty-stepper');
            if (item && item.quantity > 0) {
                addBtn.classList.add('d-none');
                stepper.classList.remove('d-none');
                stepper.querySelector('.qty-display').textContent = item.quantity;
            } else {
                addBtn.classList.remove('d-none');
                stepper.classList.add('d-none');
            }
        });
    }

    function renderCartBar() {
        var bar = document.getElementById('cartBar');
        if (!bar) {
            return;
        }
        var countEl = document.getElementById('cartItemCountLabel');
        var previousCount = Number(countEl.dataset.count) || 0;
        countEl.dataset.count = cartState.totalQuantity;
        countEl.textContent = cartState.totalQuantity + (cartState.totalQuantity === 1 ? ' item' : ' items');
        document.getElementById('cartSubtotal').textContent = money(cartState.subtotal);
        var wasHidden = bar.classList.contains('d-none');
        bar.classList.toggle('d-none', cartState.totalQuantity === 0);
        if (cartState.totalQuantity > 0 && (cartState.totalQuantity > previousCount || wasHidden)) {
            bar.classList.remove('animate-pulse-once');
            void bar.offsetWidth; // restart the animation even if it just ran
            bar.classList.add('animate-pulse-once');
        }
    }

    function renderDrawer() {
        var list = document.getElementById('cartItemsList');
        var emptyMsg = document.getElementById('cartEmptyMessage');
        if (!list) {
            return;
        }
        document.getElementById('drawerSubtotal').textContent = money(cartState.subtotal);

        if (cartState.items.length === 0) {
            list.innerHTML = '';
            emptyMsg.classList.remove('d-none');
            return;
        }
        emptyMsg.classList.add('d-none');
        list.innerHTML = cartState.items.map(function (item) {
            var imgHtml = item.imagePath
                ? '<img src="' + window.APP_CONTEXT_PATH + '/images?path=' + encodeURIComponent(item.imagePath) + '" alt="">'
                : '<div class="cart-item-img-placeholder"><i class="bi bi-cup-hot"></i></div>';
            return '<div class="d-flex gap-2 cart-item-row" data-food-item-id="' + item.foodItemId + '">' +
                imgHtml +
                '<div class="flex-grow-1 min-w-0">' +
                '<div class="d-flex justify-content-between align-items-start gap-2">' +
                '<span class="fw-semibold">' + escapeHtml(item.name) + '</span>' +
                '<button type="button" class="btn btn-sm btn-link text-danger p-0 remove-btn" aria-label="Remove"><i class="bi bi-trash3"></i></button>' +
                '</div>' +
                '<div class="d-flex justify-content-between align-items-center mt-1">' +
                '<div class="btn-group btn-group-sm qty-stepper">' +
                '<button type="button" class="btn btn-outline-brand dec-btn">-</button>' +
                '<span class="btn qty-display" style="pointer-events:none;">' + item.quantity + '</span>' +
                '<button type="button" class="btn btn-outline-brand inc-btn">+</button>' +
                '</div>' +
                '<span class="fw-semibold">' + money(item.lineTotal) + '</span>' +
                '</div>' +
                '<input type="text" class="form-control form-control-sm mt-2 note-input" ' +
                'placeholder="Add a note (e.g. no onions)" value="' + escapeHtml(item.specialInstructions || '') + '">' +
                '</div>' +
                '</div>';
        }).join('');
    }

    function quantityOf(foodItemId) {
        var item = cartState.items.find(function (i) {
            return String(i.foodItemId) === String(foodItemId);
        });
        return item ? item.quantity : 0;
    }

    function showOrderError(message) {
        var el = document.getElementById('orderError');
        if (!el) {
            return;
        }
        if (!message) {
            el.classList.add('d-none');
            el.textContent = '';
        } else {
            el.textContent = message;
            el.classList.remove('d-none');
        }
    }

    function handleCartResponse(result) {
        if (!result.ok) {
            showOrderError(result.data.error || 'Something went wrong.');
            return;
        }
        showOrderError(null);
        render(result.data);
    }

    document.addEventListener('click', function (e) {
        var addBtn = e.target.closest('.add-btn');
        var incBtn = e.target.closest('.inc-btn');
        var decBtn = e.target.closest('.dec-btn');
        var removeBtn = e.target.closest('.remove-btn');

        if (addBtn) {
            if (!window.RESTAURANT_OPEN) {
                return;
            }
            var addContainer = addBtn.closest('[data-food-item-id]');
            post('add', {foodItemId: addContainer.dataset.foodItemId, quantity: 1}).then(handleCartResponse);
        } else if (incBtn) {
            var incContainer = incBtn.closest('[data-food-item-id]');
            var incId = incContainer.dataset.foodItemId;
            post('updateQuantity', {foodItemId: incId, quantity: quantityOf(incId) + 1}).then(handleCartResponse);
        } else if (decBtn) {
            var decContainer = decBtn.closest('[data-food-item-id]');
            var decId = decContainer.dataset.foodItemId;
            post('updateQuantity', {foodItemId: decId, quantity: quantityOf(decId) - 1}).then(handleCartResponse);
        } else if (removeBtn) {
            var removeContainer = removeBtn.closest('[data-food-item-id]');
            post('remove', {foodItemId: removeContainer.dataset.foodItemId}).then(handleCartResponse);
        }
    });

    document.addEventListener('change', function (e) {
        var noteInput = e.target.closest('.note-input');
        if (!noteInput) {
            return;
        }
        var container = noteInput.closest('[data-food-item-id]');
        post('updateNote', {foodItemId: container.dataset.foodItemId, note: noteInput.value}).then(handleCartResponse);
    });

    var placeOrderBtn = document.getElementById('placeOrderBtn');
    if (placeOrderBtn) {
        placeOrderBtn.addEventListener('click', function () {
            if (cartState.items.length === 0) {
                return;
            }
            placeOrderBtn.disabled = true;
            placeOrderBtn.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Placing order...';
            var customerNote = document.getElementById('customerNote').value;
            var discountCode = document.getElementById('discountCode').value;

            fetch(window.APP_CONTEXT_PATH + '/order/place', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: new URLSearchParams({customerNote: customerNote, discountCode: discountCode}).toString()
            }).then(function (res) {
                return res.json().then(function (data) {
                    return {ok: res.ok, data: data};
                });
            }).then(function (result) {
                if (!result.ok) {
                    showOrderError(result.data.error || 'Could not place order.');
                    placeOrderBtn.disabled = false;
                    placeOrderBtn.textContent = 'Place Order';
                    return;
                }
                window.location.href = window.APP_CONTEXT_PATH + '/order/track/' + encodeURIComponent(result.data.orderNo);
            }).catch(function () {
                showOrderError('Network error - please try again.');
                placeOrderBtn.disabled = false;
                placeOrderBtn.innerHTML = '<i class="bi bi-check2-circle"></i> Place Order';
            });
        });
    }

    fetchCart();
})();
