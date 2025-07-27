package com.webjing.issues;

/**
 * 功能描述wo yong
 *
 * @author: webjing
 * @date: 2025年01月04日 21:29
 */
public enum Constant {
    ;
    public static final String GROUP = "issue.webjing.com";

    public static final String VERSION = "v1alpha1";

    public static final String ISSUE_DOCUMENT_TYPE = "issue.issue.webjing.com";

    public static final String HAS_NEW_ISSUE_ON_SUBJECT = "someone-publish-new-issue";

    public static final String HAS_NEW_ISSUE_COMMENT = "someone-comment-issue";

    public static final String HAS_NEW_REPLY_ISSUE_COMMENT = "someone-reply-issue-comment";

    public static final Integer SEARCH_DEFAULT_PAGE_SIZE = 200;

    /**
     * issue关闭通知原因
     */
    public static final String MANAGER_CLOSED_ISSUE = "manager-closed-issue";

    public static final String NEW_ISSUE_NOTIFIED_ANNO = "subscribe-new-issue-notified";

    public static final String NEW_ISSUE_COMMENT_NOTIFIED_ANNO = "subscribe-new-issue-comment-notified";

    public static final String CLOSED_ISSUE_NOTIFIED_ANNO = "subscribe-issue-closed-notified";

}
