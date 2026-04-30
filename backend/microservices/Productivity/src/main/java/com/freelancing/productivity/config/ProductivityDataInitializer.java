package com.freelancing.productivity.config;

import com.freelancing.productivity.entity.*;
import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import com.freelancing.productivity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductivityDataInitializer implements CommandLineRunner {

    private final ProductivityTaskRepository taskRepository;
    private final TodoListRepository listRepository;
    private final TodoItemRepository itemRepository;
    private final ProductivityGoalRepository goalRepository;
    private final DecisionLogEntryRepository decisionLogRepository;
    private final TaskDependencyRepository taskDependencyRepository;

    @Override
    public void run(String... args) {
        if (taskRepository.count() > 0 || listRepository.count() > 0) {
            return;
        }

        // Create 30 Productivity Goals for user 5 (Amira)
        List<ProductivityGoal> goals = new ArrayList<>();
        String[] goalTitles = {
            "Complete freelance platform MVP", "Master Spring Boot microservices", "Build personal portfolio website",
            "Learn advanced React patterns", "Improve code review skills", "Achieve 90% test coverage",
            "Optimize database queries", "Implement CI/CD pipeline", "Study system design patterns",
            "Complete AWS certification", "Build mobile app prototype", "Learn Kubernetes basics",
            "Improve communication skills", "Master Docker containerization", "Study GraphQL implementation",
            "Build real-time chat feature", "Learn Redis caching", "Implement OAuth2 security",
            "Study microservices patterns", "Build analytics dashboard", "Learn TypeScript advanced features",
            "Implement WebSocket communication", "Study event-driven architecture", "Build API gateway",
            "Master Git workflows", "Learn MongoDB aggregation", "Build recommendation engine",
            "Study machine learning basics", "Implement search functionality", "Master performance optimization"
        };
        
        for (int i = 0; i < 30; i++) {
            ProductivityGoal goal = new ProductivityGoal();
            goal.setOwnerId(5L); // All for Amira
            goal.setTitle(goalTitles[i]);
            goal.setDescription("Detailed plan and milestones for: " + goalTitles[i]);
            goal.setTargetDate(Instant.now().plus(30 + (i * 5), ChronoUnit.DAYS));
            goals.add(goalRepository.save(goal));
        }

        // Create 40 Productivity Tasks for user 5 (Amira)
        List<ProductivityTask> tasks = new ArrayList<>();
        String[] taskTitles = {
            "Design database schema", "Implement user authentication", "Create REST API endpoints",
            "Write unit tests", "Setup Docker containers", "Configure Eureka server",
            "Implement API Gateway", "Create Angular components", "Setup MySQL database",
            "Write integration tests", "Implement JWT security", "Create service layer",
            "Design UI mockups", "Setup CI/CD pipeline", "Write API documentation",
            "Implement error handling", "Create data models", "Setup logging framework",
            "Implement caching layer", "Write end-to-end tests", "Create admin dashboard",
            "Implement file upload", "Setup monitoring tools", "Create user profiles",
            "Implement search functionality", "Write technical documentation", "Setup backup strategy",
            "Implement notifications", "Create reporting module", "Optimize performance",
            "Refactor legacy code", "Implement rate limiting", "Create email templates",
            "Setup load balancing", "Implement data validation", "Create mobile responsive design",
            "Setup SSL certificates", "Implement audit logging", "Create data migration scripts",
            "Setup automated testing"
        };
        
        ProductivityPriority[] priorities = {ProductivityPriority.HIGH, ProductivityPriority.MEDIUM, ProductivityPriority.LOW};
        ProductivityTaskStatus[] statuses = {ProductivityTaskStatus.TODO, ProductivityTaskStatus.IN_PROGRESS, 
                                            ProductivityTaskStatus.DONE, ProductivityTaskStatus.BLOCKED};
        
        for (int i = 0; i < 40; i++) {
            ProductivityTask task = new ProductivityTask();
            task.setOwnerId(5L); // All for Amira
            task.setTitle(taskTitles[i]);
            task.setDescription("Implementation details for: " + taskTitles[i]);
            task.setPriority(priorities[i % 3]);
            task.setStatus(statuses[i % 4]);
            task.setPlannedMinutes(60 + (i * 15));
            task.setDueAt(Instant.now().plus(i + 1, ChronoUnit.DAYS));
            
            if (i < goals.size()) {
                task.setGoalId(goals.get(i % goals.size()).getId());
            }
            
            if (task.getStatus() == ProductivityTaskStatus.DONE) {
                task.setActualMinutes(task.getPlannedMinutes() + (i % 2 == 0 ? 10 : -5));
                task.setCompletedAt(Instant.now().minus(i, ChronoUnit.HOURS));
            } else if (task.getStatus() == ProductivityTaskStatus.IN_PROGRESS) {
                task.setActualMinutes((task.getPlannedMinutes() / 2));
            }
            
            tasks.add(taskRepository.save(task));
        }

        // Create 30 Todo Lists for user 5 (Amira)
        List<TodoList> lists = new ArrayList<>();
        String[] listNames = {
            "Daily Tasks", "Weekly Goals", "Project Backlog", "Bug Fixes", "Code Reviews",
            "Documentation", "Learning Resources", "Meeting Notes", "Ideas", "Shopping List",
            "Personal Goals", "Work Tasks", "Side Projects", "Research Topics", "Books to Read",
            "Movies to Watch", "Travel Plans", "Fitness Goals", "Meal Planning", "Home Improvements",
            "Financial Goals", "Networking", "Skills to Learn", "Blog Post Ideas", "Gift Ideas",
            "Career Development", "Health & Wellness", "Creative Projects", "Client Follow-ups", "Team Collaboration"
        };
        
        for (int i = 0; i < 30; i++) {
            TodoList list = new TodoList();
            list.setOwnerId(5L); // All for Amira
            list.setName(listNames[i]);
            lists.add(listRepository.save(list));
        }

        // Create 60 Todo Items for user 5 (Amira) - 2 per list on average
        String[] itemTitles = {
            "Review pull requests", "Update documentation", "Fix login bug", "Refactor user service",
            "Write blog post", "Prepare presentation", "Schedule team meeting", "Update dependencies",
            "Test new feature", "Deploy to staging", "Review security audit", "Optimize queries",
            "Create wireframes", "Update README", "Fix CSS issues", "Implement feedback",
            "Write test cases", "Update API docs", "Review code coverage", "Setup monitoring",
            "Create user stories", "Update project board", "Review analytics", "Fix mobile layout",
            "Implement dark mode", "Update translations", "Review performance", "Fix memory leak",
            "Update error messages", "Create demo video", "Review accessibility", "Update changelog",
            "Fix broken links", "Update dependencies", "Review logs", "Create backup",
            "Update configuration", "Review metrics", "Fix validation", "Update schema",
            "Create migration", "Review feedback", "Update UI components", "Fix race condition",
            "Update tests", "Review architecture", "Create diagrams", "Update wiki",
            "Fix edge cases", "Review security", "Implement caching", "Update styles",
            "Fix responsive design", "Review API endpoints", "Update error handling", "Create reports",
            "Fix data validation", "Review database indexes", "Update build scripts", "Create tutorials"
        };
        
        for (int i = 0; i < 60; i++) {
            TodoItem item = new TodoItem();
            item.setOwnerId(5L); // All for Amira
            item.setListId(lists.get(i % lists.size()).getId());
            item.setTitle(itemTitles[i]);
            item.setPositionIndex(i % 10);
            item.setDone(i % 3 == 0);
            if (i % 4 == 0) {
                item.setDueAt(Instant.now().plus(i % 7 + 1, ChronoUnit.DAYS));
            }
            itemRepository.save(item);
        }

        // Create 30 Decision Log Entries for user 5 (Amira)
        String[] decisionTypes = {
            "ARCHITECTURE", "TECHNOLOGY", "DESIGN", "PROCESS", "PRIORITY", "SCOPE", "RESOURCE"
        };
        String[] reasons = {
            "After evaluating multiple options, this approach provides the best balance of performance and maintainability.",
            "Team consensus reached after thorough discussion and prototyping.",
            "Based on industry best practices and our specific requirements.",
            "Cost-benefit analysis shows significant long-term advantages.",
            "Aligns with our technical roadmap and strategic goals.",
            "Reduces technical debt and improves code quality.",
            "Enhances user experience and meets accessibility standards.",
            "Improves team productivity and collaboration.",
            "Addresses critical security concerns.",
            "Enables better scalability and future growth.",
            "Simplifies maintenance and reduces complexity.",
            "Improves performance metrics significantly.",
            "Better integration with existing systems.",
            "Reduces operational costs.",
            "Enhances monitoring and debugging capabilities.",
            "Improves deployment process and reliability.",
            "Better documentation and knowledge sharing.",
            "Reduces time to market for new features.",
            "Improves code reusability and modularity.",
            "Better error handling and recovery.",
            "Enhances testing capabilities.",
            "Improves data consistency and integrity.",
            "Better resource utilization.",
            "Enhances user feedback mechanisms.",
            "Improves overall system reliability.",
            "Facilitates easier onboarding for new team members.",
            "Reduces coupling between components.",
            "Improves API design and consistency.",
            "Better handling of edge cases.",
            "Enhances system observability."
        };
        
        for (int i = 0; i < 30; i++) {
            DecisionLogEntry entry = new DecisionLogEntry();
            entry.setOwnerId(5L); // All for Amira
            if (i < tasks.size()) {
                entry.setTaskId(tasks.get(i).getId());
            }
            entry.setDecisionType(decisionTypes[i % decisionTypes.length]);
            entry.setReason(reasons[i]);
            decisionLogRepository.save(entry);
        }

        // Create 30 Task Dependencies for user 5 (Amira)
        for (int i = 0; i < 30 && i < tasks.size() - 1; i++) {
            TaskDependency dependency = new TaskDependency();
            dependency.setOwnerId(5L); // All for Amira
            dependency.setPredecessorTaskId(tasks.get(i).getId());
            dependency.setSuccessorTaskId(tasks.get(i + 1).getId());
            taskDependencyRepository.save(dependency);
        }
    }
}

