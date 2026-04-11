import {
  defineConfig,
  transformerCompileClass,
  presetWind3
} from 'unocss'

export default defineConfig({
  presets: [presetWind3()],
  transformers: [
    transformerCompileClass(),
  ],
  theme: {
    breakpoints: {
      sm: "640px",
      md: "768px",
      lg: "1024px",
      xl: "1280px",
      "2xl": "1536px",
    },
  },
});
