// XXX - use array/json to give parameters
function connectionRefresh(cnxId, receivedCommands, sentCommands, misc) {
  var node = $("table#connections_list tr#" + cnxId);
  $("td.receivedCommands", node).html(receivedCommands);
  $("td.sentCommands", node).html(sentCommands);
  $("td.misc", node).html(misc);
}
