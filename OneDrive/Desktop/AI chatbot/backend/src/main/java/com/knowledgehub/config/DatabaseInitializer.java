package com.knowledgehub.config;

import com.knowledgehub.domain.user.Role;
import com.knowledgehub.domain.user.User;
import com.knowledgehub.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KnowledgeHubProperties properties;

    public DatabaseInitializer(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               KnowledgeHubProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        initializeStorageDirectories();
        seedDefaultUsers();
    }

    private void initializeStorageDirectories() {
        File uploadDir = new File(properties.getStorage().getUploadDir());
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            log.info("Initialized upload directory: {} (created: {})", uploadDir.getAbsolutePath(), created);
        }

        File okfDir = new File(properties.getStorage().getOkfDir());
        if (!okfDir.exists()) {
            boolean created = okfDir.mkdirs();
            log.info("Initialized OKF bundle directory: {} (created: {})", okfDir.getAbsolutePath(), created);
        }
    }

    private void seedDefaultUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    "admin@knowledgehub.local",
                    passwordEncoder.encode("Admin123!"),
                    "KnowledgeHub Administrator",
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);
            log.info("Seeded default administrator user: 'admin'");
        }

        if (!userRepository.existsByUsername("demo")) {
            User demo = new User(
                    "demo",
                    "demo@knowledgehub.local",
                    passwordEncoder.encode("Demo123!"),
                    "Enterprise Demo User",
                    Role.ROLE_USER
            );
            userRepository.save(demo);
            log.info("Seeded default demo user: 'demo'");
        }
    }
}
