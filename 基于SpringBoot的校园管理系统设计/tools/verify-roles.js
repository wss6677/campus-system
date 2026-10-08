/* 验证三个角色各自的菜单与登录入口 */
const fs = require('fs');
const { JSDOM, VirtualConsole } = require('jsdom');

const html = fs.readFileSync(process.argv[2], 'utf8');
const vc = new VirtualConsole();
const errs = [];
vc.on('error', (...a) => errs.push('console.error: ' + a.join(' ')));
vc.on('jsdomError', e => { if (!/Not implemented/.test(e.message)) errs.push(e.message.split('\n')[0]); });

const dom = new JSDOM(html, { runScripts: 'dangerously', pretendToBeVisual: true, virtualConsole: vc, url: 'http://localhost/' });
const { window } = dom;
const doc = window.document;
const q = s => doc.querySelector(s);
const qa = s => Array.from(doc.querySelectorAll(s));
const click = el => el && el.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));

console.log('=== 登录页可选身份 ===');
const cards = qa('#role-grid .role-card');
cards.forEach(c => console.log('  · ' + c.textContent.replace(/\s+/g, ' ').trim()));

console.log('\n=== 各角色登录后可见菜单 ===');
for (const code of ['SUPER_ADMIN', 'EDIT_ADMIN', 'TEACHER', 'STUDENT']) {
  const card = qa('#role-grid .role-card').find(el => el.dataset.code === code);
  if (!card) { console.log(`  ${code}: ❌ 登录页没有该身份卡`); continue; }
  window.eval('ACTIONS.logout()');
  click(card);
  click(q('[data-act="login"]'));
  const role = window.eval('state.user.roleCode');
  const name = window.eval('state.user.roleName');
  const items = qa('#nav .nav-item').map(el => el.querySelector('.tx').textContent);
  console.log(`\n  【${name}】(${role})  共 ${items.length} 个菜单`);
  console.log('    ' + items.join(' / '));
}

console.log('\n=== 运行期错误 ===');
console.log(errs.length ? errs.join('\n') : '  无');
