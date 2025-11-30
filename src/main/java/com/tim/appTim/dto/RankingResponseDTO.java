package com.tim.appTim.dto;

import java.util.List;

public class RankingResponseDTO {
    private List<RankingDTO> rankings;
    private Long total;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String monthYear; 

    public RankingResponseDTO() {}

    public List<RankingDTO> getRankings() { return rankings; }
    public void setRankings(List<RankingDTO> rankings) { this.rankings = rankings; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
}

