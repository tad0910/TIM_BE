package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.File;
import com.tim.appTim.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationGuideServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private GamificationGuideService guideService;

    @Mock
    private MultipartFile multipartFile;

    private File guideFile;

    @BeforeEach
    void setUp() {
        guideFile = new File();
        guideFile.setId(1);
        guideFile.setFileName("GAMIFICATION_GUIDE_test.pdf");
        guideFile.setFileUrl("https://cloudinary.com/test.pdf");
        guideFile.setFileSize(1024L);
        guideFile.setFileType(File.FileType.DOCUMENT);
    }

    @Test
    void uploadGuide_WhenFileIsValid_ShouldUploadAndReturnFile() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide.pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("https://cloudinary.com/guide.pdf");
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.empty());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        File result = guideService.uploadGuide(multipartFile, 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFileName().startsWith("GAMIFICATION_GUIDE_"));
        assertEquals(File.FileType.DOCUMENT, result.getFileType());
        verify(fileUploadService).uploadFile(multipartFile);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void uploadGuide_WhenFileIsNull_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            guideService.uploadGuide(null, 1L);
        });
        assertTrue(exception.getMessage().contains("File không được để trống"));
        verify(fileUploadService, never()).uploadFile(any());
    }

    @Test
    void uploadGuide_WhenFileIsEmpty_ShouldThrowException() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            guideService.uploadGuide(multipartFile, 1L);
        });
        assertTrue(exception.getMessage().contains("File không được để trống"));
        verify(fileUploadService, never()).uploadFile(any());
    }

    @Test
    void uploadGuide_WhenFileNameIsNull_ShouldThrowException() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            guideService.uploadGuide(multipartFile, 1L);
        });
        assertTrue(exception.getMessage().contains("Tên file không hợp lệ"));
        verify(fileUploadService, never()).uploadFile(any());
    }

    @Test
    void uploadGuide_WithInvalidFileType_ShouldThrowException() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide.txt");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            guideService.uploadGuide(multipartFile, 1L);
        });
        assertTrue(exception.getMessage().contains("Chỉ chấp nhận file PDF, DOC, DOCX"));
        verify(fileUploadService, never()).uploadFile(any());
    }

    @Test
    void uploadGuide_WithPdfFile_ShouldAccept() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide.pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("https://cloudinary.com/guide.pdf");
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.empty());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        File result = guideService.uploadGuide(multipartFile, 1L);

        // Assert
        assertNotNull(result);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void uploadGuide_WithDocFile_ShouldAccept() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide.doc");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("https://cloudinary.com/guide.doc");
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.empty());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        File result = guideService.uploadGuide(multipartFile, 1L);

        // Assert
        assertNotNull(result);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void uploadGuide_WithDocxFile_ShouldAccept() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide.docx");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("https://cloudinary.com/guide.docx");
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.empty());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        File result = guideService.uploadGuide(multipartFile, 1L);

        // Assert
        assertNotNull(result);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void uploadGuide_WhenOldGuideExists_ShouldDeleteOldGuide() {
        // Arrange
        File oldGuide = new File();
        oldGuide.setId(2);
        oldGuide.setFileName("GAMIFICATION_GUIDE_old.pdf");

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide.pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("https://cloudinary.com/guide.pdf");
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.of(oldGuide));
        doNothing().when(fileRepository).delete(oldGuide);
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        guideService.uploadGuide(multipartFile, 1L);

        // Assert
        verify(fileRepository).delete(oldGuide);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void getActiveGuide_WhenExists_ShouldReturnGuide() {
        // Arrange
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.of(guideFile));

        // Act
        Optional<File> result = guideService.getActiveGuide();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(guideFile.getFileName(), result.get().getFileName());
        verify(fileRepository).findActiveGamificationGuide();
    }

    @Test
    void getActiveGuide_WhenNotExists_ShouldReturnEmpty() {
        // Arrange
        when(fileRepository.findActiveGamificationGuide()).thenReturn(Optional.empty());

        // Act
        Optional<File> result = guideService.getActiveGuide();

        // Assert
        assertFalse(result.isPresent());
        verify(fileRepository).findActiveGamificationGuide();
    }

    @Test
    void getGuideById_WhenExists_ShouldReturnGuide() {
        // Arrange
        when(fileRepository.findGamificationGuideById(1)).thenReturn(Optional.of(guideFile));

        // Act
        Optional<File> result = guideService.getGuideById(1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(guideFile.getId(), result.get().getId());
        verify(fileRepository).findGamificationGuideById(1);
    }

    @Test
    void getAllGuides_ShouldReturnAllGuides() {
        // Arrange
        List<File> guides = List.of(guideFile);
        when(fileRepository.findGamificationGuideFiles()).thenReturn(guides);

        // Act
        List<File> result = guideService.getAllGuides();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(fileRepository).findGamificationGuideFiles();
    }

    @Test
    void updateGuide_WhenExists_ShouldUpdateGuide() {
        // Arrange
        when(fileRepository.findGamificationGuideById(1)).thenReturn(Optional.of(guideFile));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("updated.pdf");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(fileUploadService.uploadFile(multipartFile)).thenReturn("https://cloudinary.com/updated.pdf");
        when(fileRepository.save(any(File.class))).thenReturn(guideFile);

        // Act
        File result = guideService.updateGuide(1, multipartFile, 1L);

        // Assert
        assertNotNull(result);
        assertTrue(guideFile.getFileName().startsWith("GAMIFICATION_GUIDE_"));
        verify(fileUploadService).uploadFile(multipartFile);
        verify(fileRepository).save(guideFile);
    }

    @Test
    void updateGuide_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(fileRepository.findGamificationGuideById(999)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            guideService.updateGuide(999, multipartFile, 1L);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy file hướng dẫn"));
        verify(fileRepository).findGamificationGuideById(999);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void updateGuide_WithNullFile_ShouldNotUpdateFile() {
        // Arrange
        when(fileRepository.findGamificationGuideById(1)).thenReturn(Optional.of(guideFile));
        when(fileRepository.save(any(File.class))).thenReturn(guideFile);

        // Act
        File result = guideService.updateGuide(1, null, 1L);

        // Assert
        assertNotNull(result);
        verify(fileUploadService, never()).uploadFile(any());
        verify(fileRepository).save(guideFile);
    }

    @Test
    void updateGuide_WithEmptyFile_ShouldNotUpdateFile() {
        // Arrange
        when(fileRepository.findGamificationGuideById(1)).thenReturn(Optional.of(guideFile));
        when(multipartFile.isEmpty()).thenReturn(true);
        when(fileRepository.save(any(File.class))).thenReturn(guideFile);

        // Act
        File result = guideService.updateGuide(1, multipartFile, 1L);

        // Assert
        assertNotNull(result);
        verify(fileUploadService, never()).uploadFile(any());
        verify(fileRepository).save(guideFile);
    }

    @Test
    void deleteGuide_WhenExists_ShouldDelete() {
        // Arrange
        when(fileRepository.findGamificationGuideById(1)).thenReturn(Optional.of(guideFile));
        doNothing().when(fileRepository).delete(guideFile);

        // Act
        guideService.deleteGuide(1);

        // Assert
        verify(fileRepository).findGamificationGuideById(1);
        verify(fileRepository).delete(guideFile);
    }

    @Test
    void deleteGuide_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(fileRepository.findGamificationGuideById(999)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            guideService.deleteGuide(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy file hướng dẫn"));
        verify(fileRepository).findGamificationGuideById(999);
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void deleteGuidePermanently_ShouldCallDeleteGuide() {
        // Arrange
        when(fileRepository.findGamificationGuideById(1)).thenReturn(Optional.of(guideFile));
        doNothing().when(fileRepository).delete(guideFile);

        // Act
        guideService.deleteGuidePermanently(1);

        // Assert
        verify(fileRepository).delete(guideFile);
    }

    @Test
    void getOriginalFileName_WhenHasPrefix_ShouldRemovePrefix() {
        // Act
        String result = guideService.getOriginalFileName("GAMIFICATION_GUIDE_test.pdf");

        // Assert
        assertEquals("test.pdf", result);
    }

    @Test
    void getOriginalFileName_WhenNoPrefix_ShouldReturnOriginal() {
        // Act
        String result = guideService.getOriginalFileName("test.pdf");

        // Assert
        assertEquals("test.pdf", result);
    }

    @Test
    void getOriginalFileName_WhenNull_ShouldReturnNull() {
        // Act
        String result = guideService.getOriginalFileName(null);

        // Assert
        assertNull(result);
    }

    @Test
    void uploadGuide_WithFileNoExtension_ShouldHandleGracefully() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("guide");

        // Act & Assert - should throw exception for invalid file type
        assertThrows(IllegalArgumentException.class, () -> {
            guideService.uploadGuide(multipartFile, 1L);
        });
        
        // Verify file upload service is never called
        verify(fileUploadService, never()).uploadFile(any());
        verify(fileRepository, never()).save(any());
    }
}


