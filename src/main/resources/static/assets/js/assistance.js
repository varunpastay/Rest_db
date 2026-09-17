/* Wires the "Need Help?" modal on the customer menu page to POST /assistance/request. */
(function () {
    var modalEl = document.getElementById('helpModal');
    if (!modalEl) { return; }

    var sentMessage = document.getElementById('helpSentMessage');
    var errorBox = document.getElementById('helpError');
    var noteField = document.getElementById('helpMessage');

    function send(type, message) {
        errorBox.classList.add('d-none');
        var body = new URLSearchParams({type: type});
        if (message) { body.append('message', message); }

        fetch(window.APP_CONTEXT_PATH + '/assistance/request', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body.toString()
        })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (data.error) {
                    errorBox.textContent = data.error;
                    errorBox.classList.remove('d-none');
                    return;
                }
                sentMessage.classList.remove('d-none');
                noteField.value = '';
                setTimeout(function () {
                    var modal = bootstrap.Modal.getInstance(modalEl);
                    if (modal) { modal.hide(); }
                    sentMessage.classList.add('d-none');
                }, 1400);
            })
            .catch(function () {
                errorBox.textContent = 'Could not send your request - please try again.';
                errorBox.classList.remove('d-none');
            });
    }

    document.querySelectorAll('.help-option').forEach(function (btn) {
        btn.addEventListener('click', function () {
            send(btn.dataset.type, noteField.value.trim());
        });
    });

    document.getElementById('helpOtherBtn').addEventListener('click', function () {
        var note = noteField.value.trim();
        if (!note) {
            noteField.focus();
            return;
        }
        send('OTHER', note);
    });
})();
