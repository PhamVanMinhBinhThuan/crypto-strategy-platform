from dataclasses import dataclass


@dataclass(slots=True)
class ServiceError(Exception):
    code: str
    message: str
    retryable: bool
    status_code: int
    request_id: str | None = None

