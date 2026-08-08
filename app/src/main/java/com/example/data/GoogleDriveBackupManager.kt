package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class DriveBackupFile(
    val id: String,
    val name: String,
    val createdTime: String,
    val modifiedTime: String,
    val size: Long
)

class GoogleDriveBackupManager {

    companion object {
        private const val TAG = "GoogleDriveBackupMgr"
        const val CLIENT_ID = "1094132807794-7pv06sedfuh5lfot50ufht40fl8uegn2.apps.googleusercontent.com"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        private const val BACKUP_FOLDER_NAME = "BaagBaan Boi"

        fun getGoogleSignInClient(context: Context): GoogleSignInClient {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(CLIENT_ID)
                .requestScopes(Scope(SCOPE_DRIVE_FILE))
                .build()
            return GoogleSignIn.getClient(context, gso)
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Gets an OAuth 2.0 access token for Google Drive API calls.
     */
    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("AgriCropBackupPrefs", Context.MODE_PRIVATE)

            // Try provided account, then last signed-in account from GoogleSignIn
            val targetAccount = account ?: GoogleSignIn.getLastSignedInAccount(context)

            // Resolve email from GoogleSignInAccount, SharedPreferences, or FirebaseAuth
            val accountEmail = targetAccount?.email
                ?: prefs.getString("gdrive_account_email", null)
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email

            val androidAccount: android.accounts.Account? = when {
                targetAccount?.account != null -> targetAccount.account
                !accountEmail.isNullOrBlank() -> android.accounts.Account(accountEmail, "com.google")
                else -> null
            }

            if (androidAccount == null) {
                return@withContext Result.failure(Exception("Google Account not signed in. Please authorize Google Drive access."))
            }

            val scopeString = "oauth2:$SCOPE_DRIVE_FILE"
            val token = try {
                GoogleAuthUtil.getToken(context, androidAccount, scopeString)
            } catch (e: Exception) {
                Log.w(TAG, "GoogleAuthUtil.getToken failed: ${e.message}")
                return@withContext Result.failure(e)
            }
            if (token.isNullOrEmpty()) {
                Result.failure(Exception("Unable to retrieve OAuth token for Google Drive."))
            } else {
                // Save successful account email to prefs so authorization remains persistent
                if (!accountEmail.isNullOrBlank()) {
                    prefs.edit().putString("gdrive_account_email", accountEmail).apply()
                }
                Result.success(token)
            }
        } catch (e: UserRecoverableAuthException) {
            Log.w(TAG, "User consent required for Google Drive access: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Google Drive access token: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Invalidates a stale or expired OAuth token if a 401 is encountered.
     */
    fun invalidateToken(context: Context, token: String) {
        try {
            GoogleAuthUtil.invalidateToken(context, token)
            Log.d(TAG, "Invalidated expired/stale Google Drive OAuth token.")
        } catch (e: Exception) {
            Log.w(TAG, "Error invalidating token: ${e.message}")
        }
    }

    /**
     * Finds or creates a dedicated "BaagBaan Boi" folder in Google Drive root directory.
     */
    suspend fun getOrCreateBackupFolderId(accessToken: String): String = withContext(Dispatchers.IO) {
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME' and trashed=false&fields=files(id,name)"
        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful && responseBody.isNotEmpty()) {
            val json = JSONObject(responseBody)
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                return@withContext files.getJSONObject(0).getString("id")
            }
        }

        // Create folder "BaagBaan Boi" if not found
        val createUrl = "https://www.googleapis.com/drive/v3/files"
        val folderMeta = JSONObject()
        folderMeta.put("name", BACKUP_FOLDER_NAME)
        folderMeta.put("mimeType", "application/vnd.google-apps.folder")

        val parents = org.json.JSONArray()
        parents.put("root")
        folderMeta.put("parents", parents)

        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(folderMeta.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        val createResponseBody = createResponse.body?.string() ?: ""

        if (createResponse.isSuccessful && createResponseBody.isNotEmpty()) {
            val json = JSONObject(createResponseBody)
            return@withContext json.getString("id")
        }

        ""
    }

    /**
     * Uploads local backup JSON file directly into the "BaagBaan Boi" folder in Google Drive.
     */
    suspend fun uploadBackupToDrive(
        context: Context,
        backupFile: File,
        account: GoogleSignInAccount?
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("OAuth Token error"))
            }
            val accessToken = tokenResult.getOrThrow()

            val folderId = getOrCreateBackupFolderId(accessToken)

            val metadataJson = JSONObject()
            val fileName = if (backupFile.name.startsWith("AgriCrop")) {
                backupFile.name.replace("AgriCrop", "BaagBaan_Boi")
            } else {
                backupFile.name
            }
            metadataJson.put("name", fileName)
            metadataJson.put("mimeType", "application/json")
            if (folderId.isNotEmpty()) {
                val parents = org.json.JSONArray()
                parents.put(folderId)
                metadataJson.put("parents", parents)
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(
                    metadataJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .addPart(
                    backupFile.asRequestBody("application/json".toMediaType())
                )
                .build()

            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,createdTime,modifiedTime,size"
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val json = JSONObject(responseBody)
                val driveFile = DriveBackupFile(
                    id = json.optString("id", ""),
                    name = json.optString("name", backupFile.name),
                    createdTime = json.optString("createdTime", ""),
                    modifiedTime = json.optString("modifiedTime", ""),
                    size = json.optLong("size", backupFile.length())
                )
                Log.d(TAG, "Successfully uploaded backup to BaagBaan Boi folder in Google Drive: ${driveFile.name}")
                Result.success(driveFile)
            } else {
                Log.e(TAG, "Google Drive upload failed: Code ${response.code}, $responseBody")
                Result.failure(Exception("Google Drive upload failed (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading backup to Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches list of backups directly from the "BaagBaan Boi" folder on Google Drive.
     */
    suspend fun fetchBackupsFromDrive(
        context: Context,
        account: GoogleSignInAccount?
    ): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("OAuth Token error"))
            }
            val accessToken = tokenResult.getOrThrow()

            val folderId = getOrCreateBackupFolderId(accessToken)
            val query = if (folderId.isNotEmpty()) {
                "'$folderId' in parents and trashed=false"
            } else {
                "(name contains 'BaagBaan' or name contains 'Baagbaan_Boi' or name contains 'AgriCrop_Backup') and trashed=false"
            }

            val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,createdTime,modifiedTime,size)&orderBy=createdTime%20desc"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val json = JSONObject(responseBody)
                val filesArray = json.optJSONArray("files")
                val resultList = mutableListOf<DriveBackupFile>()

                if (filesArray != null) {
                    for (i in 0 until filesArray.length()) {
                        val item = filesArray.getJSONObject(i)
                        resultList.add(
                            DriveBackupFile(
                                id = item.optString("id", ""),
                                name = item.optString("name", "Backup.json"),
                                createdTime = item.optString("createdTime", ""),
                                modifiedTime = item.optString("modifiedTime", ""),
                                size = item.optLong("size", 0L)
                            )
                        )
                    }
                }
                Log.d(TAG, "Fetched ${resultList.size} backup files from Google Drive BaagBaan Boi folder")
                Result.success(resultList)
            } else {
                Result.failure(Exception("Failed to fetch backups from Drive (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching backups from Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads content of a specified backup file from Google Drive.
     */
    suspend fun downloadBackupFromDrive(
        context: Context,
        fileId: String,
        account: GoogleSignInAccount?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = getAccessToken(context, account)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull() ?: Exception("OAuth Token error"))
            }
            val accessToken = tokenResult.getOrThrow()

            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotEmpty()) {
                Result.success(responseBody)
            } else {
                Result.failure(Exception("Failed to download backup from Drive (${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading backup from Google Drive: ${e.message}", e)
            Result.failure(e)
        }
    }
}
