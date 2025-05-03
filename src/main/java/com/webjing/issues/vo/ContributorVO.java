package com.webjing.issues.vo;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import run.halo.app.core.extension.User;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月10日 14:48
 */
@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class ContributorVO {

    private String name;

    private String avatar;

    private String bio;

    private String displayName;

    private String email;

    public static ContributorVO from(User user) {
        return builder().name(user.getMetadata().getName())
            .displayName(user.getSpec().getDisplayName())
            .avatar(user.getSpec().getAvatar())
            .bio(user.getSpec().getBio())
            .email(user.getSpec().getEmail())
            .build();
    }
}
