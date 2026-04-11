package com.webjing.issues.entity;

import com.webjing.issues.extension.IssueTemplate;
import lombok.Data;
import java.util.List;

/**
 * @description:
 * @className: IssueTemplateRender
 * @author: webjing
 * @date: 2025年07月13日 15:18
 */
@Data
public class IssueTemplateRender {

    private String displayName;

    private List<IssueTemplate.TemplateField> components;

    private List<String> annotationFields;

}
