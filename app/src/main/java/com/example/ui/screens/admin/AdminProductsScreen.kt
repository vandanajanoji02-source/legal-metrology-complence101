package com.example.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.model.ProductStatus
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.filteredProducts.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Product Registry (${products.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onProductSearchChanged(it) },
                placeholder = { Text("Search product name, brand, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onProductSearchChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("admin_search_product_input")
            )

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_card_${product.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${product.brand} • ${product.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusBadge(status = product.status)
                                }

                                Text(
                                    text = "Barcode: ${product.barcode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = "Mfg: ${product.manufacturer} | Exp: ${product.expiryDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            productToEdit = product
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_product_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { productToDelete = product },
                                        modifier = Modifier.testTag("delete_product_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            initialProduct = productToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedProduct ->
                if (productToEdit != null) {
                    viewModel.updateProduct(updatedProduct)
                } else {
                    viewModel.addProduct(
                        barcode = updatedProduct.barcode,
                        name = updatedProduct.name,
                        brand = updatedProduct.brand,
                        category = updatedProduct.category,
                        manufacturer = updatedProduct.manufacturer,
                        description = updatedProduct.description,
                        manufacturingDate = updatedProduct.manufacturingDate,
                        expiryDate = updatedProduct.expiryDate,
                        status = updatedProduct.status
                    )
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = {
                Text("Are you sure you want to remove '${productToDelete?.name}' from the registry?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { viewModel.deleteProduct(it.id) }
                        productToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditProductDialog(
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var brand by remember { mutableStateOf(initialProduct?.brand ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "") }
    var manufacturer by remember { mutableStateOf(initialProduct?.manufacturer ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var manufacturingDate by remember { mutableStateOf(initialProduct?.manufacturingDate ?: "") }
    var expiryDate by remember { mutableStateOf(initialProduct?.expiryDate ?: "") }
    var status by remember { mutableStateOf(initialProduct?.status ?: ProductStatus.VERIFIED) }

    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialProduct != null) "Edit Product" else "Add New Product")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_barcode_input")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_brand_input")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = manufacturingDate,
                    onValueChange = { manufacturingDate = it },
                    label = { Text("Mfg Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Exp Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Security Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = status.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Verification Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        ProductStatus.values().forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.title) },
                                onClick = {
                                    status = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (barcode.isNotBlank() && name.isNotBlank()) {
                        val product = (initialProduct ?: Product()).copy(
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
                        onSave(product)
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
