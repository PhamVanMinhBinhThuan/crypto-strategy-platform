package com.cryptostrategy.platform.api.config;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateUserStrategyCommand;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class StrategyAuthorizationConfigurationTest {
    @Test void authenticatedContextIsTheOnlyOwnerSource(){UUID user=UUID.randomUUID();assertEquals(user,new AuthenticatedUserContext(user,java.time.Instant.MAX).userId());assertFalse(Arrays.stream(CreateUserStrategyCommand.class.getRecordComponents()).anyMatch(component->component.getName().toLowerCase().contains("owner")||component.getType()==UUID.class));}
}
