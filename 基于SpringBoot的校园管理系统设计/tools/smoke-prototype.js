/* 原型无头冒烟测试：真实执行登录 → 遍历全部导航 → 走审批/催办/评论/编辑器主链路，捕获 runtime 错误 */
const fs = require('fs');
const { JSDOM, VirtualConsole } = require('jsdom');

const html = fs.readFileSync(process.argv[2], 'utf8');
const problems = [];
const notImpl = [];
const vc = new VirtualConsole();
vc.on('jsdomError', e => {
  if (/Not implemented/.test(e.message)) notImpl.push(e.message.split('\n')[0]);
  else problems.push('jsdomError: ' + e.message.split('\n')[0]);
});
vc.on('error', (...a) => problems.push('console.error: ' + a.join(' ')));

const dom = new JSDOM(html, { runScripts: 'dangerously', pretendToBeVisual: true, virtualConsole: vc, url: 'http://localhost/' });
const { window } = dom;
const doc = window.document;
window.addEventListener('error', e => problems.push('window.onerror: ' + e.message));

const ev = js => window.eval(js);
const click = el => { if (!el) { problems.push('点击目标不存在'); return false; } el.dispatchEvent(new window.MouseEvent('click', { bubbles: true })); return true; };
const q = s => doc.querySelector(s);
const qa = s => Array.from(doc.querySelectorAll(s));
const toastText = () => (q('#toasts') ? q('#toasts').textContent : '');
const step = (name, fn) => {
  const before = problems.length;
  try { fn(); } catch (e) { problems.push('[' + name + '] 抛出异常: ' + e.message); }
  const ok = problems.length === before;
  console.log((ok ? '  OK  ' : '  FAIL') + ' ' + name);
};

console.log('—— 启动阶段 ——');
step('脚本启动无异常', () => {
  if (!q('#role-grid').children.length) problems.push('角色卡未渲染');
  if (qa('#nav .nav-item').length !== 16) problems.push('导航项数量异常: ' + qa('#nav .nav-item').length);
});

console.log('—— 登录 ——');
step('以系统管理员登录', () => {
  click(q('[data-act="login"]'));
  if (ev('state.user.roleCode') !== 'SUPER_ADMIN') problems.push('登录后角色不正确');
  if (q('#app').style.display !== 'block') problems.push('主界面未显示');
  if (!q('#view-dashboard').innerHTML.length) problems.push('工作台未渲染');
  if (!/累计公告/.test(q('#view-dashboard').textContent)) problems.push('工作台卡片缺失');
});

console.log('—— 遍历全部视图 ——');
const navViews = qa('#nav .nav-item').map(el => el.dataset.view);
let totalHtml = 0;
navViews.forEach(v => {
  step('导航 → ' + v, () => {
    click(qa('#nav .nav-item').find(el => el.dataset.view === v));
    const sec = q('#view-' + v);
    if (!sec) { problems.push('视图节点不存在'); return; }
    if (sec.innerHTML.length < 200) problems.push('视图内容过少(' + sec.innerHTML.length + ' 字符)');
    if (!sec.classList.contains('active')) problems.push('视图未激活');
    totalHtml += sec.innerHTML.length;
  });
});
console.log('  视图渲染总字符数: ' + totalHtml.toLocaleString());

