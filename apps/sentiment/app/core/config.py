from pathlib import Path
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="SENTIMENT_", extra="forbid")
    service_token: str = Field(min_length=16)
    bundle_path: Path = Path("/opt/sentiment/bundle")
    load_timeout_seconds: float = Field(default=120.0, gt=0)
    max_concurrency: int = Field(default=4, ge=1, le=64)
    max_request_bytes: int = Field(default=262_144, ge=1024)

