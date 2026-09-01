from datetime import datetime
from decimal import Decimal
from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, field_serializer, field_validator

ULID = r"^[0-9A-HJKMNP-TV-Z]{26}$"
HASH = r"^sha256:[0-9a-f]{64}$"


class AnalyzeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    requestId: str = Field(pattern=ULID)
    newsId: str = Field(pattern=ULID)
    title: str = Field(min_length=1, max_length=1000)
    content: str = Field(min_length=1, max_length=100000)
    language: Literal["en"]
    contentHash: str = Field(pattern=HASH)
    contractVersion: Literal["sentiment-v1"]
    modelName: str = Field(min_length=1, max_length=200)
    modelVersion: str = Field(min_length=1, max_length=200)
    preprocessingVersion: str = Field(min_length=1, max_length=200)


class AnalyzeSuccess(BaseModel):
    model_config = ConfigDict(extra="forbid")
    requestId: str = Field(pattern=ULID)
    newsId: str = Field(pattern=ULID)
    language: Literal["en"]
    contentHash: str = Field(pattern=HASH)
    contractVersion: Literal["sentiment-v1"]
    modelName: str
    modelVersion: str
    preprocessingVersion: str
    label: Literal["POSITIVE", "NEUTRAL", "NEGATIVE"]
    confidence: Decimal
    polarityScore: Decimal
    analyzedAt: datetime

    @field_validator("confidence")
    @classmethod
    def confidence_range(cls, value: Decimal) -> Decimal:
        if not value.is_finite() or not Decimal(0) <= value <= Decimal(1): raise ValueError("confidence out of range")
        return value

    @field_validator("polarityScore")
    @classmethod
    def polarity_range(cls, value: Decimal) -> Decimal:
        if not value.is_finite() or not Decimal(-1) <= value <= Decimal(1): raise ValueError("polarityScore out of range")
        return value

    @field_serializer("confidence", "polarityScore")
    def decimal_string(self, value: Decimal) -> str:
        rendered = format(value, "f").rstrip("0").rstrip(".")
        return rendered if rendered and rendered != "-0" else "0"

    @field_serializer("analyzedAt")
    def utc_time(self, value: datetime) -> str:
        return value.isoformat().replace("+00:00", "Z")
