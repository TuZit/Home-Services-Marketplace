# Hướng dẫn ELK trong Home Services Marketplace

Tài liệu này giải thích **công năng** và **cách sử dụng** hệ thống ghi log tập trung ELK
(Elasticsearch + Logstash + Kibana) đã tích hợp cho backend của dự án.

Đối tượng: lập trình viên / người vận hành cần chạy, tra cứu và bảo trì log.

---

## 1. Công năng — ELK giải quyết việc gì?

Trước đây log chỉ nằm ở console của từng tiến trình, muốn tra cứu phải `tail` thủ công và
không liên kết được các dòng log của cùng một request. ELK mang lại:

| Công năng | Mô tả |
|-----------|-------|
| **Thu thập tập trung** | Log của mọi instance backend được đẩy về một kho chung (Elasticsearch), không phải xem từng máy. |
| **Truy vết theo request** | Mỗi request có một `traceId`; tìm theo `traceId` là ra **toàn bộ** dòng log của request đó (access, 401/403, lỗi xử lý...). |
| **Tìm kiếm & lọc** | Lọc theo thời gian, mức độ (`level`), `traceId`, người dùng (`userId`/`role`), HTTP status... |
| **Trực quan hóa** | Dashboard dựng sẵn: lưu lượng log, phân bố mức độ, phân bố HTTP status, độ trễ trung bình. |
| **Lưu trữ & vòng đời** | Chính sách ILM tự xóa index sau **30 ngày** (cấu hình được). |
| **An toàn vận hành** | Ghi log **fail-open**: ELK có sập cũng không ảnh hưởng giao dịch nghiệp vụ. |

---

## 2. Kiến trúc luồng log

```
┌─────────────────────┐   ghi file JSON (1 dòng = 1 JSON)
│  Backend (Spring)   │───────────────────────────────►  backend/logs/hsm-backend.json
│  structured logging │                                   (bind-mount :ro)
└─────────────────────┘                                            │
                                                                   ▼
                                              ┌─────────────────────────────┐
                                              │  Logstash (pipeline hsm-logs)│  file input + codec json
                                              └─────────────────────────────┘
                                                                   │  index: hsm-logs-YYYY.MM.dd
                                                                   ▼
                                              ┌─────────────────────────────┐
                                              │  Elasticsearch (single node) │  mapping + ILM 30 ngày
                                              └─────────────────────────────┘
                                                                   │
                                                                   ▼
                                              ┌─────────────────────────────┐
                                              │  Kibana (Discover + Dashboard)│
                                              └─────────────────────────────┘
```

- Backend **chỉ ghi file local** → không phụ thuộc ELK (đây là cơ sở của tính fail-open).
- Logstash **đọc file** (không cần backend mở cổng mạng nào).
- Elasticsearch là kho lưu + tìm kiếm; Kibana là giao diện xem/tra cứu.

---

## 3. Thành phần & công năng

| Thành phần | Vai trò trong dự án | Cổng (host) | Profile compose |
|-----------|---------------------|-------------|-----------------|
| **Elasticsearch** | Lưu index `hsm-logs-*`, áp mapping + ILM | `9200` | `elk` |
| **Logstash** | Đọc file JSON, đẩy vào ES (pipeline `hsm-logs`) | — (API 9600 nội bộ) | `elk` |
| **Kibana** | Discover + dashboard `HSM Observability` | `5601` | `elk` |
| **Backend** | Sinh JSON log có `traceId`/actor/http* | `8080` (hoặc port bạn chạy) | mặc định |

Cấu hình trong `compose.yaml`: các service ELK gắn `profiles: ["elk"]`, nên `podman compose up`
mặc định **không** bật ELK — chỉ bật khi gọi `--profile elk`.

---

## 4. Cấu trúc một dòng log

Backend dùng structured logging của Spring Boot (`logging.structured.format.file: logstash`),
mỗi dòng là một JSON object. Ví dụ thực tế:

```json
{"@timestamp":"2026-11-06T00:06:55.725677+07:00","@version":"1","message":"GET /api/v1/me/requests -> 401 (10 ms)","logger_name":"ptit.home_services_marketplace.access","thread_name":"http-nio-8099-exec-7","level":"INFO","level_value":20000,"traceId":"f37510c788264bbd","http_method":"GET","http_status":401,"http_duration_ms":10,"app":"hsm-backend","environment":"local"}
```

| Field | Nguồn | Ý nghĩa |
|-------|-------|---------|
| `@timestamp` | encoder | Thời điểm log (ISO-8601, có offset). ES chuẩn hóa về UTC. |
| `message` | code | Nội dung (access log hoặc business log). |
| `logger_name`, `thread_name` | Logback | Logger & luồng phát sinh. |
| `level`, `level_value` | Logback | Mức độ: `INFO`, `WARN`, `ERROR`... |
| `traceId` | MDC | **Khóa tương quan** giữa các dòng log của cùng request. |
| `userId`, `role` | MDC | Danh tính actor (rỗng nếu chưa xác thực). |
| `http_method`, `http_status`, `http_duration_ms` | access log | Dữ liệu HTTP có cấu trúc để vẽ biểu đồ. |
| `app`, `environment` | cấu hình | Thẻ nguồn: `hsm-backend`, `local`/`staging`... |
| `stack_trace` | encoder | Có mặt khi log kèm exception. |

