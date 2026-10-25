package com.prepgpt.config;

import com.prepgpt.entity.InterviewCategory;
import com.prepgpt.entity.Role;
import com.prepgpt.entity.User;
import com.prepgpt.repository.InterviewCategoryRepository;
import com.prepgpt.repository.RoleRepository;
import com.prepgpt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final InterviewCategoryRepository categoryRepository;
    private final QuestionSeeder questionSeeder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           InterviewCategoryRepository categoryRepository,
                           QuestionSeeder questionSeeder,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.questionSeeder = questionSeeder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed Roles
        if (roleRepository.findByName("ROLE_STUDENT").isEmpty()) {
            roleRepository.save(new Role("ROLE_STUDENT"));
        }
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role("ROLE_ADMIN"));
        }

        // Seed Interview Categories idempotently
        seedCategories();

        // Seed Questions idempotently
        questionSeeder.seedQuestions();

        // Optional admin bootstrap via environment variables.
        // Set ADMIN_EMAIL and ADMIN_PASSWORD to create an initial admin on first run.
        // If the email already exists, this block is skipped (idempotent).
        // ADMIN_PASSWORD is NEVER logged or returned in any API response.
        // No default credentials exist if env vars are absent.
        seedAdminUserIfConfigured();
    }

    private void seedAdminUserIfConfigured() {
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");

        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            // Env vars absent — skip silently. Application starts normally.
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin bootstrap skipped — user already exists for email: {}", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found during admin bootstrap"));

        // Password is BCrypt-hashed before persistence. Plaintext is never logged.
        String hashedPassword = passwordEncoder.encode(adminPassword);
        User adminUser = new User(adminEmail, hashedPassword, "Admin", adminRole);
        userRepository.save(adminUser);

        // Only email is logged at INFO level — never the password.
        log.info("Admin user bootstrapped successfully for email: {}", adminEmail);
    }

    private void seedCategories() {
        List<InterviewCategory> defaultCategories = List.of(
                new InterviewCategory("Technical Interview", "Core technical concepts, language features, frameworks, and CS fundamentals.", true),
                new InterviewCategory("DSA Interview", "Data structures, algorithms, space & time complexity analysis.", true),
                new InterviewCategory("Coding Interview", "Hands-on problem solving, live coding challenges, and code optimization.", true),
                new InterviewCategory("HR Interview", "Career goals, company culture fit, salary expectations, and general HR questions.", true),
                new InterviewCategory("Behavioral Interview", "STAR methodology scenario questions, teamwork, leadership, and conflict resolution.", true),
                new InterviewCategory("Project Interview", "Architecture defense, technical decisions, role contributions, and challenges faced.", true),
                new InterviewCategory("Resume Interview", "Questions tailored to your uploaded skills, experience, and educational background.", true),
                new InterviewCategory("Job Description Interview", "Role-targeted mock interview based on a specific target job description.", true)
        );

        for (InterviewCategory category : defaultCategories) {
            if (categoryRepository.findByName(category.getName()).isEmpty()) {
                categoryRepository.save(category);
            }
        }
    }
}
