// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightImageZoomPlugin from "starlight-image-zoom";
import starlightScrollToTop from 'starlight-scroll-to-top';


// https://astro.build/config
export default defineConfig({
    site: 'https://smartboot.tech/',
    base: '/smart-socket',
    trailingSlash: "always",
    integrations: [
        starlight({
            title: 'smart-socket',
            logo: {
                src: './src/assets/logo.svg',
            },
            customCss: [
                // 你的自定义 CSS 文件的相对路径
                './src/styles/custom.css',
            ],
            head: [
                {
                    tag: 'meta',
                    attrs: {
                        property: 'keywords',
                        content: 'smart-socket,java,nio,aio,网络通信,高性能,异步通信,java框架',
                    }
                }, {
                    tag: 'meta',
                    attrs: {
                        property: 'description',
                        content: 'smart-socket是一款基于Java AIO实现的轻量级、高性能网络通信框架',
                    }
                },
                {
                    tag: 'script',
                    content: `
                var _hmt = _hmt || [];
                (function() {
                  var hm = document.createElement("script");
                  hm.src = "https://hm.baidu.com/hm.js?ee8630857921d8030d612dbd7d751b55";
                  var s = document.getElementsByTagName("script")[0]; 
                  s.parentNode.insertBefore(hm, s);
                })();
          `
                }
            ],
            social: [
                {icon: 'github', label: 'GitHub', href: 'https://github.com/smartboot/smart-socket'},
                {icon: 'seti:git', label: 'Gitee', href: 'https://gitee.com/smartboot/smart-socket'}
            ],
            plugins: [starlightImageZoomPlugin(), starlightScrollToTop({
                // Button position
                // Tooltip text
                tooltipText: 'Back to top',
                showTooltip: true,
                // Use smooth scrolling
                // smoothScroll: true,
                // Visibility threshold (show after scrolling 20% down)
                threshold: 20,
                // Customize the SVG icon
                borderRadius: '50',
                // Show scroll progress ring
                showProgressRing: true,
                // Customize progress ring color
                progressRingColor: '#ff6b6b',
            })],
            // 为此网站设置英语为默认语言。
            defaultLocale: 'root',
            locales: {
                root: {
                    label: '简体中文',
                    lang: 'zh-CN',
                },
                // 英文文档在 `src/content/docs/en/` 中。
                'en': {
                    label: 'English',
                    lang: 'en'
                }
            },
            sidebar: [
                {
                    label: '关于',
                    autogenerate: {directory: 'guides'},
                },
                {
                    label: '插件',
                    autogenerate: {directory: 'plugin'},
                },
                {
                    label: '附录',
                    items: [
                        {
                            label: 'smart-socket实用教程📚',
                            link: 'https://mp.weixin.qq.com/mp/appmsgalbum?__biz=Mzg4MzU2NDA0Nw==&action=getalbum&album_id=4244214517749514252#wechat_redirect',
                        },
                        {
                            label: '付费服务💰️',
                            slug: 'appendix/service',
                        },
                    ],
                },
            ],
        }),
    ],
});
