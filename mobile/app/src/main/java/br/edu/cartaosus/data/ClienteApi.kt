package br.edu.cartaosus.data

import br.edu.cartaosus.BuildConfig
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Monta o cliente HTTP: envia o token em todas as chamadas quando há sessão.
object ClienteApi {
    fun criar(sessao: SessionStore): SusApi {
        val autenticacao = Interceptor { cadeia ->
            val token = sessao.token
            val pedido = if (token.isNullOrBlank()) cadeia.request()
            else cadeia.request().newBuilder().header("Authorization", "Bearer $token").build()
            cadeia.proceed(pedido)
        }

        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(autenticacao)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(SusApi::class.java)
    }
}
