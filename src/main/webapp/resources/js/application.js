var cometd = $.cometd;

cometd.websocketEnabled = true;

$(document).ready(function()
{
    function _authenticationFailed() {
        $('#body').append('<div>CometD Authentication Failed</div>');
    }

    function _connectionEstablished() {
        $('#body').append('<div>CometD Connection Established</div>');
    }

    function _connectionBroken() {
        $('#body').append('<div>CometD Connection Broken</div>');
    }

    function _connectionClosed() {
        $('#body').append('<div>CometD Connection Closed</div>');
    }

    /* Function that manages the connection status with the Bayeux server */
    var _connected = false;
    function _metaConnect(message) {
        if (cometd.isDisconnected()) {
            _connected = false;
            _connectionClosed();
            return;
        }

        var wasConnected = _connected;
        _connected = message.successful === true;
        if (!wasConnected && _connected) {
            _connectionEstablished();
        }
        else if (wasConnected && !_connected) {
            _connectionBroken();
        }
    }

    function _metaHandshake(message) {
        /* We may get 'early' failures (not authentication ones) from CometD.
         * Ultimately, an unsuccessful message with an advice to not reconnect
         * means handshake failed.
         */
        if (message.successful === true) {
            cometd.batch(function() {
                cometd.subscribe('/hello', function(message) {
                    $('#body').append('<div>Server Says: ' + message.data.greeting + '</div>');
                });
                /* Publish on a service channel since the message is for the server only */
                cometd.publish('/service/hello', { name: 'World' });
            });
        }
        else if (message.advice && (message.advice.reconnect === "none")) {
        	_authenticationFailed();
        }
    }

    /* Disconnect when the page unloads */
    $(window).unload(function() {
        cometd.disconnect(true);
    });

    var cometURL = location.protocol + "//" + location.host + config.contextPath + "/cometd";
    cometd.configure({
        url: cometURL,
        logLevel: 'debug'
    });

    cometd.addListener('/meta/handshake', _metaHandshake);
    cometd.addListener('/meta/connect', _metaConnect);

    cometd.handshake({
        ext: {
            authentication: {
                sessionId: config.sessionId
            }
        }
    });
});
