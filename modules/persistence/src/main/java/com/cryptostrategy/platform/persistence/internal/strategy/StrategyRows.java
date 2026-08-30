package com.cryptostrategy.platform.persistence.internal.strategy;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyStatus;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionStatus;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategy;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategySummary;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
public final class StrategyRows {
    private StrategyRows(){}
    public static UserStrategy root(ResultSet rs,int row) throws SQLException{return new UserStrategy(new UserStrategyId(rs.getString("user_strategy_id")),rs.getObject("owner_user_id",UUID.class),StrategyKind.valueOf(rs.getString("strategy_kind")),rs.getString("name"),rs.getString("description"),UserStrategyStatus.valueOf(rs.getString("status")),Optional.ofNullable(rs.getTimestamp("archived_at")).map(value->value.toInstant()),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant());}
    public static UserStrategySummary summary(ResultSet rs,int row)throws SQLException{return new UserStrategySummary(new UserStrategyId(rs.getString("user_strategy_id")),StrategyKind.valueOf(rs.getString("strategy_kind")),rs.getString("name"),rs.getString("description"),rs.getTimestamp("created_at").toInstant());}
    public static UserStrategyVersion singleVersion(ResultSet rs,StrategyJsonMapper json)throws SQLException{StrategyReference reference=new StrategyReference(new StrategyVersionId(rs.getString("strategy_version_id")),new StrategyPluginId(rs.getString("plugin_id")),SemanticVersion.parse(rs.getString("plugin_version")));return new UserStrategyVersion(new UserStrategyVersionId(rs.getString("user_strategy_version_id")),new UserStrategyId(rs.getString("user_strategy_id")),rs.getInt("version_no"),StrategyKind.SINGLE,new SingleStrategyDraftSource(reference,json.readParameters(rs.getString("parameters"))),UserStrategyVersionStatus.valueOf(rs.getString("lifecycle_status")),rs.getString("fingerprint"),Optional.ofNullable(rs.getTimestamp("published_at")).map(value->value.toInstant()),rs.getTimestamp("created_at").toInstant());}
}
