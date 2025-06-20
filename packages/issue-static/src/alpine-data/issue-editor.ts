import CodeMirror from 'codemirror';
import 'codemirror/mode/markdown/markdown.js';
import 'codemirror/lib/codemirror.css';
import { marked } from 'marked';

// 初始化编辑器的函数
function initMarkdownEditor(elementId:string) {
    // 创建CodeMirror实例
    const editor = CodeMirror.fromTextArea(<HTMLTextAreaElement>document.getElementById(elementId), {
        mode: 'markdown',
        lineNumbers: false,
        theme: 'default',
        indentUnit: 2,
        tabSize: 2,
        lineWrapping: true
    });

    // 返回编辑器实例和markdown解析函数
    return {
        editor,
        renderPreview: (markdownText:string) => marked.parse(markdownText)
    };
}
export default () => ({
    activeTab: 'editor',
    editor: null,
    preview: '',
    rawContent: "",
    init() {
        // 初始化编辑器
        const { editor, renderPreview } = initMarkdownEditor('editor');
        // @ts-ignore
        this.editor = editor;

        // 设置初始内容
        editor.setValue(``);

        // 监听内容变化
        editor.on('change', () => {
            this.rawContent = editor.getValue();
            // @ts-ignore
            this.preview = renderPreview(editor.getValue());
        });

        // 初始渲染
        // @ts-ignore
        this.preview = renderPreview(editor.getValue());
    },

    // 切换tab的方法
    switchTab(tab:string) {
        this.activeTab = tab;
    }
});
