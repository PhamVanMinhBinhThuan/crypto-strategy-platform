# Contract: F-011 Realtime Observer Extension

F-012 bổ sung tương thích ngược vào `RealtimeClient` hiện có:

```ts
onEvent(listener: (event: RealtimeEnvelope) => void): () => void;
onStatus(listener: (status: RealtimeStatus) => void): () => void;
```

- Mọi method F-011 hiện tại giữ nguyên signature và behavior.
- Client hỗ trợ nhiều listener; cleanup callback chỉ gỡ listener tương ứng.
- Mọi transport transition được phát cho status listeners, kể cả disconnect/reconnect.
- Parsed generic envelope được phát cho event listeners; feature adapter chịu trách nhiệm validate
  event type/version/payload trước khi dùng.
- Logout/disposal đóng socket và không để listener/subscription tạo retry loop hoặc client mới.
