// PostService.java

package com.tim.appTim.service;

import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // Không cần FileRepository ở đây nữa nếu dùng cascade
    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PostDTO createPostWithFiles(Long userId, String content, Post.Privacy privacy, List<File> filesFromController) {
        // 1. Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 2. Tạo đối tượng Post
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        post.setPrivacy(privacy);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        // 3. Gán các file vào Post (sử dụng phương thức tiện ích đã tạo)
        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                post.addFile(file); // <-- SỬA Ở ĐÂY: Gán Post cho File và thêm File vào List
            }
        }

        // 4. Lưu Post. Nhờ CascadeType.ALL, các File cũng sẽ được tự động lưu.
        Post savedPost = postRepository.save(post);

        // 5. Tạo DTO để trả về
        // Lưu ý: nên có FileDTO để tránh lộ chi tiết của Entity
        return new PostDTO(
                savedPost.getId(),
                savedPost.getUser().getId(), // Lấy ID từ đối tượng User
                savedPost.getContent(),
                savedPost.getPrivacy().name(),
                savedPost.getCreatedAt(),
                savedPost.getUpdatedAt(),
                new ArrayList<>(), // comments
                new ArrayList<>(), // reactions
                savedPost.getFiles() // Lấy danh sách file đã được lưu
        );
    }
}