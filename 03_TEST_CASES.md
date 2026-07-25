# QuickServe ERP — Complete Test Case Specification
## Every module, every edge case, every boundary

---

## Testing Strategy

### Layers
1. Unit Tests — service layer, utility classes, calculators (JUnit 5 + Mockito)
2. Integration Tests — controller + DB (SpringBootTest + Testcontainers PostgreSQL)
3. Kafka Tests — event publishing and consuming (@EmbeddedKafka)
4. WhatsApp Tests — mock Meta API responses (MockWebServer / WireMock)
5. E2E Flow Tests — full user journeys through multiple endpoints

### Test Infrastructure Setup

```java
// Base class for integration tests
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("quickserve_test")
        .withUsername("test").withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    // Helper: create authenticated test business + user
    protected TestContext createTestBusiness() { ... }
    // Helper: get JWT token for test user
    protected String getAuthToken(String email, String password) { ... }
    // Helper: create test outlet
    protected Outlet createTestOutlet(UUID businessId) { ... }
}
```

---

## MODULE 1: Auth & Registration Tests

### Unit Tests: AuthService

```java
class AuthServiceTest {

    // Registration
    @Test void register_success_createsBusinessAndOwner()
    @Test void register_duplicateMobile_throwsBusinessException()
    @Test void register_duplicateEmail_throwsBusinessException()
    @Test void register_invalidMobileFormat_throwsValidationException()
    @Test void register_passwordTooShort_throwsValidationException()
    @Test void register_passwordNoUppercase_throwsValidationException()
    @Test void register_passwordNoNumber_throwsValidationException()
    @Test void register_createsTrialSubscription_with14DayExpiry()
    @Test void register_sendsOtpToMobile()
    @Test void register_sendsWhatsAppWelcome()

    // OTP Verification
    @Test void verifyOtp_correctOtp_issuesJwt()
    @Test void verifyOtp_wrongOtp_throwsException()
    @Test void verifyOtp_expiredOtp_throwsException()
    @Test void verifyOtp_otpUsedTwice_throwsException()

    // Resend OTP
    @Test void resendOtp_firstResend_succeeds()
    @Test void resendOtp_thirdResend_succeeds()
    @Test void resendOtp_fourthResend_throwsRateLimitException()

    // Login
    @Test void login_validCredentials_returnsJwtCookie()
    @Test void login_wrongPassword_throwsException()
    @Test void login_inactiveUser_throwsException()
    @Test void login_unverifiedMobile_throwsException()
    @Test void login_updatesLastLoginTimestamp()

    // Multi-tenancy
    @Test void tenantContext_requestFromBusinessA_cannotAccessBusinessBData()
    @Test void tenantContext_missingBusinessId_throwsTenantAccessException()
}
```

---

## MODULE 2: Onboarding Tests

```java
class OnboardingServiceTest {

    @Test void businessProfile_validGstin_accepted()
    @Test void businessProfile_invalidGstinFormat_rejected()
    // GSTIN format: 2-digit state code + 10-char PAN + 1-digit entity + Z + checksum
    @Test void businessProfile_gstinWrongLength_rejected()
    @Test void businessProfile_invalidPanFormat_rejected()

    @Test void outletSetup_tableCount12_creates12TableRecords()
    @Test void outletSetup_tableCount0_createsNoTables()
    @Test void outletSetup_tableCount50_creates50TablesAndQRCodes()
    @Test void outletSetup_restaurantType_seedsRestaurantCategories()
    @Test void outletSetup_retailType_seedsRetailCategories()

    @Test void menuBulkImport_validExcel_importsAllRows()
    @Test void menuBulkImport_excelWithErrors_importsValidSkipsErrors()
    @Test void menuBulkImport_emptyFile_returns400()
    @Test void menuBulkImport_pdfFile_returns400()
    @Test void menuBulkImport_duplicateItemName_skipsSecondOccurrence()
    @Test void menuBulkImport_priceZero_rejectsRow()
    @Test void menuBulkImport_negativPrice_rejectsRow()

    @Test void complete_allStepsCompleted_activatesBusiness()
    @Test void complete_step2Missing_throwsException()
    @Test void complete_calledTwice_idempotent()  // should not double-activate

    @Test void progress_freshBusiness_returns0percent()
    @Test void progress_step2Done_returns40percent()
    @Test void progress_allDone_returns100percent()
}
```

