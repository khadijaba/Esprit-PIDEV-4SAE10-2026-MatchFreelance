package com.freelancing.contract.service;

import com.freelancing.contract.dto.ContractHealthDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import com.freelancing.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes contract health/risk score based on progress vs timeline,
 * pending extra budget, status, and rating.
 */
@Service
@RequiredArgsConstructor
public class ContractHealthService {

    private final ContractRepository contractRepository;

    @Transactional(readOnly = true)
    public ContractHealthDTO getContractHealth(Long contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        List<String> flags = new ArrayList<>();
        int score = 100;
        String timelineStatus = "UNKNOWN";
        Double progressVsExpected = null;

        if (c.getStatus() == ContractStatus.COMPLETED || c.getStatus() == ContractStatus.CANCELLED) {
            if (c.getStatus() == ContractStatus.COMPLETED) {
                score = 100;
                flags.add("COMPLETED");
                if (c.getClientRating() != null && c.getClientRating() >= 4) {
                    flags.add("HIGH_RATING");
                } else if (c.getClientRating() != null && c.getClientRating() <= 2) {
                    score = Math.min(score, 70);
                    flags.add("LOW_RATING");
                }
            } else {
                score = 0;
                flags.add("CANCELLED");
            }
            timelineStatus = c.getStatus().name();
        } else if (c.getStatus() == ContractStatus.ACTIVE) {
            int progress = c.getProgressPercent() != null ? c.getProgressPercent() : 0;
            if (c.getPendingExtraAmount() != null && c.getPendingExtraAmount() > 0) {
                score -= 15;
                flags.add("PENDING_EXTRA_BUDGET_DECISION");
            }
            if (c.getStartDate() != null && c.getEndDate() != null) {
                long totalMs = c.getEndDate().getTime() - c.getStartDate().getTime();
                long elapsedMs = System.currentTimeMillis() - c.getStartDate().getTime();
                if (totalMs <= 0) {
                    timelineStatus = "INVALID_DATES";
                    flags.add("INVALID_END_DATE");
                } else {
                    double expectedProgress = (double) elapsedMs / totalMs;
                    expectedProgress = Math.min(1.0, Math.max(0.0, expectedProgress));
                    progressVsExpected = totalMs > 0 ? round((double) progress / 100.0 / expectedProgress, 2) : null;
                    if (elapsedMs > totalMs) {
                        if (progress < 100) {
                            score -= 25;
                            flags.add("BEHIND_SCHEDULE");
                            timelineStatus = "BEHIND_SCHEDULE";
                        } else {
                            timelineStatus = "COMPLETED_ON_TIME";
                        }
                    } else {
                        if (progressVsExpected != null && progressVsExpected >= 0.9 && progressVsExpected <= 1.2) {
                            flags.add("ON_TRACK");
                            timelineStatus = "ON_TRACK";
                        } else if (progressVsExpected != null && progressVsExpected < 0.7) {
                            score -= 15;
                            flags.add("PROGRESS_BEHIND_EXPECTED");
                            timelineStatus = "PROGRESS_LAGGING";
                        } else {
                            timelineStatus = "ON_TRACK";
                        }
                    }
                }
            } else {
                timelineStatus = "NO_DATES";
                if (progress >= 100) flags.add("COMPLETED_PENDING_DATES");
            }
            if (progress >= 100) {
                flags.add("PROGRESS_100");
            }
        } else {
            score = 50;
            flags.add("DRAFT_OR_INACTIVE");
            timelineStatus = c.getStatus().name();
        }

        score = Math.max(0, Math.min(100, score));
        String level = score >= 80 ? "HEALTHY" : score >= 60 ? "MODERATE" : score >= 40 ? "AT_RISK" : "CRITICAL";

        ContractHealthDTO dto = new ContractHealthDTO();
        dto.setContractId(c.getId());
        dto.setHealthScore(score);
        dto.setHealthLevel(level);
        dto.setFlags(flags);
        dto.setTimelineStatus(timelineStatus);
        dto.setProgressVsExpectedRatio(progressVsExpected);
        return dto;
    }

    private static double round(double v, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }
}
