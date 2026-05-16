package dam_a52057.wastewatch.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

data class ProductResponse(
    val status: Int,
    val product: ProductData?
)

data class ProductData(
    @com.google.gson.annotations.SerializedName("product_name") val name: String?,
    val brands: String?,
    val categories: String?,
    val code: String?
)
