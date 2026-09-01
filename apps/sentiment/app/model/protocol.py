from dataclasses import dataclass
from decimal import Decimal
from typing import Protocol


@dataclass(frozen=True, slots=True)
class Inference:
    label: str
    confidence: Decimal
    polarity_score: Decimal


class InferenceEngine(Protocol):
    def analyze(self, title: str, content: str) -> Inference: ...
    def warm_up(self) -> None: ...

