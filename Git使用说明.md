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

> ✅ **密码问题已经解决了，公开仓库也能推。**
>
> 数据库密码、Redis 密码、邮箱授权码、JWT 密钥、Druid 控制台密码，
> 已经全部挪到 `backend/src/main/resources/application-local.yml`，
> 而该文件被 `.gitignore` 排除，**永远不会上传**。
>
> 仓库里只留了一份模板：
> `backend/src/main/resources/application-local.yml.example`（全是占位符）。
>
> **换电脑后怎么恢复？** 把模板复制一份改个名，填上自己的密码即可：
>
> ```powershell
> cd 基于SpringBoot的校园管理系统设计\backend\src\main\resources
> Copy-Item application-local.yml.example application-local.yml
> # 然后用记事本打开 application-local.yml 填密码
> ```

---

### 4. 连不上 GitHub？配置代理（重要）

如果你开了 Clash Verge / v2ray 之类的代理软件，会遇到这个坑：

> **Git 默认不走 Windows 系统代理**，会报
> `Connection was reset` 或 `Failed to connect to github.com:443 after 21056 ms`

（表现形式很迷惑：浏览器能打开 GitHub、`ping` 也通，但 `git push` 就是失败。）

先查代理软件监听的端口（Clash Verge 新版是 `7897`，老版本是 `7890`），然后：

```powershell
git config --global http.https://github.com.proxy http://127.0.0.1:7897
```

> 注意写的是 `http.https://github.com.proxy`，不是全局的 `http.proxy`。
> 这样**只有 GitHub 走代理**，以后推 Gitee 等国内仓库仍然直连，不会变慢。

**关掉代理软件后一定要撤销**，否则 git 连 GitHub 会失败：

```powershell
git config --global --unset http.https://github.com.proxy
```

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
| `application-local.yml` | **本地私密配置**：数据库密码、邮箱授权码、JWT 密钥。只有你本机有 |

**所以最终只有 154 个文件、0.6 MB 真正进仓库**，全是源代码、文档和原型。

> 💡 注意区分这两个文件：
>
> | 文件 | 是否上传 | 内容 |
> |---|---|---|
> | `application-local.yml` | ❌ 不上传 | 你的**真实密码** |
> | `application-local.yml.example` | ✅ 上传 | 只有**占位符**的模板 |
>
> 改密码时请只改前者。**千万别把真密码写进 `.example` 文件里。**

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
