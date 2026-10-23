const assert = require('node:assert/strict');
const {valid, project, unproject} = require('./map-core.js');
assert.equal(valid(-3.11, 102.14), true);
assert.equal(valid(102.14, -3.11), false, 'BMKG latitude/longitude must not be reversed');
for (const point of [[null, null], [NaN, 100], [-91, 100], [0, Infinity]]) {
  assert.equal(valid(...point), false);
  assert.throws(() => project(...point, 5), RangeError);
}
for (const [lat, lon] of [[-3.11, 102.14], [-7.2575, 112.7521], [-9.3, 110.7], [0, 0]]) {
  for (const z of [3, 5, 9]) {
    const p = project(lat, lon, z), restored = unproject(p.x, p.y, z);
    assert.ok(Math.abs(restored.lat - lat) < 1e-8);
    assert.ok(Math.abs(restored.lon - lon) < 1e-8);
  }
}
assert.ok(project(-9, 110, 5).y > project(-7, 110, 5).y, 'South must be below north');
assert.ok(project(-7, 112, 5).x > project(-7, 110, 5).x, 'East must be right of west');
console.log('Map projection, coordinate order and invalid-point checks passed.');
