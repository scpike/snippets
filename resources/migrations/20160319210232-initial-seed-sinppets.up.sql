INSERT INTO snippets (name, slug, code) VALUES
 ('echo', 'echo', '(function(x) { return x })'),
 ('sort', 'sort', '(function(x) {
  return x.split("\n").sort().join("\n");
})'),
 ('catsqlq', 'catsqlq', '(function(x) {
  if (x.length > 0) {
    return "(''" + x.split("\n").join("'',''") + "'')"
  }
})'),
 ('uniqify', 'uniqify', '(function(x) {
  var set = new Set();
  var res = [];
  x.split("\n").forEach(function(s) {
    if (!set.has(s)) {
      set.add(s);
      res.push(s);
    }
  })
  return res.sort().join("\n");
})');
