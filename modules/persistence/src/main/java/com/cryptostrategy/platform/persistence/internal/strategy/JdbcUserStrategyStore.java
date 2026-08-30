package com.cryptostrategy.platform.persistence.internal.strategy;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.StrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategy;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyComponent;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategySummary;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcUserStrategyStore implements UserStrategyStore {
    private static final String INSERT_COMPONENT="insert into strategy.user_strategy_component(user_strategy_version_id,position,strategy_version_id,parameters) values (?,?,?,?::jsonb)";
    private static final String FIND_COMPONENTS="select c.strategy_version_id,c.parameters::text,sv.plugin_id,sv.version from strategy.user_strategy_component c join strategy.strategy_version sv on sv.strategy_version_id=c.strategy_version_id where c.user_strategy_version_id=? order by c.position";
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final StrategyJsonMapper json; private final StrategyPersistenceExceptionTranslator errors;
    public JdbcUserStrategyStore(JdbcTemplate jdbc,TransactionTemplate transactions,StrategyJsonMapper json,StrategyPersistenceExceptionTranslator errors){this.jdbc=jdbc;this.transactions=transactions;this.json=json;this.errors=errors;}
    @Override public UserStrategyVersion create(UserStrategy root,UserStrategyVersion firstDraft){try{return transactions.execute(status->{insertRoot(root);insertVersion(firstDraft);return firstDraft;});}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public List<UserStrategySummary> listActive(UUID owner,int limit,Optional<String> cursor){try{String value=cursor.orElse(null);return jdbc.query(StrategySql.LIST_ROOTS,StrategyRows::summary,owner,value,value,owner,limit);}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public Optional<UserStrategy> findRoot(UUID owner,UserStrategyId id){try{return jdbc.query(StrategySql.FIND_ROOT,StrategyRows::root,owner,id.value()).stream().findFirst();}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public Optional<UserStrategyVersion> findVersion(UUID owner,UserStrategyVersionId id){try{return jdbc.query(StrategySql.FIND_VERSION,(rs,row)->mapVersion(rs,owner),owner,id.value()).stream().findFirst();}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public UserStrategyVersion createNext(UUID owner,UserStrategyVersion draft,int expected){try{return transactions.execute(status->{List<Integer> latest=jdbc.query(StrategySql.LATEST_VERSION,(rs,row)->rs.getInt(1),owner,draft.userStrategyId().value());if(latest.isEmpty()||latest.getFirst()!=expected)conflict();insertVersion(draft);return draft;});}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public UserStrategyVersion publish(UUID owner,UserStrategyVersionId id,int expected,Instant at){try{return transactions.execute(status->{int changed=jdbc.update(StrategySql.PUBLISH,Timestamp.from(at),owner,id.value(),expected);if(changed!=1)conflict();return findVersion(owner,id).orElseThrow(JdbcUserStrategyStore::notFound);});}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public UserStrategy archive(UUID owner,UserStrategyId id,Instant at){try{int changed=jdbc.update(StrategySql.ARCHIVE,Timestamp.from(at),Timestamp.from(at),owner,id.value());if(changed!=1)conflict();return findRoot(owner,id).orElseThrow(JdbcUserStrategyStore::notFound);}catch(DataAccessException exception){throw errors.translate(exception);}}
    @Override public Optional<StrategySnapshot> resolvePublished(UUID owner,UserStrategyVersionId id){return findVersion(owner,id).filter(version->version.publishedAt().isPresent()).map(version->{if(version.source() instanceof SingleStrategyDraftSource single)return new SingleStrategySnapshot(version.userStrategyId(),version.id(),version.versionNo(),owner,single,version.fingerprint());return new CompositeStrategySnapshot(version.userStrategyId(),version.id(),version.versionNo(),owner,(CompositeStrategyDraftSource)version.source(),version.fingerprint());});}
    private void insertRoot(UserStrategy root){jdbc.update(StrategySql.INSERT_ROOT,root.id().value(),root.ownerUserId(),root.kind().name(),root.name(),root.description(),Timestamp.from(root.createdAt()),Timestamp.from(root.updatedAt()));}
    private void insertVersion(UserStrategyVersion version){String strategyId=null,parameters="{}",policyId=null,policyVersion=null,policyParameters=null;if(version.source() instanceof SingleStrategyDraftSource single){strategyId=single.strategyReference().strategyVersionId().value();parameters=json.parameters(single.parameters());}else{CompositeStrategyDraftSource composite=(CompositeStrategyDraftSource)version.source();policyId=composite.policyId().value();policyVersion=composite.policyVersion().toString();policyParameters=json.parameters(composite.policyParameters());}jdbc.update(StrategySql.INSERT_VERSION,version.id().value(),version.userStrategyId().value(),version.versionNo(),version.kind().name(),strategyId,parameters,policyId,policyVersion,policyParameters,version.fingerprint(),Timestamp.from(version.createdAt()));if(version.source() instanceof CompositeStrategyDraftSource composite){int position=0;for(UserStrategyComponent component:composite.components())jdbc.update(INSERT_COMPONENT,version.id().value(),position++,component.strategyReference().strategyVersionId().value(),json.parameters(component.parameters()));}}
    private UserStrategyVersion mapVersion(java.sql.ResultSet rs,UUID owner)throws java.sql.SQLException{if("SINGLE".equals(rs.getString("strategy_kind")))return StrategyRows.singleVersion(rs,json);String id=rs.getString("user_strategy_version_id");List<UserStrategyComponent> components=jdbc.query(FIND_COMPONENTS,(component,row)->new UserStrategyComponent(new StrategyReference(new StrategyVersionId(component.getString("strategy_version_id")),new StrategyPluginId(component.getString("plugin_id")),SemanticVersion.parse(component.getString("version"))),json.readParameters(component.getString("parameters"))),id);CompositeStrategyDraftSource source=new CompositeStrategyDraftSource(new CombinationPolicyId(rs.getString("policy_id")),SemanticVersion.parse(rs.getString("policy_version")),json.readParameters(rs.getString("policy_parameters")),components);return new UserStrategyVersion(new UserStrategyVersionId(id),new UserStrategyId(rs.getString("user_strategy_id")),rs.getInt("version_no"),com.cryptostrategy.platform.strategy.api.model.StrategyKind.COMPOSITE,source,com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionStatus.valueOf(rs.getString("lifecycle_status")),rs.getString("fingerprint"),Optional.ofNullable(rs.getTimestamp("published_at")).map(value->value.toInstant()),rs.getTimestamp("created_at").toInstant());}
    private static void conflict(){throw new StrategyException(StrategyErrorCode.STRATEGY_CONFLICT,"Stale Strategy state");}
    private static StrategyException notFound(){return new StrategyException(StrategyErrorCode.STRATEGY_NOT_FOUND,"Strategy not found");}
}
