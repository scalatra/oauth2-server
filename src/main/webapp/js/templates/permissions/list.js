
jade.templates['templates/permissions/list.jade'] = function(locals) {
  var __;
  __ = jade.init();
  with (locals || {}) {;

  if (this.collection) {
    __.buf.push('<table');
    __.buf.push(__.attrs({
      'class': 'table' + ' ' + 'table-bordered' + ' ' + 'table-striped'
    }));
    __.buf.push('><caption>A list of available permissions</caption><thead><tr><th>Name</th><th>Description</th><th>System</th><th>&nbsp;</th></tr></thead><tbody>');
    this.collection.forEach(function(permission) {
      __.buf.push('<tr><td>');
      __.buf.push(__.escape(permission.name));
      __.buf.push('</td><td>');
      __.buf.push(__.escape(permission.code));
      __.buf.push('</td><td>');
      __.buf.push(__.escape(permission.isSystem));
      __.buf.push('</td><td><a');
      __.buf.push(__.attrs({
        'data-id': permission.code,
        'class': 'edit-link'
      }));
      __.buf.push('>Edit</a><a');
      __.buf.push(__.attrs({
        'data-id': permission.code,
        'class': 'delete-link'
      }));
      return __.buf.push('>Delete</a></td></tr>');
    });
    __.buf.push('</tbody></table>');
  } else {
    __.buf.push('<p>There are currently no clients registered</p>');
  }
  };

  return __.buf.join("");
};
