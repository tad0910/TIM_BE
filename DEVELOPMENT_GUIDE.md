### Programs APIs
- `GET /api/programs` - Lấy danh sách programs
- `GET /api/programs/{id}` - Lấy chi tiết program với modules
- `POST /api/programs` - Tạo program mới
- `PUT /api/programs/{id}` - Cập nhật program
- `DELETE /api/programs/{id}` - Xóa program
- `POST /api/programs/{id}/modules` - Thêm module vào program

### Modules APIs
- `GET /api/modules` - Lấy danh sách modules
- `GET /api/modules/{id}` - Lấy chi tiết module
- `POST /api/modules` - Tạo module mới
- `PUT /api/modules/{id}` - Cập nhật module
- `DELETE /api/modules/{id}` - Xóa module

### Module Sessions APIs
- `GET /api/modules/{moduleId}/sessions` - Lấy danh sách sessions
- `GET /api/sessions/{sessionId}` - Lấy chi tiết session
- `POST /api/modules/{moduleId}/sessions` - Tạo session mới
- `PUT /api/sessions/{sessionId}` - Cập nhật session
- `DELETE /api/sessions/{sessionId}` - Xóa session

### Classes APIs (Mở rộng)
- `GET /api/classes/{id}/details` - Lấy chi tiết lớp học (giáo viên, chương trình, học viên)
- `PUT /api/classes/{id}/teacher` - Gán/thay đổi giáo viên
- `GET /api/teachers/{teacherId}/classes` - Lấy danh sách lớp của giáo viên

