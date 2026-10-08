/* 原型静态一致性校验：JS 语法 / data-act 与 ACTIONS 对齐 / data-view 与视图对齐 / $('#id') 引用存在性 */
const fs = require('fs');
const path = require('path');
const file = process.argv[2];
const src = fs.readFileSync(file, 'utf8');

const m = src.match(/<script>([\s\S]*?)<\/script>/);
if (!m) { console.log('FAIL: 未找到 <script> 块'); process.exit(1); }
const js = m[1];
const tmp = path.join(require('os').tmpdir(), 'campus-check.js');
fs.writeFileSync(tmp, js, 'utf8');

const { execFileSync } = require('child_process');
try {
  execFileSync(process.execPath, ['--check', tmp], { stdio: 'inherit' });
  console.log('OK  JS 语法检查通过 (' + js.split('\n').length + ' 行脚本)');
} catch (e) { console.log('FAIL JS 语法错误'); process.exit(1); }

// 1) data-act 覆盖
const acts = new Set([...src.matchAll(/data-act="([^"]+)"/g)].map(x => x[1]));
const block = js.slice(js.indexOf('const ACTIONS = {'), js.indexOf('/* ---------- 26'));
const keys = new Set([...block.matchAll(/\n\s{2}'?([A-Za-z][\w-]*)'?\s*\(/g)].map(x => x[1]));
const missing = [...acts].filter(a => !keys.has(a));
const unused = [...keys].filter(k => !acts.has(k));
console.log('data-act 动作数: ' + acts.size + ' / ACTIONS 处理函数: ' + keys.size);
console.log(missing.length ? 'FAIL 缺少处理函数: ' + missing.join(', ') : 'OK  所有 data-act 均有处理函数');
if (unused.length) console.log('WARN 未被引用的处理函数: ' + unused.join(', '));

// 2) data-view 覆盖
const views = new Set([...src.matchAll(/data-view="([^"]+)"/g)].map(x => x[1]).filter(v => !/[+']/.test(v)));
const built = new Set([...js.matchAll(/const views = \[([^\]]+)\]/g)].flatMap(x => x[1].split(',').map(s => s.trim().replace(/'/g, ''))));
const badView = [...views].filter(v => !built.has(v));
console.log('data-view 目标: ' + [...views].join(', '));
console.log(badView.length ? 'FAIL 存在未注册视图: ' + badView.join(', ') : 'OK  所有 data-view 均已注册 (<section id="view-*">)');

// 3) $('#id') 引用存在性（动态构建的 view-* 由 views 数组生成）
const ids = new Set([...src.matchAll(/id="([^"]+)"/g)].map(x => x[1]));
[...built].forEach(v => ids.add('view-' + v));
const refs = new Set([...js.matchAll(/\$\('#([\w-]+)'\)/g)].map(x => x[1]));
const missingIds = [...refs].filter(r => !ids.has(r));
console.log('id="…" 定义: ' + ids.size + ' / $(#id) 引用: ' + refs.size);
console.log(missingIds.length ? 'FAIL 引用了不存在的 id: ' + missingIds.join(', ') : 'OK  所有 $(#id) 引用均有对应元素');

// 4) 视图渲染函数齐备
const fns = js.match(/const fns = \{([\s\S]*?)\};/);
const fnames = fns ? [...fns[1].matchAll(/(\w+):render/g)].map(x => 'render' + x[1][0].toUpperCase() + x[1].slice(1)) : [];
const missingFn = [...built].filter(v => {
  const fn = 'render' + v[0].toUpperCase() + v.slice(1);
  return !new RegExp('function ' + fn + '\\s*\\(').test(js);
});
console.log(missingFn.length ? 'FAIL 缺少渲染函数: ' + missingFn.join(', ') : 'OK  每个视图都有 render 函数');

// 5) 结构体检
const tagCount = t => (src.match(new RegExp('<' + t + '[\\s>]', 'g')) || []).length;
const closeCount = t => (src.match(new RegExp('</' + t + '>', 'g')) || []).length;
['div','section','table','aside','header','main'].forEach(t => {
  const a = tagCount(t), b = closeCount(t);
  console.log((a === b ? 'OK  ' : 'WARN') + ' 标签配对 <' + t + '>: ' + a + ' / ' + b);
});
console.log('文件大小: ' + (src.length / 1024).toFixed(1) + ' KB, 总行数: ' + src.split('\n').length);
