package com.readingtracker;

import com.readingtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loginWithDemoTeacherAccountRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "teacher@readingtracker.local")
                .param("password", "Teacher123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void teacherDashboardPageIsAccessible() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("teacher@readingtracker.local").roles("TEACHER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Dashboard")));
    }

    @Test
    void registrationCreatesStudentUser() throws Exception {
        String uniqueEmail = "student" + System.currentTimeMillis() + "@example.com";

        mockMvc.perform(post("/register")
                .param("email", uniqueEmail)
                .param("password", "StrongPass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));

        assertThat(userRepository.findByEmail(uniqueEmail)).isPresent();
    }
}

