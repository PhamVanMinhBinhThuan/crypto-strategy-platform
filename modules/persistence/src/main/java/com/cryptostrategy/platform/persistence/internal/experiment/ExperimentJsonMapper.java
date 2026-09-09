package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyComponentSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import com.cryptostrategy.platform.persistence.internal.search.SearchDefinitionJsonMapper;

public class ExperimentJsonMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SearchDefinitionJsonMapper searchDefinitions = new SearchDefinitionJsonMapper();

    public String writeSearchConfig(Map<String, Object> value) {
        return searchDefinitions.writeSearchConfig(value);
    }

    public Map<String, Object> readSearchConfig(String value) {
        return searchDefinitions.readSearchConfig(value);
    }

    public String writeCandidateDefinition(Map<String, Object> value) {
        return searchDefinitions.writeCandidateDefinition(value);
    }

    public Map<String, Object> readCandidateDefinition(String value) {
        return searchDefinitions.readCandidateDefinition(value);
    }

    public String writeJson(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to serialize JSON", exception); }
    }

    public Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to parse JSON map", exception); }
    }

    public String writeDatasetProvenance(DatasetProvenanceSnapshot value) {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("datasetVersionId", value.datasetVersionId().value()); map.put("version", value.version());
        map.put("checksum", value.checksum()); map.put("provider", value.provider()); map.put("tradingPair", value.tradingPair());
        map.put("timeframe", value.timeframe()); map.put("normalizationVersion", value.normalizationVersion());
        map.put("rangeStart", value.rangeStart().toString()); map.put("rangeEnd", value.rangeEnd().toString());
        map.put("candleCount", value.candleCount()); return writeJson(map);
    }

    public DatasetProvenanceSnapshot readDatasetProvenance(String json) {
        Map<String,Object> map=readMap(json);
        return new DatasetProvenanceSnapshot(new com.cryptostrategy.platform.domain.api.market.DatasetVersionId(text(map,"datasetVersionId")),
                text(map,"version"),text(map,"checksum"),text(map,"provider"),text(map,"tradingPair"),text(map,"timeframe"),
                text(map,"normalizationVersion"),Instant.parse(text(map,"rangeStart")),Instant.parse(text(map,"rangeEnd")),number(map,"candleCount").longValue());
    }

    public String writeStrategyProvenance(StrategyProvenanceSnapshot value) {
        Map<String,Object> map=new LinkedHashMap<>(); map.put("kind",value.kind().name());
        map.put("parameters",parameterMap(value.parameters())); map.put("strategyFingerprint",value.strategyFingerprint());
        value.sourceUserStrategyVersionId().ifPresent(id->map.put("sourceUserStrategyVersionId",id.value()));
        value.singleStrategy().ifPresent(reference->map.put("singleStrategy",referenceMap(reference)));
        value.compositePolicyId().ifPresent(id->map.put("compositePolicyId",id.value()));
        value.compositePolicyVersion().ifPresent(version->map.put("compositePolicyVersion",version.toString()));
        map.put("components",value.components().stream().map(component->{Map<String,Object> item=new LinkedHashMap<>();
            item.put("strategyReference",referenceMap(component.strategyReference())); item.put("parameters",parameterMap(component.parameters())); return item;}).toList());
        return writeJson(map);
    }

    @SuppressWarnings("unchecked")
    public StrategyProvenanceSnapshot readStrategyProvenance(String json) {
        Map<String,Object> map=readMap(json); StrategyKind kind=StrategyKind.valueOf(text(map,"kind"));
        StrategyParameterSet parameters=parameters((Map<String,Object>)map.getOrDefault("parameters",Map.of()));
        Optional<UserStrategyVersionId> source=Optional.ofNullable((String)map.get("sourceUserStrategyVersionId")).map(UserStrategyVersionId::new);
        String fingerprint=text(map,"strategyFingerprint");
        if(kind==StrategyKind.SINGLE) return StrategyProvenanceSnapshot.single(reference((Map<String,Object>)map.get("singleStrategy")),parameters,source,fingerprint);
        List<StrategyComponentSnapshot> components=new ArrayList<>();
        for(Object raw:(List<Object>)map.getOrDefault("components",List.of())){Map<String,Object> item=(Map<String,Object>)raw;
            components.add(new StrategyComponentSnapshot(reference((Map<String,Object>)item.get("strategyReference")),parameters((Map<String,Object>)item.get("parameters"))));}
        return StrategyProvenanceSnapshot.composite(new CombinationPolicyId(text(map,"compositePolicyId")),
                SemanticVersion.parse(text(map,"compositePolicyVersion")),parameters,components,source,fingerprint);
    }

    private static Map<String,Object> referenceMap(StrategyReference reference){return Map.of("strategyVersionId",reference.strategyVersionId().value(),
            "pluginId",reference.pluginId().value(),"implementationVersion",reference.implementationVersion().toString());}
    private static StrategyReference reference(Map<String,Object> map){return new StrategyReference(new StrategyVersionId(text(map,"strategyVersionId")),
            new StrategyPluginId(text(map,"pluginId")),SemanticVersion.parse(text(map,"implementationVersion")));}
    private static Map<String,Object> parameterMap(StrategyParameterSet set){Map<String,Object> result=new TreeMap<>();set.values().forEach((name,value)->result.put(name,Map.of("type",value.type().name(),"value",value.canonicalText())));return result;}
    @SuppressWarnings("unchecked") private static StrategyParameterSet parameters(Map<String,Object> map){Map<String,StrategyParameterValue> result=new TreeMap<>();map.forEach((name,raw)->{Map<String,Object> item=(Map<String,Object>)raw;ParameterType type=ParameterType.valueOf(text(item,"type"));String value=text(item,"value");result.put(name,switch(type){case INTEGER->new StrategyParameterValue.IntegerValue(Long.parseLong(value));case DECIMAL->new StrategyParameterValue.DecimalValue(new BigDecimal(value));case BOOLEAN->new StrategyParameterValue.BooleanValue(Boolean.parseBoolean(value));case TEXT->new StrategyParameterValue.TextValue(value);case ENUM->new StrategyParameterValue.EnumValue(value);});});return StrategyParameterSet.of(result);}
    private static String text(Map<String,Object> map,String name){Object value=map.get(name);if(value==null||value.toString().isBlank())throw new IllegalStateException("Missing JSON field: "+name);return value.toString();}
    private static Number number(Map<String,Object> map,String name){Object value=map.get(name);if(value instanceof Number number)return number;throw new IllegalStateException("Invalid numeric JSON field: "+name);}
}
