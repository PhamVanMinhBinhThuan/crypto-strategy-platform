import json
import unicodedata
from pathlib import Path


class FrozenWhitespaceTokenizer:
    def __init__(self, vocabulary: dict[str, int], length: int = 400):
        if vocabulary.get("<PAD>") != 0 or vocabulary.get("<OOV>") != 1: raise ValueError("PAD/OOV IDs must be 0/1")
        self.vocabulary, self.length = dict(vocabulary), length

    @classmethod
    def load(cls, path: Path): return cls(json.loads(path.read_text(encoding="utf-8")))

    def encode(self, text: str) -> list[int]:
        tokens = unicodedata.normalize("NFC", text).lower().split()
        tokens = ["<TITLE_CONTENT>" if t == "<title_content>" else t for t in tokens]
        ids = [self.vocabulary.get(token, 1) for token in tokens][-self.length:]
        return [0] * (self.length - len(ids)) + ids

