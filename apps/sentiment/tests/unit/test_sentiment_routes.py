from decimal import Decimal
from fastapi.testclient import TestClient
from app.core.config import Settings
from app.main import create_app
from app.model.manifest import ReleaseManifest
from app.model.protocol import Inference
from app.model.runtime_state import RuntimeState

TOKEN = "test-service-token-at-least-16"


class FakeRuntime:
    state = RuntimeState.READY
    manifest = ReleaseManifest("multichannel-english", "multichannel-english-1.0.0",
                               "multichannel-whitespace-en-1", "sentiment-v1", "model.keras", "vocabulary.json", {})
    async def analyze(self, _title, _content): return Inference("POSITIVE", Decimal("0.82"), Decimal("0.64"))


def request_payload():
    return {
        "requestId": "01K4A000000000000000000001", "newsId": "01K4A000000000000000000002",
        "title": "Bitcoin rises", "content": "Java normalized this English article.", "language": "en",
        "contentHash": "sha256:" + "01" * 32, "contractVersion": "sentiment-v1",
        "modelName": "multichannel-english", "modelVersion": "multichannel-english-1.0.0",
        "preprocessingVersion": "multichannel-whitespace-en-1"
    }


def client(runtime=FakeRuntime()):
    return TestClient(create_app(Settings(service_token=TOKEN), runtime))


def test_analyze_echoes_provenance_and_canonical_decimals():
    response = client().post("/api/v1/sentiment/analyze", json=request_payload(), headers={"Authorization": f"Bearer {TOKEN}"})
    assert response.status_code == 200
    assert response.json()["confidence"] == "0.82"
    assert response.json()["polarityScore"] == "0.64"
    assert response.json()["contentHash"] == request_payload()["contentHash"]


def test_requires_service_token_and_exact_release():
    assert client().post("/api/v1/sentiment/analyze", json=request_payload()).status_code == 401
    payload = request_payload(); payload["modelVersion"] = "wrong"
    response = client().post("/api/v1/sentiment/analyze", json=payload, headers={"Authorization": f"Bearer {TOKEN}"})
    assert response.status_code == 422
    assert response.json()["code"] == "UNSUPPORTED_RELEASE"


def test_health_is_independent_of_inference_route():
    assert client().get("/health/live").json() == {"status": "LIVE"}
    ready = client().get("/health/ready")
    assert ready.status_code == 200 and ready.json()["status"] == "READY"