---

## MODULE 3: Order & POS Tests

```java
class OrderServiceTest {

    // Create Order
    @Test void createOrder_validItems_calculatesSubtotalCorrectly()
    @Test void createOrder_withGst18percent_calculatesGstCorrectly()
    // e.g., item ₹100 + 18% GST = ₹118 total, tax = ₹18
    @Test void createOrder_gstInclusive_doesNotDoubleAddGst()
    // e.g., item marked ₹118 inclusive = ₹100 base + ₹18 tax (not ₹118 + ₹18)
    @Test void createOrder_withVariant_addsVariantPriceModifier()
    @Test void createOrder_withAddon_addsAddonPrice()
    @Test void createOrder_unavailableItem_throwsException()
    @Test void createOrder_invalidQuantity_throwsException()  // quantity <= 0
    @Test void createOrder_emptyCart_throwsException()

    // QR Self Order
    @Test void qrOrder_validTableId_linksToTable()
    @Test void qrOrder_occupiedTable_stillAllows()  // can add to existing table order
    @Test void qrOrder_invalidOutletId_returns404()

    // Payment
    @Test void payment_cashExactAmount_marksOrderPaid()
    @Test void payment_cashOverpayment_recordsChange()
    @Test void payment_upi_marksAsPendingUntilWebhookConfirm()
    @Test void payment_razorpay_verifySignature_marksAsPaid()
    @Test void payment_razorpay_wrongSignature_rejects()
    @Test void payment_partialPayment_orderRemainsPartiallyPaid()
    @Test void payment_cancelledOrder_throwsException()

    // Order Status
    @Test void cancelOrder_placedStatus_succeeds()
    @Test void cancelOrder_preparingStatus_requiresManagerRole()
    @Test void cancelOrder_deliveredStatus_throwsException()  // cannot cancel delivered
    @Test void cancelOrder_refundTriggeredIfPaidByCard()

    // Split Bill
    @Test void splitBill_equalSplit4Ways_eachGetsQuarter()
    @Test void splitBill_customSplit_validatesTotal()
    @Test void splitBill_singlePerson_throwsException()

    // Discount
    @Test void applyDiscount_flatDiscount_reducesTotalCorrectly()
    @Test void applyDiscount_percentageDiscount_calculatesCorrectly()
    @Test void applyDiscount_discountExceedsTotal_throwsException()
    @Test void applyDiscount_requiresManagerRoleAbove10percent()

    // GST Calculation Tests (critical for Indian compliance)
    @Test void gstCalc_cgstAndSgst_splitEqually()
    // CGST = SGST = half of total GST (for intra-state)
    @Test void gstCalc_multipleSlabs_calculatedPerItem()
    // Order with 5% item + 18% item = separate GST per item, not blended rate
    @Test void gstCalc_zeroRatedItem_noGst()
    @Test void invoiceNumber_format_alphanumericWithYear()
    // Format: QS/2024-25/000001 (financial year aware)
    @Test void invoiceNumber_crossFinancialYear_resetsSequence()
    // April 1 resets counter: QS/2025-26/000001
}
```

---

## MODULE 4: KDS Tests

```java
class KdsServiceTest {

    @Test void kdsOrders_showsOnlyPlacedOrPreparingOrders()
    @Test void kdsOrders_doesNotShowCancelledOrDelivered()
    @Test void kdsOrders_sortedByOldestFirst()  // FIFO for kitchen
    @Test void updateItemStatus_pending_to_preparing_succeeds()
    @Test void updateItemStatus_preparing_to_done_succeeds()
    @Test void updateItemStatus_done_to_preparing_succeeds()  // can reverse
    @Test void updateItemStatus_onlyKitchenStaffRole_allowed()
    @Test void websocket_newOrder_broadcastsToAllKdsClients()
    @Test void websocket_statusUpdate_broadcastsChange()
    @Test void websocket_clientReconnect_receivesCurrentState()
}
```