> **Liên hệ với HTTP:** khi xử lý request, backend trả header `X-Request-Id` cho client — giá trị
> này **chính là** `traceId` trong log. Nhờ đó chỉ cần một `X-Request-Id` là tra được toàn bộ log.

---

## 5. Cách sử dụng

### 5.1. Khởi động ELK

```bash
# chỉ bật 3 service của ELK (không đụng tới postgres)
podman compose --profile elk up -d elasticsearch logstash kibana

# chờ Elasticsearch healthy (~30–60s)
until curl -sf http://localhost:9200/_cluster/health >/dev/null; do sleep 2; done
curl -s 'http://localhost:9200/_cluster/health?pretty' | grep -E '"status"|"number_of_nodes"'
```

Kiểm tra nhanh container:

```bash
podman ps --format '{{.Names}}\t{{.Status}}' | grep hsm-
```

### 5.2. Áp cấu hình Elasticsearch + Kibana (một lần sau khi stack lên)

```bash
bash infra/elk/setup/apply-elk-config.sh
```

Script này sẽ (idempotent, chạy lại được):

1. Tạo ILM policy `hsm-logs-policy` (xóa sau 30 ngày).
2. Tạo index template `hsm-logs-template` (mapping cho `hsm-logs-*`).
3. Import Kibana saved objects: data view `hsm-logs-*`, 4 visualization, dashboard `HSM Observability`.

### 5.3. Chạy backend để sinh log

Backend ghi log vào `backend/logs/hsm-backend.json` (đường dẫn tương đối theo working dir).

```bash
cd backend
./gradlew bootRun            # hoặc ./gradlew bootRun --args='--server.port=8099'
```

Biến môi trường tùy chọn:

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `LOG_FILE` | `logs/hsm-backend.json` | Nơi ghi file JSON (đổi nếu cần). |
| `APP_ENV` | `local` | Giá trị field `environment` trong log. |

Sinh vài request để có log:

```bash
curl -s -o /dev/null http://localhost:8080/api/v1/categories
curl -s -o /dev/null http://localhost:8080/api/v1/me/requests   # 401
```

### 5.4. Tra cứu trên Kibana

1. Mở <http://localhost:5601>.
2. **Analytics → Discover**, chọn data view `hsm-logs-*`.
3. **Lọc theo một request**: gõ vào thanh KQL, ví dụ:

   ```kql
   traceId : "f37510c788264bbd"
   ```

4. **Xem dashboard**: menu → **Dashboard → HSM Observability** (lưu lượng, mức độ, HTTP status, độ trễ).

Gợi ý truy vấn hay dùng:

```kql
level : "ERROR"                         // mọi log lỗi
http_status >= 500                      // request lỗi server
role : "PROVIDER"                       // log của actor Provider
environment : "local" and level : "WARN"
http_duration_ms > 1000                 // request chậm > 1s
```

### 5.5. Truy vết một request từ đầu đến cuối (không cần Kibana)

```bash
# 1) lấy X-Request-Id của request vừa gọi
curl -s -D - -o /dev/null http://localhost:8080/api/v1/categories | grep -i x-request-id
#    X-Request-Id: 9f2c...            -> đây là traceId

# 2) tra thẳng trong Elasticsearch
curl -s 'http://localhost:9200/hsm-logs-*/_search?q=traceId:9f2c...&pretty' | grep -E '"total"|"message"'
```

### 5.6. Xác nhận toàn tuyến bằng script có sẵn

```bash
bash infra/elk/setup/verify-elk.sh
```

Script kiểm tra: (1) đã có index `hsm-logs-*` chưa, (2) một `traceId` lấy từ file log app có tìm
thấy ở Elasticsearch không. Exit code `0` = PASS.

### 5.7. Kiểm tra retention (ILM)

```bash
curl -s http://localhost:9200/_ilm/policy/hsm-logs-policy | python3 -m json.tool
curl -s 'http://localhost:9200/hsm-logs-*/_ilm/explain?pretty' | grep -E '"index"|"policy"|"phase"'
```

---

## 6. Truy vấn Elasticsearch mẫu

```bash
# liệt kê index log và số document
curl -s 'http://localhost:9200/_cat/indices/hsm-logs-*?v&h=index,docs.count,health'

# đếm request theo HTTP status
curl -s 'http://localhost:9200/hsm-logs-*/_search' -H 'Content-Type: application/json' -d '{
  "size": 0,
  "aggs": { "by_status": { "terms": { "field": "http_status" } } }
}'

# top logger phát nhiều log nhất
curl -s 'http://localhost:9200/hsm-logs-*/_search' -H 'Content-Type: application/json' -d '{
  "size": 0,
  "aggs": { "by_logger": { "terms": { "field": "logger_name" } } }
}'
```

