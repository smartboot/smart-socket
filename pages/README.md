# smart-socket 文档站

官方文档站源码，基于 [Astro](https://astro.build/) + [Starlight](https://starlight.astro.build/) 构建。

- 线上地址：https://smartboot.tech/smart-socket/
- 仓库其他部分见 [根 README](../README.md)

## 本地开发

需要 Node.js 18.17+（CI 使用 23）。在 `pages/` 目录下执行：

```bash
npm install
npm run dev       # 本地预览 http://localhost:4321/smart-socket/
npm run build     # 构建产物输出到 dist/
npm run preview   # 预览构建产物
```

> 站点 `base` 配置为 `/smart-socket`，本地访问也需带上此前缀，否则静态资源会 404。

## 目录结构

```
pages/
├── astro.config.mjs        # 站点配置：base、侧边栏、i18n、插件
├── public/                 # 静态资源，原样拷贝到产物根目录
└── src/
    ├── content/docs/       # 文档内容
    │   ├── index.mdx       # 首页
    │   ├── guides/         # 使用指南（侧边栏「关于」自动生成）
    │   └── appendix/       # 附录（侧边栏「附录」自动生成）
    ├── assets/             # Logo、插图
    └── styles/custom.css   # 自定义样式
```

`astro.config.mjs` 的 `sidebar` 使用 `autogenerate`，在 `guides/` 或 `appendix/` 下新增 `.mdx` 文件会自动出现在导航中，无需手动登记。

## 写作规范

动笔前请先读 [文档写作标准](./src/content/docs/ai_prompt.mdx)。要点：

- 代码示例必须取自 `example/` 下的真实文件，并链接到 Gitee 完整路径，禁止凭空编写
- 依赖版本号必须与当前发布版本一致（当前为 **2.2.0**）
- 包名统一使用 `io.github.smartboot.socket`
- 示例中引用的源码路径必须是 `io/github/smartboot/socket/...`，不要残留重构前的 `org/smartboot/socket/...`

## 多语言

站点已配置 `zh-CN`（默认）与 `en` 两个 locale，但 **`src/content/docs/en/` 目录尚未建立**，
英文入口目前没有对应内容。补齐英文文档前，如需避免空页面，可在 `astro.config.mjs` 中暂时移除 `en` locale。

## 发布

推送到 `master` 且改动命中 `pages/**` 时，由 GitHub Actions（[`.github/workflows/pages.yml`](../.github/workflows/pages.yml)）自动构建并部署到 GitHub Pages，也可在 Actions 页手动触发。
