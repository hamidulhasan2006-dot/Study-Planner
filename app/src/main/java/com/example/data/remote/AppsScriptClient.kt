package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AppsScriptSyncPayload(
    val action: String = "sync",
    val tabName: String = "Tasks",
    val rows: List<List<String>>
)

@JsonClass(generateAdapter = true)
data class AppsScriptResponse(
    val ok: Boolean = false,
    val title: String? = null,
    val error: String? = null,
    val rows: List<List<String>>? = null
)

class AppsScriptClient {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()
    private val payloadAdapter = moshi.adapter(AppsScriptSyncPayload::class.java)
    private val responseAdapter = moshi.adapter(AppsScriptResponse::class.java)

    suspend fun fetchRows(webAppUrl: String, tabName: String): AppsScriptResponse =
        withContext(Dispatchers.IO) {
            val separator = if (webAppUrl.contains("?")) "&" else "?"
            val url = "${webAppUrl.trim()}${separator}action=read&tabName=${java.net.URLEncoder.encode(tabName, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            executeAndParse(request)
        }

    suspend fun writeRows(
        webAppUrl: String,
        tabName: String,
        rows: List<List<String>>
    ): AppsScriptResponse = withContext(Dispatchers.IO) {
        val json = payloadAdapter.toJson(
            AppsScriptSyncPayload(
                action = "sync",
                tabName = tabName,
                rows = rows
            )
        )
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(webAppUrl.trim())
            .post(body)
            .build()

        executeAndParse(request)
    }

    private fun executeAndParse(request: Request): AppsScriptResponse {
        client.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Google Sheet returned HTTP ${response.code}. Check that your Web App deployment allows 'Anyone' access.")
            }
            if (rawBody.trimStart().startsWith("<")) {
                throw IOException("Received HTML login page instead of JSON. Re-deploy your Apps Script Web App with 'Who has access: Anyone'.")
            }
            val parsed = responseAdapter.fromJson(rawBody)
                ?: throw IOException("Unexpected response format from Google Apps Script.")
            if (!parsed.ok) {
                throw IOException(parsed.error ?: "Google Sheet script returned an error.")
            }
            return parsed
        }
    }

    companion object {
        val APPS_SCRIPT_CODE = """
function doGet(e) {
  return handleRequest(e, null);
}

function doPost(e) {
  var payload = e.postData && e.postData.contents
    ? JSON.parse(e.postData.contents)
    : {};
  return handleRequest(e, payload);
}

function handleRequest(e, payload) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tabName = (payload && payload.tabName) || (e.parameter && e.parameter.tabName) || "Tasks";
    var sheet = ss.getSheetByName(tabName) || ss.insertSheet(tabName);
    var headers = ["Task ID", "Task Title", "Subject", "Task Type", "Deadline", "Priority", "Status", "Notes"];

    if (sheet.getLastRow() === 0) {
      sheet.appendRow(headers);
      sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold");
      sheet.setFrozenRows(1);
    }

    if (payload && payload.action === "sync" && payload.rows) {
      sheet.clearContents();
      if (payload.rows.length > 0) {
        sheet.getRange(1, 1, payload.rows.length, payload.rows[0].length).setValues(payload.rows);
        sheet.getRange(1, 1, 1, payload.rows[0].length).setFontWeight("bold");
        sheet.setFrozenRows(1);
      }
    }

    var values = sheet.getDataRange().getDisplayValues();
    return ContentService
      .createTextOutput(JSON.stringify({ ok: true, title: ss.getName(), rows: values }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ ok: false, error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
        """.trimIndent()
    }
}
