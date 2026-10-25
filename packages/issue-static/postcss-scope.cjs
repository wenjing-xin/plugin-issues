const selectorParser = require('postcss-selector-parser');

const scope = ':where(.piw-app)';

/** Scope preflight, Markdown, CodeMirror and utilities, including root resets. */
module.exports = () => ({
  postcssPlugin: 'plugin-issues-scope',
  OnceExit(root) {
    root.walkRules(rule => {
      for (let parent = rule.parent; parent; parent = parent.parent) {
        if (parent.type === 'atrule' && /keyframes$/i.test(parent.name)) return;
      }
      rule.selector = selectorParser(selectors => {
        selectors.each(selector => {
          let scoped = false;
          selector.walkClasses(node => { if (node.value === 'piw-app') scoped = true; });
          if (scoped) return;
          selector.walk(node => {
            if ((node.type === 'tag' && ['html', 'body'].includes(node.value)) ||
                (node.type === 'pseudo' && [':root', ':host'].includes(node.value))) {
              node.replaceWith(selectorParser().astSync(scope).first.first.clone());
              scoped = true;
            }
          });
          if (!scoped) {
            selector.prepend(selectorParser.combinator({ value: ' ' }));
            selector.prepend(selectorParser().astSync(scope).first.first.clone());
          }
        });
      }).processSync(rule.selector);
    });
    // Animation names are global even when the selectors are scoped.
    const names = new Map();
    root.walkAtRules(/keyframes$/i, rule => {
      const name = rule.params.trim();
      const scopedName = name.startsWith('piw-') ? name : `piw-${name}`;
      names.set(name, scopedName);
      rule.params = scopedName;
    });
    root.walkDecls(/animation(?:-name)?$/, declaration => {
      declaration.value = declaration.value.replace(/[\w-]+/g, token => names.get(token) || token);
    });
  },
});
module.exports.postcss = true;
