package com.freelancing.candidature.config;

import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import com.freelancing.candidature.repository.CandidatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds freelancing_candidatures for projects 1–8 and freelancers 3–9.
 * Aligns with project-service (MySQL) and user-service seed (clients 2,10,11; freelancers 3–9).
 */
@Configuration
@RequiredArgsConstructor
public class CandidatureDataInitializer implements CommandLineRunner {

    private final CandidatureRepository candidatureRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (candidatureRepository.count() > 0) return;

        // Freelancers 3 (Amira), 4 (Youssef), 5 (Nadia), 6 (Omar) apply to projects 1–8
        save(1L, 3L, "Experienced full-stack developer, 5+ years React/Node. Available for the full redesign.", 12000.0, CandidatureStatus.PENDING);
        save(1L, 4L, "Mobile specialist, React Native. Would love to work on the fitness app.", 8500.0, CandidatureStatus.PENDING);
        save(2L, 3L, "I have delivered similar fitness apps. Can integrate wearables and charts.", 9000.0, CandidatureStatus.PENDING);
        save(2L, 4L, "Flutter developer, 3 years experience. Portfolio available on request.", 7500.0, CandidatureStatus.ACCEPTED);
        save(3L, 3L, "WordPress migration and SEO are my daily work. Quick turnaround.", 2500.0, CandidatureStatus.PENDING);
        save(3L, 5L, "Blog migrations and custom themes. Happy to do the newsletter setup.", 2800.0, CandidatureStatus.PENDING);
        save(4L, 3L, "Built ordering systems for two restaurants. Can do kitchen display and payments.", 6500.0, CandidatureStatus.ACCEPTED);
        save(4L, 4L, "Full-stack, real-time systems. Interested in this project.", 5500.0, CandidatureStatus.REJECTED);
        save(5L, 3L, "Conversion-focused landing pages. A/B tested and fast-loading.", 2000.0, CandidatureStatus.PENDING);
        save(5L, 5L, "SaaS landing pages specialist. Clean design and clear CTAs.", 2200.0, CandidatureStatus.PENDING);
        save(6L, 4L, "Vue.js dashboards and inventory UIs. Can add CSV export and alerts.", 4200.0, CandidatureStatus.PENDING);
        save(7L, 3L, "API integrations and webhooks. CRM and Shopify experience.", 3200.0, CandidatureStatus.ACCEPTED);
        save(8L, 6L, "Logo and brand identity. Delivered 50+ brand kits.", 1500.0, CandidatureStatus.PENDING);
        save(8L, 5L, "Brand designer. Vector and raster deliverables, style guides.", 1800.0, CandidatureStatus.PENDING);
    }

    private void save(Long projectId, Long freelancerId, String message, Double proposedBudget, CandidatureStatus status) {
        Candidature c = new Candidature();
        c.setProjectId(projectId);
        c.setFreelancerId(freelancerId);
        c.setMessage(message);
        c.setProposedBudget(proposedBudget);
        c.setStatus(status);
        candidatureRepository.save(c);
    }
}
