package br.edu.cartaosus.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SusApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body corpo: LoginRequest): LoginResponse

    @POST("api/v1/auth/cadastro")
    suspend fun cadastro(@Body corpo: CadastroRequest): CadastroResponse

    @GET("api/v1/auth/me")
    suspend fun me(): MeResponse

    // Telas 3 a 6: cidadão (E4, E5, E6, E10 e extras).
    @GET("api/v1/cidadao/{id}/prontuario")
    suspend fun prontuario(@Path("id") id: Long): ProntuarioResposta

    @GET("api/v1/cidadao/{id}/alertas")
    suspend fun alertas(
        @Path("id") id: Long,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PaginaAlertas

    @PUT("api/v1/cidadao/{id}/alertas/{idAlerta}")
    suspend fun marcarEntregue(@Path("id") id: Long, @Path("idAlerta") idAlerta: Long): Response<Unit>

    @GET("api/v1/cidadao/{id}/acessos")
    suspend fun acessos(@Path("id") id: Long): List<AcessoResumo>

    @GET("api/v1/cidadao/{id}/sinais-vitais")
    suspend fun sinais(@Path("id") id: Long, @Query("tipo") tipo: String? = null): SinaisResposta

    @POST("api/v1/telemetria/sinais-vitais")
    suspend fun enviarSinal(@Body corpo: SinalEnvio): SinalVitalResposta

    // Consulta SUS (E11, E12, E13 e catálogo).
    @GET("api/v1/sus/medicamentos/catalogo")
    suspend fun catalogo(@Query("nome") nome: String? = null): List<CatalogoItem>

    @GET("api/v1/sus/medicamentos")
    suspend fun medicamentos(
        @Query("municipio") municipio: String,
        @Query("catmat") catmat: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PaginaMedicamentos

    @GET("api/v1/sus/equipamentos/{cnes}")
    suspend fun equipamentos(@Path("cnes") cnes: String): EquipamentosResposta

    @GET("api/v1/unidades")
    suspend fun unidades(@Query("page") page: Int = 0, @Query("size") size: Int = 100): PaginaUnidades
}
