package com.tim.appTim.service;

import com.tim.appTim.entity.UserImage;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.InternalServerErrorException;
import com.tim.appTim.repository.UserImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class UserImageServiceImpl implements UserImageService {

    private static final Logger logger = LoggerFactory.getLogger(UserImageServiceImpl.class);

    @Autowired
    private UserImageRepository userImageRepository;

    @Value("${upload.folder:/path/to/upload}")
    private String uploadFolder;

    @Override
    public UserImage save(UserImage userImage) {
        return userImageRepository.save(userImage);
    }

    @Override
    public UserImage findById(Long id) {
        return userImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh với ID: " + id));
    }

    @Override
    public UserImage findLatestByUserId(Long userId) {
        List<UserImage> userImages = userImageRepository.findByUserId(userId);
        if (userImages == null || userImages.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh nào cho userId: " + userId);
        }
        Optional<UserImage> latestImage = userImages.stream()
                .max(Comparator.comparing(UserImage::getCreatedAt));
        return latestImage.orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy ảnh mới nhất cho userId: " + userId));
    }

    @Override
    public void delete(Long id) {
        UserImage userImage = userImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh với ID: " + id));
        try {
            String filePath = uploadFolder + File.separator + userImage.getImageUrl().substring("/uploads/".length());
            File file = new File(filePath);
            if (file.exists() && file.delete()) {
                logger.info("Đã xóa file vật lý: {}", filePath);
            }
            userImageRepository.deleteById(id);
        } catch (Exception e) {
            logger.error("Lỗi khi xóa file ảnh ID {}: {}", id, e.getMessage());
            throw new InternalServerErrorException("Lỗi khi xóa file ảnh: " + e.getMessage());
        }
    }

    @Override
    public List<UserImage> findAllByUserId(Long userId) {
        return userImageRepository.findByUserId(userId);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        List<UserImage> userImages = userImageRepository.findByUserId(userId);
        if (userImages == null || userImages.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh nào để xóa cho userId: " + userId);
        }

        for (UserImage image : userImages) {
            try {
                String filePath = uploadFolder + File.separator + image.getImageUrl().substring("/uploads/".length());
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    logger.info("Đã xóa file: {}", filePath);
                }
            } catch (Exception e) {
                logger.error("Lỗi khi xóa file ảnh ID {}: {}", image.getId(), e.getMessage());
                throw new InternalServerErrorException("Lỗi khi xóa file ảnh ID: " + image.getId());
            }
        }

        userImageRepository.deleteAllById(userImages.stream().map(UserImage::getId).toList());
    }
}
