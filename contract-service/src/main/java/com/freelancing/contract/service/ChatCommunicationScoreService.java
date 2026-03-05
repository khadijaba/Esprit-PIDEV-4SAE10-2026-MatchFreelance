package com.freelancing.contract.service;

import com.freelancing.contract.dto.CommunicationScoreDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.entity.Message;
import com.freelancing.contract.repository.ContractRepository;
import com.freelancing.contract.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Computes a 0–100 communication score for a freelancer based on their chat messages
 * in contract conversations: message quality (length, tone, clarity) and response behaviour.
 */
@Service
@RequiredArgsConstructor
public class ChatCommunicationScoreService {

    private static final Pattern PROFESSIONAL_MARKERS = Pattern.compile(
            "\\b(thanks|thank you|please|hi|hello|sure|ok|okay|done|completed|will do|understood|noted|regards|best)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final double DEFAULT_SCORE_WHEN_NO_DATA = 50.0;

    private final ContractRepository contractRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public CommunicationScoreDTO getCommunicationScore(Long freelancerId) {
        if (freelancerId == null) {
            return new CommunicationScoreDTO(DEFAULT_SCORE_WHEN_NO_DATA, 0);
        }
        List<Contract> contracts = contractRepository.findByFreelancerId(freelancerId);
        if (contracts.isEmpty()) {
            return new CommunicationScoreDTO(DEFAULT_SCORE_WHEN_NO_DATA, 0);
        }
        List<Long> contractIds = contracts.stream().map(Contract::getId).toList();
        List<Message> allMessages = messageRepository.findByContractIdInOrderByCreatedAtAsc(contractIds);
        List<Message> freelancerMessages = allMessages.stream()
                .filter(m -> freelancerId.equals(m.getSenderId()))
                .toList();
        if (freelancerMessages.isEmpty()) {
            return new CommunicationScoreDTO(DEFAULT_SCORE_WHEN_NO_DATA, 0);
        }

        double qualityScore = computeQualityScore(freelancerMessages);
        double responseScore = computeResponseTimeScore(contracts, allMessages, freelancerId);
        double combined = (qualityScore * 0.65) + (responseScore * 0.35);
        combined = Math.max(0.0, Math.min(100.0, Math.round(combined * 10) / 10.0));
        return new CommunicationScoreDTO(combined, freelancerMessages.size());
    }

    private double computeQualityScore(List<Message> freelancerMessages) {
        int totalWords = 0;
        int totalCaps = 0;
        int totalChars = 0;
        int exclamationCount = 0;
        int professionalHits = 0;
        int tooShort = 0;
        int reasonableLength = 0;

        for (Message m : freelancerMessages) {
            String content = m.getContent() != null ? m.getContent() : "";
            String trimmed = content.trim();
            int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
            totalWords += words;
            if (content.length() > 0) {
                long caps = content.chars().filter(Character::isUpperCase).count();
                totalCaps += (int) caps;
                totalChars += content.replaceAll("\\s", "").length();
            }
            for (char c : content.toCharArray()) {
                if (c == '!') exclamationCount++;
            }
            if (content.length() > 0 && PROFESSIONAL_MARKERS.matcher(content).find()) {
                professionalHits++;
            }
            if (words > 0 && words < 3) tooShort++;
            else if (words >= 3 && words <= 150) reasonableLength++;
        }

        int n = freelancerMessages.size();
        double avgWords = n > 0 ? (double) totalWords / n : 0;
        double capsRatio = totalChars > 0 ? (double) totalCaps / totalChars : 0;
        double exclamPerMsg = n > 0 ? (double) exclamationCount / n : 0;

        int score = 40;
        if (avgWords >= 5 && avgWords <= 80) score += 25;
        else if (avgWords >= 3 && avgWords <= 120) score += 15;
        if (capsRatio < 0.15) score += 15;
        else if (capsRatio > 0.5) score -= 20;
        if (exclamPerMsg <= 1) score += 10;
        else if (exclamPerMsg > 3) score -= 15;
        if (professionalHits > 0) score += Math.min(15, professionalHits * 3);
        if (reasonableLength > 0) score += Math.min(10, (reasonableLength * 10) / Math.max(1, n));
        if (tooShort > n / 2) score -= 15;

        return Math.max(0.0, Math.min(100.0, score));
    }

    private double computeResponseTimeScore(List<Contract> contracts, List<Message> allMessages, Long freelancerId) {
        List<Long> responseHours = new ArrayList<>();
        for (Contract c : contracts) {
            Long clientId = c.getClientId();
            List<Message> contractMessages = allMessages.stream()
                    .filter(m -> c.getId().equals(m.getContractId()))
                    .toList();
            for (int i = 0; i < contractMessages.size(); i++) {
                Message msg = contractMessages.get(i);
                if (!clientId.equals(msg.getSenderId())) continue;
                long clientTime = msg.getCreatedAt() != null ? msg.getCreatedAt().getTime() : 0;
                for (int j = i + 1; j < contractMessages.size(); j++) {
                    Message next = contractMessages.get(j);
                    if (freelancerId.equals(next.getSenderId())) {
                        long freelancerTime = next.getCreatedAt() != null ? next.getCreatedAt().getTime() : 0;
                        long hours = TimeUnit.MILLISECONDS.toHours(freelancerTime - clientTime);
                        if (hours >= 0 && hours <= 720) responseHours.add(hours);
                        break;
                    }
                    if (clientId.equals(next.getSenderId())) break;
                }
            }
        }
        if (responseHours.isEmpty()) return 70.0;
        double avgHours = responseHours.stream().mapToLong(Long::longValue).average().orElse(24);
        if (avgHours <= 2) return 100.0;
        if (avgHours <= 8) return 90.0;
        if (avgHours <= 24) return 80.0;
        if (avgHours <= 48) return 65.0;
        if (avgHours <= 72) return 50.0;
        return Math.max(20.0, 60.0 - (avgHours - 72) / 24.0 * 5);
    }
}
