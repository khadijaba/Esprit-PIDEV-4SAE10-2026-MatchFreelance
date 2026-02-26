package com.freelancing.project.config;

import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import com.freelancing.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) {
        if (projectRepository.count() > 0) return;

        saveProject("E-Commerce Website Redesign",
                "Full redesign of an existing e-commerce platform. We need modern UI/UX, mobile-first design, payment integration (Stripe), and improved product catalog with filters and search.",
                10000.0, 15000.0, 45, ProjectStatus.OPEN);
        saveProject("Mobile App for Fitness Tracking",
                "Build a cross-platform mobile app (React Native or Flutter) for tracking workouts, calories, and progress. Dashboard with charts, social sharing, and optional wearables sync.",
                7000.0, 10000.0, 60, ProjectStatus.OPEN);
        saveProject("WordPress Blog Migration",
                "Migrate existing blog from Blogger to WordPress. Custom theme design, SEO optimization, contact form, newsletter integration, and basic analytics setup.",
                2000.0, 3000.0, 14, ProjectStatus.OPEN);
        saveProject("Restaurant Ordering System",
                "Web-based ordering system for a restaurant. Menu management, real-time orders, kitchen display, payment processing, and admin dashboard for staff.",
                5000.0, 8000.0, 30, ProjectStatus.IN_PROGRESS);
        saveProject("Landing Page for SaaS Product",
                "Conversion-focused landing page with hero section, features, pricing table, testimonials, FAQ, and contact form. Clean, professional design.",
                1500.0, 2500.0, 10, ProjectStatus.OPEN);
        saveProject("Inventory Management Dashboard",
                "Admin dashboard for small business inventory. Track stock levels, low-stock alerts, basic reporting, and export to CSV. Vue.js or React preferred.",
                3500.0, 5000.0, 25, ProjectStatus.OPEN);
        saveProject("API Integration - CRM to Shopify",
                "Integrate existing CRM with Shopify store. Sync customers, orders, and product data. Webhook setup and error handling required.",
                2500.0, 4000.0, 20, ProjectStatus.COMPLETED);
        saveProject("Logo and Brand Identity",
                "Create logo, color palette, typography guide, and basic brand guidelines for a new tech startup. Deliverables in vector and raster formats.",
                1000.0, 2000.0, 7, ProjectStatus.OPEN);
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
