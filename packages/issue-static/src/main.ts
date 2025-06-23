import "./styles/tailwind.css";
import "./styles/main.css";
import Alpine from "alpinejs";
import dropdown from "./alpine-data/dropdown";
import colorSchemeSwitcher from "./alpine-data/color-scheme-switcher";
import uiPermission from "./alpine-data/ui-permission";
import halo from "./alpine-data/halo";
import upvote from "./alpine-data/upvote";
import message from "./alpine-data/message";
import dateUtils from "./alpine-data/date";
import issueEditor from "./alpine-data/issue-editor";
import { IssueComment } from "./types";
import { createIssueComment } from "./api";


window.Alpine = Alpine;
Alpine.data("dropdown", dropdown);
Alpine.data("colorSchemeSwitcher", colorSchemeSwitcher);
Alpine.data("halo", halo);
Alpine.data("uiPermission", uiPermission);
Alpine.data("upvote", upvote);
Alpine.data("messageBox", message);
Alpine.data("dateUtils", dateUtils);
Alpine.data("issueEditor", issueEditor)
Alpine.start();


type ColorSchemeType = "system" | "dark" | "light";
export let currentColorScheme: ColorSchemeType = "system";

export function initColorScheme(defaultColorScheme: ColorSchemeType, enableChangeColorScheme: boolean) {
    let colorScheme = defaultColorScheme;
    if (enableChangeColorScheme) {
        colorScheme = (localStorage.getItem("color-scheme") as ColorSchemeType) || defaultColorScheme;
    }
    currentColorScheme = colorScheme;
    setColorScheme(colorScheme, false);
}
export function setColorScheme(colorScheme: ColorSchemeType, store: boolean) {
    const html = document.documentElement;
    // 如果是文章页面 移除当前阅读背景
    if (window.location.pathname.indexOf("archives") !== -1) {
        const readerAreaEle = document.querySelector("#global-background");
        const coverBottomRounded = document.querySelector("#cover-bottom-rounded");
        const prefix = 'bg-';
        if (readerAreaEle) {
            readerAreaEle.classList.forEach(className => {
                if (className.startsWith(prefix) && className !== "bg-neutral-50") {
                    readerAreaEle.classList.remove(className);
                    readerAreaEle.classList.add('bg-neutral-50');
                }
            });
        }
        //移除文章顶部cover的颜色
        coverBottomRounded?.classList.forEach(className => {
            if(className.startsWith(prefix) && className !== "bg-neutral-50"){
                coverBottomRounded.classList.remove(className);
                coverBottomRounded.classList.add('bg-neutral-50');
            }
        });
    }
    if (colorScheme === "system") {
        const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        html.setAttribute("data-color-scheme", prefersDark ? "dark" : "light");
        document.documentElement.classList.add(prefersDark ? "dark" : "light");
        document.documentElement.classList.remove(prefersDark ? "light" : "dark");
    } else {
        html.setAttribute("data-color-scheme", colorScheme);
        document.documentElement.classList.add(colorScheme);
        document.documentElement.classList.remove(colorScheme === "dark" ? "light" : "dark");
    }
    currentColorScheme = colorScheme;
    if (store) {
        localStorage.setItem("color-scheme", colorScheme);
    }
}

export function publishIssueComment(issueId: string, rawContent: string, htmlContent: string, quoteCommentId:string) {
    let initComment:IssueComment = {
        kind: "IssueComment",
        apiVersion: "issue.webjing.com/v1alpha1",
        metadata: {
            name: "",
            generateName: "ic-"
        },
        spec: {
            content: {
                raw: rawContent,
                html: htmlContent,
                medium: []
            },
            issueName: issueId,
            owner: "",
            hidden: false,
            approved: false,
            top: false,
            quoteCommentUid: quoteCommentId,
            allowNotification: true,
            userAgent: navigator.userAgent
        },
    }
    createIssueComment(initComment).then(res=> {
        if(res.status == 200){
            const messageUtils = message();
            messageUtils.showMessage("success", '操作成功', 3000)
            window.location.reload();
        }
    })
}

