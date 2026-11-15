package com.example.data.remote

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ValueRangeDto(
    val range: String? = null,
    val majorDimension: String = "ROWS",
    val values: List<List<String>>? = null
)

@JsonClass(generateAdapter = true)
data class SpreadsheetResponseDto(
    val spreadsheetId: String? = null,
    val spreadsheetUrl: String? = null,
    val properties: SpreadsheetPropertiesDto? = null,
    val sheets: List<SheetMetaDto>? = null
)

@JsonClass(generateAdapter = true)
data class SpreadsheetPropertiesDto(
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class SheetMetaDto(
    val properties: SheetPropertiesDto? = null
)

@JsonClass(generateAdapter = true)
data class SheetPropertiesDto(
    val sheetId: Int? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateSpreadsheetRequestDto(
    val properties: SpreadsheetPropertiesDto,
    val sheets: List<SheetMetaDto>
)

@JsonClass(generateAdapter = true)
data class ClearValuesRequestDto(
    val range: String? = null
)

interface GoogleSheetsApi {

    @GET("v4/spreadsheets/{spreadsheetId}")
    suspend fun getSpreadsheetMetadata(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Query("fields") fields: String = "spreadsheetId,spreadsheetUrl,properties.title,sheets.properties.title"
    ): SpreadsheetResponseDto

    @POST("v4/spreadsheets")
    suspend fun createSpreadsheet(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateSpreadsheetRequestDto
    ): SpreadsheetResponseDto

    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range", encoded = true) range: String
    ): ValueRangeDto

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun updateValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range", encoded = true) range: String,
        @Query("valueInputOption") valueInputOption: String = "USER_ENTERED",
        @Body body: ValueRangeDto
    ): ValueRangeDto

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:clear")
    suspend fun clearValues(
        @Header("Authorization") bearerToken: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range", encoded = true) range: String,
        @Body body: ClearValuesRequestDto = ClearValuesRequestDto()
    )

    companion object {
        private const val BASE_URL = "https://sheets.googleapis.com/"

        fun create(): GoogleSheetsApi {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(GoogleSheetsApi::class.java)
        }
    }
}
