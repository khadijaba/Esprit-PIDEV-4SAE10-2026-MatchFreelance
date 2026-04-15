package com.freelancing.contract.repository;

import com.freelancing.contract.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByContractIdOrderByCreatedAtAsc(Long contractId);

    List<Message> findByContractIdInOrderByCreatedAtAsc(List<Long> contractIds);
}
