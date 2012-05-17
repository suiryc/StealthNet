$(function() {
    widgets.growl = new Ext.widget.Growl({
        id: 'ext_widgets_growl',
        sticky: true,
        life: 6000,
        msgs: []
    });
});

function growlMessage(severity, summary, detail, sticky, life) {
    var msg = {
        severity: severity,
        summary: summary,
        detail: detail
    };

    if (sticky != undefined) {
        msg.sticky = sticky;
    }
    if (life != undefined) {
        msg.life = life;
    }

    widgets.growl.renderMessage(msg);
}