---

## 7. Vận hành & lưu ý

- **Fail-open**: backend không kết nối tới ELK, chỉ ghi file local. Logstash/Elasticsearch có tắt
  thì request vẫn chạy bình thường (đã kiểm chứng: dừng Logstash, request vẫn trả `200/401` và
  file log vẫn tiếp tục ghi).
- **Vòng quay file local**: file JSON tự quay vòng (10MB/roll, giữ tối đa 7 bản, tổng ≤ 1GB) theo
  `logging.logback.rollingpolicy` trong `application.yaml`.
- **Không chứa bí mật**: log không ghi body, mật khẩu, token; URL trong access log đã được loại
  ký tự điều khiển và cắt độ dài. Khi thêm log mới, **không** log dữ liệu nhạy cảm/PII.
- **Bảo mật (chỉ dành cho dev)**: Elasticsearch đang chạy `xpack.security.enabled=false` và chỉ
  publish ra localhost. Với môi trường dùng chung/production **phải** bật TLS + xác thực và giới hạn
  quyền truy cập Kibana.
- **Dấu thời gian**: log ghi kèm offset (giờ máy). Elasticsearch tự chuẩn hóa về UTC nên thứ tự
  giữa các instance luôn nhất quán.
- **Giới hạn của MVP**: một node Elasticsearch, heap nhỏ (`512m`) để vừa máy Podman; chưa cấu hình
  HA/cluster.

**Xử lý sự cố thường gặp**

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|-----------|------------------------|-----------|
| Không thấy index `hsm-logs-*` | Backend chưa ghi file, hoặc Logstash chưa lên | Kiểm tra `backend/logs/hsm-backend.json` tồn tại; `podman logs hsm-logstash` để xem pipeline |
| Kibana không có dashboard | Chưa chạy script áp cấu hình | Chạy `bash infra/elk/setup/apply-elk-config.sh` |
| `bind: address already in use` khi `up` | Cổng host bị chiếm (ví dụ 5000 do AirPlay) | Dự án đã **không** publish cổng Logstash; nếu vẫn lỗi, đổi cổng `9200/5601` trong `compose.yaml` |
| Elasticsearch chết do hết RAM | Máy ảo Podman quá nhỏ | Giữ `ES_JAVA_OPTS=-Xms512m -Xmx512m`, tắt container không dùng |

---

## 8. Dừng / dọn dẹp

```bash
# dừng riêng ELK (giữ nguyên các container khác như postgres)
podman stop hsm-logstash hsm-kibana hsm-elasticsearch

# gỡ hẳn stack ELK kèm volume dữ liệu ES
podman rm -f hsm-logstash hsm-kibana hsm-elasticsearch
podman volume rm home_services_marketplace_hsm_esdata
```

> Lưu ý: tránh `podman compose down` trần vì có thể tác động cả `postgres` của môi trường dev.

---

## 9. Tùy chỉnh thường gặp

| Muốn thay đổi | Sửa ở đâu | Ghi chú |
|---------------|-----------|---------|
| Đổi thời hạn lưu log | `infra/elk/elasticsearch/ilm-policy.json` → `delete.min_age` | Chạy lại `apply-elk-config.sh`. |
| Thêm field vào log | `backend/.../application.yaml` (`logging.structured.json.add`) hoặc MDC/fluent API trong code | Cập nhật mapping trong `index-template.json` + `apply-elk-config.sh`. |
| Đổi tên/thêm dashboard | `infra/elk/kibana/saved-objects.ndjson` | Import lại bằng script áp cấu hình. |
| Đổi đường dẫn file log | Biến `LOG_FILE` hoặc `logging.file.name` | Nhớ đồng bộ mount `./backend/logs:/var/log/hsm` trong `compose.yaml`. |

---

## 10. Tham chiếu file & cổng

| Đường dẫn | Nội dung |
|-----------|----------|
| `compose.yaml` | Định nghĩa service ELK (profile `elk`) + volume |
| `backend/src/main/resources/application.yaml` | Cấu hình structured JSON logging của backend |
| `backend/src/main/java/.../shared/logging/` | `RequestTraceFilter` (traceId + access log), `LogContext` (MDC) |
| `infra/elk/logstash/pipeline/hsm-logs.conf` | Pipeline đọc file JSON → ES |
| `infra/elk/elasticsearch/{ilm-policy,index-template}.json` | Retention + mapping |
| `infra/elk/kibana/saved-objects.ndjson` | Data view + dashboard + visualization |
| `infra/elk/setup/{apply-elk-config,verify-elk}.sh` | Áp cấu hình / kiểm thử toàn tuyến |

| Dịch vụ | URL |
|---------|-----|
| Kibana | <http://localhost:5601> |
| Elasticsearch | <http://localhost:9200> |
| API backend (mặc định) | <http://localhost:8080> |

Thêm ngữ cảnh thiết kế: `specs/002-elk-logging/` (spec, plan, quickstart) và
`docs/Detailed_Design_Home_Services_Marketplace_v0.2.md` §13 Observability.
