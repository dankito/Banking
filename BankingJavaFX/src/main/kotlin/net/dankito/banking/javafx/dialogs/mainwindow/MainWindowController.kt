package net.dankito.banking.javafx.dialogs.mainwindow

import javafx.stage.StageStyle
import net.dankito.banking.Hbci4JavaBankingClient
import net.dankito.banking.IBankingClient
import net.dankito.banking.javafx.dialogs.accountdetails.AccountDetailsDialog
import net.dankito.banking.javafx.dialogs.accountingentriesdetails.AccountingEntriesDetailsDialog
import net.dankito.banking.javafx.dialogs.addaccount.AddAccountDialog
import net.dankito.banking.javafx.dialogs.bankdetails.BankDetailsDialog
import net.dankito.banking.javafx.dialogs.cashtransfer.CreateCashTransferDialog
import net.dankito.banking.javafx.dialogs.mainwindow.controls.IMainView
import net.dankito.banking.javafx.dialogs.tan.EnterTanDialog
import net.dankito.banking.model.*
import net.dankito.banking.persistence.IAccountDataPersister
import net.dankito.banking.persistence.IAccountSettingsPersister
import net.dankito.banking.persistence.JsonAccountDataPersister
import net.dankito.banking.persistence.JsonAccountSettingsPersister
import net.dankito.banking.tan.TanData
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread


class MainWindowController : Controller() {

    companion object {
        private const val DataFolderName = "data"
        private val DataFolder = File(DataFolderName)

        private const val AccountsFilename = "accounts.json"
        private val AccountsFile = File(DataFolder, AccountsFilename)

        private val logger = LoggerFactory.getLogger(MainWindowController::class.java) // cannot name it logger as there's already a log named instance variable in Controller class
    }


    var mainView: IMainView? = null
        set(value) {
            field = value

            showAccounts()
        }


    private var clientsForAccounts = ConcurrentHashMap<AccountCredentials, IBankingClient>()

    private var accountsPersister: IAccountSettingsPersister = JsonAccountSettingsPersister()

    private var accountDataPersister: IAccountDataPersister = JsonAccountDataPersister()

    private var bankInfos: MutableList<BankInfo> = ArrayList()

    private val accountEntries = ConcurrentHashMap<Account, AccountingEntries>()


    init {
        thread {
            setup()
        }
    }

    private fun setup() {
        try {
            bankInfos = accountsPersister.getPersistedAccounts(AccountsFile).toMutableList()

            bankInfos.forEach { info ->
                info.accounts.forEach { account ->
                    readPersistedAccountEntries(account)
                }
            }

            showAccounts()
        } catch(e: Exception) {
            logger.error("Could not read persisted accounts", e)
            // TODO: if it's not the first app start: inform user
        }
    }

    private fun readPersistedAccountEntries(account: Account) {
        try {
            val accountingEntries = accountDataPersister.getPersistedAccountData(getAccountEntriesFile(account))
            accountEntries.put(account, accountingEntries)
        } catch(e: Exception) {
            logger.error("Could not read persisted entries for account $account", e)
        }
    }


    fun addAccountAsync(credentials: AccountCredentials, callback: (GetAccountsResult) -> Unit) {
        val client = getClientForAccount(credentials)
        client.getAccountsAsync { result ->
            storeClientIfSuccessful(client, credentials, result)
            retrievedGetAccountsResult(result)

            callback(result)
        }
    }

    private fun retrievedGetAccountsResult(result: GetAccountsResult) {
        result.bankInfo?.let { bankInfo ->
            bankInfos.add(bankInfo)
            accountsPersister.persistAccounts(AccountsFile, bankInfos)

            showAccounts()
        }
    }

    private fun showAccounts() {
        mainView?.showAccounts(bankInfos)
    }


    fun getStoredAccountingEntries(account: Account): AccountingEntries? {
        return accountEntries[account]
    }

