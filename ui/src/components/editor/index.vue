<script lang="ts" setup>
defineOptions({
  name: "IssueTextEditor",
});

import IssueExtensionImage from "@/extensions/images";
import {
  ExtensionAudio,
  ExtensionBlockquote,
  ExtensionBold,
  ExtensionBulletList,
  ExtensionCode,
  ExtensionColor,
  ExtensionColumn,
  ExtensionColumns,
  ExtensionCommandsMenu,
  ExtensionDocument,
  ExtensionDropcursor,
    ExtensionFontSize,
    ExtensionGapCursor,
  ExtensionHardBreak,
  ExtensionHighlight,
  ExtensionHistory,
  ExtensionHorizontalRule,
  ExtensionIframe,
  ExtensionIndent,
  ExtensionItalic,
  ExtensionLink,
  ExtensionListKeymap,
  ExtensionNodeSelected,
  ExtensionOrderedList,
  ExtensionParagraph,
  ExtensionPlaceholder,
  ExtensionStrike,
  ExtensionSubscript,
  ExtensionSuperscript,
  ExtensionTable,
  ExtensionTaskList,
  ExtensionText,
  ExtensionTextAlign,
  ExtensionTrailingNode,
  ExtensionUnderline,
    ExtensionVideo,
    RichTextEditor,
    useEditor,
} from "@halo-dev/richtext-editor";
import { watch } from "vue";

const props = withDefaults(
  defineProps<{
    html: string;
    raw: string;
    isEmpty: boolean;
  }>(),
  {
    html: "",
    raw: "",
    isEmpty: true,
  }
);

const emit = defineEmits<{
  (event: "update:raw", value: string): void;
  (event: "update:html", value: string): void;
  (event: "update", value: string): void;
  (event: "update:isEmpty", value: boolean | undefined): void;
}>();

const editor = useEditor({
  content: props.raw,
  extensions: [
    ExtensionParagraph,
    ExtensionBlockquote,
    ExtensionBold,
    ExtensionBulletList,
    ExtensionCode,
    ExtensionDocument,
    ExtensionDropcursor.configure({
      width: 2,
      class: "dropcursor",
      color: "skyblue",
    }),
    ExtensionGapCursor,
    ExtensionHardBreak,
    ExtensionHistory,
    ExtensionHorizontalRule,
    ExtensionItalic,
    ExtensionOrderedList,
    ExtensionStrike,
    ExtensionText,
    IssueExtensionImage.configure({
      inline: true,
      allowBase64: false,
      HTMLAttributes: {
        loading: "lazy",
      },
    }),
    ExtensionTaskList,
    ExtensionLink.configure({
      autolink: true,
      openOnClick: false,
    }),
    ExtensionTextAlign.configure({
      types: ["heading", "paragraph"],
    }),
    ExtensionUnderline,
    ExtensionTable.configure({
      resizable: true,
    }),
    ExtensionSubscript,
    ExtensionSuperscript,
    ExtensionHighlight,
    ExtensionCommandsMenu,
    ExtensionIframe,
    ExtensionVideo,
    ExtensionAudio,
    ExtensionFontSize,
    ExtensionColor,
    ExtensionIndent,
    ExtensionColumns,
    ExtensionColumn,
    ExtensionNodeSelected,
    ExtensionTrailingNode,
    ExtensionPlaceholder.configure({
      placeholder: "",
    }),
    ExtensionHighlight,
    ExtensionListKeymap,
  ],
  autofocus: "end",
  onUpdate: () => {
    emit("update:raw", editor.value?.getHTML() + "");
    emit("update:html", editor.value?.getHTML() + "");
    emit("update:isEmpty", editor.value?.isEmpty);
    emit("update", editor.value?.getHTML() + "");
  },
});

watch(
  () => props.raw,
  () => {
    if (props.raw !== editor.value?.getHTML()) {
      editor.value?.commands.setContent(props.raw);
    }
  }
);
</script>
<template>
  <div v-if="editor" class="halo-issue-editor relative border rounded-sm">
    <RichTextEditor :editor="editor" locale="zh-CN"></RichTextEditor>
  </div>
</template>
