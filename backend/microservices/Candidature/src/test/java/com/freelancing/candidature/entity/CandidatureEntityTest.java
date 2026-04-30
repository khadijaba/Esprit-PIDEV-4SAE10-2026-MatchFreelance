package com.freelancing.candidature.entity;

import com.freelancing.candidature.enums.CandidatureStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatureEntityTest {

    @Test
    void onCreate_setsCreatedAtAndDefaultStatus() throws Exception {
        Candidature c = new Candidature();
        c.setProjectId(1L);
        c.setFreelancerId(2L);
        c.setStatus(null);

        Method onCreate = Candidature.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(c);

        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getCreatedAt()).isBeforeOrEqualTo(new Date());
        assertThat(c.getStatus()).isEqualTo(CandidatureStatus.PENDING);
    }

    @Test
    void onCreate_preservesExplicitStatus() throws Exception {
        Candidature c = new Candidature();
        c.setProjectId(1L);
        c.setFreelancerId(2L);
        c.setStatus(CandidatureStatus.ACCEPTED);

        Method onCreate = Candidature.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(c);

        assertThat(c.getStatus()).isEqualTo(CandidatureStatus.ACCEPTED);
    }
}
