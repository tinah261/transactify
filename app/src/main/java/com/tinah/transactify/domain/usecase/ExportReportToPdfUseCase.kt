package com.tinah.transactify.domain.usecase

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.tinah.transactify.domain.model.OperatorBreakdown
import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.utils.DateUtils
import com.tinah.transactify.utils.MoneyFormatter
import java.io.OutputStream

/**
 * Génère un PDF une page (A4) résumant [ReportData] : en-tête, totaux,
 * ventilation par opérateur. Utilise `android.graphics.pdf.PdfDocument`
 * (plateforme, aucune dépendance tierce) plutôt qu'une bibliothèque PDF —
 * voir la mémoire projet sur le retrait d'iText (licence/CVE).
 *
 * Reste volontairement sur une seule page : la ventilation ne compte qu'une
 * ligne par opérateur (3 aujourd'hui), donc pas de risque de débordement.
 *
 * **Non couvert par un test Robolectric** (contrairement à [ExportReportToExcelUseCase]) :
 * le shadow `PdfDocument` de Robolectric 4.13 lève `IllegalStateException:
 * document is closed!` dès `startPage()`, avec ou sans `@GraphicsMode(NATIVE)`
 * — limitation connue du shadow, pas un bug de ce code (API standard,
 * identique aux exemples officiels Android). À vérifier manuellement sur
 * appareil/émulateur, ou en test instrumenté (`androidTest`) si besoin plus tard.
 */
class ExportReportToPdfUseCase {

    operator fun invoke(reportData: ReportData, out: OutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = MARGIN
        canvas.drawText("Transactify — Rapport", MARGIN, y, titlePaint())
        y += 24f
        canvas.drawText("Période : ${reportData.period.displayLabel}", MARGIN, y, textPaint())
        y += 28f

        canvas.drawText("Total reçu : ${MoneyFormatter.format(reportData.totalReceived)}", MARGIN, y, textPaint())
        y += 18f
        canvas.drawText("Total envoyé : ${MoneyFormatter.format(reportData.totalSent)}", MARGIN, y, textPaint())
        y += 18f
        canvas.drawText("Bénéfice total : ${MoneyFormatter.format(reportData.totalProfit)}", MARGIN, y, boldPaint())
        y += 32f

        canvas.drawText("Par opérateur", MARGIN, y, boldPaint())
        y += 20f
        reportData.byOperator.forEach { breakdown ->
            canvas.drawText(operatorLine(breakdown), MARGIN, y, textPaint())
            y += 18f
        }
        y += 20f

        canvas.drawText(
            "${reportData.transactions.size} transaction(s) — généré le ${DateUtils.formatDateTime(System.currentTimeMillis())}",
            MARGIN,
            y,
            textPaint(),
        )

        document.finishPage(page)
        document.writeTo(out)
        document.close()
    }

    private fun operatorLine(breakdown: OperatorBreakdown): String =
        "${breakdown.operator.storageValue} : ${breakdown.transactionCount} tx, " +
            "${MoneyFormatter.format(breakdown.totalVolume)}, bénéfice ${MoneyFormatter.format(breakdown.totalProfit)}"

    private fun titlePaint() = Paint().apply { textSize = 20f; isFakeBoldText = true }
    private fun boldPaint() = Paint().apply { textSize = 13f; isFakeBoldText = true }
    private fun textPaint() = Paint().apply { textSize = 12f }

    private companion object {
        const val PAGE_WIDTH = 595 // A4 à 72dpi
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
    }
}
