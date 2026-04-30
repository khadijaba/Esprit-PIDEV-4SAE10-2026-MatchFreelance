package com.freelancing.contract.entity;

import com.freelancing.contract.enums.ContractStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ContractEntityTest {

    @Test
    void onCreate_setsCreatedAtAndDefaultDraftStatus() throws Exception {
        Contract c = new Contract();
        c.setProjectId(1L);
        c.setFreelancerId(2L);
        c.setClientId(3L);
        c.setStatus(null);

        Method onCreate = Contract.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(c);

        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getCreatedAt()).isBeforeOrEqualTo(new Date());
        assertThat(c.getStatus()).isEqualTo(ContractStatus.DRAFT);
    }

    @Test
    void onCreate_preservesExplicitStatus() throws Exception {
        Contract c = new Contract();
        c.setProjectId(1L);
        c.setFreelancerId(2L);
        c.setClientId(3L);
        c.setStatus(ContractStatus.ACTIVE);

        Method onCreate = Contract.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(c);

        assertThat(c.getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }
}
