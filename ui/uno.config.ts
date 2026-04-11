import { defineConfig, presetUno, transformerDirectives, presetTypography } from "unocss";

export default defineConfig({
  presets: [presetUno(), presetTypography()],
  transformers: [transformerDirectives()],
});
