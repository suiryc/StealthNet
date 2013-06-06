/* self-invoking function with local scope variables */
(function() {

var cometd = new Ext.cometd.CometD({label: getServiceName() + ' service'});


function getServiceName(channel) {
    if (channel == 'connections') {
        return 'Connections notifications';
    }

    return 'Notifications';
}

function addCnxRow(data) {
    var table = $('#connections');
    var idx = table.find('tr').length;
    var peer = data.host + ':' + data.port;

    growlMessage('info', getServiceName('connections'), 'Connection opened with peer ' + peer, false, 3000);

    data.peer = peer;
    var row = $('<tr/>').addClass('ui-widget-content').addClass(idx % 2 ? 'ui-datatable-odd' : 'ui-datatable-even').attr({id: data.id, role: 'row'})
    $(['peer', 'created', 'receivedCommands', 'sentCommands', 'status']).each(function(i, field) {
        row.append(
            $('<td/>').attr({role: 'gridcell'}).append(
                $('<div/>').addClass('ui-dt-c').attr({data: field}).html(data[field])
            )
        );
    });
    row.appendTo(table);
}

function refreshCnxRow(data) {
    var table = $('#connections');
    var row = table.find('#' + data.id);

    $(['receivedCommands', 'sentCommands', 'status']).each(function(i, field) {
        row.find('div[data="' + field + '"]').html(data[field]);
    });
}

function closeCnxRow(data) {
    var table = $('#connections');
    var row = table.find('#' + data.id);

    growlMessage('warn', getServiceName('connections'), 'Connection closed with peer ' + row.find('div[data="peer"]').html(), false, 10000);

    row.find('div[data="status"]').html('closed');
    row.fadeOut(5000, function() {
        row.remove();
        table.find('tr').each(function(i, tr) {
            if (!i) return;
            $(tr).removeClass('ui-datatable-even ui-datatable-odd').addClass(i % 2 ? 'ui-datatable-odd' : 'ui-datatable-even');
        });
    });
}

function handleNotification(msgData) {
    var channel = msgData.channel;

    if (channel == 'global') {
        handleGlobalNotification(msgData.data);
    }
    else if (channel == 'connections') {
        handleConnectionsNotification(msgData.data);
    }
    else {
        var content = Ext.functions.stringify(message.data);
        growlMessage('warn', getServiceName(channel), 'Unhandled message channel: ' + content, false, 10000);
    }
}

function handleGlobalNotification(data) {
    var level = data.level;
    var message = data.message;

    if ((level === undefined) || (message === undefined)) {
        var content = Ext.functions.stringify(data);
        growlMessage('warn', getServiceName('global'), 'Unhandled global message: ' + content, false, 10000);

        return;
    }

    growlMessage(level, getServiceName('global'), message, false, 10000);
}

function handleConnectionsNotification(data) {
    if (data.event == 'new') {
        addCnxRow(data);
    }
    else if (data.event == 'refresh') {
        refreshCnxRow(data);
    }
    else if (data.event == 'closed') {
        closeCnxRow(data);
    }
    else {
        var content = Ext.functions.stringify(data);
        growlMessage('warn', getServiceName('connections'), 'Unhandled connections notification: ' + content, false, 10000);
    }
}

$(function() {
    cometd.authenticationSucceeded = function() {
        cometd.get().batch(cometd, function() {
            cometd.get().subscribe('/notifications', function(message) {
                handleNotification(message.data);
            });

            /* Publish on a service channel since the message is for the server only */
            cometd.get().publish('/service/notifications', {
                channel: 'connections',
                active: 'true'
              }
            );
            /* Even if connection is closed upon leaving, explicitly unregister
             * ourself from notifications. */
            Ext.event.bind('preLeave', true, function() {
                cometd.get().publish('/service/notifications', {
                    channel: 'connections',
                    active: 'false'
                  }
                );
            });
        });
    }

    cometd.connect(location.protocol, location.host, config.contextPath, 'cometd', {
        sessionId: config.sessionId
    });
})

})();