---

## MODULE 5: Inventory Tests

```java
class InventoryServiceTest {

    // Stock Adjustment
    @Test void adjustStock_increase_updatesBalance()
    @Test void adjustStock_decrease_updatesBalance()
    @Test void adjustStock_decreaseBelowZero_throwsException()
    @Test void adjustStock_createsAuditTrailEntry()
    @Test void adjustStock_triggersLowStockEventIfBelowReorderPoint()
    @Test void adjustStock_doesNotTriggerLowStockIfAboveReorderPoint()

    // Stock Transfer between outlets
    @Test void transferStock_fromOutletA_toOutletB_deductsAndAdds()
    @Test void transferStock_insufficientStock_throwsException()
    @Test void transferStock_sameOutlet_throwsException()

    // Purchase Order
    @Test void createPO_validSupplier_createsDraftPO()
    @Test void createPO_invalidSupplierId_throws404()
    @Test void receivePO_partialReceive_updatesReceivedQty()
    @Test void receivePO_fullReceive_changeStatusToReceived()
    @Test void receivePO_receiveMoreThanOrdered_throwsException()
    @Test void receivePO_generatesGRN()
    @Test void receivePO_updatesInventoryStock()
    @Test void receivePO_triggersAccountsPayableEntry()  // Finance module

    // Recipe / BOM deduction
    @Test void recipeDeduction_orderPlaced_deductsIngredients()
    @Test void recipeDeduction_ingredientOutOfStock_allowsOrderButLogWarning()
    // We don't block orders — just warn. Stock can go negative (reality of kitchens).
    @Test void recipeDeduction_noRecipeMapped_skipsDeduction()

    // Low Stock Alert (deduplication)
    @Test void lowStockAlert_firstAlert_sendsWhatsApp()
    @Test void lowStockAlert_secondAlertWithin6Hours_doesNotResend()
    @Test void lowStockAlert_after6Hours_sendsAgain()

    // Valuation
    @Test void stockValuation_fifo_correctUnitCostAfterMultiplePurchases()
    @Test void stockValuation_zeroStock_returnsZeroValue()
}
```

---

## MODULE 6: Finance Tests

```java
class FinanceServiceTest {

    // Journal Entries
    @Test void journalEntry_debitEqualsCredit_balances()
    @Test void journalEntry_debitNotEqualCredit_throwsException()
    @Test void onPaymentProcessed_createsCorrectJournalEntry()
    // DR Cash, CR Sales Revenue, CR CGST Payable, CR SGST Payable
    @Test void onPaymentProcessed_cashPayment_debitsCash()
    @Test void onPaymentProcessed_upiPayment_debitsUpiClearing()
    @Test void onPurchaseReceived_creditsAccountsPayable()

    // GST Calculation Tests
    @Test void gstr1_correctlyAggregatesB2BSales()
    @Test void gstr1_separatesB2BAndB2CSales()
    @Test void gstr3b_correctlySummarizesGst()
    @Test void gstr1Export_generatesValidJson()
    @Test void gstr1Export_generatesValidExcel()

    // Invoice
    @Test void invoice_autoGeneratesSequentialNumber()
    @Test void invoice_pdfContainsGstin()
    @Test void invoice_pdfContainsIrnIfEInvoiceEnabled()
    @Test void invoice_markAsPaid_updatesStatus()
    @Test void invoice_sendViaWhatsApp_attachesPdf()

    // Trial Balance
    @Test void trialBalance_debitsSumEqualsCreditsSum()
    @Test void trialBalance_emptyPeriod_returnsZeroBalances()

    // P&L
    @Test void profitLoss_revenueMinusCogs_equalsGrossProfit()
    @Test void profitLoss_grossProfitMinusOpex_equalsNetProfit()

    // e-Invoice
    @Test void eInvoice_validInvoice_generatesIrn()
    @Test void eInvoice_irnAlreadyGenerated_returnsExisting()  // idempotent
    @Test void eInvoice_gstinMissing_throwsException()

    // Bank Reconciliation
    @Test void reconcile_matchingTransactions_marksReconciled()
    @Test void reconcile_unmatchedTransaction_flagsForReview()
}
```

