import {
  type Editor,
  ExtensionImage,
  type ExtensionOptions,
  type Range,
} from "@halo-dev/richtext-editor";
import { markRaw } from "vue";
import MdiFileImageBox from "~icons/mdi/file-image-box";

export interface ImageOptions {
  inline: boolean;
  allowBase64: boolean;
  HTMLAttributes: Record<string, unknown>;
}

const IssueExtensionImage = ExtensionImage.extend<ExtensionOptions & ImageOptions>({
  addOptions() {
    const parentOptions = this.parent?.()

    return {
      inline: true,
      allowBase64: false,
      HTMLAttributes: {},
      ...parentOptions,
      getCommandMenuItems() {
        return {
          priority: 100,
          icon: markRaw(MdiFileImageBox),
          title: "图片",
          keywords: ["image", "tupian"],
          command: ({ editor, range }: { editor: Editor; range: Range }) => {
            editor
              .chain()
              .focus()
              .deleteRange(range)
              .insertContent([
                { type: "image", attrs: { src: "" } },
                { type: "paragraph", content: "" },
              ])
              .run();
          },
        };
      },
    };
  },
});

export default IssueExtensionImage;
