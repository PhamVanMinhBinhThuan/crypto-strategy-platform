from fastapi import APIRouter, Request, Response
from ..schemas.health import HealthResponse
from ...model.runtime_state import RuntimeState

router = APIRouter()


@router.get("/health/live", response_model=HealthResponse, response_model_exclude_none=True)
async def live() -> HealthResponse:
    return HealthResponse(status="LIVE")


@router.get("/health/ready", response_model=HealthResponse, response_model_exclude_none=True)
async def ready(request: Request, response: Response) -> HealthResponse:
    runtime = request.app.state.runtime
    if runtime.state is RuntimeState.READY:
        manifest = runtime.manifest
        return HealthResponse(status="READY", contractVersion=manifest.contract_version, modelVersion=manifest.model_version)
    response.status_code = 503
    return HealthResponse(status=runtime.state.value)
