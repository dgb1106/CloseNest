# CloseNest — Agent Coding Specification

> Tài liệu này dùng làm **instruction/specification file** cho coding agent. Agent cần đọc toàn bộ file trước khi code, sau đó triển khai ứng dụng theo đúng phạm vi, kiến trúc, UI guideline, data model và coding convention bên dưới.

---

## 1. Mục tiêu sản phẩm

**CloseNest** là ứng dụng quản lý, theo dõi và nuôi dưỡng các mối quan hệ cá nhân. Ứng dụng giúp người dùng:

- Lưu trữ hồ sơ từng mối quan hệ.
- Ghi nhận lịch sử tương tác như gặp mặt, nhắn tin, gọi điện.
- Theo dõi mức độ quan tâm/thân thiết của từng mối quan hệ.
- Lưu giữ ký ức, ảnh và địa điểm kỷ niệm.
- Gợi ý người cần quan tâm và hành động nên làm tiếp theo.
- Nhắc nhở người dùng duy trì thói quen tương tác lành mạnh.
- Có chatbot để giải tỏa cảm xúc hoặc thử nói điều gì đó trước khi nói trực tiếp với người khác.

Ứng dụng không nên tạo cảm giác như một công cụ CRM khô khan. UI/UX cần gần gũi, nhẹ nhàng, cảm xúc và dễ sử dụng.

---

## 2. Triết lý thiết kế sản phẩm

Coding agent phải tuân thủ các nguyên tắc sau khi thiết kế màn hình, flow và component.

### 2.1 Reduce Cognitive Load

Người dùng không muốn “quản lý con người như dữ liệu”. Vì vậy:

- Hạn chế form dài.
- Ưu tiên thao tác nhanh.
- Ưu tiên gợi ý, tự động hóa và preset.
- Không bắt người dùng nhập quá nhiều thông tin ngay từ đầu.
- Cho phép tạo hồ sơ quan hệ với lượng thông tin tối thiểu.

### 2.2 Emotion-first Design

Mối quan hệ là con người, ký ức và cảm xúc. Vì vậy:

- Tên người, ảnh, ghi chú, kỷ niệm cần được ưu tiên hiển thị.
- Timeline nên có cảm giác kể chuyện.
- Các card nên mềm, ấm, thân thiện.
- Tránh trình bày quá nhiều số liệu kỹ thuật.

### 2.3 Actionable, not Analytical

Người dùng cần biết **nên làm gì tiếp theo**, không phải chỉ xem chỉ số.

- Với mỗi gợi ý, cần có hành động cụ thể: nhắn tin, gọi điện, hẹn gặp, thêm kỷ niệm.
- Các recommendation không chỉ nói “mối quan hệ đang giảm điểm”, mà nên nói “Bạn đã 12 ngày chưa trò chuyện với An. Gửi một tin nhắn hỏi thăm nhé?”.
- CTA phải rõ, ngắn, dễ bấm.

### 2.4 Invisible AI

AI chỉ nên là trợ lý chạy ngầm.

- Không làm UI quá “AI hóa”.
- Không hiển thị prompt, score phức tạp hoặc thuật toán cho người dùng phổ thông.
- Chỉ hiển thị kết quả hữu ích: gợi ý người cần quan tâm, gợi ý hành động, nhắc lại kỷ niệm.

### 2.5 One-screen Clarity

Mỗi màn hình chỉ nên trả lời một câu hỏi chính:

- Home/Map: “Kỷ niệm của mình nằm ở đâu?”
- Relationships: “Mình đang có những mối quan hệ nào?”
- Relationship Detail: “Mối quan hệ này đang thế nào?”
- Add: “Mình muốn ghi lại điều gì?”
- Notifications: “Hôm nay mình cần chú ý điều gì?”
- Profile: “Cài đặt và thông tin cá nhân của mình là gì?”

### 2.6 Habit Forming

Ứng dụng cần tạo thói quen nhẹ nhàng, không gây áp lực:

- Có streak tương tác.
- Có notification nhắc log cuối ngày.
- Có reflection/mindfulness ngắn.
- Có lời cổ vũ tinh thần.
- Không dùng wording gây tội lỗi như “Bạn đã bỏ quên người này”.

---

## 3. Tech Stack bắt buộc

### 3.1 Frontend Android

Sử dụng:

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Hilt
- Room
- DataStore
- WorkManager
- OpenStreetMap

IDE chính:

- Android Studio

Kiến trúc:

- MVVM
- Repository pattern
- Feature-based package structure

### 3.2 Backend

Theo tài liệu gốc, backend có đề cập FastAPI + Ktor. Để tránh mâu thuẫn khi triển khai, agent cần chọn một hướng rõ ràng theo yêu cầu dự án hiện tại.