    fun getAccountingEntriesAsync(account: Account, callback: (AccountingEntries) -> Unit) {
        val lastRetrievedEntryDate = getDateOfLastRetrievedAccountingEntry(account)

        // never retrieved or didn't retrieve in last 90 days accounting entries for this account -> get all
        if (lastRetrievedEntryDate == null || isOlderThan90Days(lastRetrievedEntryDate)) {
            getAllAccountingEntriesAsync(account, callback)
        }
        else {
            // according to PSD2 entries of last 90 days may be retrieved without second factor -> check if bank supports this
            getAccountingEntriesOfLast90DaysAsync(account, callback)
        }
    }

    fun getAllAccountingEntriesAsync(account: Account, callback: (AccountingEntries) -> Unit) {
        val client = getClientForAccount(account)

        client.getAccountingEntriesAsync(account, null) { result ->
            handleRetrievedAccountingEntries(client, account, result, callback)
        }
    }

    fun getAccountingEntriesOfLast90DaysAsync(account: Account, callback: (AccountingEntries) -> Unit) {
        val client = getClientForAccount(account)

        client.getAccountingEntriesOfLast90DaysAsync(account) { result ->
            handleRetrievedAccountingEntries(client, account, result, callback)
        }
    }

    private fun handleRetrievedAccountingEntries(client: IBankingClient, account: Account, result: AccountingEntries, callback: (AccountingEntries) -> Unit) {
        storeClientIfSuccessful(client, account.credentials, result)
        retrievedAccountingEntries(account, result)

        callback(result)
    }

    private fun retrievedAccountingEntries(account: Account, result: AccountingEntries) {
        if(result.successful) {
            accountEntries.put(account, mergeEntries(account, result))

            accountDataPersister.persistAccountData(getAccountEntriesFile(account), result)
        }
    }

    private fun mergeEntries(account: Account, result: AccountingEntries): AccountingEntries {
        val previousEntries = accountEntries[account]

        if(previousEntries == null) {
            return result
        }
        else {
            return mergeEntries(previousEntries, result)
        }
    }

    private fun mergeEntries(previousEntries: AccountingEntries, newEntries: AccountingEntries): AccountingEntries {
        val newEntriesOldestFirst = sortByDate(newEntries.entries)
        val mutableNewEntries = newEntries.entries.toMutableList()

        for(i in previousEntries.entries.size - 1 downTo 0) { // only check older ones up to the time the first match is found
            val previousEntry = previousEntries.entries[i]
            if(containsEntry(newEntriesOldestFirst, previousEntry) == false) {
                mutableNewEntries.add(previousEntry)
            }
        }

        newEntries.entries = sortByDateDescending(mutableNewEntries)
        return newEntries
    }

    private fun containsEntry(entries: List<AccountingEntry>, entryToCheck: AccountingEntry): Boolean {
        entries.forEach { entry ->
            if(entry.bookingDate == entryToCheck.bookingDate && entry.valutaDate == entryToCheck.valutaDate &&
                    entry.value.bigDecimalValue == entryToCheck.value.bigDecimalValue && entry.value.curr == entryToCheck.value.curr &&
                    entry.usage == entryToCheck.usage &&
                    entry.other.name == entryToCheck.other.name &&
                    entry.other.iban == entryToCheck.other.iban && entry.other.bic == entryToCheck.other.bic &&
                    entry.other.blz == entryToCheck.other.blz && entry.other.number == entryToCheck.other.number) {
                return true
            }
        }

        return false
    }

    private fun getAccountEntriesFile(account: Account): File {
        val filename = "${account.info.number}_${account.info.name + (account.info.name2 ?: "")}_${account.info.type}.json".replace(" ", "_")

        val file = File(File(DataFolder, account.info.blz), filename)
        file.parentFile.mkdirs()

        return file
    }


    fun findBankByIban(account: Account, enteredIban: String): BankInfo? {
        val client = getClientForAccount(account)

        return client.findBankByIban(enteredIban)
    }

    fun transferCashAsync(account: Account, cashTransfer: CashTransfer, callback: (CashTransferResult) -> Unit) {
        val client = getClientForAccount(account)

        client.transferCashAsync(cashTransfer) { result ->
            storeClientIfSuccessful(client, account.credentials, result)

            callback(result)
        }
    }


