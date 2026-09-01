import hmac
from fastapi import Header
from .config import Settings
from .errors import ServiceError


def authenticate(settings: Settings):
    async def dependency(authorization: str | None = Header(default=None)) -> None:
        prefix = "Bearer "
        supplied = authorization[len(prefix):] if authorization and authorization.startswith(prefix) else ""
        if not hmac.compare_digest(supplied, settings.service_token):
            raise ServiceError("UNAUTHORIZED", "Authentication failed", False, 401)
    return dependency

