package com.readingtracker.controller;

import com.readingtracker.domain.*;
import com.readingtracker.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookRepository bookRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassMembershipRepository classMembershipRepository;
    private final ShopItemRepository shopItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final AppSettingRepository appSettingRepository;
    private final LedgerAdjustmentRepository ledgerAdjustmentRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final AuditEventRepository auditEventRepository;

    public HomeController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          BookRepository bookRepository,
                          ReadingSessionRepository readingSessionRepository,
                          SchoolClassRepository schoolClassRepository,
                          ClassMembershipRepository classMembershipRepository,
                          ShopItemRepository shopItemRepository,
                          PurchaseRepository purchaseRepository,
                          AppSettingRepository appSettingRepository,
                          LedgerAdjustmentRepository ledgerAdjustmentRepository,
                          QuizResponseRepository quizResponseRepository,
                          QuizQuestionRepository quizQuestionRepository,
                          AuditEventRepository auditEventRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookRepository = bookRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.classMembershipRepository = classMembershipRepository;
        this.shopItemRepository = shopItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.appSettingRepository = appSettingRepository;
        this.ledgerAdjustmentRepository = ledgerAdjustmentRepository;
        this.quizResponseRepository = quizResponseRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping("/")
    public String index() {
        if (currentUser() != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            model.addAttribute("registrationError", "Uživatel s tímto e-mailem již existuje.");
            return "register";
        }

        User user = new User();
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.STUDENT);
        userRepository.save(user);
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }

        populateLayout(model, user, "dashboard");
        model.addAttribute("booksCount", countUserBooks(user));
        model.addAttribute("recordsCount", countUserSessions(user));
        model.addAttribute("featuredTitle", "Vítejte v Čtenářském pasu");
        model.addAttribute("featuredText", "Sledujte čtení, sbírejte body a pracujte se svými knihami, záznamy a výhodami v jednom přehledu.");
        return "dashboard";
    }

    @GetMapping("/books")
    public String books(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<Book> books = getUserBooks(user);
        model.addAttribute("books", books);
        model.addAttribute("newBook", new BookForm());
        populateLayout(model, user, "books");
        return "books";
    }

    @PostMapping("/books/new")
    public String createBook(@ModelAttribute("newBook") BookForm form) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            return "redirect:/books";
        }

        Book book = new Book();
        book.setOwnerUserId(user.getId());
        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor() == null || form.getAuthor().isBlank() ? "Neznámý autor" : form.getAuthor());
        book.setTotalPages(form.getTotalPages() == null ? 1 : form.getTotalPages());
        book.setStatus(BookStatus.READING);
        book.setLastPage(0);
        bookRepository.save(book);

        return "redirect:/books";
    }

    @GetMapping("/new-session")
    public String newSession(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("books", getUserBooks(user));
        model.addAttribute("selectedDate", LocalDate.now().toString());
        model.addAttribute("sessionForm", new SessionForm());
        model.addAttribute("newBookForm", new BookForm());
        populateLayout(model, user, "new-session");
        return "new-session";
    }

    @PostMapping("/new-session")
    public String saveSession(@ModelAttribute("sessionForm") SessionForm form) {
        User user = currentUser();
        if (user == null) {
            log.warn("saveSession rejected: user not authenticated");
            return "redirect:/login";
        }

        Long bookId = form.getBookId();
        log.info("saveSession start: userId={}, bookId={}, date={}, minutes={}, pagesFrom={}, pagesTo={}, note={}",
            user.getId(), bookId, form.getDate(), form.getMinutes(), form.getPagesFrom(), form.getPagesTo(), form.getNote());

        if (bookId == null) {
            log.warn("saveSession rejected: missing bookId for userId={}", user.getId());
            return "redirect:/new-session";
        }

        Book selectedBook = bookRepository.findById(bookId).orElse(null);
        if (selectedBook == null || !selectedBook.getOwnerUserId().equals(user.getId())) {
            log.warn("saveSession rejected: invalid book {} for user {}", bookId, user.getId());
            return "redirect:/new-session";
        }

        int minutes = Optional.ofNullable(form.getMinutes()).orElse(0);
        int pagesFrom = Optional.ofNullable(form.getPagesFrom()).orElse(0);
        int pagesTo = Optional.ofNullable(form.getPagesTo()).orElse(0);

        if (minutes < 0) { minutes = 0; }
        if (pagesFrom < 0) { pagesFrom = 0; }
        if (pagesTo < 0) { pagesTo = 0; }

        LocalDate sessionDate = parseSessionDate(form.getDate());

        try {
            ReadingSession session = new ReadingSession();
            session.setBookId(selectedBook.getId());
            session.setSessionDate(Instant.from(sessionDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            session.setMinutes(minutes);
            session.setPagesFrom(pagesFrom);
            session.setPagesTo(pagesTo);
            session.setNote(form.getNote());
            session.setMarkedFinished(Boolean.TRUE.equals(form.getMarkedFinished()));
            session.setCreatedAt(Instant.now());
            session.setPointsAwarded(calculateSessionPoints(form));
            readingSessionRepository.save(session);
            log.info("saveSession persisted: sessionId={}, bookId={}, points={}", session.getId(), selectedBook.getId(), session.getPointsAwarded());

            selectedBook.setLastPage(pagesTo);
            if (Boolean.TRUE.equals(form.getMarkedFinished())) {
                selectedBook.setStatus(BookStatus.FINISHED);
            }
            bookRepository.save(selectedBook);
            log.info("saveSession updated book {} lastPage={} status={}", selectedBook.getId(), pagesTo, selectedBook.getStatus());

            return "redirect:/dashboard";
        } catch (Exception ex) {
            log.error("saveSession failed for userId={} bookId={} date={}", user.getId(), bookId, form.getDate(), ex);
            throw ex;
        }
    }

    @PostMapping("/books/quick-create")
    public String createBookFromSession(@ModelAttribute("newBookForm") BookForm form) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (form.getTitle() != null && !form.getTitle().isBlank()) {
            Book book = new Book();
            book.setOwnerUserId(user.getId());
            book.setTitle(form.getTitle());
            book.setAuthor(form.getAuthor() == null || form.getAuthor().isBlank() ? "Neznámý autor" : form.getAuthor());
            book.setTotalPages(form.getTotalPages() == null ? 1 : form.getTotalPages());
            book.setStatus(BookStatus.READING);
            book.setLastPage(0);
            bookRepository.save(book);
        }
        return "redirect:/new-session";
    }

    @GetMapping("/history")
    public String history(Model model, @RequestParam(required = false) Long studentId) {
        User currentUser = currentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Determine which user's history to show
        User targetUser = currentUser;
        if (studentId != null && currentUser.getRole() == Role.TEACHER) {
            targetUser = userRepository.findById(studentId).orElse(currentUser);
            log.info("history: teacher {} viewing student {} history", currentUser.getId(), targetUser.getId());
        } else if (studentId != null && currentUser.getRole() != Role.TEACHER) {
            // Non-teacher trying to view someone else's history
            targetUser = currentUser;
            log.warn("history: student {} attempted to view student {} history - denied", currentUser.getId(), studentId);
        }

        List<Object> historyEvents = buildHistoryEvents(targetUser, currentUser.getRole() == Role.TEACHER);
        model.addAttribute("historyEvents", historyEvents);
        model.addAttribute("targetUserId", targetUser.getId());
        model.addAttribute("isOwnHistory", targetUser.getId().equals(currentUser.getId()));
        model.addAttribute("recordsCount", historyEvents.size());
        populateLayout(model, currentUser, "history");
        return "history";
    }

    @GetMapping("/history/edit")
    public String historyEdit(Model model, @RequestParam String type, @RequestParam Long id, @RequestParam(required = false) Long studentId) {
        log.info("historyEdit disabled in MVP: type={}, id={}, studentId={}", type, id, studentId);
        return "redirect:/history";
    }

    @PostMapping("/history/update")
    public String historyUpdate(@RequestParam String type, @RequestParam Long id, @RequestParam(required = false) Long studentId,
                                  @RequestParam(required = false) Integer minutes,
                                  @RequestParam(required = false) Integer pagesFrom,
                                  @RequestParam(required = false) Integer pagesTo,
                                  @RequestParam(required = false) String note,
                                  @RequestParam(required = false) BigDecimal amount,
                                  @RequestParam(required = false) String reason) {
        log.info("historyUpdate disabled in MVP: type={}, id={}, studentId={}", type, id, studentId);
        String redirect = "/history";
        if (studentId != null) {
            redirect += "?studentId=" + studentId;
        }
        return "redirect:" + redirect;
    }

    @GetMapping("/shop")
    public String shop(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<ShopItem> items = shopItemRepository.findAll();
        model.addAttribute("shopItems", items);
        populateLayout(model, user, "shop");
        return "shop";
    }

    @PostMapping("/shop/buy/{id}")
    public String buyItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }

        ShopItem item = shopItemRepository.findById(id).orElse(null);
        if (item == null) {
            return "redirect:/shop";
        }

        if (calculateWalletBalance(user).compareTo(item.getPricePoints()) < 0) {
            redirectAttributes.addFlashAttribute("shopError", "Nedostatek bodů na nákup.");
            return "redirect:/shop";
        }

        Purchase purchase = new Purchase();
        purchase.setUserId(user.getId());
        purchase.setShopItemId(item.getId());
        purchase.setPricePoints(item.getPricePoints());
        purchase.setPurchasedAt(Instant.now());
        purchaseRepository.save(purchase);

        redirectAttributes.addFlashAttribute("shopSuccess", "Nákup úspěšně proveden.");
        return "redirect:/shop";
    }

    @GetMapping("/users")
    public String users(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() != Role.TEACHER) {
            return "redirect:/dashboard";
        }
        List<User> users = userRepository.findAll().stream()
            .sorted(Comparator.comparing(User::getEmail)).collect(Collectors.toList());
        List<SchoolClass> classes = schoolClassRepository.findAll().stream()
            .sorted(Comparator.comparing(SchoolClass::getName)).collect(Collectors.toList());
        model.addAttribute("users", users);
        model.addAttribute("classes", classes);
        populateLayout(model, user, "users");
        return "teacher-users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam Role role) {
        User current = currentUser();
        if (current == null || current.getRole() != Role.TEACHER) {
            return "redirect:/login";
        }
        User target = userRepository.findById(id).orElse(null);
        if (target != null) {
            target.setRole(role);
            userRepository.save(target);
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/class")
    public String assignClass(@PathVariable Long id, @RequestParam Long classId) {
        User current = currentUser();
        if (current == null || current.getRole() != Role.TEACHER) {
            return "redirect:/login";
        }

        User target = userRepository.findById(id).orElse(null);
        SchoolClass schoolClass = schoolClassRepository.findById(classId).orElse(null);
        if (target != null && schoolClass != null) {
            ClassMembershipId membershipId = new ClassMembershipId();
            membershipId.setUserId(target.getId());
            membershipId.setSchoolClassId(schoolClass.getId());
            ClassMembership membership = new ClassMembership();
            membership.setId(membershipId);
            membership.setAssignedAt(Instant.now());
            membership.setAssignedByUserId(current.getId());
            classMembershipRepository.save(membership);
        }
        return "redirect:/users";
    }

    @GetMapping("/teacher-shop")
    public String teacherShop(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() != Role.TEACHER) {
            return "redirect:/dashboard";
        }
        model.addAttribute("shopItems", shopItemRepository.findAll());
        populateLayout(model, user, "teacher-shop");
        return "teacher-shop";
    }

    @PostMapping("/teacher-shop/{id}/price")
    public String updateShopPrice(@PathVariable Long id, @RequestParam BigDecimal pricePoints) {
        User current = currentUser();
        if (current == null || current.getRole() != Role.TEACHER) {
            return "redirect:/login";
        }
        ShopItem item = shopItemRepository.findById(id).orElse(null);
        if (item != null) {
            item.setPricePoints(pricePoints);
            shopItemRepository.save(item);
        }
        return "redirect:/teacher-shop";
    }

    @GetMapping("/teacher-questions")
    public String teacherQuestions(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() != Role.TEACHER) {
            return "redirect:/dashboard";
        }
        model.addAttribute("questionCount", 3);
        model.addAttribute("rewardPoints", BigDecimal.valueOf(5));
        populateLayout(model, user, "teacher-questions");
        return "teacher-questions";
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private List<Book> getUserBooks(User user) {
        return bookRepository.findAll().stream()
            .filter(book -> book.getOwnerUserId().equals(user.getId()))
            .sorted(Comparator.comparing(Book::getTitle))
            .collect(Collectors.toList());
    }

    private long countUserBooks(User user) {
        return getUserBooks(user).size();
    }

    private List<ReadingSession> getUserSessions(User user) {
        List<Long> userBookIds = getUserBooks(user).stream().map(Book::getId).collect(Collectors.toList());
        List<ReadingSession> all = readingSessionRepository.findAll();
        if (user.getRole() == Role.TEACHER) {
            return all.stream()
                .sorted(Comparator.comparing(ReadingSession::getCreatedAt).reversed())
                .collect(Collectors.toList());
        }
        return all.stream()
            .filter(session -> userBookIds.contains(session.getBookId()))
            .sorted(Comparator.comparing(ReadingSession::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    private long countUserSessions(User user) {
        return getUserSessions(user).size();
    }

    private BigDecimal calculateSessionPoints(SessionForm form) {
        int pagesFrom = Math.max(0, Optional.ofNullable(form.getPagesFrom()).orElse(0));
        int pagesTo = Math.max(0, Optional.ofNullable(form.getPagesTo()).orElse(0));
        int pagesRead = Math.max(0, pagesTo - pagesFrom);
        BigDecimal minutes = BigDecimal.valueOf(Math.max(0, Optional.ofNullable(form.getMinutes()).orElse(0)));
        BigDecimal pointsPerMinute = getAppSettingDecimal("points_per_minute", "0.1");
        BigDecimal pointsPerPage = getAppSettingDecimal("points_per_page", "0");
        BigDecimal pagesValue = BigDecimal.valueOf(pagesRead).multiply(pointsPerPage);
        return minutes.multiply(pointsPerMinute).add(pagesValue).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWalletBalance(User user) {
        List<Long> userBookIds = getUserBooks(user).stream().map(Book::getId).collect(Collectors.toList());
        BigDecimal earned = readingSessionRepository.findAll().stream()
            .filter(session -> userBookIds.contains(session.getBookId()))
            .map(ReadingSession::getPointsAwarded)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal spent = purchaseRepository.findAll().stream()
            .filter(purchase -> purchase.getUserId().equals(user.getId()))
            .map(Purchase::getPricePoints)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return earned.subtract(spent).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getAppSettingDecimal(String key, String defaultValue) {
        log.debug("Fetching app setting '{}', default '{}'", key, defaultValue);
        return appSettingRepository.findById(key)
            .map(AppSetting::getValue)
            .map(value -> {
                try {
                    return new BigDecimal(value);
                } catch (NumberFormatException ex) {
                    log.warn("Invalid numeric value for app setting '{}': '{}'. Using default {}.", key, value, defaultValue);
                    return new BigDecimal(defaultValue);
                }
            })
            .orElseGet(() -> new BigDecimal(defaultValue));
    }

    private String formatBalance(BigDecimal balance) {
        return balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void populateLayout(Model model, User user, String selectedNav) {
        String firstChar = user.getEmail() == null || user.getEmail().isBlank() ? "U" : user.getEmail().substring(0, 1).toUpperCase();
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("avatarText", firstChar);
        model.addAttribute("selectedNav", selectedNav);
        model.addAttribute("isTeacher", user.getRole() == Role.TEACHER);
        model.addAttribute("walletBalance", formatBalance(calculateWalletBalance(user)));
        model.addAttribute("booksCount", countUserBooks(user));
        model.addAttribute("recordsCount", countUserSessions(user));
    }

    private List<Object> buildHistoryEvents(User targetUser, boolean isTeacher) {
        List<Object> events = new ArrayList<>();

        // Sessions
        List<ReadingSession> sessions = readingSessionRepository.findAll().stream()
            .filter(s -> {
                Long bookOwnerId = bookRepository.findById(s.getBookId()).map(Book::getOwnerUserId).orElse(null);
                return bookOwnerId != null && bookOwnerId.equals(targetUser.getId());
            })
            .sorted(Comparator.comparing(ReadingSession::getCreatedAt).reversed())
            .collect(Collectors.toList());

        for (ReadingSession session : sessions) {
            Book book = bookRepository.findById(session.getBookId()).orElse(null);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "SESSION");
            event.put("id", session.getId());
            event.put("date", session.getSessionDate());
            event.put("description", book != null ? "Čtení: " + book.getTitle() : "Čtení (kniha smazána)");
            event.put("points", session.getPointsAwarded());
            event.put("details", String.format("Minuty: %d, Strany: %d–%d", session.getMinutes(), session.getPagesFrom(), session.getPagesTo()));
            event.put("note", session.getNote());
            event.put("editable", false);
            events.add(event);
        }

        // Quiz responses (linked to sessions)
        for (ReadingSession session : sessions) {
            List<QuizResponse> responses = quizResponseRepository.findByReadingSessionId(session.getId());
            for (QuizResponse response : responses) {
                QuizQuestion question = quizQuestionRepository.findById(response.getQuizQuestionId()).orElse(null);
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("type", "QUIZ");
                event.put("id", response.getId());
                event.put("date", session.getSessionDate()); // Use session date
                event.put("description", "Kvízová odpověď");
                event.put("points", response.getPointsAwarded());
                event.put("details", question != null ? question.getText() : "Otázka: N/A");
                event.put("answer", response.getAnswerText());
                event.put("editable", false);
                events.add(event);
            }
        }

        // Purchases
        List<Purchase> purchases = purchaseRepository.findAll().stream()
            .filter(p -> p.getUserId().equals(targetUser.getId()))
            .sorted(Comparator.comparing(Purchase::getPurchasedAt).reversed())
            .collect(Collectors.toList());

        for (Purchase purchase : purchases) {
            ShopItem item = shopItemRepository.findById(purchase.getShopItemId()).orElse(null);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "PURCHASE");
            event.put("id", purchase.getId());
            event.put("date", purchase.getPurchasedAt());
            event.put("description", item != null ? "Nákup: " + item.getName() : "Nákup (položka smazána)");
            event.put("points", purchase.getPricePoints().negate()); // Negative because it's spent
            event.put("details", item != null ? item.getDescription() : "");
            event.put("editable", false);
            events.add(event);
        }

        // Ledger adjustments
        List<LedgerAdjustment> adjustments = ledgerAdjustmentRepository.findByUserId(targetUser.getId());
        adjustments.forEach(adj -> {
            User createdBy = userRepository.findById(adj.getCreatedByUserId()).orElse(null);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "ADJUSTMENT");
            event.put("id", adj.getId());
            event.put("date", adj.getCreatedAt());
            event.put("description", "Úprava bodů");
            event.put("points", adj.getAmount());
            event.put("details", adj.getReason() + (createdBy != null ? " (od " + createdBy.getEmail() + ")" : ""));
            event.put("editable", false);
            events.add(event);
        });

        // Sort all events by date descending
        events.sort((a, b) -> {
            Instant dateA = (Instant) ((Map<String, Object>) a).get("date");
            Instant dateB = (Instant) ((Map<String, Object>) b).get("date");
            return dateB.compareTo(dateA);
        });

        return events;
    }

    @Getter
    @Setter
    public static class LoginForm {
        @NotBlank(message = "E-mail je povinný")
        @Email(message = "Zadejte platný e-mail")
        private String email;

        @NotBlank(message = "Heslo je povinné")
        private String password;
    }

    @Getter
    @Setter
    public static class RegisterForm {
        @NotBlank(message = "E-mail je povinný")
        @Email(message = "Zadejte platný e-mail")
        private String email;

        @NotBlank(message = "Heslo je povinné")
        private String password;
    }

    @Getter
    @Setter
    public static class BookForm {
        private String title;
        private String author;
        private Integer totalPages;
    }

    @Getter
    @Setter
    public static class SessionForm {
        private Long bookId;
        private String date;
        private Integer minutes;
        private Integer pagesFrom;
        private Integer pagesTo;
        private String note;
        private Boolean markedFinished;
    }

    private LocalDate parseSessionDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }
}
