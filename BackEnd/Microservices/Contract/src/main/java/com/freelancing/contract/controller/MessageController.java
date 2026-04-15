package com.freelancing.contract.controller;

import com.freelancing.contract.dto.MessageRequestDTO;
import com.freelancing.contract.dto.MessageResponseDTO;
import com.freelancing.contract.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts/{contractId}/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageResponseDTO>> getMessages(@PathVariable Long contractId) {
        return ResponseEntity.ok(messageService.getMessagesByContractId(contractId));
    }

    @PostMapping
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @PathVariable Long contractId,
            @Valid @RequestBody MessageRequestDTO request) {
        try {
            MessageResponseDTO created = messageService.sendMessage(contractId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
