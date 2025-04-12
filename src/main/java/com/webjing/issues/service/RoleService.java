package com.webjing.issues.service;

import reactor.core.publisher.Mono;
import java.util.Collection;

/**
 * 功能描述
 *
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

}
