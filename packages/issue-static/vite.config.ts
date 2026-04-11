import { defineConfig } from "vite";
import { fileURLToPath } from "url";

// @ts-ignore
import path from "path";


export default defineConfig({
  plugins: [],
  build: {
    // @ts-ignore
    outDir: fileURLToPath(new URL("../../src/main/resources/static/dist", import.meta.url)),
    emptyOutDir: true,
    lib: {
      entry: path.resolve(__dirname, "src/main.ts"),
      name: "main",
      fileName: "main",
      formats: ["iife"],
    },
  }
});
