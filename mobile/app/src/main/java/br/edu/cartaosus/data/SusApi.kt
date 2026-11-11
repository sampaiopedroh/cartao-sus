package br.edu.cartaosus.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SusApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body corpo: LoginRequest): LoginResponse

    @POST("api/v1/auth/cadastro")
    suspend fun cadastro(@Body corpo: CadastroRequest): CadastroResponse

    @GET("api/v1/auth/me")
    suspend fun me(): MeResponse
}
