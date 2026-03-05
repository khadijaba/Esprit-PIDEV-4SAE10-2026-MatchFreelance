package com.freelancing.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {

    private Long id;
    private Long contractId;
    private Long senderId;
    private String content;
    private Date createdAt;
    /** Set when the backend updated contract progress from this message (freelancer sent a progress phrase). */
    private Integer contractProgressPercent;
}
