Ext = {
    widget: {},
    cometd: {}
};

widgets = {};

Ext.event = {

    bindings: {},

    bind: function(name, callback, scope) {
        if (this.bindings[name] === undefined) {
            this.bindings[name] = [];
        }
        this.bindings[name].push($.proxy(callback, scope));
    },

    trigger: function(name) {
        callbacks = this.bindings[name];
        if (callbacks === undefined) {
            return;
        }

        delete(this.bindings[name]);
        $(callbacks).each(function() { this() });
    }
};

Ext.functions = {
    stringify: function(data) {
        var content = '';

        if (typeof data == 'object') {
            content = '[';
            for (var field in data) {
                if (!data.hasOwnProperty(field)) {
                    continue;
                }
                if (content.length > 1) {
                    content += '; ';
                }
                content += field + '=' + this.stringify(data[field]);
            }
            content += ']';
        }
        else {
            content = data;
        }

        return content;
    },

    shutdown: function(args) {
        if ((args !== undefined) && (args.acknowledged === true)) {
            growlMessage('info', 'Server', 'Shutdown acknowledged', false, 10000);
            Ext.event.trigger('postShutdown');
        }
        else {
            /* Seems like the session expired: reloading the page will raise
             * the issue to the user. */
            window.location.reload();
        }
    }
};

$(function() {
    Ext.event.bind('preLogout', function() {
        Ext.event.trigger('preLeave');
    });

    Ext.event.bind('preShutdown', function() {
        growlMessage('info', 'Server', 'Shutdown requested', false, 10000);
        Ext.event.trigger('preLeave');
    });

    Ext.event.bind('postShutdown', function() {
        $('.ext-admin').each(function() {
            $(this).off().attr('onclick', 'return false;').addClass('ui-state-disabled');
        });
    });

    $(window).unload(function() {
        Ext.event.trigger('preLeave');
    });
});
