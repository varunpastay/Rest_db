/* Polls /menu/table-orders every few seconds so every phone scanning this table's QR sees a
   shared, up-to-date list of everything ordered at the table so far this visit - not just
   what they personally added to their own cart. */
(function () {
    var panel = document.getElementById('tableOrdersPanel');
    var list = document.getElementById('tableOrdersList');
    if (!panel || !list) { return; }

    var STATUS_LABELS = {PENDING: 'Pending', ACCEPTED: 'Accepted', SERVED: 'Served', COMPLETED: 'Paid', CANCELLED: 'Cancelled'};

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }

    function render(items) {
        if (!items.length) {
            panel.classList.add('d-none');
            return;
        }
        panel.classList.remove('d-none');
        list.innerHTML = items.map(function (item) {
            var statusClass = 'status-' + item.orderStatus.toLowerCase();
            return '<li class="list-group-item d-flex justify-content-between align-items-center">'
                + '<span>' + item.quantity + 'x ' + escapeHtml(item.name) + '</span>'
                + '<span class="status-chip ' + statusClass + '">' + (STATUS_LABELS[item.orderStatus] || item.orderStatus) + '</span>'
                + '</li>';
        }).join('');
    }

    function poll() {
        fetch(window.APP_CONTEXT_PATH + '/menu/table-orders')
            .then(function (res) { return res.json(); })
            .then(function (data) { render(data.items || []); })
            .catch(function () { /* keep showing the last known list on a transient network error */ });
    }

    poll();
    setInterval(poll, 5000);

    // Instant push on top of the polling above: the moment anyone else at this table orders
    // (or an order's status changes), refresh right away instead of waiting for the next tick.
    if (window.connectRealtime && window.TABLE_ID) {
        window.connectRealtime(['/topic/table-orders-' + window.TABLE_ID], function () {
            poll();
        });
    }
})();
