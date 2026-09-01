#!/usr/bin/env python3
"""Offline-only builder/verifier for reviewed F-008 model inputs."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path

UPSTREAM = "fd1163a88d04e61e2b19a34e07da99e10acb6288"


def digest(path: Path) -> str: return "sha256:" + hashlib.sha256(path.read_bytes()).hexdigest()


def build(args) -> None:
    model, vocabulary, output = Path(args.model).resolve(), Path(args.vocabulary).resolve(), Path(args.output).resolve()
    if not model.is_file() or not vocabulary.is_file(): raise SystemExit("Reviewed model and vocabulary files are required")
    vocab = json.loads(vocabulary.read_text(encoding="utf-8"))
    if vocab.get("<PAD>") != 0 or vocab.get("<OOV>") != 1: raise SystemExit("Vocabulary must freeze PAD=0 and OOV=1")
    output.mkdir(parents=True, exist_ok=False)
    shutil.copyfile(model, output / "model.keras"); shutil.copyfile(vocabulary, output / "vocabulary.json")
    manifest = {"modelName": args.model_name, "modelVersion": args.model_version,
        "preprocessingVersion": "multichannel-whitespace-en-1", "contractVersion": "sentiment-v1",
        "modelFile": "model.keras", "vocabularyFile": "vocabulary.json", "labels": ["POSITIVE", "NEGATIVE", "NEUTRAL"], "sourceCommit": UPSTREAM,
        "trainingProvenance": args.training_provenance, "dependencies": {"tensorflow": args.tensorflow},
        "checksums": {"model.keras": digest(output / "model.keras"), "vocabulary.json": digest(output / "vocabulary.json")}}
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(digest(output / "manifest.json"))


def main():
    parser=argparse.ArgumentParser(); parser.add_argument("--model",required=True); parser.add_argument("--vocabulary",required=True)
    parser.add_argument("--output",required=True); parser.add_argument("--model-name",default="multichannel-english"); parser.add_argument("--model-version",required=True)
    parser.add_argument("--training-provenance",required=True); parser.add_argument("--tensorflow",default="2.19.0")
    build(parser.parse_args())


if __name__ == "__main__": main()
