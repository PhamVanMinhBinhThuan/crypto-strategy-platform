ARG PYTHON_IMAGE=python:3.11.11-slim
FROM ${PYTHON_IMAGE}

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /srv/sentiment
COPY apps/sentiment/pyproject.toml ./
COPY apps/sentiment/app ./app
RUN pip install --no-cache-dir ".[ml]"
USER 65532:65532
EXPOSE 8000
CMD ["uvicorn", "app.main:create_app", "--factory", "--host", "0.0.0.0", "--port", "8000", "--workers", "1", "--no-access-log"]
