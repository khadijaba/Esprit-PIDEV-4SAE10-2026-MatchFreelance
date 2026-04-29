package com.freelancing.contract.service;

import com.freelancing.contract.dto.MessageRequestDTO;
import com.freelancing.contract.dto.MessageResponseDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.entity.Message;
import com.freelancing.contract.repository.ContractRepository;
import com.freelancing.contract.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ContractRepository contractRepository;
    private final ChatProgressExtractorService chatProgressExtractor;

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessagesByContractId(Long contractId) {
        return messageRepository.findByContractIdOrderByCreatedAtAsc(contractId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponseDTO sendMessage(Long contractId, MessageRequestDTO dto) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        Long senderId = dto.getSenderId();
        boolean isParticipant = (contract.getClientId() != null && contract.getClientId().equals(senderId))
                || (contract.getFreelancerId() != null && contract.getFreelancerId().equals(senderId));
        if (!isParticipant) {
            throw new RuntimeException("Only the client or freelancer of this contract can send messages");
        }

        Message m = new Message();
        m.setContractId(contractId);
        m.setSenderId(senderId);
        m.setContent(dto.getContent().trim());
        Message saved = messageRepository.save(m);

        Long freelancerId = contract.getFreelancerId();
        boolean isFreelancerSender = freelancerId != null && senderId != null
                && freelancerId.longValue() == senderId.longValue();
        Integer updatedProgress = null;
        if (isFreelancerSender) {
            updatedProgress = chatProgressExtractor.extractProgressFromMessage(dto.getContent().trim()).orElse(null);
            if (updatedProgress != null) {
                contract.setProgressPercent(updatedProgress);
                contractRepository.save(contract);
            }
        }

        MessageResponseDTO response = toResponseDTO(saved);
        response.setContractProgressPercent(updatedProgress);
        return response;
    }

    private MessageResponseDTO toResponseDTO(Message m) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(m.getId());
        dto.setContractId(m.getContractId());
        dto.setSenderId(m.getSenderId());
        dto.setContent(m.getContent());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
