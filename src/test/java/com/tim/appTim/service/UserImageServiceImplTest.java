package com.tim.appTim.service;

import com.tim.appTim.entity.UserImage;
import com.tim.appTim.exception.InternalServerErrorException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.UserImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserImageServiceImplTest {

    @Mock
    private UserImageRepository userImageRepository;

    @InjectMocks
    private UserImageServiceImpl userImageService;

    private UserImage userImage;
    private String testUploadFolder;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for testing
        Path tempDir = Files.createTempDirectory("test-upload");
        testUploadFolder = tempDir.toString();
        ReflectionTestUtils.setField(userImageService, "uploadFolder", testUploadFolder);

        userImage = new UserImage();
        userImage.setId(1L);
        userImage.setUserId(1L);
        userImage.setImageUrl("/uploads/test-image.jpg");
        userImage.setDescription("Test image");
    }

    // --- save ---

    @Test
    void save_Success() {
        // Arrange
        when(userImageRepository.save(any(UserImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserImage result = userImageService.save(userImage);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(userImage);
        verify(userImageRepository).save(userImage);
    }

    // --- findById ---

    @Test
    void findById_Success() {
        // Arrange
        when(userImageRepository.findById(1L)).thenReturn(Optional.of(userImage));

        // Act
        UserImage result = userImageService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_NotFound() {
        // Arrange
        when(userImageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userImageService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh với ID: 999");
    }

    // --- findLatestByUserId ---

    @Test
    void findLatestByUserId_Success() {
        // Arrange
        when(userImageRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(userImage);

        // Act
        UserImage result = userImageService.findLatestByUserId(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findLatestByUserId_NotFound() {
        // Arrange
        when(userImageRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> userImageService.findLatestByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh nào cho userId: 1");
    }

    // --- delete ---

    @Test
    void delete_Success_FileExists() throws IOException {
        // Arrange
        // Create a test file
        String fileName = "test-image.jpg";
        File testFile = new File(testUploadFolder, fileName);
        testFile.createNewFile();

        userImage.setImageUrl("/uploads/" + fileName);
        when(userImageRepository.findById(1L)).thenReturn(Optional.of(userImage));
        doNothing().when(userImageRepository).deleteById(1L);

        // Act
        userImageService.delete(1L);

        // Assert
        verify(userImageRepository).deleteById(1L);
        assertThat(testFile.exists()).isFalse(); // File should be deleted
    }

    @Test
    void delete_Success_FileNotExists() {
        // Arrange
        userImage.setImageUrl("/uploads/non-existent.jpg");
        when(userImageRepository.findById(1L)).thenReturn(Optional.of(userImage));
        doNothing().when(userImageRepository).deleteById(1L);

        // Act
        userImageService.delete(1L);

        // Assert
        verify(userImageRepository).deleteById(1L);
    }

    @Test
    void delete_NotFound() {
        // Arrange
        when(userImageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userImageService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh với ID: 999");
    }

    @Test
    void delete_ExceptionDuringFileDeletion() {
        // Arrange
        // Use imageUrl that is too short to have "/uploads/" prefix
        userImage.setImageUrl("short"); // Too short - substring will throw StringIndexOutOfBoundsException
        when(userImageRepository.findById(1L)).thenReturn(Optional.of(userImage));

        // Act & Assert
        // substring("/uploads/".length()) on "short" (length 5) will throw StringIndexOutOfBoundsException
        assertThatThrownBy(() -> userImageService.delete(1L))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Lỗi khi xóa file ảnh");
    }

    @Test
    void delete_ImageUrlWithoutPrefix() {
        // Arrange
        // ImageUrl shorter than "/uploads/".length() (10) will cause StringIndexOutOfBoundsException
        userImage.setImageUrl("short"); // Length 5 < 10, substring(10) will throw exception
        when(userImageRepository.findById(1L)).thenReturn(Optional.of(userImage));

        // Act & Assert
        // substring("/uploads/".length()) on "short" (length 5) will throw StringIndexOutOfBoundsException
        assertThatThrownBy(() -> userImageService.delete(1L))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Lỗi khi xóa file ảnh");
    }

    // --- findAllByUserId ---

    @Test
    void findAllByUserId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserImage> imagePage = new PageImpl<>(List.of(userImage));
        when(userImageRepository.findByUserId(1L, pageable)).thenReturn(imagePage);

        // Act
        Page<UserImage> result = userImageService.findAllByUserId(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findAllByUserId_Empty() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserImage> emptyPage = Page.empty();
        when(userImageRepository.findByUserId(1L, pageable)).thenReturn(emptyPage);

        // Act
        Page<UserImage> result = userImageService.findAllByUserId(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    // --- deleteAllByUserId ---

    @Test
    void deleteAllByUserId_Success() throws IOException {
        // Arrange
        String fileName1 = "test-image-1.jpg";
        String fileName2 = "test-image-2.jpg";
        
        // Create test files
        File testFile1 = new File(testUploadFolder, fileName1);
        File testFile2 = new File(testUploadFolder, fileName2);
        testFile1.createNewFile();
        testFile2.createNewFile();

        UserImage image1 = new UserImage();
        image1.setId(1L);
        image1.setUserId(1L);
        image1.setImageUrl("/uploads/" + fileName1);

        UserImage image2 = new UserImage();
        image2.setId(2L);
        image2.setUserId(1L);
        image2.setImageUrl("/uploads/" + fileName2);

        List<UserImage> images = List.of(image1, image2);
        when(userImageRepository.findByUserId(1L)).thenReturn(images);
        doNothing().when(userImageRepository).deleteAllById(anyList());

        // Act
        userImageService.deleteAllByUserId(1L);

        // Assert
        verify(userImageRepository).deleteAllById(anyList());
        assertThat(testFile1.exists()).isFalse();
        assertThat(testFile2.exists()).isFalse();
    }

    @Test
    void deleteAllByUserId_NotFound() {
        // Arrange
        when(userImageRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> userImageService.deleteAllByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh nào để xóa cho userId: 1");
    }

    @Test
    void deleteAllByUserId_NullList() {
        // Arrange
        when(userImageRepository.findByUserId(1L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> userImageService.deleteAllByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ảnh nào để xóa cho userId: 1");
    }

    @Test
    void deleteAllByUserId_FileNotExists() {
        // Arrange
        UserImage image1 = new UserImage();
        image1.setId(1L);
        image1.setUserId(1L);
        image1.setImageUrl("/uploads/non-existent-1.jpg");

        UserImage image2 = new UserImage();
        image2.setId(2L);
        image2.setUserId(1L);
        image2.setImageUrl("/uploads/non-existent-2.jpg");

        List<UserImage> images = List.of(image1, image2);
        when(userImageRepository.findByUserId(1L)).thenReturn(images);
        doNothing().when(userImageRepository).deleteAllById(anyList());

        // Act
        userImageService.deleteAllByUserId(1L);

        // Assert
        verify(userImageRepository).deleteAllById(anyList());
    }

    @Test
    void deleteAllByUserId_ExceptionDuringFileDeletion() {
        // Arrange
        UserImage image1 = new UserImage();
        image1.setId(1L);
        image1.setUserId(1L);
        // ImageUrl shorter than "/uploads/".length() (10) will cause StringIndexOutOfBoundsException
        image1.setImageUrl("short"); // Length 5 < 10, substring(10) will throw exception

        List<UserImage> images = List.of(image1);
        when(userImageRepository.findByUserId(1L)).thenReturn(images);

        // Act & Assert
        // substring("/uploads/".length()) on "short" (length 5) will throw StringIndexOutOfBoundsException
        assertThatThrownBy(() -> userImageService.deleteAllByUserId(1L))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Lỗi khi xóa file ảnh ID: 1");
    }

    @Test
    void deleteAllByUserId_ImageUrlWithoutPrefix() {
        // Arrange
        UserImage image1 = new UserImage();
        image1.setId(1L);
        image1.setUserId(1L);
        // ImageUrl shorter than "/uploads/".length() (10) will cause StringIndexOutOfBoundsException
        image1.setImageUrl("short"); // Length 5 < 10, substring(10) will throw exception

        List<UserImage> images = List.of(image1);
        when(userImageRepository.findByUserId(1L)).thenReturn(images);

        // Act & Assert
        // substring("/uploads/".length()) on "short" (length 5) will throw StringIndexOutOfBoundsException
        assertThatThrownBy(() -> userImageService.deleteAllByUserId(1L))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Lỗi khi xóa file ảnh ID: 1");
    }

    @Test
    void deleteAllByUserId_FileDeleteReturnsFalse() throws IOException {
        // Arrange
        String fileName = "test-image.jpg";
        File testFile = new File(testUploadFolder, fileName);
        testFile.createNewFile();

        UserImage image1 = new UserImage();
        image1.setId(1L);
        image1.setUserId(1L);
        image1.setImageUrl("/uploads/" + fileName);

        List<UserImage> images = List.of(image1);
        when(userImageRepository.findByUserId(1L)).thenReturn(images);
        doNothing().when(userImageRepository).deleteAllById(anyList());

        // Make file read-only to prevent deletion
        testFile.setReadOnly();

        // Act
        userImageService.deleteAllByUserId(1L);

        // Assert
        verify(userImageRepository).deleteAllById(anyList());
        // Clean up
        testFile.setWritable(true);
        testFile.delete();
    }

    @Test
    void delete_FileDeleteReturnsFalse() throws IOException {
        // Arrange
        String fileName = "test-image.jpg";
        File testFile = new File(testUploadFolder, fileName);
        testFile.createNewFile();

        userImage.setImageUrl("/uploads/" + fileName);
        when(userImageRepository.findById(1L)).thenReturn(Optional.of(userImage));
        doNothing().when(userImageRepository).deleteById(1L);

        // Make file read-only to prevent deletion
        testFile.setReadOnly();

        // Act
        userImageService.delete(1L);

        // Assert
        verify(userImageRepository).deleteById(1L);
        // Clean up
        testFile.setWritable(true);
        testFile.delete();
    }
}

