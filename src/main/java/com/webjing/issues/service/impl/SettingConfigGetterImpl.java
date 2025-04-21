package com.webjing.issues.service.impl;

import com.webjing.issues.service.SettingConfigGetter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
@RequiredArgsConstructor
public class SettingConfigGetterImpl implements SettingConfigGetter {

    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<IssuesBasic> getIssuesBasic() {
        return settingFetcher.fetch(IssuesBasic.GROUP, IssuesBasic.class)
            .defaultIfEmpty(new IssuesBasic());
    }

}
