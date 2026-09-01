from .tokenizer import FrozenWhitespaceTokenizer


def encode_article(tokenizer: FrozenWhitespaceTokenizer, title: str, content: str) -> list[int]:
    return tokenizer.encode(f"{title} <TITLE_CONTENT> {content}")

