package com.readingtracker.config;

import com.readingtracker.domain.*;
import com.readingtracker.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final BookRepository bookRepository;
    private final ShopItemRepository shopItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserRepository userRepository,
                              SchoolClassRepository schoolClassRepository,
                              BookRepository bookRepository,
                              ShopItemRepository shopItemRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.bookRepository = bookRepository;
        this.shopItemRepository = shopItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User teacher = new User();
            teacher.setEmail("teacher@readingtracker.local");
            teacher.setPasswordHash(passwordEncoder.encode("Teacher123!"));
            teacher.setRole(Role.TEACHER);
            userRepository.save(teacher);

            User student = new User();
            student.setEmail("student@readingtracker.local");
            student.setPasswordHash(passwordEncoder.encode("Student123!"));
            student.setRole(Role.STUDENT);
            userRepository.save(student);
        }

        if (schoolClassRepository.count() == 0) {
            SchoolClass classA = new SchoolClass();
            classA.setName("7.A");
            schoolClassRepository.save(classA);

            SchoolClass classB = new SchoolClass();
            classB.setName("8.B");
            schoolClassRepository.save(classB);
        }

        if (bookRepository.count() == 0) {
            User student = userRepository.findByEmail("student@readingtracker.local").orElse(null);
            if (student != null) {
                Book b1 = new Book();
                b1.setOwnerUserId(student.getId());
                b1.setTitle("Malý princ");
                b1.setAuthor("Antoine de Saint-Exupéry");
                b1.setTotalPages(96);
                b1.setStatus(BookStatus.READING);
                b1.setLastPage(32);
                bookRepository.save(b1);

                Book b2 = new Book();
                b2.setOwnerUserId(student.getId());
                b2.setTitle("Příběhy z jedné knihovny");
                b2.setAuthor("Český autor");
                b2.setTotalPages(120);
                b2.setStatus(BookStatus.READING);
                b2.setLastPage(18);
                bookRepository.save(b2);
            }
        }

        if (shopItemRepository.count() == 0) {
            ShopItem item1 = new ShopItem();
            item1.setName("Výjimečný bonus");
            item1.setDescription("Bonusová výhoda pro čtenáře");
            item1.setPricePoints(BigDecimal.valueOf(15));
            item1.setCooldownDays(7);
            item1.setActive(Boolean.TRUE);
            shopItemRepository.save(item1);

            ShopItem item2 = new ShopItem();
            item2.setName("Knihovní odměna");
            item2.setDescription("Doplňková odměna za pravidelné čtení");
            item2.setPricePoints(BigDecimal.valueOf(30));
            item2.setCooldownDays(14);
            item2.setActive(Boolean.TRUE);
            shopItemRepository.save(item2);
        }
    }
}
