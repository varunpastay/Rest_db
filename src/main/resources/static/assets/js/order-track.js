/* Polls /order/status for the current order and updates the timeline UI without a full page reload. */
(function () {
    var orderNo = window.ORDER_NO;
    var statusOrder = ['PENDING', 'ACCEPTED', 'SERVED', 'COMPLETED'];

    function applyStatus(data) {
        var currentIndex = statusOrder.indexOf(data.status);
        document.querySelectorAll('.status-timeline li').forEach(function (li) {
            var stepIndex = statusOrder.indexOf(li.dataset.status);
            li.classList.remove('done', 'current');
            if (data.status === 'CANCELLED') {
                return;
            }
            if (stepIndex < currentIndex) {
                li.classList.add('done');
            }
            if (stepIndex === currentIndex) {
                li.classList.add('current');
            }
        });
        var banner = document.getElementById('statusBanner');
        var icon = document.getElementById('statusIcon');
        var LABELS = {PENDING: 'Order Received', ACCEPTED: 'Accepted', SERVED: 'Served', COMPLETED: 'Completed', CANCELLED: 'Cancelled'};
        if (banner) {
            var cancelled = data.status === 'CANCELLED';
            banner.textContent = LABELS[data.status] || data.status;
            banner.style.backgroundColor = cancelled ? '#6c757d' : 'var(--brand)';
        }
        if (icon) {
            icon.className = data.status === 'CANCELLED'
                ? 'bi bi-x-circle-fill text-secondary'
                : data.status === 'SERVED' || data.status === 'COMPLETED'
                    ? 'bi bi-check-circle-fill brand-text'
                    : 'bi bi-hourglass-split brand-text';
            icon.style.fontSize = '3.2rem';
        }
    }

    function poll() {
        fetch(window.APP_CONTEXT_PATH + '/order/status/' + encodeURIComponent(orderNo))
            .then(function (res) {
                return res.json();
            })
            .then(function (data) {
                if (!data.error) {
                    applyStatus(data);
                }
            })
            .catch(function () {
                /* transient network hiccup - next poll will retry */
            });
    }

    if (orderNo) {
        poll();
        setInterval(poll, 8000);

        // Instant push on top of the polling above: the moment the owner updates this order's
        // status, refresh right away instead of waiting for the next tick.
        if (window.connectRealtime) {
            window.connectRealtime(['/topic/order-' + orderNo], function (data) {
                if (data && data.status) {
                    applyStatus(data);
                } else {
                    poll();
                }
            });
        }
    }
})();
