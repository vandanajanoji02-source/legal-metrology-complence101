package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProductRepository
import com.example.data.ScanRepository
import com.example.data.ScanStatistics
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.model.ScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AdminViewModel(
    private val productRepository: ProductRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    val stats: StateFlow<ScanStatistics> = scanRepository.stats
    val allScans: StateFlow<List<ScanRecord>> = scanRepository.scans

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            combine(productRepository.products, _productSearchQuery) { list, query ->
                if (query.isBlank()) {
                    list
                } else {
                    list.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.barcode.contains(query, ignoreCase = true) ||
                        it.brand.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
                    }
                }
            }.collect {
                _filteredProducts.value = it
            }
        }
    }

    fun onProductSearchChanged(query: String) {
        _productSearchQuery.value = query
    }

    fun addProduct(
        barcode: String,
        name: String,
        brand: String,
        category: String,
        manufacturer: String,
        description: String,
        manufacturingDate: String,
        expiryDate: String,
        status: ProductStatus
    ) {
        viewModelScope.launch {
            val product = Product(
                id = "prod_" + System.currentTimeMillis(),
                barcode = barcode.trim(),
                name = name.trim(),
                brand = brand.trim(),
                category = category.trim(),
                manufacturer = manufacturer.trim(),
                description = description.trim(),
                manufacturingDate = manufacturingDate.trim(),
                expiryDate = expiryDate.trim(),
                status = status
            )
            productRepository.addProduct(product)
            _actionMessage.value = "Product added successfully."
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
            _actionMessage.value = "Product updated successfully."
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
            _actionMessage.value = "Product deleted successfully."
        }
    }

    fun seedDemoProducts() {
        viewModelScope.launch {
            productRepository.seedDemoProducts()
            _actionMessage.value = "Demo products re-seeded successfully."
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
