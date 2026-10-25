import { defineConfig } from "vite";
import { fileURLToPath } from "url";

export default defineConfig({
  plugins: [],
  build: {
    outDir: fileURLToPath(new URL("./build/dist", import.meta.url)),
    emptyOutDir: true,
    lib: {
      entry: fileURLToPath(new URL("./src/main.ts", import.meta.url)),
      name: "PluginIssues",
      fileName: "main",
      formats: ["iife"],
    },
  }
});
