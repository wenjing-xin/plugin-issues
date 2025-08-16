# plugin-issues (言答Issue)

> 灵活高效的 Issue 管理工具,本插件是一款灵活高效的 Issue 管理工具，类似于 GitHub 的 Issues 系统，但增加了全新的“_**Issue 依托主体**_”概念。通过将 Issue 与具体的主体（如文章、项目等）绑定，用户可以在每个主体下创建和管理与其相关的 Issue，从而实现更精细化的任务管理。

 📍演示站

| 网站名称         | 演示地址                    |
|--------------|-------------------------|
| **webjing**  | https://www.webjing.cn/subject/subject-x3qpwdnq |
| **Handsome** | https://www.lik.cc/subject/subject-arp3smne |

[详细使用文档](https://www.yuque.com/webjing/cr7bwp/rg9c7uquixq8p4of)

## 一 主要功能
1. **多类型主体支持**
    - 支持的文章、项目、产品、话题、留言等五种主体类型，并将持续扩展更多类型。
    - 每个主体下都可以新增 Issue，实现任务与主体的深度关联。
    - Issue 依托主体支持公共和私有。
2. **Issue 状态管理**
    - Issue 支持三种状态：_**关闭**_、_**等待处理**_、_**进行中**_，方便跟踪任务进展。
3. **协作与通知**
    - **经办人指定**：为每个 Issue 指派负责人，明确责任分工。
    - **标签管理**：支持动态指定标签范围（全局标签、特定主体下的标签、特定主体类型下的标签），提升标签的复用效率。
    - **自动化通知订阅**：用户可订阅 Issue 的动态，及时获取更新提醒。
4. **统计与搜索**
    - 提供 Issue 统计功能，帮助用户直观了解任务进展。
    - Issue 内容可集成到 Halo 搜索引擎，快速查找相关信息。
5. **Issue 模板**
    - 用户可自定义 Issue 模板，提升信息收集效率。
    - 动态模版功能为 Issue 提供了简洁易用的表单收集功能。
    - 模板支持动态作用范围，可选择特定主体类型或特定主体下生效。
6. **轻量化编辑器**
    - 使用 Codemirror 和 Marked 开发的 Markdown 编辑器，功能齐全且轻量化。
    - 提供与 GitHub Issue 编辑器一致的使用体验。
7. **审核功能**
    - 支持 Issue 评论审核和 Issue 审核，确保内容质量。

---

## 二、核心优势
+ **灵活性**：通过“Issue 依托主体”概念，将任务管理与具体主体深度结合，提升管理颗粒度。
+ **协作性**：支持经办人、标签、自动化通知订阅等功能，助力团队高效协作。
+ **高效性**：动态标签和模板管理，减少重复劳动，提升效率。
+ **扩展性**：主体类型、状态、标签范围均可扩展，满足多样化的管理需求。

---

## 三、适用场景
### 常规
+ 项目管理：为项目创建 Issue，跟踪任务进展。
+ 产品管理：在产品主体下记录和管理问题、需求。
+ 内容管理：为文章或话题创建 Issue，记录需要改进或补充的内容。
+ 客户支持：在留言主体下创建 Issue，跟踪用户反馈和问题解决进度。

### 个人
+ **个人项目管理**：记录和跟踪个人兴趣项目、学习计划或目标。
+ **学习与成长**：创建 Issue 记录学习任务、知识整理或技能提升计划。
+ **生活清单**：管理个人待办事项、购物清单或旅行计划。  
  通过 Issue 模板的动态收集功能，个人用户可以轻松提交和跟踪与个人生活相关的各种信息，例如友链提交、兴趣点整理、书籍推荐等，从而提升个人事务的管理效率。

---

## 四、功能预览
### Isssue 依托主体

![](https://github.com/user-attachments/assets/184b595a-7ba7-4e49-851b-32e7c42196d3)


### issue列表管理
![](https://github.com/user-attachments/assets/1d52a18d-e2d8-4d92-ada8-0519a38ca2c1)

### issue模版管理

![](https://github.com/user-attachments/assets/128b8adc-fac0-41e4-892a-f351a212f7a5)

![](https://github.com/user-attachments/assets/dbec696a-1c1f-4bdf-aa89-d3b6a86e4f79)


Issue标签管理

![](https://github.com/user-attachments/assets/5dda35a1-d795-44f5-98d2-971942ac9dc4)

### 主题端效果

![](https://github.com/user-attachments/assets/d75e1fe1-b7f2-44e8-a48a-3b9e6f856121)

![](https://github.com/user-attachments/assets/cf1bb848-3591-4e42-8e3f-9d05ea87427f)

![](https://github.com/user-attachments/assets/193272d2-e58e-4c52-9165-f60bb6d1d368)


### 后续计划
- [ ] 加入 AI 机器人协助处理Issue
- [ ] 增加评论自动检测审核功能
- [ ] 主题端增加 '/' 指令，如为指定Issue绑定标签指令，分配经办人等指令，@指定用户等
- [ ] 加入 hooks 便于与其他软件的issue进行双向同步

## 交流群
点击链接加入群聊【Halo - 微风静语主题插件交流群】：https://qm.qq.com/q/Ey5zZBP45y

![](https://www.webjing.cn/upload/wfly-qq.png#pic_center)



### 开发环境

插件开发的详细文档请查阅：<https://docs.halo.run/developer-guide/plugin/introduction>

所需环境：

1. Java 17
2. Node 20
3. pnpm 9
4. Docker (可选)

克隆项目：

```bash
git clone git@github.com:halo-sigs/plugin-starter.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-starter.git
```

```bash
cd path/to/plugin-starter
```

### 运行方式 1（推荐）

> 此方式需要本地安装 Docker

```bash
# macOS / Linux
./gradlew pnpmInstall

# Windows
./gradlew.bat pnpmInstall
```

```bash
# macOS / Linux
./gradlew haloServer

# Windows
./gradlew.bat haloServer
```

执行此命令后，会自动创建一个 Halo 的 Docker 容器并加载当前的插件，更多文档可查阅：<https://docs.halo.run/developer-guide/plugin/basics/devtools>

### 运行方式 2

> 此方式需要使用源码运行 Halo

编译插件：

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    fixedPluginPath:
      - "/path/to/plugin-starter"
```

最后重启 Halo 项目即可。
