package com.tim.appTim.service;

import com.tim.appTim.entity.File;
import com.tim.appTim.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class GamificationGuideService {

    private final FileRepository fileRepository;
    private final FileUploadService fileUploadService;
    
    // Prefix để đánh dấu file là gamification guide
    private static final String GAMIFICATION_GUIDE_PREFIX = "GAMIFICATION_GUIDE_";

    public GamificationGuideService(
            FileRepository fileRepository,
            FileUploadService fileUploadService) {
        this.fileRepository = fileRepository;
        this.fileUploadService = fileUploadService;
    }

    /**
     * Upload file hướng dẫn gamification
     * Nếu đã có file, sẽ vô hiệu hóa file cũ (xóa) và tạo file mới
     */
    @Transactional
    public File uploadGuide(MultipartFile file, Long uploadedBy) {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        // Kiểm tra định dạng file
        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        if (!isValidFileType(fileExtension)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF, DOC, DOCX. File hiện tại: " + fileExtension);
        }

        // Upload file lên Cloudinary
        String fileUrl = fileUploadService.uploadFile(file);

        // Xóa file cũ (nếu có) - soft delete bằng cách xóa record
        Optional<File> oldGuide = fileRepository.findActiveGamificationGuide();
        if (oldGuide.isPresent()) {
            fileRepository.delete(oldGuide.get());
        }

        // Tạo file mới với prefix để đánh dấu
        File newFile = new File();
        newFile.setFileUrl(fileUrl);
        newFile.setFileName(GAMIFICATION_GUIDE_PREFIX + originalFileName);
        newFile.setFileSize(file.getSize());
        newFile.setFileType(File.FileType.DOCUMENT);
        // Không set post, comment, replyComment để đánh dấu đây là gamification guide

        return fileRepository.save(newFile);
    }

    /**
     * Lấy file hướng dẫn đang active
     */
    public Optional<File> getActiveGuide() {
        return fileRepository.findActiveGamificationGuide();
    }

    /**
     * Lấy file hướng dẫn theo ID
     */
    public Optional<File> getGuideById(Integer id) {
        return fileRepository.findGamificationGuideById(id);
    }

    /**
     * Lấy tất cả file hướng dẫn - dành cho admin
     */
    public List<File> getAllGuides() {
        return fileRepository.findGamificationGuideFiles();
    }

    /**
     * Cập nhật file hướng dẫn
     * Có thể upload file mới hoặc chỉ cập nhật metadata
     */
    @Transactional
    public File updateGuide(Integer id, MultipartFile file, Long updatedBy) {
        File guideFile = fileRepository.findGamificationGuideById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file hướng dẫn với ID: " + id));

        // Nếu có file mới, upload và cập nhật
        if (file != null && !file.isEmpty()) {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) {
                throw new IllegalArgumentException("Tên file không hợp lệ");
            }

            // Kiểm tra định dạng file
            String fileExtension = getFileExtension(originalFileName).toLowerCase();
            if (!isValidFileType(fileExtension)) {
                throw new IllegalArgumentException("Chỉ chấp nhận file PDF, DOC, DOCX. File hiện tại: " + fileExtension);
            }

            // Upload file mới lên Cloudinary
            String newFileUrl = fileUploadService.uploadFile(file);

            // Cập nhật thông tin file
            guideFile.setFileUrl(newFileUrl);
            guideFile.setFileName(GAMIFICATION_GUIDE_PREFIX + originalFileName);
            guideFile.setFileSize(file.getSize());
        }

        return fileRepository.save(guideFile);
    }

    /**
     * Xóa file hướng dẫn
     */
    @Transactional
    public void deleteGuide(Integer id) {
        File guideFile = fileRepository.findGamificationGuideById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy file hướng dẫn với ID: " + id));
        
        fileRepository.delete(guideFile);
    }

    /**
     * Xóa hoàn toàn file hướng dẫn (hard delete)
     */
    @Transactional
    public void deleteGuidePermanently(Integer id) {
        deleteGuide(id); // Với File entity, delete là hard delete
    }

    /**
     * Lấy extension của file
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Kiểm tra định dạng file có hợp lệ không
     */
    private boolean isValidFileType(String extension) {
        return extension.equals("pdf") || 
               extension.equals("doc") || 
               extension.equals("docx");
    }
    
    /**
     * Lấy tên file gốc (bỏ prefix GAMIFICATION_GUIDE_)
     */
    public String getOriginalFileName(String fileName) {
        if (fileName != null && fileName.startsWith(GAMIFICATION_GUIDE_PREFIX)) {
            return fileName.substring(GAMIFICATION_GUIDE_PREFIX.length());
        }
        return fileName;
    }
}
