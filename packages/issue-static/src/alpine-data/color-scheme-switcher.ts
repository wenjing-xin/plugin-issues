import { currentColorScheme } from "../main";

export default () => ({
  colorSchemes: [
    { label: "暗色", value: "dark", icon: "icon-[tdesign--mode-dark-filled]" },
    { label: "亮色", value: "light", icon: "icon-[icon-park-solid--sun-one]" },
    { label: "跟随系统", value: "system", icon: "icon-[carbon--earth-filled]" },
  ],
  currentValue: currentColorScheme,
  get colorScheme() {
    return this.colorSchemes.find((x) => x.value === this.currentValue);
  },
  get nextColorScheme() {
    const index = this.colorSchemes.findIndex((x) => x.value === this.currentValue);
    return this.colorSchemes[(index + 1) % this.colorSchemes.length];
  },
});
