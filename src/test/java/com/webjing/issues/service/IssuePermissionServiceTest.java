package com.webjing.issues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

class IssuePermissionServiceTest {
    private final RoleService roles = mock(RoleService.class);
    private final IssuePermissionService permissions = new IssuePermissionService(roles);

    @Test
    void ownerAndAssigneeCanOperateWithoutManagementRole() {
        var user = new UsernamePasswordAuthenticationToken("alice", "", List.of());
        assertThat(permissions.canChangeStatus("alice", null, user).block()).isTrue();
        assertThat(permissions.canChangeStatus("bob", List.of("alice"), user).block()).isTrue();
    }

    @Test
    void managerAndSuperRoleCanOperateOtherUsersIssues() {
        when(roles.joint(any(), any())).thenAnswer(call -> {
            java.util.Collection<String> actual = call.getArgument(0);
            return Mono.just(actual.contains("role-template-issues-manage") || actual.contains("super-role"));
        });
        for (String role : List.of("role-template-issues-manage", "super-role")) {
            var user = new UsernamePasswordAuthenticationToken("manager", "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            assertThat(permissions.canChangeStatus("someone-else", null, user).block()).isTrue();
        }
        var other = new UsernamePasswordAuthenticationToken("other", "", List.of());
        assertThat(permissions.canChangeStatus("alice", null, other).block()).isFalse();
    }

    @Test
    void anonymousAndMissingContextsCannotOperate() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_anonymous")));
        assertThat(permissions.canChangeStatus("anonymousUser", List.of(), anonymous).block()).isFalse();
        when(roles.getCurrentUser()).thenReturn(Mono.empty());
        assertThat(permissions.canManage().block()).isFalse();
        assertThat(permissions.canChangeStatus("alice", null).block()).isFalse();
    }
}
