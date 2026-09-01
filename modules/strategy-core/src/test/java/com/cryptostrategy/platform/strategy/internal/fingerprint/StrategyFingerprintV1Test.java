package com.cryptostrategy.platform.strategy.internal.fingerprint;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyFingerprintV1Test {
    @Test void canonicalFingerprintIgnoresMapButRespectsCompositeOrder(){StrategyReference reference=new StrategyReference(new StrategyVersionId("01J00000000000000000000000"),new StrategyPluginId("fixture"),new SemanticVersion(1,0,0));StrategyFingerprintV1 value=new StrategyFingerprintV1();StrategyParameterSet first=StrategyParameterSet.of(Map.of("b",new StrategyParameterValue.DecimalValue(new BigDecimal("1.00")),"a",new StrategyParameterValue.IntegerValue(2)));StrategyParameterSet second=StrategyParameterSet.of(Map.of("a",new StrategyParameterValue.IntegerValue(2),"b",new StrategyParameterValue.DecimalValue(BigDecimal.ONE)));assertEquals(value.single(reference,first),value.single(reference,second));CanonicalStrategyEncoder encoder=new CanonicalStrategyEncoder();byte[] one=encoder.encodeSingle(reference,first);byte[] two="other".getBytes(java.nio.charset.StandardCharsets.UTF_8);assertNotEquals(value.composite("majority-vote@1.0.0",List.of(one,two)),value.composite("majority-vote@1.0.0",List.of(two,one)));}
}