---

## MODULE 7: HR & Payroll Tests

```java
class HrPayrollServiceTest {

    // Attendance
    @Test void checkIn_firstCheckInOfDay_succeeds()
    @Test void checkIn_alreadyCheckedIn_throwsException()
    @Test void checkOut_afterCheckIn_recordsHours()
    @Test void checkOut_withoutCheckIn_throwsException()
    @Test void checkOut_after8hours_recordsNoOvertime()
    @Test void checkOut_after9hours_records1hourOvertime()

    // Leave
    @Test void applyLeave_sufficientBalance_succeeds()
    @Test void applyLeave_insufficientBalance_throwsException()
    @Test void applyLeave_pastDate_throwsException()
    @Test void applyLeave_overlappingDates_throwsException()
    @Test void approveLeave_managerRole_succeeds()
    @Test void approveLeave_cashierRole_throwsForbidden()

    // Payroll
    @Test void processPayroll_monthlyEmployee_calculatesCorrectBasic()
    @Test void processPayroll_dailyEmployee_multipliesDailyRateByPresentDays()
    @Test void processPayroll_withPf_deductsPfCorrectly()
    // PF = 12% of basic (employee contribution), employer also contributes 12%
    @Test void processPayroll_basicOver21000_deductsEsi()
    // ESI = 0.75% of gross (employee), only if gross <= ₹21,000/month
    @Test void processPayroll_basicAbove21000_skipsEsi()
    @Test void processPayroll_calculatesTds()
    // TDS on salary: based on income tax slab (simplified)
    @Test void processPayroll_calledTwice_idempotent()
    // Running payroll for same month twice should not create duplicate slips
    @Test void payslipPdf_containsAllComponents()
    @Test void payslipPdf_sentViaWhatsApp()
    @Test void processPayroll_noAttendanceData_assumesAbsent()

    // Shift Assignment
    @Test void assignShift_validEmployee_succeeds()
    @Test void assignShift_conflictingShift_throwsException()
    @Test void assignShift_pastDate_throwsException()
}
```

---

## MODULE 8: CRM & Loyalty Tests

```java
class CrmLoyaltyServiceTest {

    // Customer Search (POS use case)
    @Test void searchByPhone_exactMatch_returnsCustomer()
    @Test void searchByPhone_partial5Digits_returnsMatchingCustomers()
    @Test void searchByPhone_noMatch_returnsEmptyList()
    @Test void searchByPhone_nonExistentPhone_returnsEmpty()

    // Customer Create / Update
    @Test void createCustomer_duplicatePhone_throwsException()
    @Test void createCustomer_noEmail_succeeds()  // email is optional
    @Test void updateCustomer_validData_updates()

    // Loyalty Points
    @Test void earnPoints_onOrderDelivered_calculatesPointsCorrectly()
    // e.g., 1 point per ₹10 spent → ₹500 order = 50 points
    @Test void earnPoints_minimumOrder_noPointsBelowThreshold()
    @Test void redeemPoints_sufficientPoints_deductsAndAppliesDiscount()
    @Test void redeemPoints_insufficientPoints_throwsException()
    @Test void redeemPoints_morePointsThanBalance_throwsException()
    @Test void tierUpgrade_pointsCrossGoldThreshold_upgradestoGold()
    @Test void tierUpgrade_sendsWhatsAppNotification()

    // Campaign Audience Segmentation
    @Test void segment_inactive30d_returnsCustomersInactive30OrMoreDays()
    @Test void segment_inactive30d_excludesOptedOutCustomers()
    @Test void segment_birthdayThisWeek_returnsCorrectCustomers()
    @Test void segment_goldTier_returnsOnlyGoldCustomers()
    @Test void segment_all_returnsAllActiveNonOptedOutCustomers()
}
```

