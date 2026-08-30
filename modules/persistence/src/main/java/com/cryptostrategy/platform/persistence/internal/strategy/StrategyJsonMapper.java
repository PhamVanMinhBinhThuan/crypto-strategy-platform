package com.cryptostrategy.platform.persistence.internal.strategy;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class StrategyJsonMapper {
    private final ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    public String descriptor(StrategyDescriptor descriptor) { return write(descriptor); }
    public String canonical(Object value) { return write(value); }
    public String parameters(StrategyParameterSet parameters) {
        ObjectNode root=mapper.createObjectNode(); parameters.values().forEach((name,value)->{ObjectNode item=root.putObject(name);item.put("type",value.type().name());item.put("value",value.canonicalText());}); return write(root);
    }
    public StrategyParameterSet readParameters(String json) {
        try { JsonNode root=mapper.readTree(json); Map<String,StrategyParameterValue> values=new TreeMap<>(); Iterator<Map.Entry<String,JsonNode>> fields=root.properties().iterator();
            while(fields.hasNext()){Map.Entry<String,JsonNode> field=fields.next();String type=field.getValue().path("type").asText();String value=field.getValue().path("value").asText();values.put(field.getKey(),switch(type){case "INTEGER"->new StrategyParameterValue.IntegerValue(Long.parseLong(value));case "DECIMAL"->new StrategyParameterValue.DecimalValue(new BigDecimal(value));case "BOOLEAN"->new StrategyParameterValue.BooleanValue(Boolean.parseBoolean(value));case "TEXT"->new StrategyParameterValue.TextValue(value);case "ENUM"->new StrategyParameterValue.EnumValue(value);default->throw new IllegalArgumentException("Unknown parameter type");});} return StrategyParameterSet.of(values);
        } catch (JsonProcessingException|IllegalArgumentException exception) { throw new StrategyException(StrategyErrorCode.INTEGRITY_ERROR,"Invalid stored Strategy parameters",exception); }
    }
    private String write(Object value){try{return mapper.writeValueAsString(value);}catch(JsonProcessingException exception){throw new StrategyException(StrategyErrorCode.INTEGRITY_ERROR,"Cannot serialize Strategy value",exception);}}
}
