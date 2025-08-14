package com.church.festival.config;

import com.church.festival.entity.User;
import com.church.festival.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data initialization service to create default admin user
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.default.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.default.admin.password:admin123}")
    private String defaultAdminPassword;

    @Value("${app.default.admin.email:admin@church.com}")
    private String defaultAdminEmail;

    @Override
    public void run(String... args) throws Exception {
        createDefaultAdmin();
    }

    private void createDefaultAdmin() {
        if (!userRepository.existsByUsername(defaultAdminUsername)) {
            User admin = new User();
            admin.setUsername(defaultAdminUsername);
            admin.setEmail(defaultAdminEmail);
            admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole(User.Role.ADMIN);
            admin.setActive(true);
            
            userRepository.save(admin);
            System.out.println("Default admin user created:");
            System.out.println("Username: " + defaultAdminUsername);
            System.out.println("Password: " + defaultAdminPassword);
            System.out.println("Email: " + defaultAdminEmail);
        }
    }
}
