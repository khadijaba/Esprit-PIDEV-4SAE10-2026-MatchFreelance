package com.freelancing.project.dto;

import com.freelancing.project.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private TaskStatus status;
    private Date dueDate;
    private Long assigneeId;
}
