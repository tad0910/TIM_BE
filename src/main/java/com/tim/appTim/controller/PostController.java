    package com.tim.appTim.controller;

    import java.io.IOException;
    import java.nio.file.*;
    import java.util.*;

    import com.tim.appTim.entity.User;
    import com.tim.appTim.service.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.security.core.Authentication;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;

    import com.tim.appTim.dto.PostDTO;
    import com.tim.appTim.entity.File;
    import com.tim.appTim.entity.Post;
    import com.tim.appTim.service.PostService;

    @RestController
    @RequestMapping("/posts")
    public class PostController {

        private final PostService postService;
        private final UserService userService;

        @Value("${upload.folder}")
        private String uploadFolder;

        @Autowired
        public PostController(PostService postService, UserService userService) {

            this.postService = postService;
            this.userService = userService;
        }

        private User getUserFromAuthentication(Authentication authentication) {
            return userService.findByUsernameOrEmail(authentication.getName());
        }

        @PostMapping("/create")
        @PreAuthorize("hasAuthority('post:create')")
        public ResponseEntity<PostDTO> createPost(
                Authentication authentication,
                @RequestParam("content") String content,
                @RequestParam("privacy") String privacy,
                @RequestParam(value = "files", required = false) List<MultipartFile> multipartFiles
        ) {
            try {
                User currentUser = getUserFromAuthentication(authentication);
                Post.Privacy privacyEnum = Post.Privacy.valueOf(privacy);
                List<File> files = new ArrayList<>();

                if (multipartFiles != null && !multipartFiles.isEmpty()) {
                    for (MultipartFile mf : multipartFiles) {
                        if (mf.isEmpty()) continue;

                        String originalName = mf.getOriginalFilename();
                        String fileExt = originalName != null && originalName.contains(".")
                                ? originalName.substring(originalName.lastIndexOf("."))
                                : "";

                        String uniqueName = UUID.randomUUID().toString() + fileExt;
                        Path uploadPath = Paths.get(uploadFolder, uniqueName);
                        Files.createDirectories(uploadPath.getParent());
                        Files.write(uploadPath, mf.getBytes());

                        String lowerName = originalName != null ? originalName.toLowerCase() : "";
                        File.FileType fileType;
                        if (lowerName.matches(".*\\.(mp4|mov|avi)$")) {
                            fileType = File.FileType.VIDEO;
                        } else if (lowerName.matches(".*\\.(pdf|docx?|xlsx?|pptx?|txt|rtf|zip|rar|7z)$")) {
                            fileType = File.FileType.DOCUMENT;
                        } else {
                            fileType = File.FileType.IMAGE;
                        }

                        File f = new File();
                        f.setFileUrl("/uploads/" + uniqueName);
                        f.setFileName(originalName);
                        f.setFileSize(mf.getSize());
                        f.setFileType(fileType);
                        files.add(f);
                    }
                }

                PostDTO createdPost = postService.createPostWithFiles(currentUser.getId(), content, privacyEnum, files);
                return ResponseEntity.ok(createdPost);

            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(null);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(null);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(null);
            }
        }

        @GetMapping
        public ResponseEntity<Page<PostDTO>> getAllPosts(Pageable pageable) {
            Page<PostDTO> posts = postService.getAllPosts(pageable);
            return ResponseEntity.ok(posts);
        }

        @GetMapping("/user/{userId}")
        public ResponseEntity<List<PostDTO>> getPostsByUserId(@PathVariable Long userId) {
            List<PostDTO> posts = postService.getPostsByUserId(userId);
            return ResponseEntity.ok(posts);
        }

        @GetMapping("/{postId}/user/{userId}")
        public ResponseEntity<PostDTO> getPostByIdForUser(
                @PathVariable Long postId,
                @PathVariable Long userId
        ) {
            PostDTO post = postService.getPostByIdForUser(userId, postId);
            return ResponseEntity.ok(post);
        }

        @PutMapping("/{postId}")
        @PreAuthorize("hasAuthority('post:update_all') or @postService.isOwner(authentication, #postId)")
        public ResponseEntity<PostDTO> updatePost(
                @PathVariable Long postId,
                Authentication authentication,
                @RequestParam("content") String content,
                @RequestParam("privacy") String privacy,
                @RequestParam(value = "files", required = false) List<MultipartFile> multipartFiles,
                @RequestParam(value = "fileIdsToDelete", required = false) List<Integer> fileIdsToDelete
        ) {
            System.out.println("File IDs to delete received from request: " + fileIdsToDelete);
            try {
                User currentUser = getUserFromAuthentication(authentication);
                Post.Privacy privacyEnum = Post.Privacy.valueOf(privacy);
                List<File> files = new ArrayList<>();

                if (multipartFiles != null && !multipartFiles.isEmpty()) {
                    for (MultipartFile mf : multipartFiles) {
                        if (mf.isEmpty()) continue;

                        String originalName = mf.getOriginalFilename();
                        String fileExt = originalName != null && originalName.contains(".")
                                ? originalName.substring(originalName.lastIndexOf("."))
                                : "";

                        String uniqueName = UUID.randomUUID().toString() + fileExt;
                        Path uploadPath = Paths.get(uploadFolder, uniqueName);
                        Files.createDirectories(uploadPath.getParent());
                        Files.write(uploadPath, mf.getBytes());

                        String lowerName = originalName != null ? originalName.toLowerCase() : "";
                        File.FileType fileType;
                        if (lowerName.matches(".*\\.(mp4|mov|avi)$")) {
                            fileType = File.FileType.VIDEO;
                        } else if (lowerName.matches(".*\\.(pdf|docx?|xlsx?|pptx?|txt|rtf|zip|rar|7z)$")) {
                            fileType = File.FileType.DOCUMENT;
                        } else {
                            fileType = File.FileType.IMAGE;
                        }

                        File f = new File();
                        f.setFileUrl("/uploads/" + uniqueName);
                        f.setFileName(originalName);
                        f.setFileSize(mf.getSize());
                        f.setFileType(fileType);
                        files.add(f);
                    }
                }

                PostDTO updatedPost = postService.updatePostWithFiles(currentUser.getId(), postId, content, privacyEnum, files, fileIdsToDelete);
                return ResponseEntity.ok(updatedPost);

            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        }

        @DeleteMapping("/{postId}")
        @PreAuthorize("hasAuthority('post:delete_all') or @postService.isOwner(authentication, #postId)")
        public ResponseEntity<String> deletePost(
                @PathVariable Long postId,
                Authentication authentication) {
            User currentUser = getUserFromAuthentication(authentication);
            postService.deletePost(currentUser.getId(), postId);
            return ResponseEntity.ok("Post with id " + postId + " deleted successfully.");
        }
    }

