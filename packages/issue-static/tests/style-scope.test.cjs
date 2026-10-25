const { test } = require('node:test');
const assert = require('node:assert/strict');
const postcss = require('postcss');
const scope = require('../postcss-scope.cjs');

async function compile(css) {
  return (await postcss([scope()]).process(css, { from: undefined })).css;
}

test('preflight and third-party styles never target the theme body', async () => {
  const result = postcss.parse(await compile('html, :host { line-height: 1.5 } body { margin: 0 } *, ::before, ::after { box-sizing: border-box } .CodeMirror, .markdown-body h1 { color: red }'));
  result.walkRules(rule => {
    rule.selectors.forEach(selector => assert.ok(selector.includes(':where(.piw-app)'), selector));
  });
  assert.ok(!result.toString().includes('body {'));
});

test('responsive, dark and comma-containing pseudo selectors stay valid', async () => {
  const css = await compile('@media (min-width: 1024px) { .lg\\:piw-grid:is(.dark *) { display: grid } .prose :is(h1, h2):not(.ignore) { color: red } }');
  assert.match(css, /@media \(min-width: 1024px\)/);
  assert.match(css, /:is\(h1, h2\):not\(\.ignore\)/);
  assert.match(css, /:where\(\.piw-app\) \.lg/);
});

test('explicit container and theme dark selectors are not double-prefixed', async () => {
  const css = '.piw-app { color: red } .dark .piw-app { color: white }';
  assert.equal(await compile(css), css);
});

test('animation names are isolated without altering keyframe steps', async () => {
  const css = await compile('@keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } } .menu { animation: fadeIn 1s ease, spin 2s linear; animation-name: fadeIn }');
  assert.match(css, /@keyframes piw-fadeIn/);
  assert.match(css, /from \{ opacity: 0 \}/);
  assert.match(css, /animation: piw-fadeIn 1s ease, spin 2s linear/);
  assert.match(css, /animation-name: piw-fadeIn/);
});

test('Tailwind-prefixed animation names remain unchanged', async () => {
  const css = '@keyframes piw-bounce { to { opacity: 1 } } .piw-app .menu { animation: piw-bounce 1s }';
  assert.equal(await compile(css), css);
});
