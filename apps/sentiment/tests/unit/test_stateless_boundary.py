from pathlib import Path


def test_service_has_no_database_cache_crawler_or_eager_tensorflow_imports():
    root = Path(__file__).parents[2] / "app"
    source = "\n".join(path.read_text(encoding="utf-8") for path in root.rglob("*.py"))
    forbidden = ("psycopg", "sqlalchemy", "supabase", "redis", "requests.get(", "httpx.get(")
    assert all(name not in source.lower() for name in forbidden)
    eager_lines = [line.strip() for line in source.splitlines() if line.strip().startswith(("import tensorflow", "from tensorflow"))]
    assert eager_lines == ["import tensorflow as tf"]

