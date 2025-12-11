package com.tim.appTim.service;

import com.tim.appTim.entity.NotificationTemplate;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private NotificationTemplateService templateService;

    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        template = new NotificationTemplate();
        template.setId(1L);
        template.setName("WELCOME");
        template.setTitle("Xin chào {user}");
        template.setContent("Nội dung {user}");
        template.setIconUrl("icon.png");
    }

    @Test
    void getAll_ShouldReturnTemplates() {
        when(templateRepository.findAll()).thenReturn(List.of(template));

        List<NotificationTemplate> result = templateService.getAll();

        assertThat(result).hasSize(1);
        verify(templateRepository).findAll();
    }

    @Test
    void getById_Found_ShouldReturnTemplate() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        NotificationTemplate result = templateService.getById(1L);

        assertThat(result.getName()).isEqualTo("WELCOME");
        verify(templateRepository).findById(1L);
    }

    @Test
    void getById_NotFound_ShouldThrow() {
        when(templateRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.getById(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy template");
    }

    @Test
    void create_ShouldPersistWithProvidedFields() {
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationTemplate created = templateService.create("NAME", "T", "C", "I");

        assertThat(created.getName()).isEqualTo("NAME");
        assertThat(created.getTitle()).isEqualTo("T");
        assertThat(created.getContent()).isEqualTo("C");
        assertThat(created.getIconUrl()).isEqualTo("I");
        verify(templateRepository).save(any(NotificationTemplate.class));
    }

    @Test
    void update_ShouldApplyNonNullFields() {
        NotificationTemplate existing = new NotificationTemplate();
        existing.setId(5L);
        existing.setName("Old");
        existing.setTitle("Old title");
        existing.setContent("Old content");
        existing.setIconUrl("old.png");

        when(templateRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationTemplate updated = templateService.update(5L, "New", null, "New content", null);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getTitle()).isEqualTo("Old title");
        assertThat(updated.getContent()).isEqualTo("New content");
        assertThat(updated.getIconUrl()).isEqualTo("old.png");

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New");
    }

    @Test
    void delete_ShouldRemoveExisting() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        templateService.delete(1L);

        verify(templateRepository).delete(eq(template));
    }

    @Test
    void render_TemplateMissing_ShouldReturnNull() {
        when(templateRepository.findFirstByNameOrderByUpdatedAtDesc("NAME")).thenReturn(Optional.empty());

        NotificationTemplateService.RenderedTemplate rendered = templateService.render("NAME", Map.of());

        assertThat(rendered).isNull();
    }

    @Test
    void render_ShouldApplyVariablesAndReturnIcon() {
        when(templateRepository.findFirstByNameOrderByUpdatedAtDesc("WELCOME"))
                .thenReturn(Optional.of(template));

        NotificationTemplateService.RenderedTemplate rendered = templateService.render("WELCOME", Map.of("user", "A"));

        assertThat(rendered).isNotNull();
        assertThat(rendered.getTitle()).isEqualTo("Xin chào A");
        assertThat(rendered.getContent()).isEqualTo("Nội dung A");
        assertThat(rendered.getIconUrl()).isEqualTo("icon.png");
    }

    @Test
    void renderById_NullId_ShouldReturnNull() {
        NotificationTemplateService.RenderedTemplate rendered = templateService.renderById(null, Map.of());

        assertThat(rendered).isNull();
    }

    @Test
    void renderById_NotFound_ShouldReturnNull() {
        when(templateRepository.findById(10L)).thenReturn(Optional.empty());

        NotificationTemplateService.RenderedTemplate rendered = templateService.renderById(10L, Map.of());

        assertThat(rendered).isNull();
    }

    @Test
    void renderById_ShouldApplyVariables() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        NotificationTemplateService.RenderedTemplate rendered = templateService.renderById(1L, Map.of("user", "B"));

        assertThat(rendered.getTitle()).isEqualTo("Xin chào B");
        assertThat(rendered.getContent()).isEqualTo("Nội dung B");
        assertThat(rendered.getIconUrl()).isEqualTo("icon.png");
    }

    @Test
    void render_WithNullInput_ShouldReturnNullsSafely() {
        template.setTitle(null);
        template.setContent(null);
        when(templateRepository.findFirstByNameOrderByUpdatedAtDesc("WELCOME"))
                .thenReturn(Optional.of(template));

        NotificationTemplateService.RenderedTemplate rendered = templateService.render("WELCOME", Map.of("user", "C"));

        assertThat(rendered.getTitle()).isNull();
        assertThat(rendered.getContent()).isNull();
    }
}