Khuyến nghị triển khai:

- Backend chính: **FastAPI**
- Authentication: JWT
- Request validation: Pydantic
- Database: PostgreSQL
- Storage: Firebase Cloud Storage
- Notification: Firebase Cloud Messaging

Nếu dự án hiện tại đã có Ktor backend sẵn, agent có thể tiếp tục dùng Ktor, nhưng không được trộn FastAPI và Ktor trong cùng một service nếu không có lý do rõ ràng.

### 3.3 Database

- Server database: PostgreSQL
- Local offline cache: Room/SQLite
- Đồng bộ dữ liệu theo hướng offline-first cho các dữ liệu quan trọng như relationship profile, interaction log, memory.

---

## 4. Kiến trúc Android App

Package gốc đề xuất:

```text
com.example.closenest
├── core/
│   ├── network/
│   │   ├── api/
│   │   ├── dto/
│   │   ├── interceptor/
│   │   └── mapper/
│   ├── database/
│   │   ├── dao/
│   │   ├── entity/
│   │   ├── relation/
│   │   └── CloseNestDatabase.kt
│   ├── datastore/
│   ├── notification/
│   ├── worker/
│   ├── ui/
│   │   ├── components/
│   │   └── theme/
│   └── utils/
│
├── features/
│   ├── auth/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   ├── repository/
│   │   └── model/
│   ├── relationships/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   ├── repository/
│   │   └── model/
│   ├── interactions/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   ├── repository/
│   │   └── model/
│   ├── memories/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   ├── repository/
│   │   └── model/
│   ├── map/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   └── repository/
│   ├── recommendations/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   ├── repository/
│   │   └── engine/
│   ├── notifications/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   └── repository/
│   ├── chatbot/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   └── repository/
│   ├── reflection/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   └── repository/
│   ├── gifts/
│   │   ├── ui/
│   │   ├── viewmodel/
│   │   └── repository/
│   └── profile/
│       ├── ui/
│       ├── viewmodel/
│       └── repository/
│
└── MainActivity.kt
```

---

## 5. Navigation

Ứng dụng dùng Bottom Navigation gồm 5 tab chính:

1. **Map**
   - Home screen mặc định.
   - Hiển thị bản đồ kỷ niệm, ảnh đã ghim theo địa điểm.

2. **Relationships**
   - Danh sách quan hệ.
   - Danh bạ quan hệ.
   - Truy cập relationship detail.
   - Truy cập memory liên quan đến từng người.

3. **Add**
   - Entry point để thêm nhanh:
     - Relationship
     - Interaction log
     - Memory
     - Reflection
     - Schedule/date

4. **Notifications**
   - Xem nhắc nhở.
   - Gợi ý tương tác.
   - Nhắc reflection.
   - Streak encouragement.

5. **Profile**
   - Thông tin cá nhân.
   - Settings.
   - Privacy.
   - Data export/delete.
   - More.

Ngoài ra cần có floating/quick action cho:

- Chatbot giải tỏa
- Quick log interaction
- Add memory

---

## 6. Core Features

## 6.1 Authentication

### Functional Requirements

Người dùng có thể:

- Đăng ký tài khoản.
- Đăng nhập.
- Đăng xuất.
- Lưu phiên đăng nhập bằng token.
- Khi token hết hạn, tự động yêu cầu đăng nhập lại hoặc refresh token nếu backend hỗ trợ.

### Registration Fields

Khi đăng ký, thu thập:

- Name
- Date of birth
- Gender
- Email
- Phone number
- Password

### Validation

- Email đúng định dạng.
- Password có tối thiểu 8 ký tự.
- Phone number có định dạng hợp lệ.
- Required fields không được rỗng.

### Suggested Screens

- LoginScreen
- RegisterScreen
- ForgotPasswordScreen
- AuthLoadingScreen

### Local Storage

- Access token lưu bằng DataStore hoặc Encrypted DataStore.
- Không hardcode token.
- Không log token ra console.

---

## 6.2 Relationship Profile

### Functional Requirements

Người dùng có thể:

- Tạo hồ sơ mối quan hệ.
- Xem danh sách hồ sơ.
- Xem chi tiết một hồ sơ.
- Sửa hồ sơ.
- Xóa hồ sơ.
- Tìm kiếm hồ sơ theo tên, tag, email, số điện thoại.
- Lọc theo tag/loại quan hệ.

### Relationship Fields

Bắt buộc:

- Name
- Tag

Không bắt buộc:

- Birthday
- Phone number
- Email
- Interests
- Notes
- Avatar
- Important dates
- Social links
- Relationship priority

