package net.dankito.banking.javafx.dialogs.mainwindow

import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.persistence.JsonAccountDataPersister
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File

class MainWindowControllerTest {

    private val underTest = MainWindowController()

    private val accountDataPersister = JsonAccountDataPersister()


    @Test
    fun mergeEntries_AllEntriesMatch() {
        val newEntries = accountDataPersister.getPersistedAccountData(File(javaClass.classLoader.getResource("test_account_data.json").toURI()))
        val previousEntries = accountDataPersister.getPersistedAccountData(File(javaClass.classLoader.getResource("test_account_data.json").toURI()))
        val countEntriesBefore = newEntries.entries.size

        val mergeEntries = underTest.javaClass.getDeclaredMethod("mergeEntries", AccountingEntries::class.java, AccountingEntries::class.java)
        mergeEntries.isAccessible = true

        val testResult = mergeEntries.invoke(underTest, previousEntries, newEntries) as AccountingEntries

        assertThat(countEntriesBefore, `is`(testResult.entries.size))
        assertThat(previousEntries.entries.size, `is`(testResult.entries.size))

        // assert correct sort order
        assertThat(testResult.entries[0].bookingDate > testResult.entries[testResult.entries.size - 1].bookingDate, `is`(true))
        assertThat(testResult.entries[0], `is`(newEntries.entries[0]))
        assertThat(testResult.entries[testResult.entries.size - 1], `is`(newEntries.entries[newEntries.entries.size - 1]))
    }

    @Test
    fun mergeEntries_HasTwoOlderEntries() {
        val newEntries = accountDataPersister.getPersistedAccountData(File(javaClass.classLoader.getResource("test_account_data.json").toURI()))
        val previousEntries = accountDataPersister.getPersistedAccountData(File(javaClass.classLoader.getResource("test_account_data_with_older_entries.json").toURI()))
        val newEntriesBackup = ArrayList(newEntries.entries) // newEntries changes during merge

        val mergeEntries = underTest.javaClass.getDeclaredMethod("mergeEntries", AccountingEntries::class.java, AccountingEntries::class.java)
        mergeEntries.isAccessible = true

        val testResult = mergeEntries.invoke(underTest, previousEntries, newEntries) as AccountingEntries

        assertThat(newEntriesBackup.size + 2, `is`(testResult.entries.size))
        assertThat(testResult.entries[0].bookingDate > testResult.entries[testResult.entries.size - 1].bookingDate, `is`(true))


        // assert correct sort order
        assertThat(testResult.entries[0], `is`(newEntries.entries[0]))
        assertThat(testResult.entries[testResult.entries.size - 3], `is`(newEntriesBackup[newEntriesBackup.size - 1]))
        assertThat(testResult.entries[testResult.entries.size - 2], `is`(previousEntries.entries[previousEntries.entries.size - 2]))
        assertThat(testResult.entries[testResult.entries.size - 1], `is`(previousEntries.entries[previousEntries.entries.size - 1]))
    }

}