//加载动画
export function triggerLazyAnimation(targetSelector: string) {
    // 基础配置参数
    const BASE_DELAY = 0.1; // 基础延迟
    const ROW_DELAY = 0.15; // 行间延迟增量
    const COL_DELAY = 0.07; // 列间延迟增量
    const STAGGER_OFFSET = 12; // 列错位幅度 10-30px
    const elements = document.querySelectorAll<HTMLElement>(targetSelector + '.t-lazy-item');
    // 布局分析数据结构
    interface LayoutInfo {
        rows: HTMLElement[][];
        positions: WeakMap<HTMLElement, {row: number; col: number}>;
    }

    // 分析元素行列布局
    const analyzeLayout = (): LayoutInfo => {
        const layout: LayoutInfo = {
            rows: [],
            positions: new WeakMap()
        };

        // 按Y坐标分组行
        const rowMap = new Map<string, HTMLElement[]>();
        elements.forEach(el => {
            const rect = el.getBoundingClientRect();
            const yKey = `${Math.round(rect.top)}`;

            if (!rowMap.has(yKey)) {
                rowMap.set(yKey, []);
            }
            rowMap.get(yKey)!.push(el);
        });

        // 排序行和列
        layout.rows = Array.from(rowMap.values())
                .sort((a, b) =>
                        a[0].getBoundingClientRect().top - b[0].getBoundingClientRect().top
                )
                .map(row =>
                        row.sort((a, b) =>
                                a.getBoundingClientRect().left - b.getBoundingClientRect().left
                        )
                );

        // 记录行列位置
        layout.rows.forEach((row, rowIndex) => {
            row.forEach((el, colIndex) => {
                layout.positions.set(el, { row: rowIndex, col: colIndex });
            });
        });

        return layout;
    };

    // 获取布局信息
    const layout = analyzeLayout();
    let activeRow = 0;

    // 初始化元素状态
    elements.forEach(el => {
        const pos = layout.positions.get(el);
        const translateX = pos ? pos.col * STAGGER_OFFSET : 0;
        el.style.opacity = '0';
        el.style.transform = `translate(${translateX}px, 15px)`;
        el.style.willChange = 'transform, opacity';
    });

    // 动态延迟计算
    const getDelay = (el: HTMLElement) => {
        const pos = layout.positions.get(el);
        if (!pos) return BASE_DELAY;

        return BASE_DELAY +
                pos.row * ROW_DELAY +
                pos.col * COL_DELAY;
    };

    // 核心加载方法
    const loadElement = (el: HTMLElement) => {
        if (el.style.opacity === '1') return;

        const delay = getDelay(el);
        el.style.animation = `radialReveal 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) ${delay}s forwards`;
        el.dataset.loaded = 'true';
    };

    // Intersection Observer
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const el = entry.target as HTMLElement;
                const pos = layout.positions.get(el);
                if (pos) activeRow = Math.max(activeRow, pos.row);
                loadElement(el);
            }
        });
    }, {
        root: null,
        rootMargin: '200px 0px',
        threshold: 0
    });

    // 滚动预加载
    const checkNearby = () => {
        const scrollY = window.scrollY + window.innerHeight;
        layout.rows.slice(activeRow, activeRow + 2).forEach(row => {
            row.forEach(el => {
                if (el.getBoundingClientRect().top < scrollY + 300) {
                    loadElement(el);
                }
            });
        });
    };

    // 初始化观察
    elements.forEach(el => observer.observe(el));
    requestAnimationFrame(checkNearby);
}

// 生成随机颜色
export function generateColors (labelsStr: string, maxColors?: number): {label:string, bgColor:string}[] {
    // 处理标签列表
    let labels:string[] =  labelsStr.replace("[", "").replace("]", "").split(",")

    // HSL 颜色生成策略 (更易控制美观度)
    const generateHSL = (hue: number): string => {
        const saturation = 70 + Math.random() * 15; // 饱和度 70-85%
        const lightness = 50 + Math.random() * 10;  // 亮度 50-60%
        return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    };

    // 转换成 RGB 格式
    const hslToRgb = (h: number, s: number, l: number): string => {
        h /= 360;
        s /= 100;
        l /= 100;

        let r, g, b;
        if (s === 0) {
            r = g = b = l;
        } else {
            // @ts-ignore
            const hue2rgb = (p, q, t) => {
                if (t < 0) t += 1;
                if (t > 1) t -= 1;
                if (t < 1/6) return p + (q - p) * 6 * t;
                if (t < 1/2) return q;
                if (t < 2/3) return p + (q - p) * (2/3 - t) * 6;
                return p;
            };

            const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            const p = 2 * l - q;
            r = hue2rgb(p, q, h + 1/3);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1/3);
        }

        const toHex = (x:number) => {
            const hex = Math.round(x * 255).toString(16);
            return hex.length === 1 ? '0' + hex : hex;
        };

        return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
    };

    // 生成色相基准值 (黄金分割分布)
    const goldenRatio = 0.618033988749895;
    const colors = [];
    const hueStep = 360 * goldenRatio;

    let currentHue = Math.random() * 360;

    for (let i = 0; i < labels.length; i++) {
        if (maxColors && i >= maxColors) break;

        const hsl = generateHSL(currentHue);
        const matches = hsl.match(/(\d+(\.\d+)?)/g);
        const rgb = hslToRgb(
                parseFloat(matches![0]),
                parseFloat(matches![1]),
                parseFloat(matches![2])
        );
        let randomColorLabel = {label:labels[i], bgColor: rgb}
        colors.push(randomColorLabel);
        currentHue = (currentHue + hueStep) % 360;
    }

    return colors;
}

// 复制链接
export function copyLink(link: string) {
    const messageUtils = message();
    let finalCopyLink = link;
    if(!link.includes('https://') || !link.includes('http://')){
        finalCopyLink = window.location.origin + link
    }
    navigator.clipboard.writeText(finalCopyLink).then(() => {
        messageUtils.showMessage("success", '链接复制成功', 3000)
    }).catch(error=> {
        messageUtils.showMessage("error", '链接复制失败：' + error.message, 3000)
    })
}