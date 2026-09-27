package com.storefront.app

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.storefront.app.model.*
import com.storefront.app.network.ApiService
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ApiServiceContractTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        apiService = NetworkModule.createApiService(mockWebServer.url("/").toString())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test ingestIsbn request payload and headers match backend contract`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"sku\":\"9780134685991\",\"name\":\"Effective Java\"}"))

        val request = IngestIsbnRequest(
            isbn = "9780134685991",
            name = "Effective Java",
            author = "Joshua Bloch",
            quantity = 5,
            price = 599.0
        )

        apiService.ingestIsbn("Bearer test-token-123", request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/inventory/ingest/isbn", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        assertEquals("Bearer test-token-123", recordedRequest.getHeader("Authorization"))

        val body = gson.fromJson(recordedRequest.body.readUtf8(), JsonObject::class.java)
        assertEquals("9780134685991", body.get("isbn").asString)
        assertEquals("Effective Java", body.get("name").asString)
        assertEquals("Joshua Bloch", body.get("author").asString)
        assertEquals(5, body.get("quantity").asInt)
        assertEquals(599.0, body.get("price").asDouble, 0.001)
    }

    @Test
    fun `test updateProduct preserves attributes and sends correct payload`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":10,\"name\":\"Updated Book\",\"basePrice\":499.0}"))

        val request = CreateProductRequest(
            sku = "SKU-BOOK-10",
            name = "Updated Book",
            basePrice = 499.0,
            type = "BOOK",
            attributes = ProductAttributes(
                type = "BOOK",
                author = "Author Name",
                isbn = "9780134685991"
            )
        )

        apiService.updateProduct("Bearer test-token-123", 10L, request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/inventory/products/10", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)

        val body = gson.fromJson(recordedRequest.body.readUtf8(), JsonObject::class.java)
        assertEquals("Updated Book", body.get("name").asString)
        assertEquals(499.0, body.get("basePrice").asDouble, 0.001)
        assertEquals("BOOK", body.get("type").asString)
        assertTrue(body.has("attributes"))
        val attrs = body.getAsJsonObject("attributes")
        assertEquals("Author Name", attrs.get("author").asString)
    }

    @Test
    fun `test updateStockCount sends valid sku and quantity`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"success\":true}"))

        val request = UpdateStockRequest(
            sku = "SKU-100",
            quantity = 25,
            storeId = 2L
        )

        apiService.updateStockCount("Bearer test-token-123", request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/inventory/stock", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)

        val body = gson.fromJson(recordedRequest.body.readUtf8(), JsonObject::class.java)
        assertEquals("SKU-100", body.get("sku").asString)
        assertEquals(25, body.get("quantity").asInt)
        assertEquals(2L, body.get("storeId").asLong)
    }

    @Test
    fun `test getInventoryView deserialization`() = runBlocking {
        val mockJson = """
            [
                {
                    "id": 1,
                    "sku": "SKU-1",
                    "name": "Math Textbook",
                    "type": "BOOK",
                    "price": 350.0,
                    "quantity": 12,
                    "attributes": {
                        "author": "Dr. Smith",
                        "isbn": "9781234567890"
                    }
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val products = apiService.getInventoryView("Bearer test-token-123", 1L)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/inventory/view?storeId=1", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)

        assertEquals(1, products.size)
        val product = products[0]
        assertEquals("Math Textbook", product.name)
        assertEquals(350.0, product.price, 0.001)
        assertEquals(12, product.quantity)
        assertEquals("Dr. Smith", product.attributes?.author)
    }
}
