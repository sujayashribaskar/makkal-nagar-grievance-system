package com.makkalnagar.service;

import com.makkalnagar.entity.ArrivalSequenceEntity;
import com.makkalnagar.entity.UserEntity;
import com.makkalnagar.repository.ArrivalSequenceRepository;
import com.makkalnagar.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataInitializerService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerService.class);

    private final UserRepository userRepository;
    private final ArrivalSequenceRepository arrivalSequenceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializerService(UserRepository userRepository,
                                  ArrivalSequenceRepository arrivalSequenceRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.arrivalSequenceRepository = arrivalSequenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Bootstrapping Makkal Nagar Corporation Initial Configuration...");

        // 1. Ensure atomic arrival sequence initialized
        arrivalSequenceRepository.findById("complaint_arrival").ifPresentOrElse(
                seq -> log.info("Arrival sequence exists at current value: {}", seq.getCurrentVal()),
                () -> {
                    arrivalSequenceRepository.save(new ArrivalSequenceEntity("complaint_arrival", 0L));
                    log.info("Initialized arrival sequence counter at 0.");
                }
        );

        // 2. Ensure default administrator account (admin / Admin@Makkal2026)
        userRepository.findByUsername("admin").ifPresentOrElse(
                adminUser -> {
                    // Refresh password hash to ensure exact match with BCrypt encoder
                    adminUser.setPasswordHash(passwordEncoder.encode("Admin@Makkal2026"));
                    adminUser.setRole("ROLE_ADMIN");
                    userRepository.save(adminUser);
                    log.info("Verified administrative user 'admin'.");
                },
                () -> {
                    UserEntity newAdmin = new UserEntity(
                            "admin",
                            passwordEncoder.encode("Admin@Makkal2026"),
                            "Chief Municipal Administrator",
                            "ROLE_ADMIN"
                    );
                    userRepository.save(newAdmin);
                    log.info("Bootstrapped initial administrative account 'admin'.");
                }
        );
    }
}
