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

window.Alpine = Alpine;
Alpine.data("dropdown", dropdown);
Alpine.data("colorSchemeSwitcher", colorSchemeSwitcher);
Alpine.data("halo", halo);
Alpine.data("uiPermission", uiPermission);
Alpine.data("upvote", upvote);
Alpine.data("messageBox", message);
Alpine.data("dateUtils", dateUtils);
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
