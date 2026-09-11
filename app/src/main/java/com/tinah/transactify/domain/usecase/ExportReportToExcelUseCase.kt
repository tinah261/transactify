package com.tinah.transactify.domain.usecase

import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.utils.DateUtils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

/**
 * Exporte le détail des transactions de [ReportData] dans un classeur Excel
 * (une feuille, une ligne par transaction). Ne touche pas à l'UI ni au
 * système de fichiers Android — [out] est fourni par l'appelant (Fragment),
 * ce qui rend cette classe testable en JVM pur (POI n'a aucune dépendance
 * Android).
 */
class ExportReportToExcelUseCase {

    operator fun invoke(reportData: ReportData, out: OutputStream) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet(SHEET_NAME)
            writeHeader(sheet.createRow(0))
            reportData.transactions.forEachIndexed { index, item ->
                writeTransaction(sheet.createRow(index + 1), item)
            }
            (0 until COLUMN_TITLES.size).forEach { sheet.autoSizeColumn(it) }
            workbook.write(out)
        }
    }

    private fun writeHeader(row: Row) {
        COLUMN_TITLES.forEachIndexed { index, title -> row.createCell(index).setCellValue(title) }
    }

    private fun writeTransaction(row: Row, item: TransactionItem) {
        row.createCell(0).setCellValue(DateUtils.formatDateTime(item.timestamp))
        row.createCell(1).setCellValue(item.operator.storageValue)
        row.createCell(2).setCellValue(item.type.displayName)
        row.createCell(3).setCellValue(item.amount)
        row.createCell(4).setCellValue(item.profit)
        row.createCell(5).setCellValue(item.phoneNumber)
        row.createCell(6).setCellValue(item.reference.orEmpty())
        row.createCell(7).setCellValue(if (item.bonusLinked) item.bonusAmount else 0.0)
    }

    private companion object {
        const val SHEET_NAME = "Transactions"
        val COLUMN_TITLES = listOf(
            "Date", "Opérateur", "Sens", "Montant (Ar)", "Bénéfice (Ar)", "Client", "Référence", "Bonus (Ar)",
        )
    }
}
