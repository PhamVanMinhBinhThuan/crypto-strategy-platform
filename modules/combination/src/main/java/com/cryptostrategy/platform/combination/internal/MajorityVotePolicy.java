package com.cryptostrategy.platform.combination.internal;
import com.cryptostrategy.platform.combination.api.CombinationPolicy;
import com.cryptostrategy.platform.combination.api.CombinationPolicyReference;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
public final class MajorityVotePolicy implements CombinationPolicy {
    private static final CombinationPolicyReference REFERENCE=new CombinationPolicyReference(new CombinationPolicyId("majority-vote"),new SemanticVersion(1,0,0));
    @Override public CombinationPolicyReference reference(){return REFERENCE;}
    @Override public StrategySignal combine(List<StrategyDecision> decisions){
        if(decisions.size()<2) throw new IllegalArgumentException("Majority vote needs two decisions");
        Map<StrategySignal,Integer> counts=new EnumMap<>(StrategySignal.class); for(StrategySignal signal:StrategySignal.values())counts.put(signal,0);
        decisions.forEach(decision->counts.compute(decision.signal(),(key,value)->value+1));
        int max=counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<StrategySignal> winners=counts.entrySet().stream().filter(entry->entry.getValue()==max).map(Map.Entry::getKey).toList();
        return winners.size()==1?winners.getFirst():StrategySignal.HOLD;
    }
}
