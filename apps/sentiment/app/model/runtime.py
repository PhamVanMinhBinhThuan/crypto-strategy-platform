import asyncio
import os
from concurrent.futures import Executor
from pathlib import Path
from threading import Lock
from typing import Callable
from .manifest import ReleaseManifest
from .multichannel_engine import MultiChannelEngine
from .protocol import Inference, InferenceEngine
from .runtime_state import RuntimeState
from .tokenizer import FrozenWhitespaceTokenizer


class ModelRuntime:
    def __init__(self, bundle: Path, timeout_seconds: float, capacity: int,
                 executor: Executor | None = None,
                 loader: Callable[[Path], tuple[ReleaseManifest, InferenceEngine]] | None = None,
                 fatal_exit: Callable[[int], None] = os._exit):
        self.bundle, self.timeout_seconds = bundle, timeout_seconds
        self._executor, self._loader, self._fatal_exit = executor, loader or self._load_bundle, fatal_exit
        self._state, self._manifest, self._engine = RuntimeState.LOADING, None, None
        self._state_lock, self._available_capacity = Lock(), capacity
        self._generation = object()

    @staticmethod
    def _load_bundle(bundle: Path):
        manifest = ReleaseManifest.load(bundle)
        tokenizer = FrozenWhitespaceTokenizer.load(bundle / manifest.vocabulary_file)
        engine = MultiChannelEngine.load(bundle / manifest.model_file, tokenizer)
        engine.warm_up()
        return manifest, engine

    @property
    def state(self): return self._state
    @property
    def manifest(self): return self._manifest

    async def start(self) -> None:
        generation = self._generation
        loop = asyncio.get_running_loop()
        future = loop.run_in_executor(self._executor, self._loader, self.bundle)
        try:
            manifest, engine = await asyncio.wait_for(future, timeout=self.timeout_seconds)
            with self._state_lock:
                if generation is self._generation and self._state is RuntimeState.LOADING:
                    self._manifest, self._engine, self._state = manifest, engine, RuntimeState.READY
        except asyncio.TimeoutError:
            self._terminal(RuntimeState.TIMED_OUT)
        except BaseException:
            self._terminal(RuntimeState.FAILED)

    def _terminal(self, state: RuntimeState) -> None:
        with self._state_lock:
            self._generation = object()
            self._state = state
        self._fatal_exit(1)

    async def analyze(self, title: str, content: str) -> Inference:
        if self._state is not RuntimeState.READY or self._engine is None: raise RuntimeError("NOT_READY")
        with self._state_lock:
            if self._available_capacity == 0: raise RuntimeError("CAPACITY_EXCEEDED")
            self._available_capacity -= 1
        try:
            return await asyncio.get_running_loop().run_in_executor(self._executor, self._engine.analyze, title, content)
        finally:
            with self._state_lock: self._available_capacity += 1