### Suggested Tags

- Family
- Friend
- Close friend
- Classmate
- Coworker
- Mentor
- Partner
- Other

### UI Requirements

Relationship list dùng card:

- Avatar hoặc initials.
- Tên người nổi bật nhất.
- Tag nhỏ hơn.
- Last interaction.
- Attention indicator nhẹ nhàng, không quá kỹ thuật.
- Suggested action nếu có.

Ví dụ card:

```text
[Avatar] Minh Anh
Friend · Last chat: 5 days ago
Suggestion: Send a quick check-in
```

### Empty State

Khi chưa có relationship:

- Hiển thị text nhẹ nhàng.
- Có CTA “Add your first relationship”.
- Không để màn hình trống.

---

## 6.3 Interaction Tracking

### Functional Requirements

Người dùng có thể:

- Ghi nhận lần tương tác với một hoặc nhiều người.
- Xem lịch sử tương tác theo từng người.
- Tick nhanh người đã chat/call trong ngày.
- Phân loại tương tác.
- Ghi chú ngắn cho từng tương tác.

### Interaction Types

- Meet
- Chat
- Call
- Video call
- Message
- Gift
- Date
- Other

### Interaction Fields

- Relationship ID
- Interaction type
- Date/time
- Duration optional
- Note optional
- Mood optional
- Created at
- Updated at

### Quick Log

Cần có quick log flow:

1. Chọn người.
2. Chọn loại tương tác.
3. Optional note.
4. Save.

Flow này không nên quá 2-3 bước.

### Daily Short Interaction Tick

Màn hình hoặc bottom sheet cho phép:

- Hiển thị danh sách người thường tương tác.
- Tick “Đã chat hôm nay”.
- Tick “Đã call hôm nay”.
- Auto-create interaction log tương ứng.

---

## 6.4 Relationship Attention Score

### Functional Requirements

Hệ thống cần chấm điểm mức độ quan tâm/thân thiết để phục vụ recommendation.

Score không cần hiển thị như thuật toán phức tạp. Có thể hiển thị dưới dạng:

- Needs attention
- Warm
- Close
- Recently connected
- Fading

### Suggested Scoring Logic for MVP

Agent có thể triển khai rule-based trước, AI/ML sau.

Các yếu tố:

- Recency: lần tương tác gần nhất.
- Frequency: số lần tương tác trong 7/30/90 ngày.
- Relationship priority.
- Important dates sắp tới.
- Sentiment/mood nếu có reflection hoặc note.
- Interaction type weight.

Ví dụ weight:

```text
Meet: 5
Date: 5
Call: 4
Video call: 4
Chat/Message: 2
Gift: 3
Other: 1
```

Pseudo formula:

```text
attentionScore =
  recencyScore * 0.4 +
  frequencyScore * 0.3 +
  priorityScore * 0.2 +
  specialDateScore * 0.1
```

Score range:

```text
0 - 39: Needs attention
40 - 69: Warm
70 - 100: Close
```

### Important

- Không làm người dùng thấy bị phán xét.
- Không dùng wording tiêu cực.
- Score chỉ là công cụ hỗ trợ gợi ý.

---

## 6.5 Memory Space

### Functional Requirements

Người dùng có thể:

- Tạo memory/kỷ niệm.
- Gắn memory với một hoặc nhiều relationship.
- Thêm ảnh.
- Thêm thời gian.
- Thêm địa điểm.
- Xem memory theo timeline.
- Xem memory theo relationship.
- Xem memory trên bản đồ.

### Memory Fields

- ID
- Title
- Description/note
- Relationship IDs
- Image URLs/local image paths
- Location latitude
- Location longitude
- Location name
- Memory date
- Created at
- Updated at

### Reminder Logic

Hệ thống có thể nhắc lại kỷ niệm theo chu kỳ:

- Sau 1 tháng.
- Sau 6 tháng.
- Sau 1 năm.
- Hằng năm vào ngày kỷ niệm.

MVP có thể dùng WorkManager local notification.

---

## 6.6 Picture of Us / Memory Map

### Functional Requirements

Người dùng có thể:

- Xem bản đồ có các marker ảnh/kỷ niệm.
- Bấm marker để xem preview memory.
- Mở memory detail từ marker.
- Ghim ảnh theo địa điểm.
- Tìm lại kỷ niệm theo vị trí địa lý.

### Map Requirements

- Dùng OpenStreetMap.
- Cần xin quyền location nếu dùng vị trí hiện tại.
- Nếu không có quyền location, app vẫn hoạt động với manual location input.
- Marker nên thân thiện: thumbnail hoặc pin mềm, không quá kỹ thuật.

