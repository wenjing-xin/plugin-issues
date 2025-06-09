package com.webjing.issues.finder.impl;

import com.webjing.issues.entity.IssueSubjectStats;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.finder.IssueSubjectFinder;
import com.webjing.issues.service.IssueSubjectService;
import com.webjing.issues.vo.ContributorVO;
import com.webjing.issues.vo.IssueSubjectVO;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.finders.Finder;
import java.util.List;

/**
 * @description:
 * @className: IssueSubjectFinderImpl
 * @author: webjing
 * @date: 2025年06月08日 10:54
 */
@Finder("IssueSubjectFinder")
@RequiredArgsConstructor
public class IssueSubjectFinderImpl implements IssueSubjectFinder {

    private final ReactiveExtensionClient client;

    private final IssueSubjectService issueSubjectService;

    @Override
    public Mono<IssueSubjectVO> get(String issueSubjectName) {
        return client.get(IssueSubject.class, issueSubjectName)
            .flatMap(this::getIssueSubjectVo);
    }

    @Override
    public Mono<IssueSubjectBasicInfo> getSubjectBasicInfo(String issueSubjectName) {
        return client.get(IssueSubject.class, issueSubjectName).map(issueSubject -> {
            IssueSubjectBasicInfo issueSubjectBasicInfo = new IssueSubjectBasicInfo(issueSubject.getMetadata().getName(),
                    issueSubject.getSpec().getSubjectType(), issueSubject.getSpec().getDisplayName());
            return issueSubjectBasicInfo;
        });
    }

    @Override
    public Mono<IssueSubjectStats> getSubjectStats(String subjectName) {
        return client.get(IssueSubject.class, subjectName)
            .flatMap(issueSubject -> issueSubjectService.fetchIssueSubjectStats(issueSubject.getMetadata().getName())
        );
    }

    private Mono<IssueSubjectVO> getIssueSubjectVo(@Nonnull IssueSubject issueSubject) {
        IssueSubjectVO issueSubjectVO = IssueSubjectVO.from(issueSubject);
        return Mono.just(issueSubjectVO)
            .flatMap(isv -> issueSubjectService.fetchIssueSubjectStats(issueSubjectVO.getMetadata().getName())
                .doOnNext(isv::setIssueSubjectStats)
                .thenReturn(isv)
            )
            .flatMap(isv -> {
                String owner = isv.getSpec().getOwner();
                return client.fetch(User.class, owner)
                    .map(ContributorVO::from)
                    .doOnNext(isv::setCreateOwner)
                    .thenReturn(isv);
            })
            .flatMap(isv -> setParticipateUsers(issueSubject.getSpec().getParticipateUsers(), isv))
            .defaultIfEmpty(issueSubjectVO);
    }

    /**
     * 查询参与用户信息
     * @param participateUsers
     * @param issueSubjectVO
     * @return
     */
    private Mono<IssueSubjectVO> setParticipateUsers(List<String> participateUsers, IssueSubjectVO issueSubjectVO){
        return Flux.fromIterable(participateUsers)
            .flatMap(participateUser -> client.fetch(User.class, participateUser)
                .map(user -> ContributorVO.from(user))
            )
            .collectList()
            .doOnNext(issueSubjectVO::setParticipateUsers)
            .thenReturn(issueSubjectVO);
    }


}