    private fun getClientForAccount(account: Account): IBankingClient {
        return getClientForAccount(account.credentials)
    }

    private fun getClientForAccount(credentials: AccountCredentials): IBankingClient {
        clientsForAccounts[credentials]?.let { return it }

        val newClient = Hbci4JavaBankingClient(credentials, DataFolder) { data ->
            showEnterTanDialogAndWaitForTanDoNotCallOnUiThread(data)
        }

        return newClient
    }

    private fun storeClientIfSuccessful(client: IBankingClient, credentials: AccountCredentials, result: ResultBase) {
        if(result.successful) {
            clientsForAccounts.put(credentials, client)
        }
    }


    private fun getDateOfLastRetrievedAccountingEntry(account: Account): Date? {
        getStoredAccountingEntries(account)?.let { storedEntries ->
            return getLastAccountingEntryDate(storedEntries.entries)
        }

        return null
    }

    private fun getLastAccountingEntryDate(entries: List<AccountingEntry>): Date? {
        return sortByDateDescending(entries).firstOrNull()?.bookingDate
    }

    private fun isOlderThan90Days(date: Date): Boolean {
        return date.time < (Date().time - Hbci4JavaBankingClient.NinetyDaysInMilliseconds)
    }

    private fun sortByDate(entries: List<AccountingEntry>): List<AccountingEntry> {
        return entries.sortedBy { it.bookingDate }
    }

    private fun sortByDateDescending(entries: List<AccountingEntry>): List<AccountingEntry> {
        return entries.sortedByDescending { it.bookingDate }
    }


    // TODO: move to router
    fun showAddAccountDialog() {
        find(AddAccountDialog::class.java, mapOf(AddAccountDialog::controller to this)).show(FX.messages["add.account.dialog.title"], owner = primaryStage)
    }

    fun showBankDetailsDialog(bankInfo: BankInfo) {
        find(BankDetailsDialog::class, mapOf(BankDetailsDialog::bankInfo to bankInfo))
                .show(messages["bank.details.name.title"], stageStyle = StageStyle.UTILITY, owner = primaryStage)
    }

    fun showAccountDetailsDialog(account: Account) {
        find(AccountDetailsDialog::class, mapOf(AccountDetailsDialog::account to account))
                .show(messages["account.details.name.title"], stageStyle = StageStyle.UTILITY, owner = primaryStage)
    }

    fun showAccountingEntriesDetailsDialog(entry: AccountingEntry) {
        find(AccountingEntriesDetailsDialog::class, mapOf(AccountingEntriesDetailsDialog::entry to entry))
                .show(messages["accounting.entries.details.title"], stageStyle = StageStyle.UTILITY, owner = primaryStage)
    }

    fun showCreateCashTransferDialog(account: Account, remitteeName: String = "", remitteeIban: String = "") {
        find(CreateCashTransferDialog::class, mapOf(CreateCashTransferDialog::controller to this,
                CreateCashTransferDialog::account to account,
                CreateCashTransferDialog::selectedRemitteeName to remitteeName,
                CreateCashTransferDialog::selectedRemitteeIban to remitteeIban))
                .show(messages["create.cash.transfer.title"], stageStyle = StageStyle.UTILITY, owner = primaryStage)
    }

    fun showEnterTanDialogAndWaitForTanDoNotCallOnUiThread(data: TanData): String? {
        val enteredTan = AtomicReference<String>(null)
        val countDownLatch = CountDownLatch(1)

        runLater {
            showEnterTanDialog(data) {
                enteredTan.set(it)

                countDownLatch.countDown()
            }
        }

        try { countDownLatch.await() } catch (ignored: Exception) { }

        return enteredTan.get()
    }

    fun showEnterTanDialog(data: TanData, enteredTan: (String?) -> Unit) {
        // didn't make it to pass enteredTan callback via params Map -> are therefor passed as constructor parameters
        EnterTanDialog(data, enteredTan).show(FX.messages["enter.tan.dialog.title"], owner = primaryStage)
    }

}