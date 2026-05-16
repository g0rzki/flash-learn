package com.example.flashlearn.ui.screens.stats

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.data.remote.ReportApiService
import com.example.flashlearn.data.remote.dto.StatsDto
import com.example.flashlearn.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val stats: StatsDto) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

sealed class PdfDownloadState {
    object Idle : PdfDownloadState()
    object Loading : PdfDownloadState()
    object Success : PdfDownloadState()
    data class Error(val message: String) : PdfDownloadState()
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: StatsRepository,
    private val reportApiService: ReportApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _pdfState = MutableStateFlow<PdfDownloadState>(PdfDownloadState.Idle)
    val pdfState: StateFlow<PdfDownloadState> = _pdfState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            repository.getStats().collect { result ->
                result.onSuccess { stats ->
                    _uiState.value = StatsUiState.Success(stats)
                }.onFailure { error ->
                    _uiState.value = StatsUiState.Error(error.message ?: "Wystąpił błąd")
                }
            }
        }
    }

    fun downloadStatsPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _pdfState.value = PdfDownloadState.Loading
            try {
                val token = "Bearer ${TokenManager.getAccessToken()}"
                val response = reportApiService.downloadStatsPdf(token)
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                        ?: throw IllegalStateException("Pusta odpowiedź serwera")
                    savePdfToDownloads(context, bytes)
                    _pdfState.value = PdfDownloadState.Success
                } else {
                    _pdfState.value = PdfDownloadState.Error("Błąd serwera: ${response.code()}")
                }
            } catch (e: Exception) {
                _pdfState.value = PdfDownloadState.Error(e.message ?: "Nieznany błąd")
            }
        }
    }

    fun resetPdfState() {
        _pdfState.value = PdfDownloadState.Idle
    }

    private fun savePdfToDownloads(context: Context, bytes: ByteArray) {
        val filename = "statystyki_${System.currentTimeMillis()}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(dir, filename).writeBytes(bytes)
        }
    }
}