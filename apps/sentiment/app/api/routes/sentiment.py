from datetime import datetime, timezone
from fastapi import APIRouter, Depends, Request
from ..schemas.sentiment import AnalyzeRequest, AnalyzeSuccess
from ...core.auth import authenticate
from ...core.errors import ServiceError
from ...model.runtime_state import RuntimeState


def routes(settings) -> APIRouter:
    router = APIRouter()

    @router.post("/api/v1/sentiment/analyze", response_model=AnalyzeSuccess, dependencies=[Depends(authenticate(settings))])
    async def analyze(payload: AnalyzeRequest, request: Request) -> AnalyzeSuccess:
        runtime = request.app.state.runtime
        if runtime.state is not RuntimeState.READY:
            raise ServiceError("NOT_READY", "Sentiment model is not ready", True, 503, payload.requestId)
        manifest = runtime.manifest
        supplied = (payload.modelName, payload.modelVersion, payload.preprocessingVersion, payload.contractVersion)
        expected = (manifest.model_name, manifest.model_version, manifest.preprocessing_version, manifest.contract_version)
        if supplied != expected:
            raise ServiceError("UNSUPPORTED_RELEASE", "Requested model release is not served", False, 422, payload.requestId)
        try: inference = await runtime.analyze(payload.title, payload.content)
        except RuntimeError as error:
            code = "CAPACITY_EXCEEDED" if str(error) == "CAPACITY_EXCEEDED" else "NOT_READY"
            raise ServiceError(code, "Inference capacity is exhausted" if code == "CAPACITY_EXCEEDED" else "Sentiment model is not ready", True, 503, payload.requestId) from error
        except Exception as error:
            raise ServiceError("INFERENCE_FAILED", "Sentiment inference failed", True, 503, payload.requestId) from error
        return AnalyzeSuccess(requestId=payload.requestId, newsId=payload.newsId, language=payload.language,
            contentHash=payload.contentHash, contractVersion=payload.contractVersion, modelName=payload.modelName,
            modelVersion=payload.modelVersion, preprocessingVersion=payload.preprocessingVersion,
            label=inference.label, confidence=inference.confidence, polarityScore=inference.polarity_score,
            analyzedAt=datetime.now(timezone.utc))
    return router

