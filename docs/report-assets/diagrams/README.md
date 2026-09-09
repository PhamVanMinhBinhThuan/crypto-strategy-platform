# Sơ đồ dùng trong báo cáo LaTeX

Thư mục này lưu nguồn Mermaid UTF-8 (`.mmd`) và bản PDF vector đã render để `docs/main.tex` có thể được biên dịch mà không cần Mermaid CLI.

`erd-full.pdf` được render từ `docs/database/drawio-erd.mmd`; các PDF còn lại có nguồn `.mmd` cùng tên ngay trong thư mục này.

Để tái tạo một sơ đồ từ thư mục gốc của repository:

```powershell
npx.cmd --yes @mermaid-js/mermaid-cli --input docs/report-assets/diagrams/container-view.mmd --output docs/report-assets/diagrams/container-view.pdf --pdfFit
```

Thay tên tệp đầu vào và đầu ra cho sơ đồ cần cập nhật. Bộ PDF hiện tại được tạo bằng Mermaid CLI 11.17.0. Sau khi render, cần kiểm tra trực quan nhãn, hướng mũi tên và vùng trắng trước khi commit.