### UI Flow

```text
Map tab
→ Memory markers
→ Tap marker
→ Memory preview bottom sheet
→ Open Memory Detail
```

---

## 6.7 Recommendation

### Functional Requirements

Hệ thống cần gợi ý:

1. Mối quan hệ cần quan tâm.
2. Hành động phù hợp để duy trì mối quan hệ.

### Recommendation Inputs

- Relationship profile.
- Interaction history.
- Attention score.
- Important dates.
- User streak.
- User reflection/mood.
- Time since last interaction.
- Relationship tag.

### Recommendation Types

- Nhắn tin hỏi thăm.
- Gọi điện.
- Hẹn gặp.
- Đi date.
- Gửi quà.
- Ghi lại memory.
- Chúc mừng sinh nhật.
- Nhắc lại một kỷ niệm.

### Recommendation Card

Mỗi recommendation nên có:

- Người liên quan.
- Lý do ngắn gọn.
- Hành động đề xuất.
- CTA.
- Dismiss button.
- Snooze button.

Ví dụ:

```text
You haven’t talked to Linh for 10 days.
Maybe send a quick message today?
[Message] [Remind later]
```

### MVP Rule-based Engine

Agent cần tạo `RecommendationEngine` trong app hoặc backend.

Rule examples:

```text
IF daysSinceLastInteraction > thresholdByTag
THEN suggest check-in

IF birthdayWithinNext7Days
THEN suggest birthday greeting or gift

IF highPriorityRelationship AND noInteractionIn7Days
THEN suggest call

IF memoryAnniversaryToday
THEN suggest revisit memory
```

Threshold example:

```text
Partner: 3 days
Close friend: 7 days
Family: 10 days
Friend: 14 days
Coworker: 21 days
Other: 30 days
```

---

## 6.8 Notifications

### Functional Requirements

Ứng dụng cần có notification cho:

- Nhắc truy cập và log lịch sử hôm nay.
- Nhắc reflection cuối ngày.
- Nhắc gợi ý tương tác.
- Nhắc kỷ niệm.
- Nhắc streak.
- Cổ vũ tinh thần.

Tần suất theo tài liệu:

- Reminder truy cập/log: 1-2 lần/ngày.
- Suggestion interaction: 1-2 lần/ngày.
- Mỗi ngày cập nhật người mới để gợi ý.

### Notification Principles

- Không gây áp lực.
- Không spam.
- Nội dung nhẹ nhàng.
- Có thể tắt/bật từng loại notification.

### Local Notification MVP

Dùng:

- WorkManager cho scheduled jobs.
- NotificationManager cho local notifications.

### FCM

Dùng Firebase Cloud Messaging cho notification từ server khi backend đã sẵn sàng.

---

## 6.9 Chatbot “Giải tỏa”

### Functional Requirements

Người dùng có thể:

- Chat với trợ lý ảo để giải tỏa tâm sự.
- Xả giận trong không gian riêng tư.
- Thử nói trước một câu trước khi nói trực tiếp với người khác.
- Nhận gợi ý wording mềm hơn.

### Safety Requirements

Chatbot không được:

- Đưa lời khuyên y tế/tâm lý như chuyên gia nếu không có disclaimer.
- Khuyến khích hành vi gây hại.
- Lưu nội dung nhạy cảm nếu người dùng không đồng ý.
- Hiển thị nội dung người dùng đã chat ở nơi công khai trong app.

### Suggested Modes

- Vent mode: chỉ lắng nghe, phản hồi nhẹ nhàng.
- Rewrite mode: giúp viết lại lời nhắn.
- Before you say it: thử nói trước rồi bot góp ý.
- Calm down mode: đưa bài tập thở/ngắn.

---

## 6.10 Interaction Streak

### Functional Requirements

Hệ thống ghi nhận streak mỗi ngày nếu người dùng:

- Truy cập app.
- Và/hoặc log lại lịch sử tương tác.
- Và/hoặc hoàn thành reflection.

MVP nên tính streak dựa trên điều kiện:

```text
A day is counted if user logs at least one interaction OR completes one reflection.
```

### UI

- Hiển thị streak trong Profile hoặc Home.
- Có message cổ vũ.
- Không dùng wording gây áp lực.

---

## 6.11 Reflection / Mindfulness

### Functional Requirements

Người dùng có thể trả lời bảng hỏi ngắn để đánh giá đời sống tinh thần và mức độ tương tác xã hội.

Có 2 dạng:

1. Ghi lại ngay lúc này.
2. Ghi lại vào cuối ngày.

### Suggested Questions

Immediate reflection:

