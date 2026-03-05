package com.freelancing.contract.config;

import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import com.freelancing.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

/**
 * Seeds demo contracts for evaluation. Runs only when no contracts exist.
 * Assumes: user-service has client 1, freelancers 2,4; project-service has projects 5 (IN_PROGRESS) and 8 (COMPLETED).
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ContractRepository contractRepository;

    @Override
    public void run(String... args) {
        if (contractRepository.count() > 0) return;

        // Project 5 – E-Learning Platform (IN_PROGRESS): contract with freelancer 2
        Contract eLearning = new Contract();
        eLearning.setProjectId(5L);
        eLearning.setFreelancerId(2L);
        eLearning.setClientId(1L);
        eLearning.setTerms("Deliver e-learning platform with video streaming, quizzes, progress tracking and certificates. React/Node.js, AWS S3. Bi-weekly demos.");
        eLearning.setProposedBudget(7500.0);
        eLearning.setExtraTasksBudget(500.0);
        eLearning.setApplicationMessage("I have delivered e-learning platforms with video upload/streaming, quizzes, progress tracking and certificates. I use React and Node.js and have integrated AWS S3. I can deliver the full platform end to end.");
        eLearning.setStatus(ContractStatus.ACTIVE);
        eLearning.setStartDate(daysAgo(30));
        eLearning.setEndDate(daysFromNow(20));
        eLearning.setProgressPercent(45);
        contractRepository.save(eLearning);

        // Project 8 – Microservices Migration (COMPLETED): contract with freelancer 4
        Contract microservices = new Contract();
        microservices.setProjectId(8L);
        microservices.setFreelancerId(4L);
        microservices.setClientId(1L);
        microservices.setTerms("Split monolith into 3–4 microservices, event-driven communication, API gateway, Docker and Kubernetes. Incremental delivery with documentation.");
        microservices.setProposedBudget(18000.0);
        microservices.setExtraTasksBudget(null);
        microservices.setApplicationMessage("Senior Java/Spring developer with experience in microservices and Docker/Kubernetes. I have designed event-driven APIs and migration strategies. I can deliver incrementally and document decisions.");
        microservices.setStatus(ContractStatus.COMPLETED);
        microservices.setStartDate(daysAgo(95));
        microservices.setEndDate(daysAgo(5));
        microservices.setProgressPercent(100);
        microservices.setClientRating(5);
        microservices.setClientReview("Excellent work. Clean architecture, on-time delivery and clear documentation. Would hire again.");
        microservices.setClientReviewedAt(new Date());
        contractRepository.save(microservices);
    }

    private static Date daysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -days);
        return c.getTime();
    }

    private static Date daysFromNow(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, days);
        return c.getTime();
    }
}
