from decimal import Decimal, ROUND_HALF_EVEN
from pathlib import Path
from .preprocessing import encode_article
from .protocol import Inference
from .tokenizer import FrozenWhitespaceTokenizer


class MultiChannelEngine:
    LABELS = ("POSITIVE", "NEGATIVE", "NEUTRAL")

    def __init__(self, model, tokenizer: FrozenWhitespaceTokenizer):
        self._model, self._tokenizer = model, tokenizer

    @classmethod
    def load(cls, model_path: Path, tokenizer: FrozenWhitespaceTokenizer):
        # Deliberately lazy: importing this module must remain lightweight and live.
        import tensorflow as tf
        return cls(tf.keras.models.load_model(model_path, compile=False), tokenizer)

    def warm_up(self) -> None: self.analyze("startup", "check")

    def analyze(self, title: str, content: str) -> Inference:
        import numpy as np
        encoded = np.asarray([encode_article(self._tokenizer, title, content)], dtype="int32")
        count = len(getattr(self._model, "inputs", [None]))
        raw = self._model.predict(encoded if count == 1 else [encoded] * count, verbose=0)
        probabilities = [Decimal(str(value)) for value in raw[0]]
        if len(probabilities) != 3 or any(not value.is_finite() or value < 0 or value > 1 for value in probabilities):
            raise ValueError("Model returned invalid probabilities")
        winner = max(range(3), key=lambda index: (probabilities[index], -index))
        quantize = lambda value: value.quantize(Decimal("0.0000000001"), rounding=ROUND_HALF_EVEN)
        return Inference(self.LABELS[winner], quantize(probabilities[winner]), quantize(probabilities[0] - probabilities[1]))