- Bạn đang cảm thấy thế nào?
- Hôm nay bạn có muốn nói chuyện với ai không?
- Có điều gì đang khiến bạn bận tâm không?

End-of-day reflection:

- Hôm nay bạn đã tương tác với ai?
- Bạn có hài lòng với các tương tác hôm nay không?
- Có mối quan hệ nào bạn muốn quan tâm hơn không?
- Tâm trạng chung hôm nay của bạn thế nào?

### Data Fields

- ID
- User ID
- Type: immediate/end_of_day
- Mood score
- Social satisfaction score
- Note
- Created at

---

## 6.12 Gift Suggestion / Gift Shop

### Functional Requirements

Ứng dụng có thể:

- Gợi ý quà dựa trên relationship tag, sở thích, dịp đặc biệt.
- Hiển thị danh sách quà.
- Cho phép lưu ý tưởng quà.
- MVP có thể chỉ là suggestion list, chưa cần thanh toán.

### Gift Fields

- ID
- Name
- Category
- Price optional
- Image URL optional
- Suitable tags
- Related interests
- External link optional

---

## 7. Data Model đề xuất

## 7.1 User

```kotlin
data class User(
    val id: String,
    val name: String,
    val dateOfBirth: LocalDate?,
    val gender: String?,
    val email: String,
    val phoneNumber: String?,
    val avatarUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## 7.2 Relationship

```kotlin
data class Relationship(
    val id: String,
    val userId: String,
    val name: String,
    val tag: String,
    val birthday: LocalDate?,
    val phoneNumber: String?,
    val email: String?,
    val interests: List<String>,
    val notes: String?,
    val avatarUrl: String?,
    val priority: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## 7.3 Interaction

```kotlin
data class Interaction(
    val id: String,
    val userId: String,
    val relationshipId: String,
    val type: InteractionType,
    val occurredAt: Instant,
    val durationMinutes: Int?,
    val note: String?,
    val mood: Int?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

```kotlin
enum class InteractionType {
    MEET,
    CHAT,
    CALL,
    VIDEO_CALL,
    MESSAGE,
    GIFT,
    DATE,
    OTHER
}
```

## 7.4 Memory

```kotlin
data class Memory(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val memoryDate: LocalDateTime,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## 7.5 MemoryRelationshipCrossRef

```kotlin
data class MemoryRelationshipCrossRef(
    val memoryId: String,
    val relationshipId: String
)
```

## 7.6 MemoryImage

```kotlin
data class MemoryImage(
    val id: String,
    val memoryId: String,
    val localPath: String?,
    val remoteUrl: String?,
    val createdAt: Instant
)
```

## 7.7 Recommendation

```kotlin
data class Recommendation(
    val id: String,
    val userId: String,
    val relationshipId: String?,
    val type: RecommendationType,
    val title: String,
    val message: String,
    val actionLabel: String,
    val status: RecommendationStatus,
    val createdAt: Instant,
    val expiresAt: Instant?
)
```

```kotlin
enum class RecommendationType {
    CHECK_IN,
    CALL,
    MEET,
    DATE,
    GIFT,
    BIRTHDAY,
    MEMORY_REMINDER,
    REFLECTION
}

enum class RecommendationStatus {
    ACTIVE,
    DISMISSED,
    COMPLETED,
    SNOOZED
}
```

## 7.8 Reflection

```kotlin
data class Reflection(
    val id: String,
    val userId: String,
    val type: ReflectionType,
    val moodScore: Int?,
    val socialSatisfactionScore: Int?,
    val note: String?,
    val createdAt: Instant
)
```

```kotlin
enum class ReflectionType {
    IMMEDIATE,
    END_OF_DAY
}
```

---

## 8. UI Design System

## 8.1 Visual Direction

UI phải tạo cảm giác:

- Personal
- Reliable
- Non-intrusive
- Warm
- Calm
- Friendly

## 8.2 Color Palette

Sử dụng palette chính theo hướng ấm + dịu.

### Primary App Palette

```text
Primary: Burnt Orange #CC5F18
Secondary: Cool Gray #9EA7AA
Neutral Background: Misty Rose #FFF8F6
Tertiary: Light Gray #BFBFBF
Error: Coral Red #F45955
On Primary/Text Dark: #4D4D4D
```

### Alternative Warm Palette

Có thể tham khảo thêm palette warm/neutral:

```text
Zapier Black: #201515
Cream White: #FFFEFB
Off-White: #FFFDF9
Zapier Orange: #FF4F00
Dark Charcoal: #36342E
Warm Gray: #939084
Sand: #C5C0B1
Light Sand: #ECEAE3
Mid Warm: #B5B2AA
```

### Implementation Requirement

Tạo theme trong:

```text
core/ui/theme/
```

Files gợi ý:

```text
Color.kt
Type.kt
Theme.kt
Shape.kt
Spacing.kt
```

Không hardcode màu trực tiếp trong screen nếu màu đó thuộc theme.

## 8.3 Typography

Ưu tiên font dễ đọc, friendly.

Font đề xuất:

- SF Pro nếu môi trường hỗ trợ.
- Nunito.
- Plus Jakarta Sans.
- System fallback.

Hierarchy:

| Element | Weight | Size Suggestion | Notes |
|---|---:|---:|---|
| Person name | SemiBold/Bold | 20-24sp | Nổi bật nhất |
| Screen title | Bold | 24-28sp | 100% dark |
| Section title | SemiBold | 18-20sp | Rõ ràng |
| Body | Regular | 14-16sp | Dễ đọc |
| Status/last interaction | Regular | 12-14sp | Nhỏ hơn |
| Notes | Regular | 14-16sp | Mềm, dễ đọc |
| Caption/timestamp | Regular | 11-12sp | Muted |

## 8.4 Layout

- Card-based.
- Bo góc mềm.
- Shadow nhẹ.
- Grid 8dp.
- Không dùng quá nhiều layer.
- Ưu tiên white/cream surfaces.
- Khoảng cách thoáng.

## 8.5 Motion

Motion cần:

- Nhẹ.
- Chậm vừa phải.
- Organic.
- Không flashy.

Ứng dụng motion cho:

- Fade + slide khi chuyển màn hình.
- Card expand khi mở profile.
- Timeline scroll mượt.
- Micro-interaction khi log interaction.
- Micro-interaction khi add memory.
- Micro-interaction khi nhận AI suggestion.

## 8.6 Iconography

- Dùng Material Symbols / Google Icons.
- Tên resource icon dùng prefix `ic_`.
- Tên image dùng prefix `img_`.

Ví dụ:

```text
ic_add_relationship.xml
ic_memory_pin.xml
img_empty_relationships.png
```

---

## 9. Screen List

Agent cần triển khai theo thứ tự ưu tiên MVP trước.

## 9.1 MVP Screens

1. LoginScreen
2. RegisterScreen
3. MainScaffoldWithBottomNav
4. MapScreen
5. RelationshipsScreen
6. RelationshipDetailScreen
7. AddRelationshipScreen
8. EditRelationshipScreen
9. AddInteractionScreen
10. InteractionHistoryScreen
11. AddMemoryScreen
12. MemoryDetailScreen
13. NotificationsScreen
14. ProfileScreen

## 9.2 Extended Screens

1. ChatbotScreen
2. ReflectionScreen
3. GiftSuggestionScreen
4. RecommendationDetailScreen
5. SettingsScreen
6. PrivacySettingsScreen
7. DataExportScreen

---

## 10. API Design đề xuất

Nếu backend được triển khai, dùng REST API.

## 10.1 Auth

```http
POST /auth/register
POST /auth/login
POST /auth/logout
POST /auth/refresh
GET  /auth/me
```

## 10.2 Relationships

```http
GET    /relationships
POST   /relationships
GET    /relationships/{id}
PUT    /relationships/{id}
DELETE /relationships/{id}
```

## 10.3 Interactions

```http
GET    /relationships/{relationshipId}/interactions
POST   /interactions
GET    /interactions/{id}
PUT    /interactions/{id}
DELETE /interactions/{id}
POST   /interactions/quick-log
```

## 10.4 Memories

```http
GET    /memories
POST   /memories
GET    /memories/{id}
PUT    /memories/{id}
DELETE /memories/{id}
POST   /memories/{id}/images
GET    /memories/map
```

## 10.5 Recommendations

```http
GET  /recommendations
POST /recommendations/{id}/complete
POST /recommendations/{id}/dismiss
POST /recommendations/{id}/snooze
```

## 10.6 Reflections

```http
GET  /reflections
POST /reflections
```

## 10.7 Notifications

```http
GET  /notifications
POST /notifications/{id}/read
```

## 10.8 Gifts

```http
GET /gifts/suggestions
GET /gifts
GET /gifts/{id}
```

---

## 11. Local-first / Offline Requirements

MVP nên hoạt động tốt kể cả khi mạng yếu.

### Required

- Cache relationships bằng Room.
- Cache interactions bằng Room.
- Cache memories metadata bằng Room.
- Cho phép tạo relationship/interaction/memory offline.
- Khi có mạng, sync lên backend nếu backend đã có.

### Sync Status

Mỗi entity local nên có:

```text
syncStatus: SYNCED | PENDING_CREATE | PENDING_UPDATE | PENDING_DELETE
lastSyncedAt
```

---

## 12. Security & Privacy Requirements

Ứng dụng xử lý dữ liệu cá nhân và dữ liệu nhạy cảm về đời sống xã hội, vì vậy bắt buộc:

- Không log thông tin nhạy cảm.
- Không hardcode API key trong source code.
- Token lưu an toàn.
- Ảnh/kỷ niệm riêng tư theo user.
- API phải check user ownership trước khi trả dữ liệu.
- Có khả năng xóa dữ liệu người dùng.
- Có setting tắt chatbot history nếu triển khai lưu hội thoại.
- Có privacy copy rõ ràng, ngắn gọn.

---

## 13. Non-functional Requirements

## 13.1 Ease of Use

- Giao diện đơn giản.
- Dễ thao tác với người dùng phổ thông.
- Các flow chính không nên quá 3 bước.
- Empty state phải có hướng dẫn.

## 13.2 Performance

- App phản hồi nhanh.
- Không block UI thread.
- Sử dụng coroutine/Flow hợp lý.
- LazyColumn cho danh sách.
- Paging nếu danh sách dài.
- Image loading cần cache.

## 13.3 Recommendation Accuracy

- Gợi ý phải dựa trên dữ liệu thực tế.
- Không random vô nghĩa.
- Có thể giải thích ngắn lý do gợi ý.
- Cho phép dismiss/snooze để cải thiện trải nghiệm.

## 13.4 Reliability

- Handle loading/error/empty state cho mọi screen.
- Không crash khi dữ liệu null.
- Offline fallback.
- Retry network request khi hợp lý.

## 13.5 Scalability

- Tách feature module/package rõ ràng.
- Repository abstraction.
- Dễ thay rule-based recommendation bằng AI/ML sau này.

---

## 14. Coding Rules

## 14.1 Kotlin Style

Áp dụng Kotlin official style:

- Class/Object: PascalCase
- Function/variable: camelCase
- Constant: UPPER_SNAKE_CASE
- File name khớp tên class chính nếu có.
- Không viết function quá dài.
- Tách composable nhỏ theo responsibility.

## 14.2 Strings

Không hardcode text trong file `.kt`.

Bắt buộc đưa text vào:

```text
res/values/strings.xml
```

Ví dụ:

```xml
<string name="add_relationship">Add relationship</string>
<string name="relationship_empty_title">No relationships yet</string>
```

## 14.3 Icons & Images

Naming convention:

```text
ic_*
img_*
bg_*
```

Ví dụ:

```text
ic_chat.xml
ic_memory_pin.xml
img_empty_map.png
bg_auth_header.png
```

## 14.4 Compose Rules

- Screen composable chỉ điều phối state, không chứa logic nghiệp vụ phức tạp.
- ViewModel xử lý UI state.
- Repository xử lý data source.
- Không gọi API trực tiếp từ composable.
- Mọi screen phải có preview nếu có thể.
- Dùng `collectAsStateWithLifecycle`.

## 14.5 State Management

Mỗi screen nên có:

```kotlin
data class ScreenUiState(
    val isLoading: Boolean = false,
    val data: ...,
    val errorMessage: String? = null
)
```

Event pattern gợi ý:

```kotlin
sealed interface ScreenUiEvent {
    data object Load : ScreenUiEvent
    data class OnNameChanged(val value: String) : ScreenUiEvent
    data object Submit : ScreenUiEvent
}
```

---

## 15. Implementation Roadmap

## Phase 1 — Project Setup

- Tạo Android project.
- Setup package structure.
- Setup Material 3 theme.
- Setup Hilt.
- Setup Navigation Compose.
- Setup Room.
- Setup DataStore.
- Tạo MainScaffold với Bottom Navigation.

## Phase 2 — Auth

- Login/Register UI.
- Auth repository.
- Token storage.
- Mock backend hoặc fake repository nếu backend chưa có.

## Phase 3 — Relationship Core

- Relationship entity/DAO.
- Relationship repository.
- RelationshipsScreen.
- RelationshipDetailScreen.
- Add/Edit/Delete relationship.
- Search/filter.

## Phase 4 — Interaction Tracking

- Interaction entity/DAO.
- AddInteractionScreen.
- InteractionHistoryScreen.
- Quick daily tick.
- Update last interaction in relationship list.

## Phase 5 — Attention Score & Recommendation MVP

- Rule-based attention score.
- RecommendationEngine.
- Recommendation cards.
- Notifications screen basic.

## Phase 6 — Memory & Map

- Memory entity/DAO.
- AddMemoryScreen.
- Image picker.
- Location input.
- OpenStreetMap integration.
- Memory markers.
- Memory detail.

## Phase 7 — Notification & Streak

- WorkManager daily reminders.
- Local notifications.
- Streak calculation.
- Profile streak display.

## Phase 8 — Reflection & Chatbot

- Reflection questionnaire.
- Chatbot screen.
- Optional LLM integration.
- Privacy settings for chat history.

## Phase 9 — Backend Sync

- FastAPI backend.
- PostgreSQL schema.
- JWT auth.
- API integration with Retrofit.
- Sync local pending changes.

---

## 16. Definition of Done

Một feature chỉ được coi là hoàn thành khi:

- Có UI hoàn chỉnh theo design system.
- Có loading state.
- Có empty state.
- Có error state.
- Không hardcode string trong Kotlin.
- Không có crash với null/empty data.
- Có ViewModel.
- Có Repository.
- Có local data handling nếu feature cần lưu dữ liệu.
- Có navigation đúng.
- Code đặt đúng feature package.
- Tên file/class/function đúng convention.
- Có thể chạy được trên emulator.

---

## 17. MVP Acceptance Criteria

MVP hoàn chỉnh cần đáp ứng:

1. User có thể đăng ký/đăng nhập/đăng xuất.
2. User có thể tạo/xem/sửa/xóa relationship.
3. User có thể log interaction cho relationship.
4. User có thể xem lịch sử interaction.
5. App tính được attention status cơ bản.
6. App gợi ý ít nhất một hành động dựa trên interaction history.
7. User có thể tạo memory với ảnh/thời gian/địa điểm.
8. User có thể xem memory trên map.
9. App có notification/reminder local cơ bản.
10. UI dùng bottom navigation 5 tab.
11. Code tổ chức theo feature.
12. Strings không hardcode trong Kotlin.
13. App chạy ổn định trên Android emulator.

---

## 18. Important Agent Instructions

Khi coding agent thực hiện task:

1. Đọc file này trước khi code.
2. Nếu task mơ hồ, ưu tiên kiến trúc và design system trong file này.
3. Không tạo code nằm ngoài package structure đã quy định nếu không cần thiết.
4. Không hardcode string trong Kotlin.
5. Không hardcode màu nếu màu thuộc theme.
6. Luôn tạo loading/error/empty state cho màn hình có dữ liệu.
7. Với feature mới, tạo đủ:
   - UI
   - ViewModel
   - Repository
   - Model/Entity nếu cần
   - Navigation route nếu cần
8. Với dữ liệu nhạy cảm, không log ra console.
9. Nếu backend chưa có, dùng fake repository hoặc local Room trước.
10. Ưu tiên MVP chạy được trước, sau đó mới mở rộng AI/ML.
11. Không làm UI quá kỹ thuật; luôn giữ cảm giác gần gũi, nhẹ nhàng.
12. Recommendation phải có lý do và hành động rõ ràng.
13. Chatbot phải tôn trọng quyền riêng tư và tránh lời khuyên nguy hiểm.
14. Khi thêm dependency, giải thích ngắn lý do.
15. Sau mỗi task, báo lại:
    - Files changed
    - What was implemented
    - How to test
    - Known limitations

---

## 19. Suggested First Prompt for Coding Agent

```text
Bạn đang code ứng dụng Android CloseNest. Hãy đọc file `CLOSENEST_AGENT_SPEC.md` trước.

Nhiệm vụ hiện tại:
- Setup project architecture theo package structure trong spec.
- Tạo Material 3 theme với palette CloseNest.
- Tạo MainActivity dùng Navigation Compose.
- Tạo bottom navigation 5 tab: Map, Relationships, Add, Notifications, Profile.
- Tạo placeholder screen cho từng tab.
- Không hardcode string trong Kotlin, đưa text vào strings.xml.
- Code theo MVVM và chuẩn Kotlin official style.

Sau khi làm xong, hãy liệt kê files changed và cách test trên emulator.
```

---

## 20. Notes for Future AI/ML Extension

MVP dùng rule-based recommendation. Sau này có thể nâng cấp:

- ML model dự đoán relationship attention.
- LLM tạo gợi ý tin nhắn.
- Sentiment analysis cho notes/reflection.
- Personalized recommendation dựa trên feedback dismiss/complete.
- Clustering mối quan hệ theo hành vi tương tác.

Khi nâng cấp AI/ML:

- Không gửi dữ liệu nhạy cảm lên model nếu chưa có consent.
- Ẩn chi tiết kỹ thuật khỏi UI.
- Cho phép người dùng tắt AI suggestion.
- Cần có fallback rule-based khi AI lỗi.
