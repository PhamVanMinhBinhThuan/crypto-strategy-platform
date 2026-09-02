package com.cryptostrategy.platform.persistence.internal.strategy;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
class JdbcUserStrategyStoreTest {
    @Test void everyPrivateSqlUsesOwnerAndPortHasNoRawFindById(){assertTrue(StrategySql.FIND_ROOT.contains("owner_user_id=?"));assertTrue(StrategySql.FIND_VERSION.contains("owner_user_id=?"));assertTrue(StrategySql.FIND_LATEST_VERSION.contains("owner_user_id=?"));assertTrue(StrategySql.LIST_ROOTS.contains("owner_user_id=?"));assertFalse(Arrays.stream(com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore.class.getMethods()).map(Method::getName).anyMatch("findById"::equals));}
}