console.log('—— 公告详情 / 审批链路 ——');
step('打开公告详情抽屉', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'dashboard'));
  click(q('#view-dashboard [data-act="open-detail"]'));
  if (!q('#drawer').classList.contains('open')) problems.push('抽屉未打开');
  if (!/加利顿大学文件/.test(q('#drawer').textContent)) problems.push('红头文件未渲染');
  if (!/审批流转记录/.test(q('#drawer').textContent)) problems.push('审批时间线缺失');
  click(q('#drawer [data-act="close-layer"]'));
});
step('一键催办', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'receipt'));
  click(q('#view-receipt [data-act="urge"]'));
  if (!/催办已发送/.test(toastText())) problems.push('未出现催办提示');
});
step('审核通过（含意见必填校验）', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'approval'));
  const pendingBefore = ev('ANNS.filter(function(a){return a.status==="PENDING"}).length');
  const targetId = ev('ANNS.filter(function(a){return a.status==="PENDING"})[0].id');
  click(qa('#view-approval [data-act="open-audit"]').find(el => el.dataset.mode === 'pass'));
  if (!q('#modal').classList.contains('open')) problems.push('审核弹窗未打开');
  click(q('#modal [data-act="do-audit"]'));            // 未填意见 → 应被拦截
  if (!/请填写审核意见/.test(toastText())) problems.push('空意见未被拦截');
  const rm = q('#modal #audit-remark');
  rm.value = '内容无误，同意发布';
  rm.dispatchEvent(new window.Event('input', { bubbles: true }));
  click(q('#modal [data-act="do-audit"]'));
  const pendingAfter = ev('ANNS.filter(function(a){return a.status==="PENDING"}).length');
  const st = ev('ANNS.find(function(a){return a.id===' + targetId + '}).status');
  if (pendingAfter !== pendingBefore - 1) problems.push('审核通过未生效: ' + pendingBefore + ' → ' + pendingAfter);
  if (st !== 'PUBLISHED') problems.push('审核后状态应为 PUBLISHED，实际 ' + st);
  if (q('#modal').classList.contains('open')) problems.push('审核后弹窗未关闭');
});
step('驳回必须填写 ≥5 字意见', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'approval'));
  const targetId = ev('ANNS.filter(function(a){return a.status==="PENDING"})[0].id');
  click(qa('#view-approval [data-act="open-audit"]').find(el => el.dataset.mode === 'reject'));
  const rm = q('#modal #audit-remark');
  rm.value = '不行';
  rm.dispatchEvent(new window.Event('input', { bubbles: true }));
  click(q('#modal [data-act="do-audit"]'));
  if (!/驳回意见太短/.test(toastText())) problems.push('过短驳回意见未被拦截');
  rm.value = '附件缺失，请补充生源信息表后重新提交';
  rm.dispatchEvent(new window.Event('input', { bubbles: true }));
  click(q('#modal [data-act="do-audit"]'));
  const st = ev('ANNS.find(function(a){return a.id===' + targetId + '}).status');
  if (st !== 'REJECTED') problems.push('驳回后状态应为 REJECTED，实际 ' + st);
  if (ev('ANNS.find(function(a){return a.id===' + targetId + '}).auditRemark') !== '附件缺失，请补充生源信息表后重新提交') problems.push('驳回意见未写入公告');
});
step('评论审核 / 官方回复', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'comment'));
  click(q('#view-comment [data-act="c-reply"]'));
  q('#modal #reply-text').value = '同学你好，表格已上传到附件，请重新下载。';
  click(q('#modal [data-act="do-reply"]'));
  if (!/回复已发送/.test(toastText())) problems.push('官方回复未生效');
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'comment'));
  click(q('#view-comment [data-act="c-approve"]'));
  if (!/评论已通过/.test(toastText())) problems.push('评论通过未生效');
});
step('组织树 / 用户停启用', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'dept'));
  click(q('#view-dept .node'));
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'users'));
  click(q('#view-users [data-act="toggle-user"]'));
  if (!/已停用|已启用/.test(toastText())) problems.push('用户停启用未生效');
});
step('内容安全在线检测', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'sensitive'));
  const ta = q('#sw-test');
  ta.value = '本课程包过，绝对安全，请放心报名';
  ta.dispatchEvent(new window.Event('input', { bubbles: true }));
  const box = q('#sw-result');
  if (!box.classList.contains('bad')) problems.push('阻断级敏感词未触发红色提示');
  if (!/包过/.test(box.textContent)) problems.push('未命中敏感词「包过」');
});
step('切换深色模式', () => {
  click(q('#app [data-act="theme"]'));
  if (doc.documentElement.dataset.theme !== 'dark') problems.push('主题未切换');
  click(q('[data-act="theme"]'));
});
step('消息中心全部已读', () => {
  click(q('#app [data-act="pop-notice"]'));
  click(q('#pop-notice [data-act="notice-all-read"]'));
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'notice'));
  if (!/全部已读|已读/.test(q('#view-notice').textContent)) problems.push('消息已读状态未更新');
});

