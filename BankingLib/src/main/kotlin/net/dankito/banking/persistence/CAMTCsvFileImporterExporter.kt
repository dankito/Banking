package net.dankito.banking.persistence

import net.dankito.banking.model.Account
import net.dankito.banking.model.AccountingEntry
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import org.kapott.hbci.structures.Konto
import org.kapott.hbci.structures.Value
import org.slf4j.LoggerFactory
import java.io.*
import java.math.BigDecimal
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class CAMTCsvFileImporterExporter : IImporter, IExporter {

    companion object {
        val Headers = Arrays.asList("Auftragskonto", "Buchungstag", "Valutadatum", "Buchungstext", "Verwendungszweck", "Glaeubiger ID", "Mandatsreferenz",
                "Kundenreferenz (End-to-End)", "Sammlerreferenz", "Lastschrift Ursprungsbetrag", "Auslagenersatz Ruecklastschrift", "Beguenstigter/Zahlungspflichtiger",
                "Kontonummer/IBAN", "BIC (SWIFT-Code)", "Betrag", "Waehrung", "Info").toTypedArray()

        val DateFormat = SimpleDateFormat("dd.MM.yy")
        val NumberFormat = java.text.NumberFormat.getNumberInstance()

        private val log = LoggerFactory.getLogger(CAMTCsvFileImporterExporter::class.java)
    }


    override fun exportAccountEntriesAsync(destinationFile: File, account: Account, entries: List<AccountingEntry>, done: () -> Unit) {
        thread {
            exportAccountEntries(destinationFile, account, entries)
            done()
        }
    }

    override fun exportAccountEntries(destinationFile: File, account: Account, entries: List<AccountingEntry>) {
        val csvFileFormat = CSVFormat.EXCEL.withHeader(*Headers)
        val fileWriter = OutputStreamWriter(FileOutputStream(destinationFile), Charset.forName("UTF-8").newEncoder())
        val csvFilePrinter = CSVPrinter(fileWriter, csvFileFormat)

        entries.forEach { entry ->
            writeEntry(csvFilePrinter, account, entry)
        }


        fileWriter.flush()
        csvFilePrinter.close()
    }

    private fun writeEntry(csvFilePrinter: CSVPrinter, account: Account, entry: AccountingEntry) {
        csvFilePrinter.printRecord(account.info.number, DateFormat.format(entry.bookingDate), DateFormat.format(entry.valutaDate),
                entry.type, getUsage(entry), entry.creditorIdentifier, entry.mandatsreferenz, entry.endToEndReference,
                entry.kundenreferenz, entry.originalAmount, entry.compensationAmount, // TODO: what to pass here?
                getBeguenstigter(entry), entry.other.iban, entry.other.bic,
                NumberFormat.format(entry.value.bigDecimalValue), entry.value.curr, "Umsatz gebucht") // TODO: where to get that value from?
    }

    private fun getBeguenstigter(entry: AccountingEntry): String {
        entry.abweichenderAuftraggeber?.let { abweichenderAuftraggeber ->
            if(entry.type == "AUSZAHLUNG" ) {
                return abweichenderAuftraggeber
            }
        }

        return entry.other.name + (entry.other.name2 ?: "")
    }

    private fun getUsage(entry: AccountingEntry): String {
        entry.sepaVerwendungszweck?.let { return it }

        if(entry.parsedUsages.size > 0) {
            return entry.parsedUsages[0]
        }
        else {
            return entry.getUsage1()
        }
    }


    override fun importEntriesAsync(file: File, done: (List<AccountingEntry>) -> Unit) {
        thread {
            done(importEntries(file))
        }
    }

    override fun importEntries(file: File): List<AccountingEntry> {
        val csvFileFormat = CSVFormat.EXCEL.withFirstRecordAsHeader()
        val fileReader = InputStreamReader(FileInputStream(file), Charset.forName("UTF-8").newDecoder())

        val records = csvFileFormat.parse(fileReader)

        val entries = ArrayList<AccountingEntry>()

        records.forEach { record ->
            try {
                entries.add(mapRecordToEntry(record))
            } catch(e: Exception) { log.error("Could not map record $record to AcountEntry", e) }
        }

        fileReader.close()

        return entries
    }

    private fun mapRecordToEntry(record: CSVRecord): AccountingEntry {
        val usage = record["Verwendungszweck"]

        // i don't know why it's that complicated but i didn't get any other BigDecimal parsing to work
        val entry = AccountingEntry(Value(BigDecimal.valueOf(NumberFormat.parse(record["Betrag"]).toDouble()), record["Waehrung"]),
                DateFormat.parse(record["Buchungstag"]), DateFormat.parse(record["Valutadatum"]),
                record["Buchungstext"], parseOtherKonto(record), Value(), usage) // TODO: may parse Saldo

        entry.sepaVerwendungszweck = usage
        entry.creditorIdentifier = record["Glaeubiger ID"]
        entry.mandatsreferenz = record["Mandatsreferenz"]
        entry.endToEndReference = record["Kundenreferenz (End-to-End)"]
        entry.kundenreferenz = record["Sammlerreferenz"]
        entry.originalAmount = record["Lastschrift Ursprungsbetrag"]
        entry.compensationAmount = record["Auslagenersatz Ruecklastschrift"]


        return entry
    }

    private fun parseOtherKonto(record: CSVRecord): Konto {
        val other = Konto()

        other.name = record["Beguenstigter/Zahlungspflichtiger"]
        other.iban = record["Kontonummer/IBAN"]
        other.bic = record["BIC (SWIFT-Code)"]

        return other
    }

}