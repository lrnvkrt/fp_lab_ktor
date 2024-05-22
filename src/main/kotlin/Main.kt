import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.*

const val unsplashUrl = "https://source.unsplash.com/random"
const val imagesCount = 100
val ktorClient = HttpClient(CIO)

suspend fun main() {
    println("Скачиваем 100 картинок с изначальным именем")
    // передаем в функцию количиство картинок url с которого качаем и функцию которая генерирует название картинки
    downloadImagesAndSaveWithName(imagesCount, unsplashUrl) { response ->
        // в функцию передается response из которого можно выцепить оригинальный url картинки
        // здесь оборачиваем в Java объект URL потому что изначальный url содержит и параметры с плохими знаками для файловой системы, мы берем только атрибут path (путь) и забираем последний элемент (название оригинальной картинки)
        "originalName/${URL(response.request.url.toString()).path.split("/").last()}.png"
    }
    println("Завершили и сохранили")

    println("Скачиваем 100 картинок с сгенерированным именем")
    downloadImagesAndSaveWithName(imagesCount, unsplashUrl) {
        "generatedName/${UUID.randomUUID()}.png"
    }
    println("Завершили")
}

suspend fun downloadImagesAndSaveWithName(
    imagesCount: Int,
    url: String,
    nameGenerator: (response: HttpResponse) -> String
) {
    // используем дефолтный контекст dispatcher из примера с презентации
    withContext(Dispatchers.Default) {
        // делаем список из повторяющегося юрла (одного и того же)
        List(imagesCount) { url }.map { url ->
            // асинхронно получаем с этого url`а картинку (под капотом перенаправление на фактический url картинки,
            // который мы сможем достать через HttpResponse в генераторе названий картинок)
            async {
                ktorClient.get(url)
            }
            // Проходимся по каждому
        }.map {
            // Ждем пока получим картинку
            val response = it.await()
            //Передаем в генератор ответ а пишем в файл body ответа в формате массива байтов (картинку)
            File(nameGenerator(response)).writeBytes(response.body<ByteArray>())
        }
    }
}





