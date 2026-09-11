package com.tinah.transactify.domain.usecase

import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.TransactionType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExportReportToExcelUseCaseTest {

    private val useCase = ExportReportToExcelUseCase()

    private fun item(amount: Double, reference: String?) = TransactionItem(
        id = 1,
        operator = OperatorType.ORANGE_MONEY,
        type = TransactionType.RECU,
        amount = amount,
        phoneNumber = "+26132000000",
        reference = reference,
        timestamp = 1_000L,
        bonusAmount = 0.0,
        bonusLinked = false,
        profit = amount * 0.03,
    )

    @Test
    fun `ecrit une ligne d'en-tete et une ligne par transaction`() {
        val reportData = ReportData(
            period = ReportPeriod.ALL_TIME,
            transactions = listOf(item(10_000.0, "R1"), item(20_000.0, null)),
            totalReceived = 30_000.0,
            totalSent = 0.0,
            totalProfit = 900.0,
            byOperator = emptyList(),
            dailyProfit = emptyList(),
        )

        val out = ByteArrayOutputStream()
        useCase(reportData, out)

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertEquals("Date", sheet.getRow(0).getCell(0).stringCellValue)
            assertEquals(3, sheet.physicalNumberOfRows) // en-tête + 2 transactions

            val firstRow = sheet.getRow(1)
            assertEquals("Orange Money", firstRow.getCell(1).stringCellValue)
            assertEquals(10_000.0, firstRow.getCell(3).numericCellValue, 0.0)

            // reference null -> cellule vide plutôt qu'une exception
            assertEquals("", sheet.getRow(2).getCell(6).stringCellValue)
        }
    }
}
