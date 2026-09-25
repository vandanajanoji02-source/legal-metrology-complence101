package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.network.ApiClient
import com.example.network.ProductDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProductRepository(private val context: Context) {
    private val TAG = "ProductRepository"

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val initialDemoProducts = listOf(
        Product(
            id = "prod_demo_1",
            barcode = "8901234567890",
            name = "Demo Verified Product",
            brand = "SIH Demo",
            category = "Electronics",
            manufacturer = "SIH Certified Tech Labs",
            description = "Official authenticated prototype test unit with verified cryptographic batch ID and regulatory clearance.",
            manufacturingDate = "2026-01-15",
            expiryDate = "2028-01-15",
            status = ProductStatus.VERIFIED,
            createdAt = System.currentTimeMillis() - 86400000L * 10
        ),
        Product(
            id = "prod_demo_2",
            barcode = "8901234567891",
            name = "Demo Warning Product",
            brand = "SIH Demo",
            category = "Pharmaceuticals",
            manufacturer = "MediCorp Healthcare Ltd",
            description = "Batch flagged for secondary verification due to near-expiry date and humidity packaging alert.",
            manufacturingDate = "2025-03-10",
            expiryDate = "2026-10-01",
            status = ProductStatus.WARNING,
            createdAt = System.currentTimeMillis() - 86400000L * 8
        ),
        Product(
            id = "prod_demo_3",
            barcode = "8901234567892",
            name = "Demo Invalid Product",
            brand = "SIH Demo",
            category = "Consumer Goods",
            manufacturer = "Unverified Third Party",
            description = "Reported counterfeit batch or revoked safety certificate. Do not distribute or accept.",
            manufacturingDate = "2024-05-20",
            expiryDate = "2025-05-20",
            status = ProductStatus.INVALID,
            createdAt = System.currentTimeMillis() - 86400000L * 5
        ),
        Product(
            id = "prod_demo_4",
            barcode = "8909876543210",
            name = "Organic Pure Himalayan Honey",
            brand = "NaturePure India",
            category = "Food & Agriculture",
            manufacturer = "Himalayan Apiaries Trust",
            description = "FSSAI Grade A certified raw multi-floral honey with geo-tagged origin tracking.",
            manufacturingDate = "2026-02-01",
            expiryDate = "2027-02-01",
            status = ProductStatus.VERIFIED,
            createdAt = System.currentTimeMillis() - 86400000L * 3
        )
    )

    init {
        ApiClient.initialize(context)
        FirebaseManager.initialize(context)
        _products.value = initialDemoProducts
        CoroutineScope(Dispatchers.IO).launch {
            loadProducts()
        }
    }

    suspend fun loadProducts() {
        // 1. Try Backend API first
        try {
            val response = ApiClient.apiService.getProducts()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                if (dtos.isNotEmpty()) {
                    val list = dtos.map { it.toDomain() }
                    _products.value = list
                    Log.d(TAG, "Loaded ${list.size} products from Backend API")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend API loadProducts failed (offline or unreachable): ${e.message}")
        }

        // 2. Fallback to Firestore
        loadFromFirestore()
    }

    private suspend fun loadFromFirestore() {
        val firestore = FirebaseManager.firestore ?: return
        try {
            val snapshot = firestore.collection("products").get().await()
            if (!snapshot.isEmpty) {
                val list = snapshot.documents.map { doc ->
                    Product.fromMap(doc.id, doc.data ?: emptyMap())
                }
                _products.value = list
                Log.d(TAG, "Loaded ${list.size} products from Firestore")
            } else {
                Log.d(TAG, "Firestore products collection empty, seeding demo products...")
                seedDemoProducts()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching products from Firestore: ${e.message}")
        }
    }

    suspend fun findByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return@withContext null

        // 1. Try Backend API first (REST endpoint /api/products/{barcode})
        try {
            val response = ApiClient.apiService.getProductByBarcode(cleanBarcode)
            if (response.isSuccessful && response.body() != null) {
                val product = response.body()!!.toDomain()
                Log.d(TAG, "Found product via Backend API: ${product.name}")
                updateLocalCache(product)
                return@withContext product
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend API product lookup failed for $cleanBarcode: ${e.message}")
        }

        // 1b. Try alternative search endpoint /api/products/search?barcode=
        try {
            val searchResponse = ApiClient.apiService.searchProductByBarcode(cleanBarcode)
            if (searchResponse.isSuccessful && searchResponse.body() != null) {
                val product = searchResponse.body()!!.toDomain()
                Log.d(TAG, "Found product via Backend API search: ${product.name}")
                updateLocalCache(product)
                return@withContext product
            }
        } catch (ignored: Exception) {
            // Ignored - will proceed to Firestore fallback
        }

        // 2. Fallback to Firestore
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                val querySnapshot = firestore.collection("products")
                    .whereEqualTo("barcode", cleanBarcode)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val product = Product.fromMap(doc.id, doc.data ?: emptyMap())
                    updateLocalCache(product)
                    return@withContext product
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore query by barcode failed: ${e.message}")
            }
        }

        // 3. Fallback to local memory list
        _products.value.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }
    }

    suspend fun addProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        val id = if (product.id.isEmpty()) "prod_" + System.currentTimeMillis() else product.id
        val newProduct = product.copy(id = id)

        // 1. Try Backend API
        try {
            ApiClient.apiService.createProduct(com.example.network.ProductDto.fromDomain(newProduct))
            Log.d(TAG, "Added product to Backend API: $id")
        } catch (e: Exception) {
            Log.w(TAG, "Backend API addProduct failed: ${e.message}")
        }

        // 2. Try Firestore
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                firestore.collection("products").document(id).set(newProduct.toMap()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore addProduct failed: ${e.message}")
            }
        }

        updateLocalCache(newProduct)
        Result.success(newProduct)
    }

    suspend fun updateProduct(product: Product): Result<Product> = withContext(Dispatchers.IO) {
        // 1. Try Backend API
        try {
            ApiClient.apiService.updateProduct(product.id, com.example.network.ProductDto.fromDomain(product))
            Log.d(TAG, "Updated product on Backend API: ${product.id}")
        } catch (e: Exception) {
            Log.w(TAG, "Backend API updateProduct failed: ${e.message}")
        }

        // 2. Try Firestore
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                firestore.collection("products").document(product.id).set(product.toMap()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore updateProduct failed: ${e.message}")
            }
        }

        updateLocalCache(product)
        Result.success(product)
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. Try Backend API
        try {
            ApiClient.apiService.deleteProduct(productId)
            Log.d(TAG, "Deleted product on Backend API: $productId")
        } catch (e: Exception) {
            Log.w(TAG, "Backend API deleteProduct failed: ${e.message}")
        }

        // 2. Try Firestore
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                firestore.collection("products").document(productId).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore deleteProduct failed: ${e.message}")
            }
        }

        _products.value = _products.value.filterNot { it.id == productId }
        Result.success(Unit)
    }

    suspend fun seedDemoProducts(): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = FirebaseManager.firestore
        for (prod in initialDemoProducts) {
            if (firestore != null) {
                try {
                    firestore.collection("products").document(prod.id).set(prod.toMap()).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Seeding product ${prod.id} to Firestore failed: ${e.message}")
                }
            }
            updateLocalCache(prod)
        }
        Result.success(Unit)
    }

    private fun updateLocalCache(product: Product) {
        val current = _products.value.toMutableList()
        val index = current.indexOfFirst { it.id == product.id || it.barcode == product.barcode }
        if (index >= 0) {
            current[index] = product
        } else {
            current.add(0, product)
        }
        _products.value = current
    }
}
