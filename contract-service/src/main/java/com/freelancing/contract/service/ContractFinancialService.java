package com.freelancing.contract.service;

import com.freelancing.contract.dto.FinancialSummaryDTO;
import com.freelancing.contract.dto.PaymentMilestoneDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes contract financial summary: total value, platform fee, freelancer net,
 * and progress-based payment schedule with releasable amount.
 */
@Service
@RequiredArgsConstructor
public class ContractFinancialService {

    private static final double PLATFORM_FEE_PERCENT = 10.0;
    private static final int[] MILESTONE_PERCENTS = { 25, 50, 75, 100 };

    private final ContractRepository contractRepository;

    @Transactional(readOnly = true)
    public FinancialSummaryDTO getFinancialSummary(Long contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        double base = c.getProposedBudget() != null ? c.getProposedBudget() : 0.0;
        double extra = c.getExtraTasksBudget() != null ? c.getExtraTasksBudget() : 0.0;
        double total = base + extra;
        double feePercent = PLATFORM_FEE_PERCENT;
        double feeAmount = round(total * (feePercent / 100.0), 2);
        double freelancerNet = round(total - feeAmount, 2);
        int progress = c.getProgressPercent() != null ? c.getProgressPercent() : 0;

        List<PaymentMilestoneDTO> schedule = buildPaymentSchedule(total, progress);
        double releasable = computeReleasableAmount(total, progress, schedule);

        FinancialSummaryDTO dto = new FinancialSummaryDTO();
        dto.setContractId(c.getId());
        dto.setBaseBudget(base);
        dto.setExtraTasksBudget(extra);
        dto.setTotalContractValue(round(total, 2));
        dto.setPlatformFeePercent(feePercent);
        dto.setPlatformFeeAmount(feeAmount);
        dto.setFreelancerNetAmount(freelancerNet);
        dto.setClientTotalAmount(round(total, 2));
        dto.setAmountReleasableByProgress(round(releasable, 2));
        dto.setPaymentSchedule(schedule);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PaymentMilestoneDTO> getPaymentSchedule(Long contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        double total = (c.getProposedBudget() != null ? c.getProposedBudget() : 0.0)
                + (c.getExtraTasksBudget() != null ? c.getExtraTasksBudget() : 0.0);
        int progress = c.getProgressPercent() != null ? c.getProgressPercent() : 0;
        return buildPaymentSchedule(total, progress);
    }

    private List<PaymentMilestoneDTO> buildPaymentSchedule(double totalValue, int currentProgressPercent) {
        List<PaymentMilestoneDTO> list = new ArrayList<>();
        int prevTrigger = 0;
        double cumulative = 0.0;
        for (int i = 0; i < MILESTONE_PERCENTS.length; i++) {
            int trigger = MILESTONE_PERCENTS[i];
            double share = (trigger - prevTrigger) / 100.0 * totalValue;
            cumulative += share;
            boolean released = currentProgressPercent >= trigger;
            String status = released ? "RELEASED" : (currentProgressPercent >= prevTrigger ? "PENDING" : "LOCKED");
            list.add(new PaymentMilestoneDTO(
                    i + 1,
                    trigger,
                    round(share, 2),
                    released,
                    String.format("%d%% progress → %.2f ( %s )", trigger, share, status)
            ));
            prevTrigger = trigger;
        }
        return list;
    }

    private double computeReleasableAmount(double totalValue, int currentProgress, List<PaymentMilestoneDTO> schedule) {
        double releasable = 0.0;
        for (PaymentMilestoneDTO m : schedule) {
            if (m.isReleased()) {
                releasable += m.getAmount();
            }
        }
        return releasable;
    }

    private static double round(double v, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }
}
