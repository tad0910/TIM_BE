# Phương án Quản lý Xếp hạng Gamification

## Tổng quan

Hệ thống xếp hạng sẽ dựa trên điểm gamification (Diligence, Competence, Experience) và được quản lý theo tháng.

## 1. Quản lý Hệ thống Xếp hạng

### 1.1. Cấu trúc dữ liệu

**Bảng `ranking` (đã cập nhật):**
- `id`: ID ranking
- `nguoi_dung_id`: ID người dùng
- `total_diligence_score`: Tổng điểm chuyên cần (từ gamification)
- `total_competence_score`: Tổng điểm năng lực (từ gamification)
- `total_experience_score`: Tổng điểm kinh nghiệm (từ gamification)
- `classes_id`: ID lớp học (optional)
- `update_time`: Thời gian cập nhật
- `courses_id`: ID khóa học (optional)

**Lưu ý:** 
- Đã bỏ cột `diem_tong_hop` và `program_id` theo yêu cầu.
- Tên cột có suffix `_score` để tránh trùng với các cột trong bảng khác.

**Bảng mới cần tạo: `ranking_monthly`**
```sql
CREATE TABLE ranking_monthly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    month_year VARCHAR(7) NOT NULL, -- Format: 'YYYY-MM' (ví dụ: '2025-11')
    total_diligence_score INT DEFAULT 0,
    total_competence_score INT DEFAULT 0,
    total_experience_score INT DEFAULT 0,
    rank_position INT, -- Vị trí xếp hạng trong tháng (tính từ total_experience_score hoặc tổng điểm)
    class_id INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_month (user_id, month_year, class_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    INDEX idx_month_year (month_year),
    INDEX idx_total_experience_score (total_experience_score DESC),
    INDEX idx_total_competence_score (total_competence_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Lưu ý:** Không có `program_id` và `total_score` trong bảng monthly.

### 1.2. Công thức xếp hạng

**Vì không có điểm tổng hợp, xếp hạng sẽ dựa trên:**

**Option 1: Xếp hạng theo Experience (khuyến nghị)**
- Ưu tiên `total_experience_score` (cao nhất)
- Nếu bằng nhau, xét `total_competence_score`
- Nếu vẫn bằng nhau, xét `total_diligence_score`

**Option 2: Xếp hạng theo Competence**
- Ưu tiên `total_competence_score`
- Nếu bằng nhau, xét `total_experience_score`
- Nếu vẫn bằng nhau, xét `total_diligence_score`

**Option 3: Xếp hạng theo tổng điểm (tính toán tạm thời)**
- Tính `totalScore = totalDiligenceScore + totalCompetenceScore + totalExperienceScore` (chỉ để xếp hạng, không lưu)
- Xếp hạng theo `totalScore` giảm dần

### 1.3. Logic cập nhật Ranking

**Khi nào cập nhật:**
1. Mỗi khi user nhận điểm mới (trong `GamificationService.awardPoints()`)
2. Tự động tính lại ranking khi có thay đổi điểm
3. Cuối mỗi tháng: Tạo snapshot vào `ranking_monthly`

**Cách cập nhật:**
1. Lấy `UserGamificationStats` của user
2. Cập nhật `total_diligence_score`, `total_competence_score`, `total_experience_score` vào bảng `ranking`
3. Tính lại vị trí xếp hạng (rank_position) cho tất cả users dựa trên tiêu chí xếp hạng đã chọn

## 2. Hiển thị Bảng Xếp hạng theo Tháng

### 2.1. Lưu snapshot theo tháng

**Khi nào lưu:**
- Tự động vào cuối mỗi tháng (scheduled job)
- Hoặc khi admin yêu cầu tạo snapshot

**Cách lưu:**
1. Lấy tất cả `UserGamificationStats` tại thời điểm cuối tháng
2. Lấy `total_diligence`, `total_competence`, `total_experience` cho mỗi user
3. Sắp xếp theo tiêu chí xếp hạng (ví dụ: `total_experience_score` giảm dần)
4. Gán `rank_position` cho mỗi user
5. Lưu vào bảng `ranking_monthly` với `month_year = 'YYYY-MM'`

### 2.2. API Endpoints

**1. Lấy bảng xếp hạng hiện tại (real-time)**
```
GET /gamification/ranking/current
Query params:
  - classId (optional): Lọc theo lớp
  - sortBy (optional): 'experience' | 'competence' | 'diligence' | 'total' (default: 'experience')
  - limit (optional): Số lượng kết quả (default: 100)
  - offset (optional): Phân trang