---

## MODULE 9: Analytics Tests

```java
class AnalyticsServiceTest {

    @Test void dashboard_noOrders_returnsZeroMetrics()
    @Test void dashboard_singleOrder_returnsCorrectRevenue()
    @Test void dashboard_multiplePaymentMethods_breaksDownCorrectly()
    @Test void topItems_returnsTop5ByQuantity()
    @Test void topItems_tieBreak_sortsByRevenue()
    @Test void salesReport_dailyGrouping_returnsOneDayPerRow()
    @Test void salesReport_dateRangeFilter_excludesOutsideDates()
    @Test void salesReport_tenantIsolation_businessACannotSeeBusinessBData()
    @Test void plSummary_revenue_minusCogs_minusOpex_equalsNetProfit()
    @Test void export_excelFormat_validXlsx()
    @Test void export_pdfFormat_validPdf()

    // Performance tests (response time)
    @Test void dashboard_with1000Orders_respondsUnder500ms()
    // Use @Sql to seed 1000 orders before test
}
```

---

## MODULE 10: WhatsApp Tests

```java
class WhatsAppServiceTest {

    @Test void sendTemplateMessage_success_logsWithStatusSent()
    @Test void sendTemplateMessage_metaApi4xx_marksAsFailed_doesNotRetry()
    @Test void sendTemplateMessage_metaApi5xx_publishesToRetryTopic()
    @Test void sendTemplateMessage_timeout_publishesToRetryTopic()
    @Test void sendTemplateMessage_configNotFound_returnsFalse_noException()
    @Test void sendTemplateMessage_configInactive_returnsFalse_noException()
    @Test void sendTemplateMessage_optedOutCustomer_returnsFalse()
    @Test void sendDocument_validPdfUrl_sendsDocumentTemplate()

    @Test void webhook_getVerification_returnsChallenge()
    @Test void webhook_wrongVerifyToken_returns403()
    @Test void webhook_deliveredStatus_updatesMessageLog()
    @Test void webhook_readStatus_updatesMessageLog()
    @Test void webhook_failedStatus_updatesMessageLog()

    @Test void incomingMessage_stop_setsOptOut()
    @Test void incomingMessage_start_clearsOptOut()
    @Test void incomingMessage_orderStatus_repliesWithStatus()
    @Test void incomingMessage_menu_repliesWithMenuLink()

    @Test void retryConsumer_firstRetry_retries()
    @Test void retryConsumer_thirdRetry_succeeds_stopsRetrying()
    @Test void retryConsumer_thirdRetry_fails_marksPermanentlyFailed()

    @Test void dailySummary_noOrdersYesterday_doesNotSend()
    @Test void dailySummary_hasOrders_sendsToAllActiveBusinesses()
    @Test void dailySummary_whatsappDisabled_skips()

    @Test void lowStockAlert_firstAlert_sends()
    @Test void lowStockAlert_within6Hours_doesNotSend()
    @Test void lowStockAlert_after6Hours_sends()

    // Token encryption
    @Test void encryptToken_canBeDecrypted()
    @Test void encryptToken_differentInputSamePlaintext_differentCiphertext()
    // (due to random IV in AES-GCM)
    @Test void encryptToken_tamperedCiphertext_throwsDecryptionException()
}
```

---

## MODULE 11: Multi-Tenancy Security Tests

