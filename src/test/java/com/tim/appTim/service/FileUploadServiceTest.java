package com.tim.appTim.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private FileUploadService fileUploadService;

    @Mock
    private MultipartFile multipartFile;

    @Test
    void uploadFile_WhenFileIsValid_ShouldReturnSecureUrl() throws IOException {
        // Arrange
        byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};
        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/image.jpg");
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(uploadResult);

        // Act
        String result = fileUploadService.uploadFile(multipartFile);

        // Assert
        assertNotNull(result);
        assertEquals("https://cloudinary.com/image.jpg", result);
        verify(multipartFile).getBytes();
        verify(cloudinary).uploader();
        verify(uploader).upload(eq(fileBytes), any(Map.class));
    }

    @Test
    void uploadFile_WhenUploadFails_ShouldThrowException() throws IOException {
        // Arrange
        byte[] fileBytes = new byte[]{1, 2, 3};
        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("Upload failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadFile(multipartFile);
        });
        assertTrue(exception.getMessage().contains("Không thể upload file"));
        verify(multipartFile).getBytes();
        verify(cloudinary).uploader();
        verify(uploader).upload(any(byte[].class), any(Map.class));
    }

    @Test
    void uploadFile_WhenGetBytesFails_ShouldThrowException() throws IOException {
        // Arrange
        when(multipartFile.getBytes()).thenThrow(new IOException("Cannot read file"));
        // Note: cloudinary.uploader() is called before getBytes() in the upload() method,
        // but since getBytes() throws exception immediately, uploader().upload() is never called
        // However, we need to mock uploader() to avoid NPE if it's called
        when(cloudinary.uploader()).thenReturn(uploader);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadFile(multipartFile);
        });
        assertTrue(exception.getMessage().contains("Không thể upload file"));
        verify(multipartFile).getBytes();
        // uploader().upload() is never called because getBytes() throws exception
        verify(uploader, never()).upload(any(byte[].class), any(Map.class));
    }

    @Test
    void uploadFile_ShouldUseAutoResourceType() throws IOException {
        // Arrange
        byte[] fileBytes = new byte[]{1, 2, 3};
        when(multipartFile.getBytes()).thenReturn(fileBytes);
        when(cloudinary.uploader()).thenReturn(uploader);
        
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/file.pdf");
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(uploadResult);

        // Act
        fileUploadService.uploadFile(multipartFile);

        // Assert
        verify(cloudinary).uploader();
        verify(uploader).upload(eq(fileBytes), argThat(map -> 
            map.containsKey("resource_type") && "auto".equals(map.get("resource_type"))
        ));
    }
}

