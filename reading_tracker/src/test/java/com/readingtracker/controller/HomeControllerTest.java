package com.readingtracker.controller;

import com.readingtracker.domain.Book;
import com.readingtracker.domain.BookStatus;
import com.readingtracker.domain.Role;
import com.readingtracker.domain.User;
import com.readingtracker.repository.BookRepository;
import com.readingtracker.repository.ReadingSessionRepository;
import com.readingtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReadingSessionRepository readingSessionRepository;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        readingSessionRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("student@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.STUDENT);
        user = userRepository.save(user);

        book = new Book();
        book.setOwnerUserId(user.getId());
        book.setTitle("Testová kniha");
        book.setAuthor("Autor");
        book.setTotalPages(120);
        book.setStatus(BookStatus.READING);
        book.setLastPage(0);
        book = bookRepository.save(book);
    }

    @Test
    void saveSession_shouldNotCrashWhenDateIsMalformed() throws Exception {
        mockMvc.perform(post("/new-session")
                .param("bookId", String.valueOf(book.getId()))
                .param("date", "not-a-date")
                .param("minutes", "30")
                .param("pagesFrom", "10")
                .param("pagesTo", "25")
                .param("note", "Testovací záznam")
                .param("markedFinished", "true")
                .with(SecurityMockMvcRequestPostProcessors.user("student@example.com").roles("STUDENT")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));

        assertThat(readingSessionRepository.findAll()).hasSize(1);
    }
}

