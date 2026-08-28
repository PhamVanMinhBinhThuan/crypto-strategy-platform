# Observability Boundary Contract

## Correlation header

- Header: `X-Correlation-Id`.
- Accept a nonblank value of at most 128 characters without control characters.
- Missing/invalid value is replaced with a generated uppercase ULID.
- Every API response, including authentication/error response, returns the effective ID.
- `ErrorEnvelope.correlationId` equals the response header.

## Logging

- Console output uses structured JSON.
- Required context: timestamp, level, application/service, logger, message and
  `correlationId` when processing a request.
- Request filter writes correlation ID to MDC before downstream handling and clears it in
  `finally` to prevent thread reuse leakage.
- Never log Authorization header, raw JWT, password, full JDBC URL, service-role key or
  stack trace in a public response.
- F-002 does not deploy tracing collector, metrics dashboard or observability backend.

API và Worker dùng Spring Boot structured console format `logstash`. API filter chỉ log
HTTP method, request path, response status và MDC correlation ID; header, query value và
request body không nằm trong completion log. Unexpected exception log chỉ ghi exception
type cùng correlation context, không ghi exception message có thể chứa token/credential.
Framework logger được giữ ở `INFO` để biến môi trường chung `DEBUG` của shell không vô
tình bật request/exception diagnostic quá chi tiết. Hai composition entry point đặt
Spring Boot `debug=false` mặc định trước startup; muốn debug có chủ đích phải truyền
command-line `--debug` hoặc JVM system property tương ứng.

## Required evidence

- Provided valid ID is preserved in header and logs.
- Missing/invalid ID produces one valid generated ID.
- Success, authentication failure and unexpected-error fixtures share one ID across
  response/error/log.
- A following request on the same test thread does not inherit the previous MDC value.
- Secret-like fixture values do not occur in captured logs or public response.
