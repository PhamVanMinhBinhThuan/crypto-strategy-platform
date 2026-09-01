from enum import Enum


class RuntimeState(str, Enum):
    LOADING = "LOADING"
    READY = "READY"
    FAILED = "FAILED"
    TIMED_OUT = "TIMED_OUT"

