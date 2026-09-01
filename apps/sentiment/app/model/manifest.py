import hashlib
import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True, slots=True)
class ReleaseManifest:
    model_name: str
    model_version: str
    preprocessing_version: str
    contract_version: str
    model_file: str
    vocabulary_file: str
    checksums: dict[str, str]
    labels: tuple[str, str, str] = ("POSITIVE", "NEGATIVE", "NEUTRAL")

    @classmethod
    def load(cls, bundle: Path) -> "ReleaseManifest":
        payload = json.loads((bundle / "manifest.json").read_text(encoding="utf-8"))
        manifest = cls(payload["modelName"], payload["modelVersion"], payload["preprocessingVersion"],
                       payload["contractVersion"], payload["modelFile"], payload["vocabularyFile"], payload["checksums"], tuple(payload["labels"]))
        if manifest.contract_version != "sentiment-v1": raise ValueError("Unsupported contract version")
        if manifest.labels != ("POSITIVE", "NEGATIVE", "NEUTRAL"): raise ValueError("Unsupported class order")
        for name, expected in manifest.checksums.items():
            actual = "sha256:" + hashlib.sha256((bundle / name).read_bytes()).hexdigest()
            if actual != expected: raise ValueError(f"Checksum mismatch: {name}")
        return manifest
