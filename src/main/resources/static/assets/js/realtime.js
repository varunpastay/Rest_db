/* Thin wrapper around SockJS + STOMP so any page can subscribe to a topic and get pushed
   updates instantly instead of waiting for its next poll tick. Polling stays in place on every
   page that uses this as the source of truth and safety net - if the socket never connects, or
   drops and can't reconnect (older browser, restrictive proxy, brief network blip), the page
   still updates correctly within a few seconds via polling. This only ever triggers an
   immediate extra poll; it never renders anything by itself. */
window.connectRealtime = function (topics, onMessage) {
    if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') {
        return; // library failed to load (e.g. offline) - polling alone still keeps the page correct
    }

    var client = new StompJs.Client({
        webSocketFactory: function () { return new SockJS('/ws'); },
        reconnectDelay: 4000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: function () {
            topics.forEach(function (topic) {
                client.subscribe(topic, function (message) {
                    try {
                        onMessage(JSON.parse(message.body), topic);
                    } catch (e) {
                        onMessage(null, topic);
                    }
                });
            });
        }
    });
    client.activate();
    return client;
};
