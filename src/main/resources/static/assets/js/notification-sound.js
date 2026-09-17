/* Generates a short two-tone "new order" chime via the Web Audio API - no
   audio file asset needed, works fully offline. Shared by kitchen/counter
   dashboards (any screen that wants to alert staff to something new). */
window.playNotificationSound = function () {
    try {
        var AudioContextClass = window.AudioContext || window.webkitAudioContext;
        var ctx = new AudioContextClass();
        var now = ctx.currentTime;
        [880, 1108.73].forEach(function (freq, index) {
            var osc = ctx.createOscillator();
            var gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.value = freq;
            var start = now + index * 0.18;
            gain.gain.setValueAtTime(0, start);
            gain.gain.linearRampToValueAtTime(0.3, start + 0.02);
            gain.gain.linearRampToValueAtTime(0, start + 0.16);
            osc.connect(gain).connect(ctx.destination);
            osc.start(start);
            osc.stop(start + 0.2);
        });
    } catch (e) {
        /* Web Audio unavailable/blocked by autoplay policy - never let a sound failure break the dashboard. */
    }
};
