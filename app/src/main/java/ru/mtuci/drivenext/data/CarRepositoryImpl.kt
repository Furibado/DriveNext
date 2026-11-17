package ru.mtuci.drivenext.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import ru.mtuci.drivenext.domain.Car
import ru.mtuci.drivenext.domain.CarRepository

internal class CarRepositoryImpl(private val client: SupabaseClient) : CarRepository {
    override suspend fun getCars(query: String): List<Car> = withContext(Dispatchers.IO) {
        val route = "/rest/v1/cars?select=id,brand,model,price_per_day,transmission,fuel_type,image_path" +
            "&is_available=eq.true&order=id.asc" + CarSearchQuery.filter(query)
        val rows = JSONArray(client.authorizedGet(route))
        List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            val imagePath = row.getString("image_path").split('/').joinToString("/") {
                android.net.Uri.encode(it)
            }
            Car(row.getString("id"), row.getString("brand"), row.getString("model"),
                row.getString("price_per_day").toBigDecimal(), row.getString("transmission"),
                row.getString("fuel_type"),
                "${SupabaseClient.BASE_URL}/storage/v1/object/public/car-images/$imagePath")
        }
    }
}
