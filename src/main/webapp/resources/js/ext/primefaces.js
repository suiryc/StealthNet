/**
 * Extended PrimeFaces Growl Widget.
 *
 * Handles per-message properties (sticky, life).
 *
 * @note Most of the original code was preserved.
 */
Ext.widget.Growl = PrimeFaces.widget.Growl.extend({

    init: function(cfg) {
        this.cfg = cfg;
        this.id = this.cfg.id;
        this.jqId = PrimeFaces.escapeClientId(this.id);

        this.render();

        $(this.jqId + '_s').remove();
    },

    renderMessage: function(msg) {
        var markup = '<div class="ui-growl-item-container ui-state-highlight ui-corner-all ui-helper-hidden ui-shadow">';
        markup += '<div class="ui-growl-item">';
        markup += '<div class="ui-growl-icon-close ui-icon ui-icon-closethick" style="display:none"></div>';
        markup += '<span class="ui-growl-image ui-growl-image-' + msg.severity + '" />';
        markup += '<div class="ui-growl-message">';
        markup += '<span class="ui-growl-title">' + msg.summary + '</span>';
        markup += '<p>' + msg.detail + '</p>';
        markup += '</div><div style="clear: both;"></div></div></div>';

        var message = $(markup);
        message.msg = msg;

        this.bindEvents(message);

        message.appendTo(this.jq).fadeIn();
    },

    bindEvents: function(message) {
        var _self = this,
        sticky = this.cfg.sticky;

        if ('sticky' in message.msg) {
            sticky = message.msg.sticky;
        }

        message.mouseover(function() {
            var msg = $(this);

            if (!msg.is(':animated')) {
                msg.find('div.ui-growl-icon-close:first').show();
            }
        })
        .mouseout(function() {        
            $(this).find('div.ui-growl-icon-close:first').hide();
        });

        message.find('div.ui-growl-icon-close').click(function() {
            _self.removeMessage(message);

            if (!sticky) {
                clearTimeout(message.data('timeout'));
            }
        });

        if (!sticky) {
            this.setRemovalTimeout(message);
        }
    },

    setRemovalTimeout: function(message) {
        var _self = this,
        life = this.cfg.life;

        if ('life' in message.msg) {
            life = message.msg.life;
        }

        var timeout = setTimeout(function() {
            _self.removeMessage(message);
        }, life);

        message.data('timeout', timeout);
    }
});
