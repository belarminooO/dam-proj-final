# WasteWatch – Utilização de APIs

## Open Food Facts API

Base de dados colaborativa e gratuita de produtos alimentares. Usada para identificar produtos a partir do código de barras.

**API endpoint**
```
GET https://world.openfoodfacts.org/api/v0/product/{barcode}.json
```

**Example response**
```json
{
  "status": 1,
  "product": {
    "code": "5449000000996",
    "product_name": "Coca-Cola",
    "brands": "Coca-Cola",
    "categories": "Bebidas, Refrigerantes",
    "image_url": "https://images.openfoodfacts.org/..."
  }
}
```

**Campos utilizados na app**

```
product_name  →  Product.name
brands        →  Product.brand
categories    →  sugestão de Category
code          →  Product.barcode
```

**Notas:**
- A API não fornece data de validade — deve ser sempre inserida manualmente pelo utilizador
- Não requer autenticação
- Se `status == 0` o produto não foi encontrado → redirecionar para adição manual

---

## Implementação Retrofit

```kotlin
interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

data class ProductResponse(
    val status: Int,
    val product: ProductData?
)

data class ProductData(
    @SerializedName("product_name") val name: String?,
    val brands: String?,
    val categories: String?,
    val code: String?
)
```

---

## Tratamento de Erros

```kotlin
suspend fun fetchProductByBarcode(barcode: String): Result<Product> {
    return try {
        val response = api.getProduct(barcode)
        if (response.status == 1 && response.product != null) {
            Result.success(response.product.toProduct(barcode))
        } else {
            Result.failure(Exception("Produto não encontrado"))
        }
    } catch (e: IOException) {
        Result.failure(Exception("Sem ligação à internet"))
    } catch (e: HttpException) {
        Result.failure(Exception("Erro de servidor: ${e.code()}"))
    }
}
```

---

## Permissões Android Necessárias

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
