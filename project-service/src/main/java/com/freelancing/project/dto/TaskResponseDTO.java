package com.freelancing.project.dto;

import com.freelancing.project.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private Date dueDate;
    private Long assigneeId;
}
