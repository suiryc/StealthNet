var cometd = new Ext.cometd.CometD({label: 'Hello service'});

$(function() {
    cometd.authenticationSucceeded = function() {
        cometd.get().batch(cometd, function() {
            cometd.get().subscribe('/hello', function(message) {
                $('#body').append('<div>Server Says: ' + message.data.greeting + '</div>');
            });
            /* Publish on a service channel since the message is for the server only */
            cometd.get().publish('/service/hello', { name: 'World' });
        });
    }

    cometd.connect(location.protocol, location.host, config.contextPath, "cometd", {
        sessionId: config.sessionId
    });
})
