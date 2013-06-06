Ext.cometd.CometD = Class.extend({

    init: function(cfg) {
        this.cometd = new $.Cometd();
        this.label = cfg.label;
        this.connected = false;
        this.disconnecting = false;
    },

    get: function() {
        return this.cometd;
    },

    connectionEstablished: function() {
        growlMessage('info', this.label, 'Connection established', false);
    },

    connectionBroken: function() {
        if (!this.disconnecting) {
            growlMessage('warn', this.label, 'Connection broken', false);
        }
    },

    connectionClosed: function() {
        if (!this.disconnecting) {
            growlMessage('info', this.label, 'Connection closed', false);
        }
    },

    authenticationSucceeded: function() {
        growlMessage('info', this.label, 'Authentication succeeded', false);
    },

    authenticationFailed: function() {
        growlMessage('error', this.label, 'Authentication failed', true);
    },

    metaConnect: function(message) {
        if (this.cometd.isDisconnected()) {
            this.connected = false;
            this.connectionClosed();
            return;
        }

        var wasConnected = this.connected;
        this.connected = message.successful === true;
        if (!wasConnected && this.connected) {
            this.connectionEstablished();
        }
        else if (wasConnected && !this.connected) {
            this.connectionBroken();
        }
    },

    metaHandshake: function(message) {
        /* We may get 'early' failures (not authentication ones) from CometD.
         * Ultimately, an unsuccessful message with an advice to not reconnect
         * means handshake failed.
         */
        if (message.successful === true) {
            this.authenticationSucceeded();
        }
        else if (message.advice && (message.advice.reconnect === "none")) {
            this.authenticationFailed();
        }
    },

    connect: function(protocol, host, contextPath, servletPath, auth) {
        Ext.event.bind('preLeave', true, this.disconnect, this);

        var url = protocol + '//' + host + contextPath + '/' + servletPath;
        this.cometd.configure({
            url: url,
            logLevel: 'debug'
        });

        this.cometd.addListener('/meta/handshake', this, this.metaHandshake);
        this.cometd.addListener('/meta/connect', this, this.metaConnect);

        var params = (auth === undefined) ? undefined : {
            ext: {
                authentication: auth
            }
        };
        this.cometd.handshake(params);
    },

    disconnect: function() {
        this.disconnecting = true;
        this.cometd.disconnect(true);
    }

});
