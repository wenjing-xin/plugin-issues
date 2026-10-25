import DOMPurify from "dompurify";
import { marked } from "marked";

/** Supports Markdown and safe HTML without admitting scripts or Alpine directives. */
export function renderDescription(raw = ""): string {
    return DOMPurify.sanitize(marked.parse(raw, { async: false }), {
        ALLOW_DATA_ATTR: false,
        FORBID_TAGS: ["style", "form", "input", "button", "textarea", "select"],
    });
}
