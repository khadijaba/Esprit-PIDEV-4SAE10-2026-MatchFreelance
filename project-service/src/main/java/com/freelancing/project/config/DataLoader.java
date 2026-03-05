package com.freelancing.project.config;

import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import com.freelancing.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds demo projects for evaluation. Runs only when the project table is empty.
 * Client ID 1 must exist in user-service.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) {
        if (projectRepository.count() > 0) return;

        saveProject("SaaS Analytics Dashboard – React & TypeScript",
                "Build a B2B analytics dashboard with real-time data, charts (D3/Recharts), multi-tenant architecture and role-based access. Required: React, TypeScript, Node.js, PostgreSQL, state management (Redux/Zustand), REST/GraphQL APIs. CI/CD and testing (Jest, Cypress) expected.",
                8000.0, 12000.0, 60, ProjectStatus.OPEN);

        saveProject("FinTech Payment API – Full-Stack",
                "Design and implement the core of our B2B payment processing API: secure flows, webhooks, Stripe integration, admin dashboard. Security is critical: PCI-DSS awareness, encryption, audit logs. Stack: Django REST Framework, React, AWS, Redis. Prior payment or fintech experience required.",
                10000.0, 15000.0, 90, ProjectStatus.OPEN);

        saveProject("Logistics & Tracking – Backend API",
                "RESTful APIs for a logistics platform: real-time shipment status, driver location (WebSockets), route optimization, SMS/email notifications. MongoDB, Node.js, Express, Docker. All APIs documented with Swagger/OpenAPI. Clean code and API versioning required.",
                5000.0, 7000.0, 45, ProjectStatus.OPEN);

        saveProject("Healthcare Patient Dashboard – React",
                "Patient and appointment management dashboard with HIPAA-aware design, role-based access and reporting. React, TypeScript, Node.js, PostgreSQL. Experience in healthcare or regulated domains is a plus.",
                9000.0, 14000.0, 75, ProjectStatus.OPEN);

        saveProject("E-Learning Platform – Video & Quizzes",
                "Online learning platform: video upload/streaming, quizzes, progress tracking, certificates. React or Angular frontend, Node.js or Django backend, cloud storage (e.g. AWS S3). End-to-end delivery expected.",
                6000.0, 9500.0, 50, ProjectStatus.IN_PROGRESS);

        saveProject("Real Estate Listings – Mobile App",
                "Mobile app for property listings: search, filters, map view, favorites, contact agents. React Native or Flutter, REST API, push notifications. Published apps to App Store / Play Store preferred.",
                5500.0, 8500.0, 55, ProjectStatus.OPEN);

        saveProject("API Documentation Portal",
                "Developer portal for our public API: interactive docs (OpenAPI/Swagger), code samples, authentication guide. React or static site generator. Clear, maintainable documentation structure.",
                2500.0, 4000.0, 18, ProjectStatus.OPEN);

        saveProject("Microservices Migration – Spring Boot",
                "Split an existing Java Spring Boot monolith into 3–4 microservices. Event-driven communication, API gateway, Docker and Kubernetes. Strong Java and Spring experience required.",
                15000.0, 22000.0, 90, ProjectStatus.COMPLETED);
    }

    private void saveProject(String title, String description, double minBudget, double maxBudget, int duration, ProjectStatus status) {
        Project p = new Project();
        p.setTitle(title);
        p.setDescription(description);
        p.setMinBudget(minBudget);
        p.setMaxBudget(maxBudget);
        p.setDuration(duration);
        p.setStatus(status);
        p.setClientId(1L);
        projectRepository.save(p);
    }
}
