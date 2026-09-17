/* Owner Dashboard: merges the old separate Kitchen queue (accept/advance through
   prep) and Counter queue (bill + mark paid) into one polling board, since the
   single OWNER role now does both jobs. Two independently-polled sections:
   #ordersGrid (active kitchen-style board) and #billingGrid (served, awaiting
   payment). Diff-updates the kitchen grid like the original kitchen.js so cards
   don't flicker/re-animate; the billing grid is small enough to just re-render. */
(function () {
    var grid = document.getElementById('ordersGrid');
    var emptyMsg = document.getElementById('emptyQueueMessage');
    var billingGrid = document.getElementById('billingGrid');
    var billingEmptyMsg = document.getElementById('billingEmptyMessage');
    var knownOrderIds = null;
    var knownBillingIds = null;

    var STATUS_LABELS = {PENDING: 'New', ACCEPTED: 'Accepted', SERVED: 'Served'};
    var NEXT_ACTION_LABEL = {
        PENDING: '<i class="bi bi-check2"></i> Accept Order',
        ACCEPTED: '<i class="bi bi-box-seam"></i> Mark Served'
    };

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }

    function money(value) {
        return (window.CURRENCY_SYMBOL || '') + Number(value).toFixed(2);
    }

    function isStale(isoString) {
        return (Date.now() - new Date(isoString).getTime()) / 1000 > 600;
    }

    function timeAgo(isoString) {
        var then = new Date(isoString);
        var seconds = Math.max(0, Math.floor((Date.now() - then.getTime()) / 1000));
        if (seconds < 60) { return seconds + 's ago'; }
        var minutes = Math.floor(seconds / 60);
        if (minutes < 60) { return minutes + 'm ago'; }
        var hours = Math.floor(minutes / 60);
        return hours + 'h ' + (minutes % 60) + 'm ago';
    }

    // ---------- Live kitchen-style board (Pending -> Accepted -> Preparing -> Ready -> Served) ----------

    function orderCard(order, isNew) {
        var statusClass = 'status-' + order.status.toLowerCase();
        var itemsHtml = order.items.map(function (item) {
            var note = item.specialInstructions
                ? '<div class="small text-muted-brand ms-4 ps-1">&#8618; ' + escapeHtml(item.specialInstructions) + '</div>'
                : '';
            return '<li class="d-flex align-items-center mb-1"><span class="order-item-qty">' + item.quantity + '</span>'
                + '<span>' + escapeHtml(item.name) + '</span></li>' + note;
        }).join('');

        var orderNoteHtml = order.customerNote
            ? '<div class="small text-muted-brand mt-1 pt-2 border-top"><i class="bi bi-sticky"></i> ' + escapeHtml(order.customerNote) + '</div>'
            : '';

        var actionLabel = NEXT_ACTION_LABEL[order.status] || '';
        var actionBtn = actionLabel
            ? '<button type="button" class="btn btn-brand w-100 mt-2 advance-btn" data-order-id="' + order.orderId + '">' + actionLabel + '</button>'
            : '';
        var cancelBtn = (order.status === 'PENDING' || order.status === 'ACCEPTED')
            ? '<button type="button" class="btn btn-outline-danger btn-sm w-100 mt-2 cancel-btn" data-order-id="' + order.orderId + '"><i class="bi bi-x-circle"></i> Cancel</button>'
            : '';

        return '<div class="col-sm-6 col-lg-4 col-xl-3" data-order-card="' + order.orderId + '">'
            + '<div class="card h-100 kitchen-order-card ' + statusClass + (isNew ? ' is-new' : '') + '">'
            + '<div class="card-body d-flex flex-column">'
            + '<div class="d-flex justify-content-between align-items-start mb-1">'
            + '<div><strong class="fs-6">' + escapeHtml(order.orderNo) + '</strong><div class="small text-muted-brand"><i class="bi bi-grid-3x3-gap"></i> Table '
            + escapeHtml(order.tableNo) + '</div></div>'
            + '<span class="status-chip ' + statusClass + '">' + (STATUS_LABELS[order.status] || order.status) + '</span>'
            + '</div>'
            + '<ul class="list-unstyled small my-2 flex-grow-1">' + itemsHtml + '</ul>'
            + orderNoteHtml
            + '<div class="small text-muted-brand mt-2"><i class="bi bi-clock-history"></i> <span class="order-age'
            + (isStale(order.createdAt) ? ' stale' : '') + '" data-created-at="'
            + order.createdAt + '">' + timeAgo(order.createdAt) + '</span></div>'
            + actionBtn + cancelBtn
            + '</div></div></div>';
    }

    function cardSignature(order) {
        return order.status + '|' + order.items.length + '|' + (order.customerNote || '') + '|'
            + order.items.map(function (i) { return i.quantity + ':' + i.specialInstructions; }).join(',');
    }

    function renderBoard(orders) {
        emptyMsg.classList.toggle('d-none', orders.length > 0);
        var seenIds = {};
        var previousSibling = null;

        orders.forEach(function (order) {
            var id = String(order.orderId);
            seenIds[id] = true;
            var signature = cardSignature(order);
            var existing = grid.querySelector('[data-order-card="' + id + '"]');
            var el;

            if (existing && existing.dataset.signature === signature) {
                el = existing;
            } else {
                var isNew = knownOrderIds !== null && knownOrderIds.indexOf(order.orderId) === -1;
                var wrapper = document.createElement('div');
                wrapper.innerHTML = orderCard(order, isNew && !existing);
                el = wrapper.firstElementChild;
                el.dataset.signature = signature;
                if (existing) {
                    existing.replaceWith(el);
                } else {
                    grid.appendChild(el);
                }
            }

            var desiredAfter = previousSibling ? previousSibling.nextElementSibling : grid.firstElementChild;
            if (desiredAfter !== el) {
                grid.insertBefore(el, desiredAfter);
            }
            previousSibling = el;
        });

        Array.prototype.slice.call(grid.children).forEach(function (card) {
            if (!seenIds[card.dataset.orderCard]) {
                card.remove();
            }
        });
    }

    function pollBoard() {
        fetch(window.APP_CONTEXT_PATH + '/owner/orders')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                var orders = data.orders || [];
                var currentIds = orders.map(function (o) { return o.orderId; });
                if (knownOrderIds !== null) {
                    var hasNewOrder = currentIds.some(function (id) { return knownOrderIds.indexOf(id) === -1; });
                    if (hasNewOrder && window.playNotificationSound) {
                        window.playNotificationSound();
                    }
                }
                knownOrderIds = currentIds;
                renderBoard(orders);
                setLiveIndicator(true);
            })
            .catch(function () { setLiveIndicator(false); });
    }

    // ---------- Billing queue (whole table sessions, all orders Served, awaiting payment) ----------

    function billingCard(session, index) {
        var itemsHtml = session.items.map(function (item) {
            var note = item.specialInstructions
                ? '<div class="small text-muted-brand ms-4 ps-1">&#8618; ' + escapeHtml(item.specialInstructions) + '</div>'
                : '';
            return '<li class="d-flex align-items-center mb-1"><span class="order-item-qty">' + item.quantity + '</span>'
                + '<span>' + escapeHtml(item.name) + '</span></li>' + note;
        }).join('');
        var orderCountLabel = session.orderCount + (session.orderCount === 1 ? ' order' : ' orders combined');

        return '<div class="col-sm-6 col-lg-4 col-xl-3" data-billing-card="' + session.tableSessionId + '">'
            + '<div class="card h-100 counter-order-card status-served animate-in" style="animation-delay:' + ((index % 8) * 0.04) + 's">'
            + '<div class="card-body">'
            + '<div class="d-flex justify-content-between align-items-start">'
            + '<div><strong class="fs-6">Table ' + escapeHtml(session.tableNo) + '</strong>'
            + '<div class="small text-muted-brand">' + orderCountLabel + '</div></div>'
            + '<span class="status-chip status-served">Served</span>'
            + '</div>'
            + '<ul class="list-unstyled small my-2">' + itemsHtml + '</ul>'
            + '<div class="fs-5 fw-bold mt-1 brand-text">' + money(session.grandTotal) + '</div>'
            + '<div class="small text-muted-brand mt-2"><i class="bi bi-clock-history"></i> ' + timeAgo(session.openedAt) + '</div>'
            + '<div class="d-flex gap-2 mt-2">'
            + '<select class="form-select form-select-sm pay-method" data-session-id="' + session.tableSessionId + '">'
            + '<option value="CASH">Cash</option><option value="CARD">Card</option><option value="UPI">UPI</option><option value="OTHER">Other</option>'
            + '</select>'
            + '<button type="button" class="btn btn-brand btn-sm text-nowrap pay-btn" data-session-id="' + session.tableSessionId + '"><i class="bi bi-cash-coin"></i> Mark Paid</button>'
            + '</div>'
            + '<a class="btn btn-outline-brand btn-sm w-100 mt-2" target="_blank" href="' + window.APP_CONTEXT_PATH + '/owner/table-sessions/' + session.tableSessionId + '/invoice"><i class="bi bi-receipt"></i> Invoice</a>'
            + '</div></div></div>';
    }

    function renderBilling(sessions) {
        if (!billingGrid) { return; }
        billingEmptyMsg.classList.toggle('d-none', sessions.length > 0);
        billingGrid.innerHTML = sessions.map(billingCard).join('');
    }

    function pollBilling() {
        if (!billingGrid) { return; }
        fetch(window.APP_CONTEXT_PATH + '/owner/table-sessions/billing')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                var sessions = data.sessions || [];
                var currentIds = sessions.map(function (s) { return s.tableSessionId; });
                if (knownBillingIds !== null) {
                    var hasNewSession = currentIds.some(function (id) { return knownBillingIds.indexOf(id) === -1; });
                    if (hasNewSession && window.playNotificationSound) {
                        window.playNotificationSound();
                    }
                }
                knownBillingIds = currentIds;
                renderBilling(sessions);
            });
    }

    // ---------- Table assistance requests (call waiter, clean table, complaint, etc.) ----------

    var REQUEST_TYPE_LABELS = {
        CALL_WAITER: 'Call the owner over',
        CLEAN_TABLE: 'Please clean this table',
        WATER_REFILL: 'Water refill',
        REQUEST_BILL: 'Bring the bill',
        COMPLAINT: 'Complaint',
        OTHER: 'Other request'
    };
    var REQUEST_TYPE_ICON = {
        CALL_WAITER: 'bi-person-raised-hand',
        CLEAN_TABLE: 'bi-droplet',
        WATER_REFILL: 'bi-cup-straw',
        REQUEST_BILL: 'bi-receipt',
        COMPLAINT: 'bi-exclamation-triangle',
        OTHER: 'bi-chat-dots'
    };
    var assistanceGrid = document.getElementById('assistanceGrid');
    var assistanceEmptyMsg = document.getElementById('assistanceEmptyMessage');
    var knownAssistanceIds = null;

    function assistanceCard(request, index) {
        var isComplaint = request.requestType === 'COMPLAINT';
        var noteHtml = request.message
            ? '<div class="small text-muted-brand mt-1">"' + escapeHtml(request.message) + '"</div>'
            : '';
        return '<div class="col-sm-6 col-lg-4 col-xl-3" data-assistance-card="' + request.assistanceRequestId + '">'
            + '<div class="card h-100 ' + (isComplaint ? 'border-danger' : 'border-warning') + ' animate-in" style="animation-delay:' + ((index % 8) * 0.04) + 's">'
            + '<div class="card-body">'
            + '<div class="d-flex justify-content-between align-items-start">'
            + '<div><i class="bi ' + (REQUEST_TYPE_ICON[request.requestType] || 'bi-bell') + ' me-1"></i>'
            + '<strong>' + (REQUEST_TYPE_LABELS[request.requestType] || request.requestType) + '</strong>'
            + '<div class="small text-muted-brand"><i class="bi bi-grid-3x3-gap"></i> Table ' + escapeHtml(request.tableNo) + '</div></div>'
            + '</div>'
            + noteHtml
            + '<div class="small text-muted-brand mt-2"><i class="bi bi-clock-history"></i> ' + timeAgo(request.createdAt) + '</div>'
            + '<button type="button" class="btn btn-brand btn-sm w-100 mt-2 resolve-assistance-btn" data-request-id="' + request.assistanceRequestId + '">'
            + '<i class="bi bi-check2"></i> Mark Handled</button>'
            + '</div></div></div>';
    }

    function renderAssistance(requests) {
        if (!assistanceGrid) { return; }
        assistanceEmptyMsg.classList.toggle('d-none', requests.length > 0);
        assistanceGrid.innerHTML = requests.map(assistanceCard).join('');
    }

    function pollAssistance() {
        if (!assistanceGrid) { return; }
        fetch(window.APP_CONTEXT_PATH + '/owner/assistance')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                var requests = data.requests || [];
                var currentIds = requests.map(function (r) { return r.assistanceRequestId; });
                if (knownAssistanceIds !== null) {
                    var hasNew = currentIds.some(function (id) { return knownAssistanceIds.indexOf(id) === -1; });
                    if (hasNew && window.playNotificationSound) {
                        window.playNotificationSound();
                    }
                }
                knownAssistanceIds = currentIds;
                renderAssistance(requests);
            });
    }

    function setLiveIndicator(isLive) {
        var indicator = document.getElementById('liveIndicator');
        if (!indicator) { return; }
        if (isLive) {
            indicator.className = 'badge bg-success';
            indicator.innerHTML = '<i class="bi bi-broadcast"></i> Live';
        } else {
            indicator.className = 'badge bg-danger';
            indicator.innerHTML = '<i class="bi bi-broadcast-pin"></i> Offline';
        }
    }

    // ---------- Actions ----------

    document.addEventListener('click', function (e) {
        var advanceBtn = e.target.closest('.advance-btn');
        var cancelBtn = e.target.closest('.cancel-btn');
        var payBtn = e.target.closest('.pay-btn');
        var resolveBtn = e.target.closest('.resolve-assistance-btn');

        if (advanceBtn) {
            advanceBtn.disabled = true;
            advanceBtn.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Updating...';
            fetch(window.APP_CONTEXT_PATH + '/owner/orders/' + advanceBtn.dataset.orderId + '/advance', {method: 'POST', headers: window.getCsrfHeaders()})
                .then(function () { pollBoard(); pollBilling(); });
        } else if (cancelBtn) {
            if (!confirm('Cancel this order?')) { return; }
            fetch(window.APP_CONTEXT_PATH + '/owner/orders/' + cancelBtn.dataset.orderId + '/cancel', {method: 'POST', headers: window.getCsrfHeaders()})
                .then(function () { pollBoard(); });
        } else if (payBtn) {
            var select = document.querySelector('.pay-method[data-session-id="' + payBtn.dataset.sessionId + '"]');
            var method = select ? select.value : 'CASH';
            payBtn.disabled = true;
            payBtn.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>';
            fetch(window.APP_CONTEXT_PATH + '/owner/table-sessions/' + payBtn.dataset.sessionId + '/settle?method=' + method, {method: 'POST', headers: window.getCsrfHeaders()})
                .then(function () { pollBilling(); });
        } else if (resolveBtn) {
            resolveBtn.disabled = true;
            resolveBtn.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>';
            fetch(window.APP_CONTEXT_PATH + '/owner/assistance/' + resolveBtn.dataset.requestId + '/resolve', {method: 'POST', headers: window.getCsrfHeaders()})
                .then(function () { pollAssistance(); });
        }
    });

    setInterval(function () {
        document.querySelectorAll('.order-age').forEach(function (el) {
            var seconds = Math.floor((Date.now() - new Date(el.dataset.createdAt).getTime()) / 1000);
            el.textContent = timeAgo(el.dataset.createdAt);
            el.classList.toggle('stale', seconds > 600);
        });
    }, 15000);

    pollBoard();
    pollBilling();
    pollAssistance();
    setInterval(pollBoard, window.REFRESH_INTERVAL_MS || 5000);
    setInterval(pollBilling, window.REFRESH_INTERVAL_MS || 5000);
    setInterval(pollAssistance, window.REFRESH_INTERVAL_MS || 5000);

    // Instant push on top of the polling above: the moment an order or assistance request
    // changes anywhere, re-poll right away instead of waiting for the next tick. Polling above
    // remains the source of truth/safety net if the socket never connects or drops.
    if (window.connectRealtime) {
        window.connectRealtime(['/topic/owner-orders', '/topic/owner-assistance'], function (message, topic) {
            if (topic === '/topic/owner-assistance') {
                pollAssistance();
            } else {
                pollBoard();
                pollBilling();
            }
        });
    }
})();
