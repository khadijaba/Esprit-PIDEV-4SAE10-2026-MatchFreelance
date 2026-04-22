<<<<<<< HEAD
package com.freelancing.interview.dto;

import com.freelancing.interview.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDTO {

    private Long id;
    private Long candidatureId;
    private Instant scheduledAt;
    private InterviewStatus status;
    private String notes;
}
=======
package com.freelancing.interview.dto;

import com.freelancing.interview.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDTO {

    private Long id;
    private Long candidatureId;
    private Instant scheduledAt;
    private InterviewStatus status;
    private String notes;
}
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
