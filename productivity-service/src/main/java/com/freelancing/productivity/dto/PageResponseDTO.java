package com.freelancing.productivity.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponseDTO<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PageResponseDTO<T> from(Page<T> pageData) {
        PageResponseDTO<T> response = new PageResponseDTO<>();
        response.setContent(pageData.getContent());
        response.setPage(pageData.getNumber());
        response.setSize(pageData.getSize());
        response.setTotalElements(pageData.getTotalElements());
        response.setTotalPages(pageData.getTotalPages());
        response.setLast(pageData.isLast());
        return response;
    }
}

