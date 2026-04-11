import CodeMirror from 'codemirror';
import 'codemirror/mode/markdown/markdown.js';
import 'codemirror/lib/codemirror.css';
import { marked } from 'marked';
marked.setOptions({
    breaks: true
});
export default () => ({
    activeTab: 'editor',
    editor: null,
    preview: '',
    rawContent: '',

    init() {
        // 使用 x-ref 获取编辑器元素
        // @ts-ignore
        const editorElement = this.$refs.editor;

        // 创建 CodeMirror 实例
        // @ts-ignore
        this.editor = CodeMirror.fromTextArea(editorElement, {
            mode: 'markdown',
            lineNumbers: false,
            theme: 'default',
            indentUnit: 2,
            tabSize: 2,
            lineWrapping: true,
            pasteLinesPerSelection: true,
            extraKeys: {
                'Enter': function (cm) {
                    const pos = cm.getCursor();
                    const line = cm.getLine(pos.line);

                    // 处理任务列表
                    const taskMatch = line.match(/^(\s*)- \[( |x)\] (.*)$/);
                    if (taskMatch) {
                        const indent = taskMatch[1];
                        const content = taskMatch[3];
                        // 如果当前行只有前缀（即内容为空），回车只换行
                        if (content.trim() === '') {
                            cm.replaceSelection('\n' + indent);
                        } else {
                            cm.replaceSelection('\n' + indent + '- [ ] ');
                        }
                        return;
                    }

                    // 处理有序列表
                    const orderedMatch = line.match(/^(\s*)(\d+)\.\s(.*)$/);
                    if (orderedMatch) {
                        const indent = orderedMatch[1];
                        const nextNumber = parseInt(orderedMatch[2], 10) + 1;
                        const content = orderedMatch[3];
                        if (content.trim() === '') {
                            cm.replaceSelection('\n' + indent);
                        } else {
                            cm.replaceSelection(`\n${indent}${nextNumber}. `);
                        }
                        return;
                    }

                    // 处理无序列表
                    const unorderedMatch = line.match(/^(\s*)[-*+]\s(.*)$/);
                    if (unorderedMatch) {
                        const indent = unorderedMatch[1];
                        const content = unorderedMatch[2];
                        if (content.trim() === '') {
                            cm.replaceSelection('\n' + indent);
                        } else {
                            cm.replaceSelection('\n' + indent + '- ');
                        }
                        return;
                    }

                    // 处理块引用
                    const blockquoteMatch = line.match(/^(\s*)>\s(.*)$/);
                    if (blockquoteMatch) {
                        const indent = blockquoteMatch[1];
                        const content = blockquoteMatch[2];
                        if (content.trim() === '') {
                            cm.replaceSelection('\n' + indent);
                        } else {
                            cm.replaceSelection('\n' + indent + '> ');
                        }
                        return;
                    }

                    // 默认行为
                    cm.execCommand('newlineAndIndent');
                }
            },

        });

        // 设置初始内容
        // @ts-ignore
        this.editor.setValue('');

        // 监听内容变化
        // @ts-ignore
        this.editor.on('change', () => {
            // @ts-ignore
            this.rawContent = this.editor.getValue();
            // @ts-ignore
            this.preview = marked.parse(this.rawContent);
        });

        // 初始渲染
        // @ts-ignore
        this.preview = marked.parse(this.rawContent);
    },

    // 切换 tab 的方法
    // @ts-ignore
    switchTab(tab) {
        this.activeTab = tab;
    },

    // 统一的插入处理函数
    // @ts-ignore
    insertMarkdown(processor) {
        if (!this.editor) return;

        const cm = this.editor;
        // @ts-ignore
        const doc = cm.getDoc();
        const selection = doc.getSelection();

        // 保存当前滚动位置
        // @ts-ignore
        const scrollInfo = cm.getScrollInfo();

        // 执行处理逻辑
        processor(cm, selection);

        // 恢复滚动位置
        // @ts-ignore
        cm.scrollTo(scrollInfo.left, scrollInfo.top);

        // 聚焦编辑器
        // @ts-ignore
        cm.focus();

        // 更新预览
        // @ts-ignore
        this.rawContent = doc.getValue();
        // @ts-ignore
        this.preview = marked.parse(doc.getValue());
    },
    // 加粗
    bold() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            if (selection) {
                doc.replaceSelection(`**${selection}**`);
            } else {
                const cursor = doc.getCursor();
                doc.replaceRange('****', cursor);
                doc.setCursor(cursor.line, cursor.ch + 2);
            }
        });
    },

    // 斜体
    italic() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            if (selection) {
                doc.replaceSelection(`*${selection}*`);
            } else {
                const cursor = doc.getCursor();
                doc.replaceRange('**', cursor);
                doc.setCursor(cursor.line, cursor.ch + 1);
            }
        });
    },

    // 标题
    heading(level: number) {
        // @ts-ignore
        this.insertMarkdown((cm, selection: string) => {
            const doc = cm.getDoc();
            const cursor = doc.getCursor();
            const line = doc.getLine(cursor.line);

            // 检测当前标题级别
            const headingMatch = line.match(/^(#{1,6})\s/);
            const currentLevel = headingMatch ? headingMatch[1].length : 0;

            if (currentLevel === level) {
                // 如果当前级别和传入级别一致，去掉标题
                doc.replaceRange('', { line: cursor.line, ch: 0 }, { line: cursor.line, ch: currentLevel + 1 });
            } else {
                // 替换为指定级别标题
                const newPrefix = '#'.repeat(level) + ' ';
                if (currentLevel > 0) {
                    doc.replaceRange(newPrefix, { line: cursor.line, ch: 0 }, { line: cursor.line, ch: currentLevel + 1 });
                } else {
                    doc.replaceRange(newPrefix, { line: cursor.line, ch: 0 });
                }
            }
            // 将光标移至行尾
            doc.setCursor(cursor.line, doc.getLine(cursor.line).length);
        });
    },

    // 无序列表
    unorderedList() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            const cursor = doc.getCursor();
            const line = doc.getLine(cursor.line);

            if (line.trim() === '') {
                // 空行：直接插入列表标记
                doc.replaceRange('- ', cursor);
                doc.setCursor(cursor.line, cursor.ch + 2);
            } else if (!line.match(/^(\s*)[-*+]\s/)) {
                // 非列表行：转换为列表
                doc.replaceRange('- ', { line: cursor.line, ch: 0 });
                doc.setCursor(cursor.line, cursor.ch + 2);
            } else {
                // 已是列表行：移除列表标记
                const indentMatch = line.match(/^(\s*)/);
                const indent = indentMatch ? indentMatch[1] : '';
                doc.replaceRange(indent, { line: cursor.line, ch: 0 }, { line: cursor.line, ch: indent.length + 2 });
            }
        });
    },

    // 有序列表
    orderedList() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            const cursor = doc.getCursor();
            const line = doc.getLine(cursor.line);

            if (line.trim() === '') {
                // 空行：直接插入列表标记
                doc.replaceRange('1. ', cursor);
                doc.setCursor(cursor.line, cursor.ch + 3);
            } else if (!line.match(/^\d+\.\s/)) {
                // 非列表行：转换为列表
                doc.replaceRange('1. ', { line: cursor.line, ch: 0 });
                doc.setCursor(cursor.line, cursor.ch + 3);
            } else {
                // 已是列表行：移除列表标记
                const indentMatch = line.match(/^(\s*)/);
                const indent = indentMatch ? indentMatch[1] : '';
                doc.replaceRange(indent, { line: cursor.line, ch: 0 }, { line: cursor.line, ch: indent.length + 3 });
            }
        });
    },

    // 插入链接
    insertLink() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            const url = prompt('请输入链接地址：', 'https://');

            if (url !== null) {
                if (selection) {
                    doc.replaceSelection(`[${selection}](${url})`);
                } else {
                    const cursor = doc.getCursor();
                    doc.replaceRange(`[链接文本](${url})`, cursor);
                    doc.setSelection(
                        { line: cursor.line, ch: cursor.ch + 1 },
                        { line: cursor.line, ch: cursor.ch + 5 }
                    );
                }
            }
        });
    },

    // 插入任务列表
    insertTaskList() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            const cursor = doc.getCursor();
            const line = doc.getLine(cursor.line);

            if (line.trim() === '') {
                // 空行插入未完成任务
                doc.replaceRange('- [ ] ', cursor);
                doc.setCursor(cursor.line, cursor.ch + 6);
            } else if (!line.match(/^(\s*)- \[.\] /)) {
                // 非任务行，插入未完成任务
                doc.replaceRange('- [ ] ', { line: cursor.line, ch: 0 });
                doc.setCursor(cursor.line, cursor.ch + 6);
            } else {
                // 已是任务行，切换完成/未完成
                if (line.match(/- \[ \] /)) {
                    doc.replaceRange('- [x] ', { line: cursor.line, ch: 0 }, { line: cursor.line, ch: 6 });
                } else {
                    doc.replaceRange('- [ ] ', { line: cursor.line, ch: 0 }, { line: cursor.line, ch: 6 });
                }
            }
        });
    },
    insertInlineCode() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            if (selection) {
                doc.replaceSelection('`' + selection + '`');
            } else {
                const cursor = doc.getCursor();
                doc.replaceRange('``', cursor);
                doc.setCursor(cursor.line, cursor.ch + 1);
            }
        });
    },

    insertImage() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            const url = prompt('请输入图片链接：', 'https://');
            if (url !== null) {
                if (selection) {
                    doc.replaceSelection(`![${selection}](${url})`);
                } else {
                    const cursor = doc.getCursor();
                    doc.replaceRange('![图片描述](' + url + ')', cursor);
                    // 光标选中"图片描述"
                    doc.setSelection(
                        { line: cursor.line, ch: cursor.ch + 2 },
                        { line: cursor.line, ch: cursor.ch + 6 }
                    );
                }
            }
        });
    },
    insertBlockquote() {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const doc = cm.getDoc();
            const cursor = doc.getCursor();
            const line = doc.getLine(cursor.line);

            if (line.trim() === '') {
                // 空行直接插入
                doc.replaceRange('> ', cursor);
                doc.setCursor(cursor.line, cursor.ch + 2);
            } else if (!line.match(/^>\s/)) {
                // 非引用行，添加引用
                doc.replaceRange('> ', { line: cursor.line, ch: 0 });
                doc.setCursor(cursor.line, cursor.ch + 2);
            } else {
                // 已是引用行，移除引用
                doc.replaceRange('', { line: cursor.line, ch: 0 }, { line: cursor.line, ch: 2 });
            }
        });
    },

    insertQuoteByPaste(content: string) {
        // @ts-ignore
        this.insertMarkdown((cm, selection) => {
            const quotedContent = content
                    .split('\n')
                    .map(line => `> ${line}`)
                    .join('\n') + '\n';
            // 创建粘贴事件
            const clipboardData = new DataTransfer();
            clipboardData.setData('text/plain', quotedContent + '\n');

            const pasteEvent = new ClipboardEvent('paste', {
                clipboardData,
                bubbles: true,
                cancelable: true
            });

            // 让编辑器获得焦点
            // @ts-ignore
            cm.focus();

            // 触发粘贴事件
            // @ts-ignore
            cm.getInputField().dispatchEvent(pasteEvent);

        });
    }
});