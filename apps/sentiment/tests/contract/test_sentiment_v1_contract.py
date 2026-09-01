import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker
from app.api.schemas.sentiment import AnalyzeRequest, AnalyzeSuccess

ROOT = Path(__file__).parents[4] / "specs" / "008-news-sentiment" / "contracts" / "sentiment-v1"


def load(relative): return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def validate(schema, instance): Draft202012Validator(load(schema), format_checker=FormatChecker()).validate(instance)


def test_shared_valid_request_and_success_match_schema_and_pydantic():
    request=load("fixtures/valid-request.json"); success=load("fixtures/valid-success.json")
    validate("analyze-request.schema.json",request); validate("analyze-success.schema.json",success)
    AnalyzeRequest.model_validate(request); AnalyzeSuccess.model_validate(success)


def test_schemas_are_closed_and_scores_are_exact_decimal_strings():
    request=load("fixtures/valid-request.json"); request["unknown"]=True
    errors=list(Draft202012Validator(load("analyze-request.schema.json")).iter_errors(request))
    assert any(error.validator=="additionalProperties" for error in errors)
    success=load("fixtures/valid-success.json"); success["confidence"]=0.82
    assert list(Draft202012Validator(load("analyze-success.schema.json")).iter_errors(success))
