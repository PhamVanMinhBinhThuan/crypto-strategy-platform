from app.model.tokenizer import FrozenWhitespaceTokenizer


def test_whitespace_oov_pre_padding_and_pre_truncation_are_frozen():
    tokenizer = FrozenWhitespaceTokenizer({"<PAD>": 0, "<OOV>": 1, "hello": 2, "world": 3}, length=4)
    assert tokenizer.encode("  HELLO   world unknown ") == [0, 2, 3, 1]
    assert tokenizer.encode("unknown unknown hello world unknown") == [1, 2, 3, 1]
