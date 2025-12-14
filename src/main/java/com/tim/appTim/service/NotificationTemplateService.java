package com.tim.appTim.service;

import com.tim.appTim.entity.NotificationTemplate;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.NotificationTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<NotificationTemplate> getAll() {
        return templateRepository.findAll();
    }

    public Page<NotificationTemplate> getAll(Pageable pageable) {
        return templateRepository.findAll(pageable);
    }

    public NotificationTemplate getById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template với id: " + id));
    }

    public NotificationTemplate create(String name, String title, String content, String iconUrl) {
        NotificationTemplate template = new NotificationTemplate();
        template.setName(name);
        template.setTitle(title);
        template.setContent(content);
        template.setIconUrl(iconUrl);
        
        return templateRepository.save(template);
    }

    public NotificationTemplate update(Long id, String name, String title, String content, String iconUrl) {
        NotificationTemplate existing = getById(id);

        if (name != null) existing.setName(name);
        if (title != null) existing.setTitle(title);
        if (content != null) existing.setContent(content);
        if (iconUrl != null) existing.setIconUrl(iconUrl);

        return templateRepository.save(existing);
    }

    public void delete(Long id) {
        NotificationTemplate existing = getById(id);
        templateRepository.delete(existing);
    }

    public RenderedTemplate render(String templateName, Map<String, Object> variables) {
        Optional<NotificationTemplate> templateOpt = templateRepository
                .findFirstByNameOrderByUpdatedAtDesc(templateName);

        if (templateOpt.isEmpty()) {
            return null;
        }

        NotificationTemplate template = templateOpt.get();
        String title = applyVariables(template.getTitle(), variables);
        String content = applyVariables(template.getContent(), variables);

        return new RenderedTemplate(title, content, template.getIconUrl());
    }

    public RenderedTemplate renderById(Long templateId, Map<String, Object> variables) {
        if (templateId == null) {
            return null;
        }

        Optional<NotificationTemplate> templateOpt = templateRepository.findById(templateId);

        if (templateOpt.isEmpty()) {
            return null;
        }

        NotificationTemplate template = templateOpt.get();
        String title = applyVariables(template.getTitle(), variables);
        String content = applyVariables(template.getContent(), variables);

        return new RenderedTemplate(title, content, template.getIconUrl());
    }

    private String applyVariables(String input, Map<String, Object> variables) {
        if (input == null || variables == null || variables.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result = result.replace("{" + key + "}", value == null ? "" : value.toString());
        }
        return result;
    }

    public static class RenderedTemplate {
        private final String title;
        private final String content;
        private final String iconUrl;

        public RenderedTemplate(String title, String content, String iconUrl) {
            this.title = title;
            this.content = content;
            this.iconUrl = iconUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getIconUrl() {
            return iconUrl;
        }
    }
}

