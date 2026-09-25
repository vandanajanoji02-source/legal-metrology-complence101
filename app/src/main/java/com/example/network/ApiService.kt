package com.example.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- Products ---

    @GET("products")
    suspend fun getProducts(): Response<List<ProductDto>>

    @GET("products/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<ProductDto>

    @GET("products/verify/{barcode}")
    suspend fun verifyProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<ProductDto>

    @GET("products/search")
    suspend fun searchProductByBarcode(
        @Query("barcode") barcode: String
    ): Response<ProductDto>

    @POST("products")
    suspend fun createProduct(
        @Body product: ProductDto
    ): Response<ProductDto>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body product: ProductDto
    ): Response<ProductDto>

    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: String
    ): Response<Unit>

    // --- Inspections ---

    @GET("inspections")
    suspend fun getInspections(): Response<List<InspectionDto>>

    @GET("inspections/{id}")
    suspend fun getInspectionById(
        @Path("id") id: String
    ): Response<InspectionDto>

    @POST("inspections")
    suspend fun submitInspection(
        @Body inspection: InspectionDto
    ): Response<InspectionSaveResponseDto>

    // --- Scans ---

    @GET("scans")
    suspend fun getScans(): Response<List<ScanRecordDto>>

    @POST("scans")
    suspend fun recordScan(
        @Body scan: ScanRecordDto
    ): Response<ScanRecordDto>

    // --- Dashboard & Analytics ---

    @GET("dashboard/stats")
    suspend fun getDashboardStats(): Response<DashboardStatsDto>

    // --- Server Health Check ---

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}
