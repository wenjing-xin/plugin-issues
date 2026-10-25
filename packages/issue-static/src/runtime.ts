import Alpine from "alpinejs";
import dropdown from "./alpine-data/dropdown";
import halo from "./alpine-data/halo";
import uiPermission from "./alpine-data/ui-permission";
import upvote from "./alpine-data/upvote";
import message from "./alpine-data/message";
import dateUtils from "./alpine-data/date";
import issueEditor from "./alpine-data/issue-editor";
import issueUpdateEditor from "./alpine-data/issue-update-editor";
import issueSubmit from "./alpine-data/issue-submit";
import postReference from "./alpine-data/post-reference";
import issueLabels from "./alpine-data/issue-labels";
import dicebear from "./alpine-data/dicebear";

/** Register only namespaced providers; a theme's Alpine instance retains ownership. */
export function mount() {
    const roots = Array.from(document.querySelectorAll<HTMLElement>("[data-piw-root]"))
        .filter(root => !root.dataset.piwMounted);
    if (!roots.length) return;
    const runtime = window.Alpine || Alpine;
    const ownsRuntime = !window.Alpine;
    runtime.data("piwPostReference", postReference);
    runtime.data("piwIssueLabels", issueLabels);
    runtime.data("piwApp", () => ({
        size: 32,
        avatarStyle: "adventurer",
        init() {
            this.size = Number(this.$el.dataset.avatarSize || 32);
            this.avatarStyle = this.$el.dataset.avatarStyle || "adventurer";
        },
    }));
    runtime.data("piwDropdown", dropdown);
    runtime.data("piwHalo", halo);
    runtime.data("piwUiPermission", uiPermission);
    runtime.data("piwUpvote", upvote);
    runtime.data("piwMessageBox", message);
    runtime.data("piwDateUtils", dateUtils);
    runtime.data("piwIssueEditor", issueEditor);
    runtime.data("piwIssueSubmit", issueSubmit);
    runtime.data("piwIssueUpdateEditor", issueUpdateEditor);
    runtime.data("piwDiceBearAvatar", dicebear);

    runtime.mutateDom(() => {
        roots.forEach(root => {
            // Themes may already have processed x-ignore. Let Alpine clean it up.
            runtime.destroyTree(root);
            root.removeAttribute("x-ignore");
            root.dataset.piwMounted = "true";
        });
    });
    if (ownsRuntime) {
        window.Alpine = runtime;
        runtime.start();
    } else {
        roots.forEach(root => runtime.initTree(root));
    }
}

// Deferred theme scripts finish before this event; the IIFE export is also ready.
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mount, { once: true });
} else {
    queueMicrotask(mount);
}