```java
class TenantIsolationTest {

    // These are the most critical tests in the entire system.
    // A data breach between tenants is catastrophic.

    @Test void ordersEndpoint_businessAToken_returnsOnlyBusinessAOrders()
    @Test void ordersEndpoint_businessBToken_returnsOnlyBusinessBOrders()
    @Test void getOrder_businessAOwnsOrder_businessBRequestReturns404()
    // Business B should not even know business A's order exists

    @Test void menuEndpoint_businessAToken_returnsOnlyBusinessAMenu()
    @Test void customerEndpoint_tenantIsolation()
    @Test void inventoryEndpoint_tenantIsolation()
    @Test void analyticsEndpoint_tenantIsolation()

    @Test void directIdAccess_businessARequestsBusinessBResource_returns404()
    // Attempting to GET /api/orders/{businessB_orderId} with business A token
    // must return 404, NOT 403 (403 reveals resource exists)

    @Test void staffCrossOutletAccess_outletAStaff_cannotAccessOutletBOrders()
    // Within same business, outlet-level isolation

    @Test void superAdmin_canAccessAnyBusiness()
    @Test void businessOwner_canAccessAllTheirOutlets()
    @Test void outletManager_canOnlyAccessAssignedOutlet()
}
```

---

## MODULE 12: API Security Tests

```java
class SecurityTest {

    @Test void unauthenticatedRequest_protectedEndpoint_returns401()
    @Test void expiredJwt_returns401()
    @Test void tamperedJwt_returns401()
    @Test void validJwt_allowsAccess()

    @Test void cashierRole_accessesFinanceEndpoint_returns403()
    @Test void cashierRole_accessesPosEndpoint_returns200()
    @Test void kitchenStaff_accessesKdsEndpoint_returns200()
    @Test void kitchenStaff_accessesPayrollEndpoint_returns403()

    @Test void rateLimiting_otpResend_4thRequestReturns429()
    @Test void rateLimiting_loginAttempts_6thAttemptReturns429()
    // Bucket4j: max 5 login attempts per IP per minute

    @Test void sqlInjection_orderSearchParam_sanitized()
    // "'; DROP TABLE orders;--" as search param should not crash

    @Test void fileUpload_executableFile_rejected()
    @Test void fileUpload_oversizedFile_returns413()
    @Test void fileUpload_validImage_returns200()
}
```

---

## Vibe-Coding Prompt: Test Infrastructure

```
Set up the complete test infrastructure for QuickServe ERP.

1. pom.xml test dependencies:
   - spring-boot-starter-test (JUnit 5, Mockito, MockMvc)
   - testcontainers (postgresql, redis, kafka — BOM import)
   - testcontainers:junit-jupiter
   - spring-kafka-test (@EmbeddedKafka)
   - wiremock-standalone (for Meta WhatsApp API mocking)
   - awaitility (for async tests — waiting for Kafka events)
   - datafaker (realistic test data generation)

2. src/test/resources/application-test.yml:
   - Flyway: enabled, clean-on-validation-error=true
   - Kafka: use @EmbeddedKafka
   - Redis: use testcontainers
   - MinIO: use fake in-memory implementation
   - WhatsApp: mock all sends (never actually call Meta in tests)
   - JWT secret: use fixed test secret

3. BaseIntegrationTest.java with:
   - Testcontainers setup for PostgreSQL + Redis
   - @BeforeEach that truncates all business data (not Flyway schema tables)
   - Helper methods: createBusiness(), createOutlet(), createStaff(), createMenuItem()
   - Helper: authenticateAs(Role role) → returns test JWT token
   - Helper: performPost/Get/Put with auth header pre-set

4. WhatsAppMockConfig.java:
   - WireMock server setup
   - Default stub: POST to /v18.0/*/messages → 200 with valid wamid response
   - Helper: verifyWhatsAppSent(templateName) — assert WireMock received request
   - Helper: stubWhatsApp5xx() — configure failure for testing retry

5. TestDataFactory.java:
   - Creates realistic test data using DataFaker
   - createTestOrder(outletId, itemCount)
   - createTestCustomer(businessId)
   - createTestEmployee(outletId)
   - createTestInvoice(businessId, amount)

Write all files completely.
Ensure tests can run in parallel (each test creates its own isolated business).
Ensure CI pipeline can run tests in under 5 minutes.
```
