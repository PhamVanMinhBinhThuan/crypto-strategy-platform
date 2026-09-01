from pydantic import BaseModel, ConfigDict, Field
from typing import Literal


class ErrorResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    requestId: str | None = Field(default=None, pattern=r"^[0-9A-HJKMNP-TV-Z]{26}$")
    code: Literal["INVALID_REQUEST", "UNAUTHORIZED", "UNSUPPORTED_LANGUAGE", "UNSUPPORTED_RELEASE", "NOT_READY", "CAPACITY_EXCEEDED", "INFERENCE_FAILED"]
    message: str = Field(min_length=1, max_length=300)
    retryable: bool

