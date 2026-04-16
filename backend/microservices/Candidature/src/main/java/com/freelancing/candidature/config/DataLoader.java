package com.freelancing.candidature.config;

import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import com.freelancing.candidature.repository.CandidatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds demo applications (candidatures) for evaluation. Runs only when no candidatures exist.
 * Assumes: user-service has client id 1, freelancers 2, 4, 5, 6; project-service has projects 1–8.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CandidatureRepository candidatureRepository;

    private static final List<Long> FREELANCER_IDS = List.of(2L, 4L, 5L, 6L);
    private static final int MAX_PROJECT_ID = 8;

    /** Professional application messages, two variants per project (1–8). Index = (projectId-1)*2 + variant(0|1). */
    private static final List<String> MESSAGES = List.of(
            // Project 1 – SaaS Analytics Dashboard
            "I have 5+ years with React, TypeScript and Node.js and have shipped several B2B analytics dashboards with real-time data and D3/Recharts. I am comfortable with multi-tenant setups and RBAC and can deliver a maintainable codebase with tests and CI/CD.",
            "Senior front-end developer here. I specialise in React and TypeScript, state management (Redux/Zustand) and GraphQL integration. I have built dashboards for large datasets with virtualization and performance tuning. Happy to align with your stack and timeline.",
            // Project 2 – FinTech Payment API
            "I have production experience with Django REST Framework and React in FinTech. I have implemented Stripe, webhooks and PCI-DSS aware design and built admin dashboards for payment monitoring. I can commit to your security and delivery requirements.",
            "Full-stack engineer with a focus on payment systems. I have worked on B2B payment APIs, idempotent flows and AWS (EC2, RDS, Redis). I am used to documentation and clean API design and can start quickly.",
            // Project 3 – Logistics Backend API
            "I have 4 years of backend experience with Node.js, Express and MongoDB. I have implemented WebSockets for real-time tracking and REST APIs for logistics, with Docker and Swagger. I can deliver well-documented, versioned APIs.",
            "Backend developer with experience in Node.js, MongoDB and real-time systems. I have integrated Twilio/SendGrid and built APIs with OpenAPI docs. I am comfortable with Docker and agile delivery.",
            // Project 4 – Healthcare Patient Dashboard
            "I have built healthcare-oriented dashboards with HIPAA-aware design and role-based access using React, TypeScript and Node.js. I understand regulated environments and can deliver a secure, accessible solution.",
            "React and TypeScript developer with experience in regulated domains. I have implemented RBAC, audit trails and reporting. I can work with your backend and compliance requirements.",
            // Project 5 – E-Learning Platform
            "I have delivered e-learning platforms with video upload/streaming, quizzes, progress tracking and certificates. I use React and Node.js and have integrated AWS S3. I can deliver the full platform end to end.",
            "Full-stack developer with experience in learning management systems. I have built video and quiz modules and progress dashboards. I am comfortable with cloud storage and modern front-end and backend stacks.",
            // Project 6 – Real Estate Mobile App
            "I develop mobile apps with React Native and Flutter. I have built property listing apps with maps, filters, favourites and push notifications and integrated REST APIs. I care about performance and UX.",
            "Mobile developer with React Native and Flutter experience. I have shipped apps with map integration, search and notifications. I can deliver for both iOS and Android and work with your API.",
            // Project 7 – API Documentation Portal
            "I have built developer portals with interactive OpenAPI/Swagger docs, code samples and auth guides. I use React and static site generators and focus on clear, maintainable documentation structure.",
            "Front-end developer with experience in technical documentation sites. I have integrated OpenAPI specs and code examples and care about clarity and consistency for developers.",
            // Project 8 – Microservices Migration
            "I have 3+ years with Java and Spring Boot and have worked on microservices, event-driven systems and Kubernetes. I have helped split monoliths and set up API gateways and service communication. I can support your migration and standards.",
            "Senior Java/Spring developer with experience in microservices and Docker/Kubernetes. I have designed event-driven APIs and migration strategies. I can deliver incrementally and document decisions."
    );

    @Override
    public void run(String... args) {
        if (candidatureRepository.count() > 0) return;

        for (long projectId = 1; projectId <= MAX_PROJECT_ID; projectId++) {
            int variant = 0;
            for (Long freelancerId : FREELANCER_IDS) {
                if (candidatureRepository.existsByProjectIdAndFreelancerId(projectId, freelancerId)) continue;
                int messageIndex = (int) (projectId - 1) * 2 + (variant % 2);
                String message = MESSAGES.get(messageIndex);

                Candidature c = new Candidature();
                c.setProjectId(projectId);
                c.setFreelancerId(freelancerId);
                c.setMessage(message);
                c.setProposedBudget(4500.0 + (projectId * 350) + (freelancerId * 80));
                c.setExtraTasksBudget(projectId % 2 == 0 ? 400.0 : null);
                c.setStatus(CandidatureStatus.PENDING);
                candidatureRepository.save(c);
                variant++;
            }
        }
    }
}
