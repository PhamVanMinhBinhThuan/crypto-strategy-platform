import asyncio
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from threading import Event
import pytest
from app.model.manifest import ReleaseManifest
from app.model.protocol import Inference
from app.model.runtime import ModelRuntime
from app.model.runtime_state import RuntimeState


class FakeEngine:
    def warm_up(self): pass
    def analyze(self, title, content): return Inference("NEUTRAL", 0, 0)


def manifest(): return ReleaseManifest("name", "version", "prep", "sentiment-v1", "model", "vocab", {})


def test_loader_runs_once_off_event_loop_and_becomes_ready():
    calls = []
    def loader(_path): calls.append(1); return manifest(), FakeEngine()
    runtime = ModelRuntime(Path("unused"), 1, 1, ThreadPoolExecutor(max_workers=1), loader, lambda _code: None)
    asyncio.run(runtime.start())
    assert calls == [1] and runtime.state is RuntimeState.READY


def test_terminal_load_failure_is_sticky_and_requests_nonzero_exit():
    exits = []
    def loader(_path): raise ValueError("bad bundle")
    runtime = ModelRuntime(Path("unused"), 1, 1, None, loader, exits.append)
    asyncio.run(runtime.start())
    assert runtime.state is RuntimeState.FAILED and exits == [1]


@pytest.mark.asyncio
async def test_inference_capacity_rejects_without_waiting_or_sleeping():
    entered, release = Event(), Event()
    class BlockingEngine(FakeEngine):
        def analyze(self, title, content): entered.set(); release.wait(); return super().analyze(title, content)
    executor=ThreadPoolExecutor(max_workers=1)
    runtime=ModelRuntime(Path("unused"),1,1,executor,lambda _path:(manifest(),BlockingEngine()),lambda _code:None)
    await runtime.start()
    first=asyncio.create_task(runtime.analyze("a","b"))
    await asyncio.get_running_loop().run_in_executor(None,entered.wait)
    with pytest.raises(RuntimeError,match="CAPACITY_EXCEEDED"): await runtime.analyze("c","d")
    release.set(); await first; executor.shutdown()
