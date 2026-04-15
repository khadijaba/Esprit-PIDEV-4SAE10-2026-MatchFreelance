package com.freelancing.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDTO {

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotBlank(message = "Content is required")
    @Size(max = 2000)
    private String content;
}
