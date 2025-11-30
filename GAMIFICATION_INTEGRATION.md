# Hướng dẫn tích hợp Gamification vào các Service

Tài liệu này mô tả cách tích hợp hệ thống Gamification vào các service hiện có để tự động trao điểm khi người dùng hoàn thành các hành vi.

## Tổng quan

Hệ thống Gamification đã được tích hợp vào các service sau:
1. **AttendanceService** - Điểm danh đúng giờ
2. **GradeService** - Đạt điểm cao (80%, 95%)
3. **PostService** - Đăng bài viết đầu tiên, bài viết có >10 likes, bài viết chia sẻ
4. **ReactionService** - Kiểm tra khi bài viết đạt >10 likes
5. **NewsService** - Lần đầu truy cập tin tức

## Cách sử dụng

### 1. Inject GamificationService

Trong mỗi service cần tích hợp, inject `GamificationService`:

```java
private final GamificationService gamificationService;

public YourService(..., GamificationService gamificationService) {
    this.gamificationService = gamificationService;
}
```

### 2. Gọi awardPoints() khi điều kiện được thỏa mãn

```java
try {
    gamificationService.awardPoints(userId, "BEHAVIOR_CODE");
} catch (Exception e) {
    // Log error nhưng không làm gián đoạn flow chính
    log.error("Failed to award points for behavior: {}", e.getMessage());
}
```

**Lưu ý quan trọng:**
- Luôn wrap trong try-catch để không làm gián đoạn flow chính
- Gamification là tính năng phụ, không nên ảnh hưởng đến business logic chính

## Mapping hành vi với Behavior Code

Dựa vào dữ liệu trong `abc.md`, các behavior code tương ứng:

| Behavior Code | Mô tả | Điều kiện kích hoạt |
|--------------|-------|---------------------|
| `ATTEND_ON_TIME` | Điểm danh đúng giờ | Status = `present` và không `late` |
| `HIGH_POINT_1` | Đạt hơn 80% điểm | Điểm >= 80% (chỉ trao 1 lần) |
| `HIGH_POINT_2` | Đạt hơn 95% điểm | Điểm >= 95% (chỉ trao 1 lần) |
| `FIRST_POST` | Đăng bài viết đầu tiên | Lần đầu tạo bài viết (chỉ trao 1 lần) |
| `POST'S_LIKE` | Bài viết có hơn 10 lượt thích | `totalReactions >= 10` (tối đa 10 lần/tháng) |
| `POST_SHARE` | Đăng bài viết chia sẻ kiến thức | Bài viết có link hoặc nội dung chia sẻ (tối đa 10 lần/tháng) |
| `READ_BLOG` | Lần đầu tiên truy cập tin tức | Lần đầu gọi API news (chỉ trao 1 lần) |
| `GIVING_SCORES` | Cho điểm 10 cho học sinh | Khi giáo viên chấm điểm 10 cho học sinh (tối đa 10 lần/tháng) |

## Chi tiết tích hợp

### 1. AttendanceService

**File:** `src/main/java/com/tim/appTim/service/AttendanceService.java`

**Vị trí:** Sau khi lưu `AttendanceRecord` với status = `present` và session không `isLate`

**Logic:**
```java
if (record.getStatus() == AttendanceRecord.AttendanceStatus.present && 
    !session.getIsLate()) {
    try {
        gamificationService.awardPoints(record.getStudentId().longValue(), "ATTEND_ON_TIME");
    } catch (Exception e) {
        log.error("Failed to award points for attendance", e);
    }
}
```

### 2. GradeService

**File:** `src/main/java/com/tim/appTim/service/GradeServiceImpl.java`

**Vị trí:** Trong method `checkAndNotify()` sau khi tính điểm tổng

**Logic:**
- Tính điểm tổng: `(theoryScore + practiceScore) / 2`
- Nếu >= 95%: Trao `HIGH_POINT_2` (chỉ trao 1 lần)
- Nếu >= 80%: Trao `HIGH_POINT_1` (chỉ trao 1 lần)

### 3. PostService

**File:** `src/main/java/com/tim/appTim/service/PostService.java`

**Vị trí 1:** Trong `createPostWithFiles()` - Kiểm tra bài viết đầu tiên
```java
// Kiểm tra xem đây có phải bài viết đầu tiên không
long postCount = postRepository.countByUserId(userId);
if (postCount == 1) {
    try {
        gamificationService.awardPoints(userId, "FIRST_POST");
    } catch (Exception e) {
        log.error("Failed to award points for first post", e);
    }
}
```

**Vị trí 2:** Trong `createPostWithFiles()` - Kiểm tra bài viết chia sẻ
```java
// Kiểm tra nếu bài viết có link (chia sẻ kiến thức)
if (savedPost.getLinkUrl() != null) {
    try {
        gamificationService.awardPoints(userId, "POST_SHARE");
    } catch (Exception e) {
        log.error("Failed to award points for post share", e);
    }
}
```

### 4. ReactionService

**File:** `src/main/java/com/tim/appTim/service/ReactionService.java`

**Vị trí:** Trong `createOrUpdateReaction()` sau khi lưu reaction

**Logic:**
```java
// Cập nhật totalReactions
post.setTotalReactions(post.getReactions().size());
postRepository.save(post);

// Kiểm tra nếu đạt >10 likes
if (post.getTotalReactions() >= 10) {
    try {
        gamificationService.awardPoints(post.getUser().getId(), "POST'S_LIKE");
    } catch (Exception e) {
        log.error("Failed to award points for post likes", e);
    }
}
```

### 5. NewsService

**File:** `src/main/java/com/tim/appTim/service/NewsService.java`

**Vị trí:** Trong các method `getLatestBlogs()`, `getFeaturedBlogs()`, `getTechNews()`

**Logic:**
- Cần một bảng hoặc cache để track user đã đọc blog chưa
- Nếu lần đầu: Trao `READ_BLOG`

**Lưu ý:** Có thể cần tạo một entity `UserBlogRead` để track.

### 6. Giving Scores (Chấm điểm cho học sinh)

**File:** `src/main/java/com/tim/appTim/service/GradeServiceImpl.java`

**Vị trí:** Sau khi lưu grades, đếm số lần giáo viên đã cho điểm 10

**Logic:**
- Sau khi giáo viên chấm điểm, kiểm tra xem có điểm nào = 10 không (theoryScore = 10 hoặc practiceScore = 10)
- Mỗi lần cho điểm 10, trao điểm `GIVING_SCORES` cho giáo viên (không phải học sinh)
- Tối đa 10 lần/tháng (được quản lý bởi GamificationService dựa trên frequencyType = MONTHLY)

---

## Testing

Sau khi tích hợp, test các trường hợp:

1. ✅ Điểm danh đúng giờ → Nhận điểm `ATTEND_ON_TIME`
2. ✅ Đạt điểm 85% → Nhận điểm `HIGH_POINT_1`
3. ✅ Đạt điểm 96% → Nhận điểm `HIGH_POINT_2`
4. ✅ Đăng bài viết đầu tiên → Nhận điểm `FIRST_POST`
5. ✅ Bài viết đạt 10 likes → Nhận điểm `POST'S_LIKE`
6. ✅ Đăng bài viết có link → Nhận điểm `POST_SHARE`
7. ✅ Lần đầu đọc blog → Nhận điểm `READ_BLOG`
8. ✅ Chấm điểm 10 cho 10 bạn → Nhận điểm `GIVING_SCORES`

---

**Tài liệu này được cập nhật:** 2025-11-30

