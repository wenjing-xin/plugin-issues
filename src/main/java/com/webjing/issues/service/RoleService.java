package com.webjing.issues.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import java.util.Collection;

/**
 * 用户角色认证相关
 * @author: webjing
 * @date: 2025年03月10日 11:42
 */
public interface RoleService {

    /**
     * verify whether the source role contains any role in the candidates.
     *
     * @param source the role to be verified
     * @param candidates the roles to be verified
     * @return <p>true if the source role contains any role in the candidates, otherwise false</p>
     */
    Mono<Boolean> joint(Collection<String> source, Collection<String> candidates);

    Mono<User> getContextUser();

    Mono<Authentication> getCurrentUser();
}
