package dam_a52057.wastewatch.data.remote

import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject

class ProductRemoteDataSource @Inject constructor(
    private val api: OpenFoodFactsApi
) {
    suspend fun fetchProductByBarcode(barcode: String): Result<ProductData> {
        return try {
            val response = api.getProduct(barcode)
            if (response.status == 1 && response.product != null) {
                Result.success(response.product)
            } else {
                Result.failure(Exception("Produto não encontrado"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Sem ligação à internet"))
        } catch (e: HttpException) {
            Result.failure(Exception("Erro de servidor: ${e.code()}"))
        }
    }
}
