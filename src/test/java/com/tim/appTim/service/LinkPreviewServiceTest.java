package com.tim.appTim.service;

import com.tim.appTim.dto.LinkPreviewDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkPreviewServiceTest {

    @InjectMocks
    private LinkPreviewService linkPreviewService;

    @Test
    void getLinkPreview_WithOgTags_ShouldReturnPreview() throws IOException, URISyntaxException {
        // Arrange
        String url = "https://example.com/article";
        String html = "<html><head>" +
                "<meta property=\"og:title\" content=\"Test Title\">" +
                "<meta property=\"og:description\" content=\"Test Description\">" +
                "<meta property=\"og:image\" content=\"https://example.com/image.jpg\">" +
                "<meta property=\"og:site_name\" content=\"Example Site\">" +
                "</head></html>";

        // Create Document before mocking static Jsoup
        Document doc = Jsoup.parse(html);
        
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            org.jsoup.Connection connection = mock(org.jsoup.Connection.class);
            org.jsoup.Connection connectionWithUserAgent = mock(org.jsoup.Connection.class);
            when(connection.userAgent(anyString())).thenReturn(connectionWithUserAgent);
            when(connectionWithUserAgent.get()).thenReturn(doc);
            jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);

            // Act
            LinkPreviewDTO result = linkPreviewService.getLinkPreview(url);

            // Assert
            assertNotNull(result);
            assertEquals(url, result.url());
            assertEquals("Test Title", result.title());
            assertEquals("Test Description", result.description());
            assertEquals("https://example.com/image.jpg", result.imageUrl());
            assertEquals("Example Site", result.domain());
        }
    }

    @Test
    void getLinkPreview_WithoutOgTags_ShouldUseFallbacks() throws IOException, URISyntaxException {
        // Arrange
        String url = "https://example.com/article";
        String html = "<html><head><title>Page Title</title>" +
                "<meta name=\"description\" content=\"Meta Description\">" +
                "</head></html>";

        // Create Document before mocking static Jsoup
        Document doc = Jsoup.parse(html);
        
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            org.jsoup.Connection connection = mock(org.jsoup.Connection.class);
            org.jsoup.Connection connectionWithUserAgent = mock(org.jsoup.Connection.class);
            when(connection.userAgent(anyString())).thenReturn(connectionWithUserAgent);
            when(connectionWithUserAgent.get()).thenReturn(doc);
            jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);

            // Act
            LinkPreviewDTO result = linkPreviewService.getLinkPreview(url);

            // Assert
            assertNotNull(result);
            assertEquals("Page Title", result.title());
            assertEquals("Meta Description", result.description());
            assertEquals("example.com", result.domain());
        }
    }

    @Test
    void getLinkPreview_WhenConnectionFails_ShouldThrowException() throws IOException {
        // Arrange
        String url = "https://example.com/article";

        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            org.jsoup.Connection connection = mock(org.jsoup.Connection.class);
            org.jsoup.Connection connectionWithUserAgent = mock(org.jsoup.Connection.class);
            when(connection.userAgent(anyString())).thenReturn(connectionWithUserAgent);
            when(connectionWithUserAgent.get()).thenThrow(new IOException("Connection failed"));
            jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);

            // Act & Assert
            assertThrows(IOException.class, () -> {
                linkPreviewService.getLinkPreview(url);
            });
        }
    }

    @Test
    void getLinkPreview_WithInvalidUrl_ShouldThrowException() {
        // Arrange
        String invalidUrl = "not-a-valid-url";

        // Act & Assert - Jsoup.connect() throws IllegalArgumentException for invalid URLs
        // before reaching new URI() which would throw URISyntaxException
        assertThrows(IllegalArgumentException.class, () -> {
            linkPreviewService.getLinkPreview(invalidUrl);
        });
    }

    @Test
    void getLinkPreview_WithEmptyOgTags_ShouldUseFallbacks() throws IOException, URISyntaxException {
        // Arrange
        String url = "https://example.com/article";
        String html = "<html><head><title>Document Title</title></head></html>";

        // Create Document before mocking static Jsoup
        Document doc = Jsoup.parse(html);
        
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            org.jsoup.Connection connection = mock(org.jsoup.Connection.class);
            org.jsoup.Connection connectionWithUserAgent = mock(org.jsoup.Connection.class);
            when(connection.userAgent(anyString())).thenReturn(connectionWithUserAgent);
            when(connectionWithUserAgent.get()).thenReturn(doc);
            jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);

            // Act
            LinkPreviewDTO result = linkPreviewService.getLinkPreview(url);

            // Assert
            assertNotNull(result);
            assertEquals("Document Title", result.title());
            assertEquals("", result.description());
            assertEquals("example.com", result.domain());
        }
    }
}

