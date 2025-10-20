# Hướng dẫn sử dụng API Comment, Reaction và Reply Comment

## 1. API Comments

### Tạo comment mới
```
POST /api/comments/posts/{postId}
Parameters:
- userId: Long (required)
- content: String (required)
- emotion: String (optional) - like, love, haha, sad, angry
- fileId: Long (optional)

curl --location 'http://localhost:8081/api/comments/posts/1' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Authorization: Bearer 'Token' \
--data-urlencode 'userId=33' \
--data-urlencode 'content=This is a test comment 2'


```
### Lấy tất cả comments của một post

GET /api/comments/posts/{postId}


### Cập nhật comment
```
PUT /api/comments/{commentId}
Parameters:
- userId: Long (required)
- content: String (required)


```
### Xóa comment

DELETE /api/comments/{commentId}
Parameters:
- userId: Long (required)


## 2. API Reply Comments

### Tạo reply comment
```
POST /api/comments/{commentId}/replies
Parameters:
- content: String (required)
- emotion: String (optional) - like, love, haha, sad, angry
- fileId: Long (optional)

```
### Lấy tất cả reply comments của một comment

GET /api/comments/{commentId}/replies


### Cập nhật reply comment
```
PUT /api/comments/replies/{replyCommentId}
Parameters:
- content: String (required)
- emotion: String (optional)

```
### Xóa reply comment
```
DELETE /api/comments/replies/{replyCommentId}

```
## 3. API Reactions

### Tạo hoặc cập nhật reaction
```
POST /api/reactions/posts/{postId}
Parameters:
- userId: Long (required)
- emotionType: String (required) - like, love, haha, wow, sad, angry

```
### Lấy tất cả reactions của một post

GET /api/reactions/posts/{postId}



### Xóa reaction

DELETE /api/reactions/posts/{postId}
Parameters:
- userId: Long (required)


### Kiểm tra user đã reaction chưa
```
GET /api/reactions/posts/{postId}/users/{userId}/has-reacted



### Lấy reaction của user cho một post
```
GET /api/reactions/posts/{postId}/users/{userId}


### Đếm số lượng reaction theo loại
```
GET /api/reactions/posts/{postId}/count/{emotionType}






