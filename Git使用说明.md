# Git 使用说明（针对本项目）

> 你现在的仓库位置：`D:\campus-system`
> 也就是说，**这个文件夹里的一切改动，Git 都会帮你记着**。

---

## 一、Git 到底是干什么的

一句话：**Git 是给文件夹拍快照的时光机。**

你可以把它想成游戏里的「存档」：

| 游戏 | Git |
|---|---|
| 打 BOSS 前先存档 | 改代码前先 `commit` 一次 |
| 存档可以有很多个 | 提交历史可以有很多条 |
| 打输了读档 | 代码改坏了 `git restore` 回退 |
| 存档里能看到当时的状态 | `git log` 看每次改了什么 |

它和「复制一份文件夹改名叫 `项目-最终版-真的最终版`」的区别：

- 只存**改动**，不重复存整个文件夹，所以很省地方（你这个仓库才 0.26 MB）
- 能精确看到**哪一行**在**哪一天**被改了
- 能随时回到**任意一个**历史版本
- 可以同步到 Gitee / GitHub，电脑坏了代码还在

---

## 二、先确认 Git 能用

打开一个**新的** PowerShell 或 CMD 窗口（不是以前开着的旧的），输入：

```powershell
git --version
```

看到 `git version 2.56.0.windows.2` 之类的输出就说明装好了。
（你的 Git 装在 `D:\Git`，安装程序已经把它加进了系统 PATH。）

如果提示「不是内部或外部命令」，就**注销一次 Windows 再登录**，或者重启电脑。

---

## 三、日常只用记住 3 条命令

先进入项目文件夹：

```powershell
cd D:\campus-system
```

### 1. 看现在有什么改动

```powershell
git status
```

- 红色文件 = 改了但还没准备提交
- 绿色文件 = 已经放进「待提交清单」
- 什么都没输出 = 一切正常，没有未保存的改动

### 2. 把改动放进待提交清单

```powershell
git add .
```

（`.` 表示「当前文件夹下所有改动」）

### 3. 正式存档，并写一句话说明

```powershell
git commit -m "写一句人话：这次改了什么"
```

比如：

```powershell
git commit -m "公告列表增加按院系筛选"
```

> 建议养成习惯：**每完成一个小功能就提交一次**。
> 不要攒一星期再提交一次，那样历史就没用了。

---

## 四、看历史 / 看改了什么

```powershell
git log --oneline          # 一行一条，看所有存档
git log --oneline --graph  # 带分支线，更直观
git show                   # 看最近一次提交具体改了哪些行
git diff                   # 看「还没 add」的改动内容
git diff --staged          # 看「已经 add」的改动内容
```

看某个文件的历史：

```powershell
git log --oneline -- "基于SpringBoot的校园管理系统设计/backend/pom.xml"
```

---

## 五、改坏了怎么回退（重点）

### 情况 A：改乱了，想还原成上次提交的样子

```powershell
git restore 文件路径          # 丢掉这个文件的未提交改动（不可恢复！）
git restore .                 # 丢掉所有未提交改动（不可恢复！）
```

### 情况 B：已经 commit 了，想撤销这次提交但保留改动

```powershell
git reset --soft HEAD~1
```

### 情况 C：已经 commit 了，想彻底回到上一个版本

```powershell
git revert HEAD              # 安全做法：生成一条「反向提交」，历史还留着
```

> `git restore` 和 `git reset --hard` 会**真的删掉**你还没提交的东西，用之前先 `git status` 确认一遍。

---

## 六、备份到云端（Gitee / GitHub）

这一步是可选的，但**强烈建议做**——本地仓库只能防「改错代码」，防不了「硬盘坏了」。

### 1. 注册账号

- 国内推荐 **Gitee（码云）**：<https://gitee.com> —— 速度快，不需要科学上网
- 国际用 **GitHub**：<https://github.com>

### 2. 在网站上新建一个仓库

- 仓库名例如 `campus-announcement`
- **不要**勾选「初始化仓库 / 添加 README / 添加 .gitignore」（因为本地已经有了）

### 3. 把本地仓库连上去并推送

在 `D:\campus-system` 里执行（把地址换成你自己的）：

```powershell
# 第一次推送时，告诉 Git 远程仓库在哪
git remote add origin https://gitee.com/你的用户名/campus-announcement.git

# 把 main 分支推上去
git push -u origin main
```

之后每次改完想同步，只要：

```powershell
git push
```

第一次推送会弹窗让你登录 Gitee/GitHub 账号。

> ⚠️ **推送前务必注意：** `backend/src/main/resources/application.yml` 和 `application-dev.yml`
> 里写着**数据库密码、JWT 密钥、邮箱授权码**。
> 如果推到**公开仓库**，全世界都能看到。
> 真要公开的话，请打开 `.gitignore` 最后那两行的注释，把这两个文件排除掉。

---

## 七、这个仓库的规矩（.gitignore）

`.gitignore` 文件决定了「哪些东西不进版本管理」。本项目已经排除了：

| 被排除的 | 为什么 |
|---|---|
| `target/` | Maven 编译产物，`mvn package` 会重新生成 |
| `node_modules/` | 前端依赖，`npm install` 会重新下载 |
| `tools/apache-maven-3.9.9/` | Maven 程序本体，10 MB，不该进代码库 |
| `tools/maven.zip` | 安装包，8.8 MB |
| `.idea/` | IDEA 的**你这台电脑**的配置，别人不需要 |
| `logs/`、`*.log` | 运行日志，天天变，提交它没意义 |
| `upload/` | 用户上传的文件，属于运行数据 |

**所以最终只有 152 个文件、0.6 MB 真正进仓库**，全是源代码、文档和原型。

---

## 八、几个容易踩的坑

1. **不要把数据库数据文件放进 Git。**
   MySQL 的数据文件（`D:\MySQL\...\data`）二进制且巨大，而且数据库有专门的备份工具
   （`mysqldump`）。Git 管的是**代码**，不是**运行数据**。
   建表脚本 `schema.sql` / `data.sql` 已经在仓库里了，这已经足够重建数据库。

2. **项目路径里有中文**（`基于SpringBoot的校园管理系统设计`）。
   能正常工作，但偶尔某些老旧工具会出问题。如果以后遇到莫名其妙的报错，
   可以考虑把它改成英文名，例如 `campus-announcement-design`。

3. **`git add .` 之前先 `git status` 看一眼。**
   养成这个习惯，就不会不小心把密码文件、几十 MB 的压缩包提交进去。

4. **不要删除 `.git` 文件夹。**
   它就在 `D:\campus-system\.git`，是**整个版本库的本体**，删了历史就全没了。

---

## 九、速查表

```powershell
cd D:\campus-system        # 进入项目

git status                 # 现在有什么改动
git add .                  # 全部加入待提交
git commit -m "说明"        # 存档

git log --oneline          # 看所有存档
git diff                   # 看未提交的改动内容

git restore 文件            # 撤销某个文件的改动（危险）
git remote add origin <地址>  # 第一次连接远程仓库
git push                   # 推到云端
git pull                   # 从云端拉取最新
```
