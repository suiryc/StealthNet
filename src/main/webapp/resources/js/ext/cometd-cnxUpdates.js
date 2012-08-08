/* self-invoking function with local scope variables */
(function() {

var cometd = new Ext.cometd.CometD({label: 'Connection updates service'});


function addCnxRow(data) {
    var table = $('#connections');
    var idx = table.find('tr').length;

    $('<tr/>').addClass('ui-widget-content').addClass(idx % 2 ? 'ui-datatable-odd' : 'ui-datatable-even').attr({id: data.id, role: 'row'}).append(
        $('<td/>').attr({role: 'gridcell'}).append(
            $('<div/>').addClass('ui-dt-c').html(data.host + ':' + data.port)
        )
    ).append(
        $('<td/>').attr({role: 'gridcell'}).append(
            $('<div/>').addClass('ui-dt-c').html(data.created)
        )
    ).append(
        $('<td/>').attr({role: 'gridcell'}).append(
            $('<div/>').addClass('ui-dt-c').attr({data: 'receivedCommands'}).html(data.receivedCommands)
        )
    ).append(
        $('<td/>').attr({role: 'gridcell'}).append(
            $('<div/>').addClass('ui-dt-c').attr({data: 'sentCommands'}).html(data.sentCommands)
        )
    ).append(
        $('<td/>').attr({role: 'gridcell'}).append(
            $('<div/>').addClass('ui-dt-c').attr({data: 'misc'}).html(data.misc)
        )
    ).appendTo(table);
}

function refreshCnxRow(data) {
    var table = $('#connections');
    var row = table.find('#' + data.id);

    $(['receivedCommands', 'sentCommands', 'misc']).each(function(i, field) {
        row.find('div[data="' + field + '"]').html(data[field]);
    });
}

function closeCnxRow(data) {
    $('#connections').find('#' + data.id).find('div[data="misc"]').html('closed');
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
