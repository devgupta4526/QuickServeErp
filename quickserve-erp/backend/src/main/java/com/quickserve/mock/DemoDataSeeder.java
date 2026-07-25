package com.quickserve.mock;

import com.quickserve.modules.auth.entity.Business;
import com.quickserve.modules.auth.entity.Role;
import com.quickserve.modules.auth.entity.User;
import com.quickserve.modules.auth.repository.BusinessRepository;
import com.quickserve.modules.auth.repository.RoleRepository;
import com.quickserve.modules.auth.repository.UserRepository;
import com.quickserve.modules.menu.entity.Category;
import com.quickserve.modules.menu.entity.MenuItem;
import com.quickserve.modules.menu.entity.TaxSlab;
import com.quickserve.modules.menu.repository.CategoryRepository;
import com.quickserve.modules.menu.repository.MenuItemRepository;
import com.quickserve.modules.menu.repository.TaxSlabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Automatically seeds demo data when running with mock profile.
 * Creates a demo business, owner user, menu categories and items
 * so you can immediately log in and test without any setup.
 *
 * Demo credentials:
 *   Mobile: 9999999999
 *   Password: Demo@1234
 *   OTP bypass code: 123456
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "quickserve.mock.seed-demo-data", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final BusinessRepository  businessRepository;
    private final UserRepository      userRepository;
    private final RoleRepository      roleRepository;
    private final CategoryRepository  categoryRepository;
    private final MenuItemRepository  menuItemRepository;
    private final TaxSlabRepository   taxSlabRepository;
    private final PasswordEncoder     passwordEncoder;

    private static final String DEMO_MOBILE   = "9999999999";
    private static final String DEMO_PASSWORD = "Demo@1234";
    private static final UUID   DEMO_BIZ_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID   DEMO_USER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByPhone(DEMO_MOBILE)) {
            log.info("[MOCK] Demo data already seeded — skipping");
            printLoginBanner();
            return;
        }

        log.info("[MOCK] Seeding demo data...");

        // 1. Business
        Business biz = Business.builder()
                .name("Demo Restaurant")
                .businessType(Business.BusinessType.RESTAURANT)
                .status(Business.BusinessStatus.ACTIVE)
                .trialEndsAt(Instant.now().plus(365, ChronoUnit.DAYS))
                .currencyCode("INR")
                .timezone("Asia/Kolkata")
                .gstInclusive(false)
                .onboardingStep(5)
                .build();
        // force a known ID so seed is idempotent
        biz = businessRepository.save(biz);
        final UUID bizId = biz.getId();

        // 2. Owner role
        Role ownerRole = roleRepository.findByNameAndSystemTrue("BUSINESS_OWNER")
                .orElseGet(() -> {
                    Role r = Role.builder().name("BUSINESS_OWNER")
                            .permissions(new String[]{"manage:all"}).system(true).build();
                    return roleRepository.save(r);
                });

        // 3. Demo user
        User owner = User.builder()
                .businessId(bizId)
                .roleId(ownerRole.getId())
                .name("Demo Owner")
                .phone(DEMO_MOBILE)
                .email("demo@quickserve.in")
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .active(true)
                .mobileVerified(true)
                .emailVerified(true)
                .build();
        userRepository.save(owner);

        // 4. Cashier role + cashier user (for POS testing)
        Role cashierRole = roleRepository.findByNameAndSystemTrue("CASHIER")
                .orElseGet(() -> {
                    Role r = Role.builder().name("CASHIER")
                            .permissions(new String[]{"create:orders","process:payments"}).system(true).build();
                    return roleRepository.save(r);
                });
        User cashier = User.builder()
                .businessId(bizId)
                .roleId(cashierRole.getId())
                .name("Demo Cashier")
                .phone("8888888888")
                .email("cashier@quickserve.in")
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .active(true)
                .mobileVerified(true)
                .build();
        userRepository.save(cashier);

        // 5. Kitchen staff user (for KDS testing)
        Role kitchenRole = roleRepository.findByNameAndSystemTrue("KITCHEN_STAFF")
                .orElseGet(() -> {
                    Role r = Role.builder().name("KITCHEN_STAFF")
                            .permissions(new String[]{"view:kds","update:kds"}).system(true).build();
                    return roleRepository.save(r);
                });
        User kitchen = User.builder()
                .businessId(bizId)
                .roleId(kitchenRole.getId())
                .name("Demo Kitchen")
                .phone("7777777777")
                .email("kitchen@quickserve.in")
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .active(true)
                .mobileVerified(true)
                .build();
        userRepository.save(kitchen);

        // 6. Tax slabs
        List<BigDecimal> pcts = List.of(BigDecimal.ZERO, new BigDecimal("5"),
                new BigDecimal("12"), new BigDecimal("18"), new BigDecimal("28"));
        for (BigDecimal pct : pcts) {
            TaxSlab slab = TaxSlab.builder().name("GST " + pct + "%").percentage(pct).active(true).build();
            slab.setBusinessId(bizId);
            taxSlabRepository.save(slab);
        }
        TaxSlab tax5 = taxSlabRepository.findByBusinessIdAndActiveTrue(bizId).stream()
                .filter(t -> t.getPercentage().compareTo(new BigDecimal("5")) == 0)
                .findFirst().orElse(null);
        TaxSlab tax18 = taxSlabRepository.findByBusinessIdAndActiveTrue(bizId).stream()
                .filter(t -> t.getPercentage().compareTo(new BigDecimal("18")) == 0)
                .findFirst().orElse(null);

        // 7. Menu categories + items
        String[][] catData = {
                {"Starters", "1"},
                {"Main Course", "2"},
                {"Breads", "3"},
                {"Beverages", "4"},
                {"Desserts", "5"},
        };
        for (String[] cd : catData) {
            Category cat = Category.builder().name(cd[0]).sortOrder(Integer.parseInt(cd[1])).active(true).build();
            cat.setBusinessId(bizId);
            categoryRepository.save(cat);
        }

        // Fetch saved categories
        List<Category> cats = categoryRepository.findByBusinessIdOrderBySortOrderAsc(bizId);
        UUID catStarters = cats.stream().filter(c -> c.getName().equals("Starters")).findFirst().map(Category::getId).orElse(null);
        UUID catMain     = cats.stream().filter(c -> c.getName().equals("Main Course")).findFirst().map(Category::getId).orElse(null);
        UUID catBreads   = cats.stream().filter(c -> c.getName().equals("Breads")).findFirst().map(Category::getId).orElse(null);
        UUID catBev      = cats.stream().filter(c -> c.getName().equals("Beverages")).findFirst().map(Category::getId).orElse(null);
        UUID catDesserts = cats.stream().filter(c -> c.getName().equals("Desserts")).findFirst().map(Category::getId).orElse(null);
        UUID tax5Id  = tax5  != null ? tax5.getId()  : null;
        UUID tax18Id = tax18 != null ? tax18.getId() : null;

        Object[][] items = {
                // {category, name, price, taxSlabId, isVeg, description}
                {catStarters, "Paneer Tikka",          280, tax5Id,  true,  "Grilled cottage cheese with spices"},
                {catStarters, "Veg Spring Rolls",      180, tax5Id,  true,  "Crispy rolls with mixed vegetables"},
                {catStarters, "Chicken 65",            320, tax5Id,  false, "Spicy South Indian fried chicken"},
                {catMain,     "Paneer Butter Masala",  320, tax5Id,  true,  "Rich creamy tomato gravy with paneer"},
                {catMain,     "Dal Tadka",             180, tax5Id,  true,  "Yellow lentils tempered with ghee"},
                {catMain,     "Chicken Biryani",       380, tax5Id,  false, "Fragrant basmati rice with chicken"},
                {catMain,     "Kadai Paneer",          300, tax5Id,  true,  "Paneer in spicy kadai gravy"},
                {catBreads,   "Butter Naan",            50, tax5Id,  true,  "Soft leavened bread with butter"},
                {catBreads,   "Paratha",                45, tax5Id,  true,  "Flaky whole wheat flatbread"},
                {catBev,      "Fresh Lime Soda",        80, tax18Id, true,  "Refreshing lime with soda"},
                {catBev,      "Mango Lassi",           120, tax18Id, true,  "Sweet yogurt drink with mango"},
                {catBev,      "Masala Chai",            40, tax18Id, true,  "Spiced Indian tea"},
                {catBev,      "Cold Coffee",           150, tax18Id, true,  "Chilled coffee with milk"},
                {catDesserts, "Gulab Jamun",            80, tax5Id,  true,  "Soft milk dumplings in sugar syrup"},
                {catDesserts, "Ice Cream (Vanilla)",    90, tax18Id, true,  "Creamy vanilla ice cream"},
        };

        for (Object[] row : items) {
            MenuItem item = MenuItem.builder()
                    .categoryId((UUID) row[0])
                    .name((String) row[1])
                    .basePrice(new BigDecimal(row[2].toString()))
                    .taxSlabId((UUID) row[3])
                    .veg((Boolean) row[4])
                    .description((String) row[5])
                    .available(true)
                    .archived(false)
                    .build();
            item.setBusinessId(bizId);
            menuItemRepository.save(item);
        }

        log.info("[MOCK] Demo data seeded successfully with {} menu items", items.length);
        printLoginBanner();
    }

    private void printLoginBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          QuickServe ERP — MOCK MODE ACTIVE                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Demo credentials:                                           ║");
        System.out.println("║    Owner   → mobile: 9999999999  pw: Demo@1234               ║");
        System.out.println("║    Cashier → mobile: 8888888888  pw: Demo@1234               ║");
        System.out.println("║    Kitchen → mobile: 7777777777  pw: Demo@1234               ║");
        System.out.println("║                                                              ║");
        System.out.println("║  OTP bypass code: 123456 (works for any mobile)              ║");
        System.out.println("║                                                              ║");
        System.out.println("║  API Swagger: http://localhost:8080/swagger-ui.html          ║");
        System.out.println("║  Frontend:    http://localhost:5173                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
