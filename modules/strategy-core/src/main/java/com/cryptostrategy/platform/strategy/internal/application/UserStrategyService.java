package com.cryptostrategy.platform.strategy.internal.application;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyStatus;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionStatus;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.StrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.StrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategy;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyComponent;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import com.cryptostrategy.platform.strategy.api.model.user.command.ArchiveUserStrategyCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateNextStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateUserStrategyCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.PublishStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.query.GetUserStrategyQuery;
import com.cryptostrategy.platform.strategy.api.model.user.query.ResolveStrategySnapshotQuery;
import com.cryptostrategy.platform.strategy.api.model.user.query.StrategyCatalogPage;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyCatalog;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyPageRequest;
import com.cryptostrategy.platform.strategy.api.model.user.query.UserStrategyPage;
import com.cryptostrategy.platform.strategy.api.port.in.ArchiveUserStrategyUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.CreateUserStrategyUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.CreateUserStrategyVersionUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.GetUserStrategyUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.ListUsableStrategiesUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.PublishUserStrategyVersionUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.ResolveStrategySnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore;
import com.cryptostrategy.platform.strategy.internal.fingerprint.CanonicalStrategyEncoder;
import com.cryptostrategy.platform.strategy.internal.fingerprint.StrategyFingerprintV1;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class UserStrategyService implements UserStrategyApplication {
    private final StrategyRegistry registry; private final UserStrategyStore store; private final Clock clock;
    private final StrategyFingerprintV1 fingerprint = new StrategyFingerprintV1(); private final CanonicalStrategyEncoder encoder = new CanonicalStrategyEncoder();
    public UserStrategyService(StrategyRegistry registry, UserStrategyStore store, Clock clock) { this.registry=Objects.requireNonNull(registry); this.store=Objects.requireNonNull(store); this.clock=Objects.requireNonNull(clock); }
    @Override public UsableStrategyCatalog listUsableStrategies(UUID owner, UsableStrategyPageRequest request) {
        Objects.requireNonNull(owner); List<StrategyDescriptor> all=registry.listAvailable(); int offset=parseCursor(request.systemCursor());
        int end=Math.min(offset+request.systemPageSize(),all.size()); List<StrategyDescriptor> page=offset>=all.size()?List.of():all.subList(offset,end);
        Optional<String> next=end<all.size()?Optional.of(Integer.toString(end)):Optional.empty();
        var privateItems=store.listActive(owner,request.privatePageSize(),request.privateCursor());
        Optional<String> privateNext=privateItems.size()==request.privatePageSize()?Optional.of(privateItems.getLast().id().value()):Optional.empty();
        return new UsableStrategyCatalog(new StrategyCatalogPage(page,next), new UserStrategyPage(privateItems,privateNext));
    }
    @Override public UserStrategyVersion createUserStrategy(UUID owner, CreateUserStrategyCommand command) {
        Objects.requireNonNull(owner); Instant now=clock.instant(); StrategyDraftSource source=validate(command.source()); String print=fingerprint(source);
        UserStrategy root=new UserStrategy(UserStrategyId.generate(),owner,command.kind(),command.name(),command.description(),UserStrategyStatus.ACTIVE,Optional.empty(),now,now);
        UserStrategyVersion version=new UserStrategyVersion(UserStrategyVersionId.generate(),root.id(),1,command.kind(),source,UserStrategyVersionStatus.DRAFT,print,Optional.empty(),now);
        return store.create(root,version);
    }
    @Override public UserStrategyVersion createNextVersion(UUID owner, CreateNextStrategyVersionCommand command) {
        UserStrategy root=requireRoot(owner,command.userStrategyId()); if(root.status()!=UserStrategyStatus.ACTIVE) conflict("Archived Strategy");
        StrategyDraftSource source=validate(command.source()); Instant now=clock.instant(); UserStrategyVersion draft=new UserStrategyVersion(UserStrategyVersionId.generate(),root.id(),command.expectedLatestVersionNo()+1,root.kind(),source,UserStrategyVersionStatus.DRAFT,fingerprint(source),Optional.empty(),now);
        return store.createNext(owner,draft,command.expectedLatestVersionNo());
    }
    @Override public StrategySnapshot publish(UUID owner, PublishStrategyVersionCommand command) {
        UserStrategyVersion published=store.publish(owner,command.versionId(),command.expectedVersionNo(),clock.instant()); return snapshot(owner,published);
    }
    @Override public UserStrategy getUserStrategy(UUID owner, GetUserStrategyQuery query) { return requireRoot(owner,query.userStrategyId()); }
    @Override public StrategySnapshot resolveSnapshot(UUID owner, ResolveStrategySnapshotQuery query) { return store.resolvePublished(owner,query.versionId()).orElseThrow(UserStrategyService::notFound); }
    @Override public UserStrategy archive(UUID owner, ArchiveUserStrategyCommand command) { requireRoot(owner,command.userStrategyId()); return store.archive(owner,command.userStrategyId(),clock.instant()); }
    private StrategyDraftSource validate(StrategyDraftSource source) {
        if(source instanceof SingleStrategyDraftSource single){
            StrategyParameterSet resolved = registry.resolveParameters(single.strategyReference().pluginId(), single.strategyReference().implementationVersion(), single.parameters().values());
            return new SingleStrategyDraftSource(single.strategyReference(), resolved);
        }
        CompositeStrategyDraftSource composite=(CompositeStrategyDraftSource)source;
        if(!composite.policyId().value().equals("majority-vote")||!composite.policyVersion().toString().equals("1.0.0")) throw new StrategyException(StrategyErrorCode.UNSUPPORTED_VERSION,"Unsupported combination policy");
        List<UserStrategyComponent> resolvedComponents = composite.components().stream().map(component -> {
            StrategyParameterSet resolved = registry.resolveParameters(component.strategyReference().pluginId(), component.strategyReference().implementationVersion(), component.parameters().values());
            return new UserStrategyComponent(component.strategyReference(), resolved);
        }).toList();
        return new CompositeStrategyDraftSource(composite.policyId(), composite.policyVersion(), composite.policyParameters(), resolvedComponents);
    }
    private String fingerprint(StrategyDraftSource source) {
        if(source instanceof SingleStrategyDraftSource single) return fingerprint.single(single.strategyReference(),single.parameters());
        CompositeStrategyDraftSource composite=(CompositeStrategyDraftSource)source;
        List<byte[]> parts=composite.components().stream().map(component->encoder.encodeSingle(component.strategyReference(),component.parameters())).toList();
        return fingerprint.composite(composite.policyId()+"@"+composite.policyVersion(),parts);
    }
    private StrategySnapshot snapshot(UUID owner, UserStrategyVersion version) {
        if(version.status()!=UserStrategyVersionStatus.PUBLISHED) conflict("Version is not published");
        if(version.source() instanceof SingleStrategyDraftSource single) return new SingleStrategySnapshot(version.userStrategyId(),version.id(),version.versionNo(),owner,single,version.fingerprint());
        return new CompositeStrategySnapshot(version.userStrategyId(),version.id(),version.versionNo(),owner,(CompositeStrategyDraftSource)version.source(),version.fingerprint());
    }
    private UserStrategy requireRoot(UUID owner, UserStrategyId id){return store.findRoot(owner,id).orElseThrow(UserStrategyService::notFound);}
    private static int parseCursor(Optional<String> cursor){try{return cursor.map(Integer::parseInt).orElse(0);}catch(NumberFormatException exception){throw new IllegalArgumentException("Invalid cursor",exception);}}
    private static StrategyException notFound(){return new StrategyException(StrategyErrorCode.STRATEGY_NOT_FOUND,"Strategy not found");}
    private static void conflict(String message){throw new StrategyException(StrategyErrorCode.STRATEGY_CONFLICT,message);}
}