console.log('—— 编辑器主链路 ——');
step('新建公告：改为「活动预告」免审分类', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'editor'));
  const sel = q('#view-editor [data-bind="form.categoryId"]');
  sel.value = '4';
  sel.dispatchEvent(new window.Event('input', { bubbles: true }));
  if (!/免审/.test(toastText())) problems.push('切换分类未提示免审策略');
});
step('空表单提交被拦截', () => {
  click(q('#view-editor [data-act="submit-form"]'));
  if (!/提交失败/.test(toastText())) problems.push('空表单未被拦截');
});
step('填写完整内容后提交（免审直发）', () => {
  const before = ev('ANNS.length');
  const t = q('#view-editor [data-bind="form.title"]');
  t.value = '关于举办第十七届校园程序设计大赛的通知';
  t.dispatchEvent(new window.Event('input', { bubbles: true }));
  const s = q('#view-editor [data-bind="form.summary"]');
  s.value = '比赛定于 12 月 28 日举行，即日起接受报名，欢迎各学院组队参加。';
  s.dispatchEvent(new window.Event('input', { bubbles: true }));
  ev('state.form.content = "本次大赛由信息中心与计算机学院联合承办，面向全校本科生与研究生开放报名。\\n比赛采用 ACM 赛制，三人一队，限时五小时，共设八道题目。\\n报名截止时间为 12 月 24 日 17:00，请各学院统一报送参赛名单。\\n比赛地点为南苑计算机楼 A301 机房，请参赛选手携带校园卡入场。\\n获奖队伍将获得创新学分与奖金，优秀选手推荐参加省级赛事。"');
  click(q('#view-editor [data-act="submit-form"]'));
  const after = ev('ANNS.length');
  if (after !== before + 1) problems.push('提交后公告未入库: ' + before + ' → ' + after);
  if (!/已发布|已提交审核/.test(toastText())) problems.push('提交无成功提示');
  const st = ev('ANNS[0].status');
  if (st !== 'PUBLISHED') problems.push('免审分类未直接发布，状态=' + st);
  if (ev('ANNS[0].targetCount') !== 18420) problems.push('未生成回执目标数');
});
step('保存草稿', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'editor'));
  const t = q('#view-editor [data-bind="form.title"]');
  t.value = '关于寒假期间实验室封闭管理的通知（草稿）';
  t.dispatchEvent(new window.Event('input', { bubbles: true }));
  const before = ev('ANNS.length');
  click(q('#view-editor [data-act="save-draft"]'));
  if (ev('ANNS.length') !== before + 1) problems.push('草稿未入库');
  if (ev('ANNS[0].status') !== 'DRAFT') problems.push('草稿状态不正确');
});
step('红头文件预览', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'editor'));
  click(q('#view-editor [data-act="preview"]'));
  if (!/加利顿大学文件/.test(q('#drawer').textContent)) problems.push('预览未渲染红头');
  click(q('#drawer [data-act="close-layer"]'));
});

console.log('—— 列表筛选 / 分页 ——');
step('关键词 + 状态筛选', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'announcements'));
  const kw = q('#view-announcements [data-bind="filter.keyword"]');
  kw.value = '安全';
  kw.dispatchEvent(new window.Event('input', { bubbles: true }));
  const rows = qa('#list-wrap tbody tr').length;
  if (rows < 1) problems.push('关键词筛选无结果');
  const sel = q('#view-announcements [data-bind="filter.status"]');
  sel.value = 'PUBLISHED';
  sel.dispatchEvent(new window.Event('input', { bubbles: true }));
  if (!/共 \d+ 条/.test(q('#list-wrap').textContent)) problems.push('分页信息缺失');
  click(q('#view-announcements [data-act="reset-filter"]'));
});
step('全局搜索联动列表', () => {
  click(qa('#nav .nav-item').find(el => el.dataset.view === 'dashboard'));
  const g = q('#global-search');
  g.value = '招标';
  g.dispatchEvent(new window.Event('input', { bubbles: true }));
  if (ev('state.view') !== 'announcements') problems.push('全局搜索未跳转公告列表');
  if (qa('#list-wrap tbody tr').length < 1) problems.push('全局搜索无结果');
});
step('分页翻页', () => {
  click(q('#view-announcements [data-act="reset-filter"]'));
  const next = qa('#list-wrap [data-act="page"]').pop();
  click(next);
  if (ev('state.page') < 1) problems.push('分页状态异常');
});

console.log('—— 其他角色视角 ——');
['EDIT_ADMIN', 'TEACHER', 'STUDENT'].forEach(code => {
  step('切换到 ' + code + ' 并浏览工作台/公告/回执', () => {
    ev('ACTIONS.logout()');
    click(qa('#role-grid .role-card').find(el => el.dataset.code === code));
    click(q('[data-act="login"]'));
    if (ev('state.user.roleCode') !== code) problems.push('角色未切换成功');
    click(qa('#nav .nav-item').find(el => el.dataset.view === 'dashboard'));
    click(qa('#nav .nav-item').find(el => el.dataset.view === 'announcements'));
    const receiptNav = qa('#nav .nav-item').find(el => el.dataset.view === 'receipt');
    if (receiptNav) {                       // 普通师生用户看不到回执统计菜单
      click(receiptNav);
      if (ev('state.view') !== 'receipt') problems.push('角色视角导航失败');
    }
  });
});

console.log('\n================ 测试结论 ================');
console.log('未被处理的点击动作（"原型未实现"）: ' + ((toastText().match(/原型未实现/g) || []).length));
console.log('环境未实现项（canvas 等，浏览器中正常）: ' + new Set(notImpl).size + ' 类');
[...new Set(notImpl)].forEach(x => console.log('   · ' + x));
if (problems.length) {
  console.log('\n发现 ' + problems.length + ' 个问题:');
  [...new Set(problems)].forEach(p => console.log('   ✗ ' + p));
  process.exit(1);
} else {
  console.log('\n✅ 全部 ' + '冒烟用例通过：脚本零异常，主链路（登录/17 视图/审批/催办/评论/编辑器/筛选/角色切换）均正常。');
}
