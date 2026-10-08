/* 数据层一致性校验：实体字段 ↔ schema.sql 建表列 ↔ data.sql 插入列/值 数量 */
const fs = require('fs');
const path = require('path');
const ROOT = path.join(__dirname, '..', 'backend', 'src', 'main');

const snake = s => s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
const read = p => fs.readFileSync(p, 'utf8');
const problems = [];

/* ---------- 1. 实体 ---------- */
const entityDir = path.join(ROOT, 'java', 'com', 'ivy', 'campus', 'entity');
const entities = {};
for (const f of fs.readdirSync(entityDir).filter(f => f.endsWith('.java'))) {
  const src = read(path.join(entityDir, f));
  const cls = f.replace('.java', '');
  const tm = src.match(/@TableName\("([^"]+)"\)/);
  if (!tm) { problems.push(cls + ' 缺少 @TableName'); continue; }
  const lines = src.split('\n');
  const fields = [];
  lines.forEach((ln, i) => {
    const m = ln.match(/^\s*private\s+[\w<>,\[\]\s\.]+\s+(\w+)\s*;/);
    if (!m) return;
    let transient = false;
    for (let k = i - 1; k >= Math.max(0, i - 4); k--) {
      if (/@TableField\s*\(/.test(lines[k])) { transient = /exist\s*=\s*false/.test(lines[k]); break; }
      if (/@TableId|private |^\s*\/\/\s*$/.test(lines[k])) break;
    }
    if (!transient) fields.push(m[1]);
  });
  entities[cls] = {table: tm[1], fields};
}

/* ---------- 2. schema.sql ---------- */
const schema = read(path.join(ROOT, 'resources', 'db', 'schema.sql'));
const tables = {};
const tableRe = /create\s+table\s+(?:if\s+not\s+exists\s+)?`?(\w+)`?\s*\(([\s\S]*?)\n\)/gi;
let m;
while ((m = tableRe.exec(schema))) {
  const name = m[1], body = m[2];
  const cols = [];
  body.split('\n').forEach(ln => {
    const t = ln.trim();
    if (!t || /^(primary|unique|key|index|constraint|foreign)\b/i.test(t)) return;
    const c = t.match(/^`?(\w+)`?\s/);
    if (c) cols.push(c[1]);
  });
  tables[name] = cols;
}

/* ---------- 3. 比对 ---------- */
Object.entries(entities).forEach(([cls, e]) => {
  if (!tables[e.table]) { problems.push('实体 ' + cls + ' 的表 ' + e.table + ' 在 schema.sql 中不存在'); return; }
  const cols = tables[e.table];
  e.fields.forEach(f => {
    const c = snake(f);
    if (!cols.includes(c)) problems.push(cls + '.' + f + '（' + c + '） 在表 ' + e.table + ' 中缺少对应列');
  });
  cols.forEach(c => {
    const f = c.replace(/_(\w)/g, (_, x) => x.toUpperCase());
    if (!e.fields.includes(f) && !e.fields.includes(c)) problems.push('表 ' + e.table + ' 的列 ' + c + ' 在实体 ' + cls + ' 中没有对应字段');
  });
});

/* ---------- 4. data.sql ---------- */
const data = read(path.join(ROOT, 'resources', 'db', 'data.sql'));
const insRe = /insert\s+into\s+`?(\w+)`?\s*\(([^)]*)\)\s*values([\s\S]*?);/gi;
let n = 0, rows = 0;
let mm;
while ((mm = insRe.exec(data))) {
  const table = mm[1];
  const colList = mm[2].split(',').map(s => s.trim().replace(/`/g, '')).filter(Boolean);
  n++;
  if (!tables[table]) { problems.push('data.sql 向不存在的表 ' + table + ' 插入数据'); continue; }
  colList.forEach(c => { if (!tables[table].includes(c)) problems.push('data.sql：表 ' + table + ' 的列 ' + c + ' 不存在'); });
  const tuples = mm[3].split(/\)\s*,\s*\(/).map(s => s.replace(/^\s*\(|\)\s*$/g, ''));
  tuples.forEach(t => {
    rows++;
    let depth = 0, cnt = 1, inStr = false;
    for (let i = 0; i < t.length; i++) {
      const ch = t[i];
      if (ch === "'" && t[i - 1] !== '\\') inStr = !inStr;
      if (inStr) continue;
      if (ch === '(') depth++;
      if (ch === ')') depth--;
      if (ch === ',' && depth === 0) cnt++;
    }
    if (t.trim() && cnt !== colList.length) problems.push('data.sql：表 ' + table + ' 有一行值的个数(' + cnt + ')与列数(' + colList.length + ')不一致');
  });
}

/* ---------- 5. 结果 ---------- */
console.log('实体数: ' + Object.keys(entities).length + ' / schema 表数: ' + Object.keys(tables).length);
console.log('data.sql INSERT 语句: ' + n + ' 条，数据行: ' + rows + ' 行');
const totalCols = Object.values(tables).reduce((s, c) => s + c.length, 0);
console.log('建表总列数: ' + totalCols + '，实体总字段数: ' + Object.values(entities).reduce((s, e) => s + e.fields.length, 0));
console.log('索引/唯一键条数: ' + (schema.match(/\b(key|unique)\s+`?\w+`?\s*\(/gi) || []).length);
if (problems.length) {
  console.log('\n发现 ' + problems.length + ' 处不一致:');
  [...new Set(problems)].slice(0, 40).forEach(p => console.log('   ✗ ' + p));
  process.exit(1);
} else {
  console.log('\n✅ 实体 / 建表 / 种子数据 三者完全一致（列名、列数、值个数全部匹配）');
}
