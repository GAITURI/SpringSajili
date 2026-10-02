package sajili

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import sajili.agent.SajiliApplication
import sajili.agent.inventory.dto.IssueStockRequest
import sajili.agent.inventory.dto.ReceiveStockRequest
import sajili.agent.product.entity.ProductEntity
import sajili.agent.product.entity.ProductRepository
import sajili.agent.user.entity.UserProfileEntity
import sajili.agent.user.repository.UserProfileRepository
import sajili.agent.warehouse.entity.WarehouseEntity
import sajili.agent.warehouse.entity.WarehouseRepository
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithMockJwtSecurityContextFactory::class)
annotation class WithMockJwt(
    val subject: String = "test-auth-subject" // Must match a sub in your test DB / user profile
)


@SpringBootTest(classes = [SajiliApplication::class],
    properties = ["spring.main.allow-bean-definition-overriding=true"]
    )
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockJwt(subject = "my-test-subject-id") // Injects a Jwt principal instead of a User object

@Transactional
class InventoryApiConcurrencyTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var productRepository: ProductRepository // 1.

    @Autowired
    private lateinit var warehouseRepository: WarehouseRepository

    private val testTenantId: UUID = UUID.randomUUID() // Shared single tenant ID source of truth
    lateinit var testProductId: UUID

    private lateinit var testWarehouseId: UUID

    @BeforeEach
    fun setUp() {
        // Ensure the UserProfile exists in the test DB so SpringSecurityCurrentUserProvider passes
        // 1. Ensure the Warehouse exists in the test DB so InventorySecurityValidator passes
        val warehouse=warehouseRepository.save(
                WarehouseEntity(
                    tenantId = testTenantId,
                    name = "Test Warehouse",
                    code ="WH-TEST-01" ,
                    erpLocationCode ="ERP-LOC-01"
                    // ... add any other required non-null fields your WarehouseEntity requires
                )
            )
            testWarehouseId = warehouse.id!! // Capture the actual assigned UUID
        // 3. Save the Product so the inventory service can find it
       val product= productRepository.save(
            ProductEntity(
                tenantId = testTenantId,
                name = "Test Product",
                sku = "SKU-TEST-01",
                erpItemCode = "ERP-ITEM-01"
            ))
         testProductId= product.id!!
        if (userProfileRepository.findByAuthSubject("my-test-subject-id") == null) {
            userProfileRepository.save(
                UserProfileEntity(
                    authSubject = "my-test-subject-id",
                    email = "test-user@sajili.agent",
                    displayName = "Test User",
                    tenantId =testTenantId,
                    isActive = true,
                    authorizedWarehouses = setOf(testWarehouseId) // Clean, type-safe Set
                )
            )
        }
        // Seed initial stock so concurrency tests have a concrete baseline (100 units available)
        val receiveRequest = ReceiveStockRequest(
            warehouseId = testWarehouseId,
            productId = testProductId,
            quantity = BigDecimal("100.00"),
            referenceId = "SEED-INITIAL-STOCK",
            idempotencyKey = UUID.randomUUID().toString(),
            notes = "Initial test stock seeding"
        )

        mockMvc.post("/api/inventory/receive") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer dummy-token")
            content = objectMapper.writeValueAsString(receiveRequest)
        }
    }

    @Test
    fun `test concurrent stock issuance prevents race conditions and overdrafts`() {
        val numberOfThreads = 10
        val quantityPerRequest = BigDecimal("15.00") // 10 threads * 15 = 150 units (Available: 100)
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val latch = CountDownLatch(1)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        for (i in 0 until numberOfThreads) {
            executor.submit {
                try {
                    latch.await()

                    val request = IssueStockRequest(
                        warehouseId = testWarehouseId,
                        productId = testProductId,
                        quantity = quantityPerRequest,
                        referenceId = "CONCURRENT-TEST-$i",
                        idempotencyKey = UUID.randomUUID().toString()
                    )

                    val response = mockMvc.post("/api/inventory/issue") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer dummy-token")
                        content = objectMapper.writeValueAsString(request)
                    }.andReturn().response

                    if (response.status == 200) {
                        successCount.incrementAndGet()
                    } else {
                        failureCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                }
            }
        }

        // Release all threads simultaneously to trigger race condition
        latch.countDown()
        executor.shutdown()
        while (!executor.isTerminated) {
            Thread.sleep(50)
        }

        // SDET Assertion: Exactly 6 succeed (90 units) and 4 fail due to pessimistic locking
        assertEquals(6, successCount.get())
        assertEquals(4, failureCount.get())
    }

    @Test
    fun `test request idempotency prevents duplicate processing on network retry`() {
        val sharedIdempotencyKey = UUID.randomUUID().toString()
        val request = ReceiveStockRequest(
            warehouseId = testWarehouseId,
            productId = testProductId,
            quantity = BigDecimal("50.00"),
            referenceId = "RETRY-TEST",
            idempotencyKey = sharedIdempotencyKey,
            notes = "Idempotency retry validation"
        )

        val firstResponse = mockMvc.post("/api/inventory/receive") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer dummy-token")
            content = objectMapper.writeValueAsString(request)
        }.andReturn().response

        assertEquals(201, firstResponse.status)

        // Simulate mobile client retry with identical idempotency key
        val secondResponse = mockMvc.post("/api/inventory/receive") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer dummy token")
            content = objectMapper.writeValueAsString(request)
        }.andReturn().response

        assertEquals(200, secondResponse.status)
    }
}