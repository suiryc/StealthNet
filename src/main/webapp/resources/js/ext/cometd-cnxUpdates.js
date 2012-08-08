/* self-invoking function with local scope variables */
(function() {

var serviceName = 'Connection updates';
var cometd = new Ext.cometd.CometD({label: serviceName + ' service'});


function addCnxRow(data) {
    var table = $('#connections');
    var idx = table.find('tr').length;
    var peer = data.host + ':' + data.port;

    growlMessage('info', serviceName, 'Connection opened with peer ' + peer, false, 3000);

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

    growlMessage('warn', serviceName, 'Connection closed with peer ' + row.find('div[data="peer"]').html(), false, 10000);

    row.find('div[data="status"]').html('closed');
    row.fadeOut(5000, function() {
        row.remove();
        table.find('tr').each(function(i, tr) {
            if (!i) return;
            $(tr).removeClass('ui-datatable-even ui-datatable-odd').addClass(i % 2 ? 'ui-datatable-odd' : 'ui-datatable-even');
        });
    });
}

$(function() {
    cometd.authenticationSucceeded = function() {
        cometd.get().batch(cometd, function() {
            cometd.get().subscribe('/cnxUpdates', function(message) {
                if (message.data.event == 'new') {
                    addCnxRow(message.data);
                }
                else if (message.data.event == 'refresh') {
                    refreshCnxRow(message.data);
                }
                else if (message.data.event == 'closed') {
                    closeCnxRow(message.data);
                }
                else {
                    var content = '[';
                    for (var field in message.data) {
                        if (!message.data.hasOwnProperty(field)) {
                            continue;
                        }
                        if (content.length > 1) {
                            content += '; ';
                        }
                        content += field + '=' + message.data[field];
                    }
                    content += ']';
                    $('#body').append('<div>Server Says: ' + content + '</div>');
                }
            });
            /* Publish on a service channel since the message is for the server only */
            cometd.get().publish('/service/cnxUpdater', { active: 'true' });
        });
    }

    cometd.connect(location.protocol, location.host, config.contextPath, 'cometd', {
        sessionId: config.sessionId
    });

    Ext.event.bind('preLeave', function() {
        cometd.disconnect();
    });
})

})();
