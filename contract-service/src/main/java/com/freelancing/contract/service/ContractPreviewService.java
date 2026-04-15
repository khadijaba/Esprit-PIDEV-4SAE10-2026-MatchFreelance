package com.freelancing.contract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelancing.contract.client.ClaudeClient;
import com.freelancing.contract.client.OllamaChatClient;
import com.freelancing.contract.client.ProjectClient;
import com.freelancing.contract.config.ContractAiProperties;
import com.freelancing.contract.dto.PreviewFeedbackDTO;
import com.freelancing.contract.dto.PreviewResponseDTO;
import com.freelancing.contract.dto.RegeneratePreviewRequestDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.entity.ContractPreview;
import com.freelancing.contract.enums.PreviewStatus;
import com.freelancing.contract.repository.ContractPreviewRepository;
import com.freelancing.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContractPreviewService {

    private final ContractRepository contractRepository;
    private final ContractPreviewRepository previewRepository;
    private final OllamaChatClient ollamaClient;
    private final ClaudeClient claudeClient;
    private final ProjectClient projectClient;
    private final ContractAiProperties contractAiProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public PreviewResponseDTO generatePreview(Long contractId, String designStyle) {
        // Check if either AI service is enabled
        if (!contractAiProperties.getOllama().isEnabled() && !contractAiProperties.getClaude().isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI services are disabled. Enable either Ollama or Claude in configuration.");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        // Get project details
        ProjectClient.ProjectResponse project = projectClient.getProjectById(contract.getProjectId());

        // Build context for AI
        String context = buildPreviewContext(contract, project, designStyle);

        // Generate HTML/CSS with AI
        String systemPrompt = buildSystemPrompt(designStyle);

        try {
            String htmlContent;
            
            // Use Claude if enabled, otherwise fall back to Ollama
            if (contractAiProperties.getClaude().isEnabled()) {
                htmlContent = claudeClient.chat(systemPrompt, context);
            } else {
                htmlContent = ollamaClient.chat(systemPrompt, context);
            }

            // Clean up the response (remove markdown code blocks if present)
            htmlContent = cleanHtmlResponse(htmlContent);

            // Extract features count
            int featuresCount = countFeatures(htmlContent);

            // Save preview
            ContractPreview preview = new ContractPreview();
            preview.setContractId(contractId);
            preview.setHtmlContent(htmlContent);
            preview.setGeneratedAt(new Date());
            preview.setDesignStyle(designStyle != null ? designStyle : "modern");
            preview.setFeaturesCount(featuresCount);
            preview.setStatus(PreviewStatus.DRAFT);

            // Get latest version and increment
            Integer latestVersion = previewRepository.findFirstByContractIdOrderByVersionDesc(contractId)
                    .map(ContractPreview::getVersion)
                    .orElse(0);
            preview.setVersion(latestVersion + 1);

            preview = previewRepository.save(preview);

            return toResponseDTO(preview);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate preview: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public String getPreviewHtml(Long contractId, Long previewId) {
        ContractPreview preview = previewRepository.findByContractIdAndId(contractId, previewId)
                .orElseThrow(() -> new RuntimeException("Preview not found"));
        return preview.getHtmlContent();
    }

    @Transactional(readOnly = true)
    public List<PreviewResponseDTO> getContractPreviews(Long contractId) {
        return previewRepository.findByContractIdOrderByVersionDesc(contractId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public void submitFeedback(Long contractId, Long previewId, PreviewFeedbackDTO feedback) {
        ContractPreview preview = previewRepository.findByContractIdAndId(contractId, previewId)
                .orElseThrow(() -> new RuntimeException("Preview not found"));

        preview.setClientFeedback(feedback.getFeedback());
        if (feedback.getStatus() != null) {
            preview.setStatus(feedback.getStatus());
        }
        previewRepository.save(preview);
    }

    @Transactional
    public PreviewResponseDTO regenerateWithFeedback(Long contractId, Long previewId, RegeneratePreviewRequestDTO request) {
        ContractPreview oldPreview = previewRepository.findByContractIdAndId(contractId, previewId)
                .orElseThrow(() -> new RuntimeException("Preview not found"));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        ProjectClient.ProjectResponse project = projectClient.getProjectById(contract.getProjectId());

        // Build context with feedback
        String context = buildPreviewContextWithFeedback(contract, project, request);
        String systemPrompt = buildSystemPrompt(request.getDesignStyle());

        try {
            String htmlContent;
            
            // Use Claude if enabled, otherwise fall back to Ollama
            if (contractAiProperties.getClaude().isEnabled()) {
                htmlContent = claudeClient.chat(systemPrompt, context);
            } else {
                htmlContent = ollamaClient.chat(systemPrompt, context);
            }
            
            htmlContent = cleanHtmlResponse(htmlContent);
            int featuresCount = countFeatures(htmlContent);

            // Create new version
            ContractPreview newPreview = new ContractPreview();
            newPreview.setContractId(contractId);
            newPreview.setHtmlContent(htmlContent);
            newPreview.setGeneratedAt(new Date());
            newPreview.setDesignStyle(request.getDesignStyle() != null ? request.getDesignStyle() : oldPreview.getDesignStyle());
            newPreview.setFeaturesCount(featuresCount);
            newPreview.setStatus(PreviewStatus.DRAFT);
            newPreview.setVersion(oldPreview.getVersion() + 1);
            newPreview.setClientFeedback(request.getFeedback());

            newPreview = previewRepository.save(newPreview);

            // Archive old preview
            oldPreview.setStatus(PreviewStatus.ARCHIVED);
            previewRepository.save(oldPreview);

            return toResponseDTO(newPreview);

        } catch (Exception e) {
            throw new RuntimeException("Failed to regenerate preview: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt(String designStyle) {
        String styleGuidance = switch (designStyle != null ? designStyle : "modern") {
            case "bold" -> "Use vibrant colors (#FF6B6B, #4ECDC4, #FFE66D), large typography (text-5xl, text-6xl), bold visual elements, high contrast, and energetic feel.";
            case "corporate" -> "Professional, conservative color palette (blues #1E40AF, grays #64748B, white). Clean lines, formal typography, trust-building elements.";
            default -> "Modern, clean, minimalist design. Subtle colors (#6366F1, #8B5CF6, #EC4899), good whitespace, elegant typography, smooth gradients.";
        };

        return String.format("""
            You are an expert web designer and frontend developer. Generate a complete, professional, ANNOTATED homepage 
            using HTML and Tailwind CSS based on the project description provided.
            
            DESIGN STYLE: %s
            
            CRITICAL - ADD EXPLANATORY ANNOTATIONS:
            Each major section MUST include a visible annotation box that explains:
            - What this section represents
            - What content will go here
            - What customization options are available
            - Example: "This is your hero section - the first thing visitors see. You can customize the headline, 
              add your brand colors, change the background image, or add a video background."
            
            ANNOTATION STYLE:
            - Use a light yellow/blue info box with border-l-4 and bg-blue-50 or bg-yellow-50
            - Include a small info icon emoji 💡 or 📝
            - Use italic text and smaller font (text-sm)
            - Position at the TOP of each major section
            - Make it visually distinct but not overwhelming
            
            REQUIRED SECTIONS WITH ANNOTATIONS:
            
            1. NAVIGATION BAR
               - Logo placeholder and menu items
               - Annotation: "💡 Navigation Bar - Your site's main menu. Customize: logo, menu items (Home, About, Services, Contact), 
                 colors, sticky behavior, mobile hamburger menu."
            
            2. HERO SECTION (Full-screen, eye-catching)
               - Compelling headline based on project title
               - Subheadline explaining the value proposition
               - Primary CTA button (e.g., "Get Started", "Learn More")
               - Background with gradient or image
               - Annotation: "🎨 Hero Section - First impression area. Customize: headline text, background (solid color, gradient, 
                 image, or video), CTA button text/color, add secondary CTA, include trust badges or client logos."
            
            3. FEATURES SECTION (3-6 feature cards)
               - Grid layout with emoji icons (⚡ 🎯 🚀 💡 ✨ 🎨 📱 💻 🔥 ⭐)
               - Each feature: icon, title, description
               - Annotation: "⚡ Features Section - Highlight key benefits. Customize: number of features (3-12), icons, 
                 titles, descriptions, layout (grid/carousel), add hover effects, include statistics or metrics."
            
            4. ABOUT/DESCRIPTION SECTION
               - Project description with rich formatting
               - Image placeholder on side
               - Bullet points or highlights
               - Annotation: "📖 About Section - Tell your story. Customize: text content, images, add team photos, 
                 include timeline, add testimonials, embed video, show company values or mission statement."
            
            5. SHOWCASE/GALLERY SECTION (Optional but recommended)
               - 3-4 image placeholders showing examples
               - Grid or carousel layout
               - Annotation: "🖼️ Showcase Section - Display your work. Customize: add portfolio items, case studies, 
                 before/after comparisons, client projects, product screenshots, or service examples."
            
            6. TESTIMONIALS/SOCIAL PROOF (Optional but recommended)
               - 2-3 testimonial cards with quotes
               - Avatar placeholders
               - Star ratings
               - Annotation: "⭐ Testimonials - Build trust. Customize: add real client reviews, include company logos, 
                 show ratings, add video testimonials, display awards or certifications."
            
            7. PRICING/PACKAGES SECTION (If applicable)
               - 2-3 pricing tiers
               - Feature comparison
               - Annotation: "💰 Pricing Section - Show your offerings. Customize: pricing tiers, features per plan, 
                 highlight popular option, add FAQ, include money-back guarantee, show payment options."
            
            8. CALL-TO-ACTION SECTION
               - Strong CTA with urgency
               - Contact form or button
               - Annotation: "🎯 Call-to-Action - Convert visitors. Customize: CTA text, form fields (email, phone, message), 
                 add urgency (limited time offer), include contact info, add live chat, show response time."
            
            9. FOOTER
               - Links, social media, copyright
               - Annotation: "📌 Footer - Essential links. Customize: add sitemap links, social media icons, newsletter signup, 
                 contact info, privacy policy, terms of service, business hours."
            
            DESIGN REQUIREMENTS:
            - Use Tailwind CSS CDN (include in <head>)
            - CDN link format: <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
            - DO NOT include integrity or crossorigin attributes on CDN links
            - Fully responsive (mobile-first approach)
            - For images: Use https://dummyimage.com/WIDTHxHEIGHT/6366f1/ffffff&text=Label
              Examples:
              * Hero: https://dummyimage.com/1200x600/6366f1/ffffff&text=Hero+Image
              * Feature: https://dummyimage.com/400x300/8b5cf6/ffffff&text=Feature
              * Gallery: https://dummyimage.com/600x400/ec4899/ffffff&text=Gallery
              * Avatar: https://dummyimage.com/100x100/4f46e5/ffffff&text=Avatar
            - Use descriptive text in placeholder images (e.g., &text=Hero+Image)
            - For icons: ONLY use EMOJI (⚡ 🎯 🚀 💡 ✨ 🎨 📱 💻 🔥 ⭐ 💪 🌟 ❤️ 🏆 ⚙️ 🔒)
            - DO NOT use SVG icons or inline SVG elements
            - Professional color scheme matching the design style
            - Proper semantic HTML5 tags (header, nav, main, section, article, footer)
            - Accessible (ARIA labels, alt text, proper heading hierarchy)
            - Smooth scroll behavior and subtle animations
            - Add hover effects on interactive elements
            - Include spacing and visual hierarchy
            
            QUALITY STANDARDS:
            - Make it look PROFESSIONAL and POLISHED
            - Use proper typography hierarchy (h1, h2, h3)
            - Consistent spacing (padding, margins)
            - Visual balance and alignment
            - Color contrast for readability
            - Mobile-responsive breakpoints
            - Fast loading (no heavy resources)
            
            OUTPUT FORMAT - CRITICAL:
            - Return ONLY pure, static HTML code
            - NO template syntax (NO Blade, NO PHP, NO Jinja, NO Handlebars, NO Vue/React syntax)
            - NO server-side directives (@if, @foreach, @endif, {{ }}, etc.)
            - NO dynamic variables or loops - use hardcoded example content
            - Do NOT include markdown code blocks (no ```)
            - Do NOT include explanations outside the HTML
            - Start directly with <!DOCTYPE html>
            - End with </html> and nothing after
            - The HTML should be production-ready and render directly in a browser
            - All annotations should be INSIDE the HTML as visible elements
            - Use actual example text, not template placeholders
            """, styleGuidance);
    }

    private String buildPreviewContext(Contract contract, ProjectClient.ProjectResponse project, String designStyle) {
        try {
            ObjectNode context = objectMapper.createObjectNode();
            context.put("projectTitle", project.getTitle());
            context.put("projectDescription", project.getDescription());
            context.put("budget", contract.getProposedBudget());
            context.put("duration", project.getDuration());
            context.put("designStyle", designStyle != null ? designStyle : "modern");

            // Extract tech stack from description
            String techStack = extractTechStack(project.getDescription());
            context.put("techStack", techStack);

            // Extract key features from contract terms
            String features = extractKeyFeatures(contract.getTerms(), project.getDescription());
            context.put("keyFeatures", features);

            // Add target audience hint
            context.put("targetAudience", extractTargetAudience(project.getDescription()));

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build context", e);
        }
    }

    private String buildPreviewContextWithFeedback(Contract contract, ProjectClient.ProjectResponse project, RegeneratePreviewRequestDTO request) {
        try {
            ObjectNode context = objectMapper.createObjectNode();
            context.put("projectTitle", project.getTitle());
            context.put("projectDescription", project.getDescription());
            context.put("budget", contract.getProposedBudget());
            context.put("duration", project.getDuration());
            context.put("designStyle", request.getDesignStyle() != null ? request.getDesignStyle() : "modern");
            context.put("clientFeedback", request.getFeedback());
            context.put("instruction", "Regenerate the preview incorporating this feedback: " + request.getFeedback());

            String techStack = extractTechStack(project.getDescription());
            context.put("techStack", techStack);

            String features = extractKeyFeatures(contract.getTerms(), project.getDescription());
            context.put("keyFeatures", features);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build context with feedback", e);
        }
    }

    private String extractTechStack(String description) {
        List<String> techKeywords = List.of(
                "React", "Angular", "Vue", "Node.js", "Python", "Java", "Spring", "Django",
                "MongoDB", "PostgreSQL", "MySQL", "Redis", "Docker", "Kubernetes",
                "AWS", "Azure", "GCP", "TypeScript", "JavaScript", "PHP", "Laravel"
        );

        List<String> found = new ArrayList<>();
        String lowerDesc = description.toLowerCase();
        for (String tech : techKeywords) {
            if (lowerDesc.contains(tech.toLowerCase())) {
                found.add(tech);
            }
        }

        return found.isEmpty() ? "Modern web technologies" : String.join(", ", found);
    }

    private String extractKeyFeatures(String terms, String description) {
        String combined = (terms != null ? terms : "") + " " + (description != null ? description : "");

        List<String> featureKeywords = List.of(
                "authentication", "user login", "dashboard", "admin panel", "payment",
                "shopping cart", "search", "filter", "notification", "chat", "messaging",
                "analytics", "reporting", "export", "import", "api", "integration"
        );

        List<String> found = new ArrayList<>();
        String lowerCombined = combined.toLowerCase();
        for (String feature : featureKeywords) {
            if (lowerCombined.contains(feature)) {
                found.add(feature);
            }
        }

        return found.isEmpty() ? "Core functionality as described" : String.join(", ", found);
    }

    private String extractTargetAudience(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("business") || lower.contains("enterprise") || lower.contains("b2b")) {
            return "Business professionals";
        } else if (lower.contains("student") || lower.contains("education")) {
            return "Students and educators";
        } else if (lower.contains("ecommerce") || lower.contains("shop")) {
            return "Online shoppers";
        } else {
            return "General users";
        }
    }

    private String cleanHtmlResponse(String html) {
        // Remove ALL markdown code blocks (at start, middle, or end)
        html = html.replaceAll("```[a-z]*\\s*", "");
        html = html.replaceAll("```", "");
        html = html.trim();

        // Find the actual HTML document boundaries
        int doctypeIndex = html.toLowerCase().indexOf("<!doctype html>");
        int htmlEndIndex = html.toLowerCase().lastIndexOf("</html>");
        
        if (doctypeIndex >= 0 && htmlEndIndex > doctypeIndex) {
            // Extract ONLY the HTML document (from <!DOCTYPE to </html>)
            html = html.substring(doctypeIndex, htmlEndIndex + 7); // +7 for "</html>"
        } else if (doctypeIndex >= 0) {
            // If we found DOCTYPE but no closing tag, start from DOCTYPE
            html = html.substring(doctypeIndex);
        }

        // Remove any text AFTER </html> tag (AI explanations)
        int finalHtmlEnd = html.toLowerCase().lastIndexOf("</html>");
        if (finalHtmlEnd > 0 && finalHtmlEnd + 7 < html.length()) {
            html = html.substring(0, finalHtmlEnd + 7);
        }

        // Remove Blade/template syntax that shouldn't be there
        html = html.replaceAll("@if\\s*\\([^)]*\\)", "");
        html = html.replaceAll("@endif", "");
        html = html.replaceAll("@foreach\\s*\\([^)]*\\)", "");
        html = html.replaceAll("@endforeach", "");
        html = html.replaceAll("@else", "");
        html = html.replaceAll("@elseif\\s*\\([^)]*\\)", "");
        html = html.replaceAll("\\{\\{\\s*[^}]*\\s*\\}\\}", "");
        html = html.replaceAll("\\{!!\\s*[^}]*\\s*!!\\}", "");

        // Remove integrity and crossorigin attributes from CDN links (they cause issues)
        html = html.replaceAll("\\s+integrity=[\"'][^\"']*[\"']", "");
        html = html.replaceAll("\\s+crossorigin=[\"'][^\"']*[\"']", "");

        // Remove references to local CSS files that don't exist
        // Keep only CDN links (https://) and inline styles
        html = html.replaceAll("<link[^>]*href=[\"'](?!https?://)[^\"']*\\.css[\"'][^>]*>", "");
        
        // Remove references to local JS files that don't exist
        html = html.replaceAll("<script[^>]*src=[\"'](?!https?://)[^\"']*\\.js[\"'][^>]*></script>", "");

        // Fix broken image URLs - replace with dummyimage.com
        // Replace unsplash URLs that might not work
        html = html.replaceAll("https://images\\.unsplash\\.com/[^\"']*", "https://dummyimage.com/800x600/6366f1/ffffff&text=Image");
        
        // Replace via.placeholder.com with dummyimage.com (more reliable)
        html = html.replaceAll("https://via\\.placeholder\\.com/(\\d+)x(\\d+)/([^/\"']+)/([^/\"'?]+)\\?text=([^\"']*)", "https://dummyimage.com/$1x$2/$3/$4&text=$5");
        html = html.replaceAll("https://via\\.placeholder\\.com/(\\d+)x(\\d+)", "https://dummyimage.com/$1x$2/6366f1/ffffff&text=Image");
        
        // Replace placehold.co with dummyimage.com
        html = html.replaceAll("https://placehold\\.co/(\\d+)x(\\d+)/png", "https://dummyimage.com/$1x$2/6366f1/ffffff&text=Image");
        html = html.replaceAll("https://placehold\\.co/(\\d+)x(\\d+)", "https://dummyimage.com/$1x$2/6366f1/ffffff&text=Image");
        
        // Replace any local image references with placeholders
        html = html.replaceAll("<img([^>]*)src=[\"'](?!https?://)[^\"']*[\"']", "<img$1src=\"https://dummyimage.com/600x400/6366f1/ffffff&text=Image\"");
        
        // Fix broken image URLs (images/, assets/, etc.)
        html = html.replaceAll("src=[\"'](images|assets|img)/[^\"']*[\"']", "src=\"https://dummyimage.com/600x400/6366f1/ffffff&text=Image\"");

        // Remove broken SVG elements with invalid path data
        html = html.replaceAll("<svg[^>]*>.*?</svg>", "");

        return html.trim();
    }

    private int countFeatures(String html) {
        // Count sections or feature divs
        Pattern pattern = Pattern.compile("<section|<div[^>]*class=\"[^\"]*feature", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return Math.max(3, Math.min(count, 8)); // Reasonable range
    }

    private PreviewResponseDTO toResponseDTO(ContractPreview preview) {
        return PreviewResponseDTO.builder()
                .previewId(preview.getId())
                .contractId(preview.getContractId())
                .htmlUrl("/api/contracts/" + preview.getContractId() + "/preview/" + preview.getId())
                .generatedAt(preview.getGeneratedAt())
                .version(preview.getVersion())
                .status(preview.getStatus())
                .designStyle(preview.getDesignStyle())
                .featuresCount(preview.getFeaturesCount())
                .clientFeedback(preview.getClientFeedback())
                .build();
    }
}
