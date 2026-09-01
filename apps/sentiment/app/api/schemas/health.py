from pydantic import BaseModel, ConfigDict
from typing import Literal


class HealthResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    status: Literal["LIVE", "LOADING", "READY", "FAILED", "TIMED_OUT"]
    contractVersion: Literal["sentiment-v1"] | None = None
    modelVersion: str | None = None