```

**2. Lấy bảng xếp hạng theo tháng**
```
GET /gamification/ranking/monthly
Query params:
  - monthYear (required): Format 'YYYY-MM' (ví dụ: '2025-11')
  - classId (optional): Lọc theo lớp
  - sortBy (optional): 'experience' | 'competence' | 'diligence' | 'total' (default: 'experience')
  - limit (optional): Số lượng kết quả (default: 100)
  - offset (optional): Phân trang
```

**3. Lấy vị trí xếp hạng của user hiện tại**
```
GET /gamification/ranking/my-position
Query params:
  - monthYear (optional): Format 'YYYY-MM', nếu không có thì lấy tháng hiện tại
  - classId (optional): Lọc theo lớp
  - sortBy (optional): 'experience' | 'competence' | 'diligence' | 'total' (default: 'experience')
```

**4. Tạo snapshot cho tháng hiện tại (Admin only)**
```
POST /gamification/ranking/snapshot
Body:
  - monthYear (optional): Format 'YYYY-MM', nếu không có thì dùng tháng hiện tại
```

## 3. Implementation Plan

### Phase 1: Tạo Entity và Repository
1. Tạo `RankingMonthly` entity
2. Tạo `RankingMonthlyRepository`
3. Tạo `RankingRepository` (nếu chưa có)

### Phase 2: Tạo Service
1. Tạo `RankingService` với các method:
   - `updateRanking()`: Cập nhật ranking cho user (từ UserGamificationStats)
   - `recalculateAllRankings()`: Tính lại ranking cho tất cả users
   - `createMonthlySnapshot()`: Tạo snapshot cho tháng
   - `getCurrentRanking()`: Lấy bảng xếp hạng hiện tại
   - `getMonthlyRanking()`: Lấy bảng xếp hạng theo tháng
   - `getUserRankPosition()`: Lấy vị trí xếp hạng của user
   - `sortRankings()`: Sắp xếp ranking theo tiêu chí (experience/competence/diligence/total)

### Phase 3: Tích hợp với GamificationService
1. Sau khi cập nhật `UserGamificationStats`, gọi `RankingService.updateRanking()`
2. Tính lại ranking cho user đó

### Phase 4: Tạo Controller
1. Tạo `GamificationRankingController`
2. Implement các endpoints như trên

### Phase 5: Scheduled Job (Optional)
1. Tạo scheduled job để tự động tạo snapshot vào cuối mỗi tháng
2. Sử dụng `@Scheduled` annotation

## 4. Response Format

**GET /gamification/ranking/current?sortBy=experience**
```json
{
  "rankings": [
    {
      "userId": 1,
      "username": "user1",
      "totalDiligenceScore": 100,
      "totalCompetenceScore": 200,
      "totalExperienceScore": 300,
      "rankPosition": 1,
      "classId": 1,
      "className": "Fullstack K2025"
    },
    {
      "userId": 2,
      "username": "user2",
      "totalDiligenceScore": 80,
      "totalCompetenceScore": 150,
      "totalExperienceScore": 250,
      "rankPosition": 2,
      "classId": 1,
      "className": "Fullstack K2025"
    }
  ],
  "total": 50,
  "page": 0,
  "size": 100,
  "sortBy": "experience"
}
```

**GET /gamification/ranking/monthly?monthYear=2025-11&sortBy=experience**
```json
{
  "monthYear": "2025-11",
  "rankings": [
    {
      "userId": 1,
      "username": "user1",
      "totalDiligenceScore": 100,
      "totalCompetenceScore": 200,
      "totalExperienceScore": 300,
      "rankPosition": 1,
      "classId": 1,
      "className": "Fullstack K2025"
    }
  ],
  "total": 50,
  "sortBy": "experience"
}
```

## 5. Lưu ý quan trọng

1. **Performance**: 
   - Index trên `total_score` để query nhanh
   - Cache ranking nếu cần
   - Tính lại ranking theo batch, không tính từng user một

2. **Consistency**:
   - Đảm bảo ranking được cập nhật đồng bộ với `UserGamificationStats`
   - Sử dụng transaction khi cập nhật

3. **Ranking Position**:
   - Xếp hạng dựa trên `sortBy` parameter:
     - `experience`: totalExperienceScore DESC → totalCompetenceScore DESC → totalDiligenceScore DESC → userId ASC
     - `competence`: totalCompetenceScore DESC → totalExperienceScore DESC → totalDiligenceScore DESC → userId ASC
     - `diligence`: totalDiligenceScore DESC → totalExperienceScore DESC → totalCompetenceScore DESC → userId ASC
     - `total`: (totalDiligenceScore + totalCompetenceScore + totalExperienceScore) DESC → userId ASC

4. **Monthly Snapshot**:
   - Chỉ lưu snapshot, không xóa dữ liệu trong `ranking`
   - Snapshot giúp xem lại lịch sử xếp hạng

---

**Tài liệu này được tạo:** 2025-11-30

