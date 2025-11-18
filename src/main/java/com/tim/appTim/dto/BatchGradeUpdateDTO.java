package com.tim.appTim.dto;

import java.time.LocalDate; // <-- THÊM IMPORT NÀY
import java.util.List;

public class BatchGradeUpdateDTO {

    private Long classModuleId;
    private LocalDate entryDate; // <-- THÊM TRƯỜNG NÀY
    private List<StudentScoreEntryDTO> scores;

    // Getters & Setters
    public Long getClassModuleId() { return classModuleId; }
    public void setClassModuleId(Long classModuleId) { this.classModuleId = classModuleId; }

    // --- THÊM CÁC HÀM NÀY ---
    public LocalDate getEntryDate() {
        return entryDate;
    }
    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }
    // -------------------------

    public List<StudentScoreEntryDTO> getScores() { return scores; }
    public void setScores(List<StudentScoreEntryDTO> scores) { this.scores = scores; }
}