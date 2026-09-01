package com.cryptostrategy.platform.evaluation.api.model;
import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
public record EvaluationResultId(String value) implements UlidIdentifier {
    public EvaluationResultId { value=Ulids.requireValid(value); }
    public static EvaluationResultId generate(){return new EvaluationResultId(Ulids.generate());}
}
