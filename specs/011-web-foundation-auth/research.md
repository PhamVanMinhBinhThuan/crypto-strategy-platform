# Research: Web Foundation and Authentication

- **Decision**: Một Next.js App Router; không nhập build Vite cũ. **Why**: Constitution bắt buộc;
  chỉ tái sử dụng visual ideas phù hợp.
- **Decision**: Supabase email/password, bắt buộc email confirmation và password recovery;
  SSR-aware session. **Why**: Provider sở hữu password/session, Java sở hữu business authorization.
- **Decision**: Client trung tâm bao F-009 REST/WebSocket; realtime dùng one-time ticket,
  reconnect/resubscribe và REST recovery. **Why**: Khớp security/recovery contract hiện có.
- **Decision**: Public ports có real/fixture adapters; fixture chỉ development/test và có marker.
  **Why**: F-012/F-013 làm song song mà không gắn component vào backend chưa sẵn sàng.
- **Decision**: English-only dark theme, responsive 360–1440px. **Why**: Khớp sketch và giới hạn MVP.

