# Tài liệu yêu cầu chức năng — Backend Chat

> Người viết: BA (dựa trên việc đọc mã nguồn tại commit `ac38ce9`)
> Ngày: 2026-10-01
> Phạm vi: Spring Boot 4.1.1 / Java 17 / MySQL / JWT

Tài liệu gồm 5 phần:

1. [Hiện trạng](#1-hiện-trạng) — dự án đã có gì
2. [Đề xuất SQL / NoSQL](#2-đề-xuất-lưu-trữ-sql-hay-nosql) — cái gì lưu ở đâu, vì sao
3. [Yêu cầu chức năng mới](#3-yêu-cầu-chức-năng-mới) — phần cần code thêm
4. [Lỗi và khoảng trống ở chức năng đã có](#4-lỗi-và-khoảng-trống-ở-chức-năng-đã-có) — nên sửa trước khi làm tiếp
5. [Thứ tự triển khai](#5-thứ-tự-triển-khai-đề-xuất)

Mức ưu tiên: **P0** = thiếu thì chưa gọi là app chat, **P1** = nên có, **P2** = làm sau.

---

## 1. Hiện trạng

| Module | Đã có | Chưa có |
|---|---|---|
| Auth | Đăng ký, đăng nhập, đăng xuất, refresh token, `/me` | Đổi mật khẩu, quên mật khẩu, đăng xuất mọi thiết bị, cập nhật hồ sơ |
| Bạn bè | Gửi / chấp nhận / từ chối lời mời, huỷ kết bạn, danh sách bạn, lời mời đang chờ | Tìm kiếm người dùng, lời mời đã gửi, thu hồi lời mời, chặn |
| Hội thoại | Tạo nhóm, tạo/lấy chat 1-1, thêm / xoá thành viên, danh sách hội thoại | Rời nhóm, đổi tên/ảnh nhóm, chuyển quyền admin, chi tiết + danh sách thành viên |
| **Tin nhắn** | Chỉ có entity `Message` — **không có repository, service, controller** | Toàn bộ: gửi, lấy lịch sử, đã đọc, sửa, xoá |
| Realtime | Không có (chưa có dependency WebSocket) | Toàn bộ |
| Upload file | Không có (`avatarUrl`, `imgUrl` chỉ là chuỗi do client tự gửi) | Toàn bộ |

Nhận xét: phần "khung" (user, bạn bè, hội thoại) đã xong, nhưng **lõi của app chat là gửi/nhận tin nhắn thì chưa có**. Các trường `lastMessage*`, `unreadCount`, `lastSeenAt` đã khai báo trong DB nhưng chưa có đoạn code nào ghi vào.

---

## 2. Đề xuất lưu trữ: SQL hay NoSQL

### 2.1. Kết luận

| Dữ liệu | Lưu ở | Trạng thái |
|---|---|---|
| `users` | **SQL (MySQL)** | Giữ nguyên |
| `sessions` (refresh token) | **SQL** (chuyển sang Redis ở giai đoạn sau) | Giữ nguyên |
| `friends`, `friend_requests`, `blocks` (mới) | **SQL** | Giữ nguyên |
| `conversations`, `conversation_participants` | **SQL** | Giữ nguyên |
| **`messages`** | **NoSQL (MongoDB)** | **Chuyển** — bỏ entity JPA `Message` |
| Reaction, trạng thái sửa/xoá, file đính kèm của tin nhắn | **NoSQL** — nhúng trong document tin nhắn | Mới |
| Thông báo (`notifications`) | **NoSQL (MongoDB)** | Mới, P2 |
| Online/offline, "đang gõ…", rate limit | **NoSQL (Redis)**, không lưu bền | Mới, P1–P2 |
| File ảnh/tệp | Object storage (S3 / MinIO / Cloudinary), DB chỉ lưu URL | Mới |

### 2.2. Vì sao phần quan hệ nên ở SQL

- **Cần ràng buộc chặt.** Các rule như "mỗi cặp chỉ có 1 quan hệ bạn bè" (`uk_user_a_user_b`), "mỗi user chỉ vào 1 hội thoại 1 lần" (`uk_conversation_user`), username/email duy nhất đang được DB đảm bảo bằng unique constraint. Đây là thế mạnh của SQL.
- **Cần transaction.** Chấp nhận kết bạn = tạo `friends` + xoá `friend_requests`; tạo nhóm = tạo `conversations` + nhiều `conversation_participants`. Hoặc cùng thành công hoặc cùng thất bại.
- **Cần JOIN.** "Tìm chat 1-1 giữa A và B", "danh sách hội thoại của tôi kèm tên đối phương" là truy vấn nhiều bảng.
- **Dữ liệu ít, tăng chậm.** Số user và hội thoại nhỏ hơn số tin nhắn hàng nghìn lần.

### 2.3. Vì sao tin nhắn nên ở NoSQL (MongoDB)

- **Khối lượng và tần suất ghi lớn nhất hệ thống**, chỉ thêm vào (append), gần như không bao giờ JOIN.
- **Chỉ có một kiểu đọc chính:** "lấy N tin mới nhất của hội thoại X, cuộn lên để lấy tiếp" → đúng 1 index `{conversationId, _id}`.
- **Cấu trúc hay thay đổi.** Tin văn bản, ảnh, tệp, tin hệ thống ("A đã thêm B vào nhóm"), trả lời, reaction… Với SQL mỗi loại là thêm cột hoặc thêm bảng; với document chỉ là thêm field.
- **Mã nguồn đã sẵn hướng này:** `Conversation.lastMessageId` đang là kiểu `String` (hợp với ObjectId), trong khi `Message.id` lại là `Long` — hai chỗ đang lệch nhau, chuyển sang MongoDB là hết lệch.

> **Lưu ý trung thực:** với quy mô nhỏ (vài nghìn user), để `messages` trong MySQL vẫn chạy tốt và đơn giản hơn (một DB, có transaction). Tách sang MongoDB là lựa chọn đúng nếu mục tiêu là học kiến trúc polyglot hoặc dự kiến dữ liệu lớn. Cái giá phải trả ghi ở mục 2.5.

### 2.4. Schema đề xuất cho MongoDB

Collection `messages`:

```json
{
  "_id": "ObjectId",
  "conversationId": 12,
  "senderId": 5,
  "type": "TEXT | IMAGE | FILE | SYSTEM",
  "content": "Xin chào",
  "attachments": [
    { "url": "https://...", "mimeType": "image/png", "name": "a.png", "size": 20480 }
  ],
  "replyTo": { "messageId": "ObjectId", "senderId": 7, "preview": "50 ký tự đầu..." },
  "reactions": [ { "userId": 7, "emoji": "❤️" } ],
  "clientMessageId": "uuid do client sinh, để chống gửi trùng",
  "editedAt": null,
  "deletedAt": null,
  "createdAt": "ISODate"
}
```

Index:

- `{ conversationId: 1, _id: -1 }` — lấy lịch sử, phân trang
- `{ conversationId: 1, clientMessageId: 1 }` unique, sparse — chống gửi trùng
- (P2) text index trên `content` — tìm kiếm tin nhắn

`conversationId`, `senderId` là `Long`, trỏ sang ID bên MySQL (không có khoá ngoại thật — code phải tự kiểm tra).

### 2.5. Hệ quả khi dùng hai DB

Gửi một tin nhắn sẽ ghi vào **hai nơi không chung transaction**:

1. Insert document vào MongoDB `messages`
2. Update MySQL: `conversations.last_message_*` và `conversation_participants.unread_count`

Quy ước xử lý:

- **Ghi MongoDB trước, MySQL sau.** Nếu bước 2 lỗi thì tin nhắn vẫn còn, chỉ phần xem trước/số chưa đọc bị lệch tạm thời — chấp nhận được. Ngược lại (xem trước có mà tin nhắn không có) thì không chấp nhận được.
- `unread_count` phải tăng bằng **một câu UPDATE nguyên tử**, không đọc-rồi-ghi:
  `UPDATE conversation_participants SET unread_count = unread_count + 1 WHERE conversation_id = ? AND user_id <> ?`
- `last_message_*` chỉ ghi đè khi tin mới hơn: thêm điều kiện `WHERE last_message_at IS NULL OR last_message_at <= ?`.

### 2.6. Thay đổi schema MySQL cần làm

| Bảng | Thay đổi | Phục vụ |
|---|---|---|
| `conversations` | thêm `avatar_url` | Ảnh nhóm (FR-CONV-03) |
| `conversation_participants` | thêm `last_read_message_id VARCHAR(24)` | Đã đọc (FR-MSG-04) |
| `conversation_participants` | thêm `muted_until DATETIME` | Tắt thông báo (P2) |
| `users` | thêm `display_name`, `last_active_at` | Hồ sơ, trạng thái |
| `sessions` | thêm `device_info`, `ip` | Quản lý thiết bị (P2) |
| `blocks` (mới) | `blocker_id`, `blocked_id`, unique cặp | Chặn (FR-FRIEND-05) |
| `messages` | **xoá bảng**, bỏ entity JPA | Chuyển sang MongoDB |

---

## 3. Yêu cầu chức năng mới

Quy ước chung cho mọi API bên dưới:

- Yêu cầu header `Authorization: Bearer <accessToken>` (trừ khi ghi khác).
- Trả về theo `ApiResponse { status, message, data }` như hiện tại.
- Đường dẫn theo phong cách sẵn có của dự án (`/api/<module>/<hành-động>`).

### 3.1. Tin nhắn (module mới — P0)

#### FR-MSG-01 — Gửi tin nhắn · P0

**Mô tả:** Thành viên của hội thoại gửi tin nhắn văn bản và/hoặc tệp đính kèm.

**API:** `POST /api/message/send`

```json
{ "conversationId": 12, "content": "Xin chào", "attachments": [], "replyToId": null, "clientMessageId": "uuid" }
```

**Quy tắc nghiệp vụ:**

1. Người gửi phải là thành viên của hội thoại (`existsByConversationIdAndUserId`). Không phải → 403.
2. Phải có `content` (sau khi trim) **hoặc** ít nhất 1 tệp đính kèm. Cả hai trống → 400.
3. `content` tối đa 4000 ký tự.
4. Hội thoại DIRECT: nếu một trong hai bên đã chặn bên kia → 403 (phụ thuộc FR-FRIEND-05).
5. Nếu `clientMessageId` đã tồn tại trong hội thoại → trả lại tin nhắn cũ, không tạo mới (client bấm gửi lại do mạng chậm).
6. Sau khi lưu: cập nhật `last_message_*` của hội thoại; tăng `unread_count` cho **mọi thành viên trừ người gửi** (xem 2.5).
7. Đẩy sự kiện realtime `message.new` tới các thành viên đang online (FR-RT-02).

**Tiêu chí chấp nhận:**

- [ ] Gửi thành công trả về tin nhắn đầy đủ (có `_id`, `createdAt`).
- [ ] Người ngoài hội thoại gửi → 403, không có document nào được tạo.
- [ ] Sau khi gửi, `GET /api/conversation/list` của người nhận hiển thị đúng `lastMessage` và `unreadCount` tăng 1; của người gửi `unreadCount` không đổi.
- [ ] Gửi 2 lần cùng `clientMessageId` chỉ tạo 1 tin nhắn.

#### FR-MSG-02 — Lấy lịch sử tin nhắn · P0

**API:** `GET /api/message/list?conversationId=12&before=<messageId>&limit=30`

**Quy tắc nghiệp vụ:**

1. Chỉ thành viên mới được xem.
2. Phân trang theo **con trỏ** (`before` = `_id` của tin cũ nhất đang có), không dùng số trang — vì tin mới đến liên tục sẽ làm lệch trang.
3. `limit` mặc định 30, tối đa 100. Sắp xếp mới → cũ.
4. Thành viên nhóm **chỉ thấy tin nhắn từ thời điểm `joined_at`** của mình trở đi.
5. Tin đã xoá (FR-MSG-06) vẫn trả về nhưng `content` và `attachments` rỗng, kèm `deletedAt`.
6. Trả thêm `nextCursor` (null khi hết).

**Tiêu chí chấp nhận:**

- [ ] Không truyền `before` → trả 30 tin mới nhất.
- [ ] Cuộn tới hết → `nextCursor = null`, không lặp và không sót tin.
- [ ] Thành viên vào nhóm sau không đọc được tin trước khi vào.

#### FR-MSG-03 — Xem chi tiết hội thoại · P0

**API:** `GET /api/conversation/detail?conversationId=12`

Trả về thông tin hội thoại + danh sách thành viên (id, username, avatarUrl, role, joinedAt, lastReadMessageId). Chỉ thành viên được xem. Cần cho màn hình chat để hiển thị tên/ảnh người gửi và trạng thái "đã xem".

#### FR-MSG-04 — Đánh dấu đã đọc · P0

**API:** `POST /api/message/mark-read` — `{ "conversationId": 12, "lastReadMessageId": "..." }`

**Quy tắc nghiệp vụ:**

1. Đặt `unread_count = 0`, `last_seen_at = now`, `last_read_message_id = <id>` cho người gọi.
2. `last_read_message_id` chỉ được tiến, không lùi (bỏ qua nếu id gửi lên cũ hơn giá trị hiện có).
3. Đẩy sự kiện `message.read` cho các thành viên khác để hiển thị "đã xem".

**Thiết kế:** lưu "đã đọc đến đâu" theo **từng thành viên**, không lưu danh sách người đã đọc trên từng tin nhắn — nhóm 100 người sẽ không làm phình document.

**Tiêu chí chấp nhận:**

- [ ] Sau khi gọi, `unreadCount` trong danh sách hội thoại về 0.
- [ ] Người gửi nhận được sự kiện "đã xem" ở chat 1-1.

#### FR-MSG-05 — Sửa tin nhắn · P1

**API:** `POST /api/message/edit` — `{ "messageId": "...", "content": "..." }`

- Chỉ người gửi được sửa; chỉ sửa tin loại TEXT; trong vòng 15 phút kể từ khi gửi.
- Ghi `editedAt`. Nếu đó là tin cuối của hội thoại → cập nhật `last_message_content`.
- Đẩy sự kiện `message.updated`.

#### FR-MSG-06 — Xoá (thu hồi) tin nhắn · P1

**API:** `POST /api/message/delete` — `{ "messageId": "..." }`

- Người gửi được thu hồi tin của mình; ADMIN nhóm được xoá tin của bất kỳ ai trong nhóm.
- **Xoá mềm:** ghi `deletedAt`, xoá trắng `content` và `attachments`. Không xoá document (để tin trả lời không trỏ vào chỗ trống).
- Nếu là tin cuối → `last_message_content` đổi thành "Tin nhắn đã bị thu hồi".
- Đẩy sự kiện `message.deleted`.

#### FR-MSG-07 — Trả lời tin nhắn · P1

Dùng trường `replyToId` trong FR-MSG-01. Tin gốc phải cùng hội thoại. Server lưu bản chụp `{messageId, senderId, preview}` vào `replyTo` để hiển thị mà không cần truy vấn lại.

#### FR-MSG-08 — Thả cảm xúc · P2

**API:** `POST /api/message/react` — `{ "messageId": "...", "emoji": "❤️" }`

Mỗi user 1 reaction trên 1 tin; gửi lại cùng emoji = bỏ; gửi emoji khác = thay. Giới hạn trong danh sách emoji cho phép.

#### FR-MSG-09 — Tin nhắn hệ thống · P1

Khi tạo nhóm, thêm/xoá thành viên, rời nhóm, đổi tên nhóm → tự sinh tin `type = SYSTEM` ("A đã thêm B vào nhóm"). Không tăng `unread_count`.

#### FR-MSG-10 — Tìm kiếm tin nhắn · P2

**API:** `GET /api/message/search?conversationId=12&q=...` — chỉ tìm trong hội thoại mình là thành viên, bỏ qua tin đã xoá.

---

### 3.2. Realtime (module mới — P0)

Đề xuất kỹ thuật: `spring-boot-starter-websocket` với STOMP. Endpoint `/ws`.

#### FR-RT-01 — Kết nối và xác thực · P0

- Client gửi access token trong frame `CONNECT`. Token sai/hết hạn → từ chối kết nối.
- **Không** truyền token qua query string (lộ trong log).
- Một user có thể kết nối từ nhiều thiết bị cùng lúc; sự kiện phải tới tất cả.
- Lưu ý: `AuthMiddleware` hiện chặn mọi request thiếu header `Authorization`, cần cho `/ws` đi qua và xác thực ở tầng STOMP.

#### FR-RT-02 — Nhận tin nhắn theo thời gian thực · P0

Mỗi user subscribe một kênh riêng `/user/queue/events`. Server đẩy các sự kiện:

| Sự kiện | Khi nào | Gửi tới |
|---|---|---|
| `message.new` | FR-MSG-01 | Mọi thành viên (kể cả các thiết bị khác của người gửi) |
| `message.updated` / `message.deleted` | FR-MSG-05/06 | Mọi thành viên |
| `message.read` | FR-MSG-04 | Các thành viên khác |
| `conversation.created` / `conversation.updated` | Tạo nhóm, đổi tên, thêm/xoá thành viên | Thành viên liên quan |
| `conversation.removed` | Bị xoá khỏi nhóm | Người bị xoá |
| `friend.request` / `friend.accepted` | Module bạn bè | Người nhận / người gửi lời mời |

**Quy tắc:** server phải tự kiểm tra tư cách thành viên trước khi đẩy — không tin vào việc client tự subscribe theo `conversationId`.

**Tiêu chí chấp nhận:**

- [ ] A gửi tin, B đang online nhận được trong < 1 giây mà không cần gọi lại API.
- [ ] B bị xoá khỏi nhóm thì ngay lập tức không còn nhận tin của nhóm đó.
- [ ] B offline → khi online lại gọi FR-MSG-02 vẫn lấy đủ tin (REST là nguồn chính, WebSocket chỉ là kênh đẩy).

#### FR-RT-03 — Trạng thái online / offline · P1

- Online khi có ít nhất 1 kết nối WebSocket; offline khi kết nối cuối cùng đóng → ghi `users.last_active_at`.
- Chỉ **bạn bè** mới thấy trạng thái của nhau.
- Lưu trong Redis (key có TTL), không lưu MySQL.

#### FR-RT-04 — "Đang gõ…" · P2

Client gửi `typing.start` / `typing.stop`; server chuyển tiếp cho thành viên khác. Không lưu DB, tự hết sau 5 giây.

---

### 3.3. Hội thoại (bổ sung)

#### FR-CONV-01 — Rời nhóm · P0

**API:** `POST /api/conversation/leave` — `{ "conversationId": 12 }`

1. Chỉ áp dụng cho GROUP.
2. Nếu người rời là **ADMIN duy nhất** và nhóm còn người khác → tự động chuyển ADMIN cho thành viên vào nhóm sớm nhất (hoặc bắt chọn người kế nhiệm — cần chốt).
3. Nếu là người cuối cùng → xoá hội thoại.
4. Sinh tin hệ thống "A đã rời nhóm".

#### FR-CONV-02 — Phân quyền admin · P1

**API:** `POST /api/conversation/set-role` — `{ "conversationId", "memberId", "role": "ADMIN|MEMBER" }`

Chỉ ADMIN được gọi. Nhóm luôn phải còn ít nhất 1 ADMIN.

#### FR-CONV-03 — Đổi tên / ảnh nhóm · P1

**API:** `POST /api/conversation/update` — `{ "conversationId", "name", "avatarUrl" }`

Chỉ GROUP. Tên 1–100 ký tự. Cần chốt: mọi thành viên được đổi hay chỉ ADMIN (đề xuất: chỉ ADMIN).

#### FR-CONV-04 — Phân trang danh sách hội thoại · P1

`GET /api/conversation/list` hiện trả toàn bộ. Thêm `?limit=20&before=<lastMessageAt>`.

#### FR-CONV-05 — Giới hạn nhóm · P1

Tối đa 200 thành viên/nhóm. Tạo nhóm cần ít nhất 2 người khác ngoài người tạo (nhóm 2 người là chat 1-1).

#### FR-CONV-06 — Tắt thông báo / ghim hội thoại · P2

Theo từng thành viên, lưu trên `conversation_participants`.

---

### 3.4. Bạn bè và người dùng (bổ sung)

#### FR-USER-01 — Tìm kiếm người dùng · P0

**API:** `GET /api/user/search?q=...&limit=20`

Không có chức năng này thì người dùng **không có cách nào biết `toUserId`** để gửi lời mời kết bạn.

- Tìm theo username (khớp tiền tố) hoặc email/số điện thoại (khớp chính xác).
- `q` tối thiểu 2 ký tự. Không trả về chính mình, không trả người đã chặn mình.
- Mỗi kết quả kèm trạng thái quan hệ: `NONE | FRIEND | REQUEST_SENT | REQUEST_RECEIVED`.
- **Không** trả `email`, `phone` của người lạ.

#### FR-USER-02 — Xem hồ sơ người khác · P1

**API:** `GET /api/user/profile?userId=5` — trả thông tin công khai + trạng thái quan hệ.

#### FR-USER-03 — Cập nhật hồ sơ của mình · P1

**API:** `POST /api/user/update-profile` — `bio`, `avatarUrl`, `phone`, `displayName`. Không cho đổi `username` qua API này.

#### FR-USER-04 — Đổi mật khẩu · P1

**API:** `POST /api/auth/change-password` — `{ "oldPassword", "newPassword" }`

Sai mật khẩu cũ → 400. Thành công → **xoá mọi session khác** của user (đăng xuất các thiết bị còn lại).

#### FR-USER-05 — Quên mật khẩu · P2

Gửi mã/đường dẫn đặt lại qua email, hết hạn sau 15 phút, dùng 1 lần. Câu trả lời luôn giống nhau dù email có tồn tại hay không.

#### FR-FRIEND-01 — Lời mời đã gửi · P1

**API:** `GET /api/friend/sent-requests` (repository đã có sẵn `findByFromUserId`).

#### FR-FRIEND-02 — Thu hồi lời mời · P1

**API:** `POST /api/friend/cancel-request` — `{ "requestId" }`. Chỉ người gửi được thu hồi.

#### FR-FRIEND-03 — Lời mời kèm thông tin người gửi · P0

`GET /api/friend/pending-requests` hiện trả thẳng entity `FriendRequest` (chỉ có `fromUserId`). Giao diện không hiển thị được tên/ảnh. Cần trả DTO có `fromUser { id, username, avatarUrl }`.

#### FR-FRIEND-04 — Xử lý lời mời chéo · P1

Nếu A gửi cho B trong khi B đã gửi cho A từ trước → tự động thành bạn bè (hoặc báo lỗi rõ ràng). Hiện tại sẽ tồn tại 2 lời mời ngược chiều.

#### FR-FRIEND-05 — Chặn người dùng · P1

**API:** `POST /api/friend/block`, `POST /api/friend/unblock`, `GET /api/friend/blocked-list`

- Chặn → tự huỷ kết bạn và xoá lời mời đang chờ giữa hai người.
- Người bị chặn không gửi được lời mời, không tạo được chat 1-1, không gửi được tin trong chat 1-1 đã có.
- Trong nhóm chung: vẫn thấy tin của nhau (cần chốt).

#### FR-FRIEND-06 — Ai được nhắn cho ai · cần chốt

Hiện tại **bất kỳ ai cũng tạo được chat 1-1 với bất kỳ ai** và **thêm bất kỳ ai vào nhóm** (không kiểm tra bạn bè). Cần chủ dự án quyết định:

- (a) Chỉ bạn bè mới nhắn và thêm vào nhóm được — đề xuất, vì đã xây module bạn bè; hoặc
- (b) Ai cũng nhắn được, người lạ vào mục "tin nhắn chờ".

---

### 3.5. Upload tệp (module mới — P1)

#### FR-FILE-01 — Tải ảnh / tệp lên

**API:** `POST /api/upload` (multipart) → trả `{ url, mimeType, size, name }`

- Ảnh: jpg/png/webp/gif, tối đa 5 MB. Tệp khác: tối đa 20 MB, theo danh sách cho phép.
- Kiểm tra loại tệp bằng nội dung thật, không tin phần mở rộng.
- Lưu ở object storage; DB chỉ lưu URL.
- `avatarUrl` và `attachments[].url` chỉ chấp nhận URL do chính hệ thống cấp — hiện client gửi chuỗi nào cũng được lưu.

---

### 3.6. Yêu cầu phi chức năng

| Mã | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-01 | Mã HTTP đúng nghĩa: 401 chưa đăng nhập, 403 không có quyền, 404 không tìm thấy, 409 trùng. Hiện mọi `RuntimeException` đều trả 400. | P0 |
| NFR-02 | Giới hạn tần suất: đăng nhập 5 lần/phút/IP; gửi tin 30 tin/phút/user. | P1 |
| NFR-03 | Mọi API danh sách phải có phân trang. | P1 |
| NFR-04 | Thời gian lưu và trả về theo UTC (ISO-8601 có múi giờ). Hiện dùng `LocalDateTime.now()` theo giờ máy chủ. | P1 |
| NFR-05 | Quản lý schema bằng migration (Flyway), tắt `ddl-auto=update` ở production. | P1 |
| NFR-06 | Chạy nhiều instance: WebSocket cần broker chung (Redis pub/sub) để tin tới user đang nối ở máy khác. | P2 |
| NFR-07 | Test tự động cho luồng chính: đăng ký → kết bạn → tạo chat → gửi → đọc. Hiện chỉ có test `contextLoads`. | P1 |
| NFR-08 | Không ghi token, mật khẩu vào log. | P0 |

---

## 4. Lỗi và khoảng trống ở chức năng đã có

Phát hiện khi đọc code. Nên xử lý nhóm "Nghiêm trọng" trước khi làm tính năng mới.

### Nghiêm trọng

| # | Vị trí | Vấn đề | Hệ quả |
|---|---|---|---|
| B1 | `ConversationController` `add-member`, `delete-member` | Ép kiểu `(Long) body.get("conversationId")`. Jackson đọc số nhỏ trong JSON thành `Integer`. | `ClassCastException` → **hai API này luôn lỗi** với ID thông thường. Nên thay `Map` bằng DTO (như `CreateGroupRequest`). |
| B2 | `AuthMiddleware` | Request `OPTIONS` (CORS preflight) không có header `Authorization` nên bị trả 401. | Frontend chạy trên trình duyệt khác origin **không gọi được API nào** cần đăng nhập. Cần bỏ qua `OPTIONS`. |
| B3 | `JwtService` | Access token và refresh token cùng khoá ký, cùng cấu trúc, không có claim phân loại. | Refresh token (7 ngày) dùng được như access token; đăng xuất không vô hiệu được nó ở vai trò này. |
| B4 | `application.properties`, `JwtService` | `jwt.secret` mặc định là `123456`; `.env.example` không có `JWT_SECRET`. | Quên cấu hình → ai cũng tự ký được token của bất kỳ user nào. Nên bắt buộc có, không đặt mặc định. |
| B5 | `ConversationServiceImpl.createGroupConversation`, `addMember…` | Không kiểm tra `memberIds` có tồn tại; controller thiếu `@Valid` nên `@NotBlank`/`@NotEmpty` không chạy. | Tạo được nhóm không tên, nhóm chứa user không tồn tại. |
| B6 | `UserServiceImpl.logout` | Ghi nguyên refresh token vào log. | Lộ token qua log. |

### Trung bình

| # | Vị trí | Vấn đề |
|---|---|---|
| B7 | `AuthMiddleware` | `authHeader.substring(7)` không kiểm tra tiền tố `Bearer ` và nằm ngoài `try` → header ngắn gây lỗi 500. |
| B8 | `AuthMiddleware` | `/api/auth/logout` cần access token còn hạn → access token hết hạn thì không đăng xuất được. `GET /` (health check) cũng bị chặn. |
| B9 | `UserServiceImpl.login` | Trả "User not found!" khác với "Invalid username or password" → dò được username nào tồn tại. |
| B10 | `AuthController.login` | Refresh token vừa nằm trong cookie HttpOnly vừa trả trong body JSON → mất tác dụng của HttpOnly. |
| B11 | `UserServiceImpl` | Mỗi lần đăng nhập thêm 1 dòng `sessions`, session hết hạn không bao giờ được dọn (trừ khi chính nó được dùng lại). Refresh không xoay vòng token. |
| B12 | `deleteMemberGroupConversation` | ADMIN xoá được chính mình → nhóm không còn admin. Không kiểm tra `memberId` có trong nhóm (vẫn báo thành công). |
| B13 | `FriendServiceImpl.sendFriendRequest` | Không kiểm tra `toUserId` có tồn tại. |
| B14 | `FriendServiceImpl.acceptFriendRequest` | Nếu hai người đã là bạn (do lời mời chéo, xem FR-FRIEND-04) → vi phạm unique, trả lỗi khó hiểu. |
| B15 | `createDirectConversation` | Tham số `userBid` không có annotation → phải truyền qua query string, khác mọi API còn lại (JSON body). Hai request đồng thời có thể tạo 2 hội thoại 1-1 cho cùng một cặp. |
| B16 | `getListConversation` | Mỗi hội thoại chạy thêm 2–3 câu SQL (N+1). 50 hội thoại ≈ 150 truy vấn. |
| B17 | `FriendRequestController` | `body.get("toUserId").toString()` → thiếu field gây `NullPointerException`, thông báo lỗi trả về là `null`. |

### Nhỏ

- `ConversationServiceImpl` khai báo logger bằng `FriendServiceImpl.class`.
- Thông báo lỗi lẫn lộn tiếng Anh / tiếng Việt có dấu / không dấu.
- `spring.jpa.show-sql=true` nên tắt ngoài môi trường dev.
- H2 có trong `pom.xml` nhưng không dùng.

---

## 5. Thứ tự triển khai đề xuất

| Giai đoạn | Nội dung | Kết quả |
|---|---|---|
| **0. Sửa nền** | B1–B6, NFR-01, NFR-08; chốt FR-FRIEND-06 | API hiện có chạy đúng, gọi được từ trình duyệt |
| **1. Chat qua REST** | Thêm MongoDB; FR-MSG-01, 02, 03, 04; FR-USER-01; FR-FRIEND-03 | Hai người tìm thấy nhau, nhắn tin, xem lịch sử, có số chưa đọc (phải tải lại để thấy tin mới) |
| **2. Realtime** | FR-RT-01, 02; FR-CONV-01; FR-MSG-09 | Tin nhắn hiện ngay, đủ để demo |
| **3. Hoàn thiện** | FR-MSG-05/06/07; FR-CONV-02→05; FR-FRIEND-01/02/04/05; FR-USER-02/03/04; FR-FILE-01; B7–B17 | Sản phẩm dùng được hằng ngày |
| **4. Mở rộng** | Redis (FR-RT-03/04, NFR-02, NFR-06); FR-MSG-08/10; FR-USER-05; thông báo | Sẵn sàng tăng tải |

### Câu hỏi cần chủ dự án chốt

1. **FR-FRIEND-06:** chỉ bạn bè mới nhắn tin / thêm vào nhóm được, hay ai cũng được?
2. **FR-CONV-01:** admin duy nhất rời nhóm → tự chuyển quyền hay bắt chọn người kế nhiệm?
3. **FR-CONV-03:** ai được đổi tên/ảnh nhóm?
4. **FR-MSG-05/06:** giới hạn thời gian sửa (đề xuất 15 phút) và thu hồi (đề xuất không giới hạn)?
5. **FR-MSG-02:** thành viên mới có được xem tin nhắn cũ của nhóm không (đề xuất: không)?
6. **Mục 2.3:** xác nhận dùng MongoDB cho tin nhắn, hay giữ MySQL cho đơn giản?
