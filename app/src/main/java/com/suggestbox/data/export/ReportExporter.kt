package com.suggestbox.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.suggestbox.domain.model.AdminAnalytics
import com.suggestbox.domain.model.Suggestion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates shareable CSV and native Android PDF summary reports for municipal officials.
 */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun exportCsvFile(csvContent: String, fileName: String = "suggestbox_report.csv"): Uri =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "reports").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(csvContent)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    suspend fun exportPdfSummary(
        analytics: AdminAnalytics,
        suggestions: List<Suggestion>,
        fileName: String = "suggestbox_summary.pdf",
    ): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0A4DA2")
            textSize = 18f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
        }

        var y = 52f
        canvas.drawText("SuggestBox — Local Governance Summary Report", 40f, y, titlePaint)
        y += 28f
        canvas.drawText(
            "Total Tickets: ${analytics.totalCount}   |   SLA Overdue: ${analytics.overdueCount}   |   Avg Resolution: ${analytics.averageResolutionHours} hrs",
            40f,
            y,
            bodyPaint,
        )
        y += 28f
        canvas.drawText("Recent Tickets", 40f, y, headerPaint)
        y += 18f

        suggestions.take(22).forEach { item ->
            val line = "${item.trackingId}  [${item.status.name}]  (${item.priority.name})  ${item.title.take(54)}"
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 18f
        }

        pdf.finishPage(page)
        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
