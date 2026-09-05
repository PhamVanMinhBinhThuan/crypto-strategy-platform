import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from .api.routes import health
from .api.routes.sentiment import routes
from .api.schemas.error import ErrorResponse
from .core.config import Settings
from .core.errors import ServiceError
from .model.runtime import ModelRuntime

logger = logging.getLogger(__name__)


def create_app(settings: Settings | None = None, runtime=None) -> FastAPI:
    settings = settings or Settings()
    owned_runtime = runtime or ModelRuntime(settings.bundle_path, settings.load_timeout_seconds, settings.max_concurrency)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.runtime = owned_runtime
        task = asyncio.create_task(owned_runtime.start()) if runtime is None else None
        yield
        if task and not task.done(): task.cancel()

    app = FastAPI(title="English Sentiment Service", version="sentiment-v1", lifespan=lifespan)
    app.state.runtime = owned_runtime
    app.include_router(health.router)
    app.include_router(routes(settings))

    @app.middleware("http")
    async def body_limit(request: Request, call_next):
        length = request.headers.get("content-length")
        if length and int(length) > settings.max_request_bytes:
            return JSONResponse(status_code=413, content=ErrorResponse(code="INVALID_REQUEST", message="Request body is too large", retryable=False).model_dump(exclude_none=True))
        return await call_next(request)

    @app.exception_handler(ServiceError)
    async def service_error(request: Request, error: ServiceError):
        logger.warning(
            "Sentiment request rejected path=%s code=%s status=%s retryable=%s",
            request.url.path,
            error.code,
            error.status_code,
            error.retryable,
        )
        body = ErrorResponse(requestId=error.request_id, code=error.code, message=error.message, retryable=error.retryable)
        return JSONResponse(status_code=error.status_code, content=body.model_dump(exclude_none=True))

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, error: RequestValidationError):
        violations = [
            {"location": ".".join(str(part) for part in violation["loc"]), "type": violation["type"]}
            for violation in error.errors()
        ]
        logger.warning(
            "Sentiment request rejected path=%s code=INVALID_REQUEST status=422 retryable=false violations=%s",
            request.url.path,
            violations,
        )
        body = ErrorResponse(code="INVALID_REQUEST", message="Request validation failed", retryable=False)
        return JSONResponse(status_code=422, content=body.model_dump(exclude_none=True))
    return app
