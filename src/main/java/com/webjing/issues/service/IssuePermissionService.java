package com.webjing.issues.service;

import com.webjing.issues.util.AuthorityUtils;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Shared authorization for frontend controls and their mutation endpoints. */
@Component
@RequiredArgsConstructor
public class IssuePermissionService {
    private final RoleService roleService;

    public Mono<Boolean> canManage() {
        return roleService.getCurrentUser().flatMap(this::canManage).defaultIfEmpty(false);
    }

    public Mono<Boolean> canManage(Authentication user) {
        if (!isAuthenticated(user)) return Mono.just(false);
        return roleService.joint(AuthorityUtils.authoritiesToRoles(user.getAuthorities()),
            Set.of(AuthorityUtils.ISSUE_MESSAGE_MANAGEMENT_ROLE_NAME,
                AuthorityUtils.SUPER_ROLE_NAME));
    }

    public Mono<Boolean> canChangeStatus(String owner, Collection<String> assignees) {
        return roleService.getCurrentUser()
            .flatMap(user -> canChangeStatus(owner, assignees, user)).defaultIfEmpty(false);
    }

    public Mono<Boolean> canChangeStatus(String owner, Collection<String> assignees, Authentication user) {
        if (!isAuthenticated(user)) return Mono.just(false);
        if (user.getName().equals(owner) || (assignees != null && assignees.contains(user.getName()))) {
            return Mono.just(true);
        }
        return canManage(user);
    }

    private boolean isAuthenticated(Authentication user) {
        return user != null && user.isAuthenticated()
            && !(user instanceof AnonymousAuthenticationToken)
            && !"anonymousUser".equals(user.getName());
    }
}
