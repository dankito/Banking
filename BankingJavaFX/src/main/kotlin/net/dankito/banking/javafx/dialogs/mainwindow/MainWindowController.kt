package net.dankito.banking.javafx.dialogs.mainwindow

import net.dankito.banking.Hbci4JavaBankingClient
import net.dankito.banking.IBankingClient
import net.dankito.banking.javafx.dialogs.addaccount.AddAccountDialog
import net.dankito.banking.javafx.dialogs.mainwindow.controls.IMainView
import net.dankito.banking.model.*
import net.dankito.banking.persistence.*
import org.kapott.hbci.structures.Value
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


class MainWindowController : Controller() {

    companion object {
        private const val DataFolderName = "data"
        private val DataFolder = File(DataFolderName)

        private const val AccountsFilename = "accounts.json"
        private val AccountsFile = File(DataFolder, AccountsFilename)

        private val logger = LoggerFactory.getLogger(MainWindowController::class.java)
    }


    var mainView: IMainView? = null
        set(value) {
            field = value

            showAccounts()
        }


    private var clientsForAccounts = ConcurrentHashMap<AccountCredentials, IBankingClient>()

    private var accountsPersister: IAccountSettingsPersister = JsonAccountSettingsPersister()

    private var persistedAccountEntriesImporter: IImporter = CAMTCsvFileImporterExporter()

    private var persistedAccountEntriesExporter: IExporter = persistedAccountEntriesImporter as CAMTCsvFileImporterExporter

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
            val entries = persistedAccountEntriesImporter.importEntries(getAccountEntriesFile(account))
            accountEntries.put(account, AccountingEntries(true, Value(), entries))
        } catch(e: Exception) {
            logger.error("Could not read persisted entries for account $account", e)
        }
    }


    fun addAccountAsync(credentials: AccountCredentials, callback: (GetAccountsResult) -> Unit) {
        getClientForAccount(credentials).getAccountsAsync { result ->
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


    fun getAccountingEntriesAsync(account: Account, callback: (AccountingEntries) -> Unit) {
        accountEntries[account]?.let { callback(it) }

        getClientForAccount(account).getAccountingEntriesAsync(account) { result ->
            retrievedAccountingEntries(account, result)

            callback(result)
        }
    }

    private fun retrievedAccountingEntries(account: Account, result: AccountingEntries) {
        if(result.successful) {
            accountEntries.put(account, mergeEntries(account, result))

            persistedAccountEntriesExporter.exportAccountEntriesAsync(getAccountEntriesFile(account), account, result.entries) { }
        }
    }

    private fun mergeEntries(account: Account, result: AccountingEntries): AccountingEntries {
        val previousEntries = accountEntries[account]

        if(previousEntries == null) {
            return result
        }
        else {
            return result // TODO
        }
    }

    private fun getAccountEntriesFile(account: Account): File {
        val filename = "${account.info.number}_${account.info.name + (account.info.name2 ?: "")}_${account.info.type}.csv".replace(" ", "_")

        val file = File(File(DataFolder, account.info.blz), filename)
        file.parentFile.mkdirs()

        return file
    }


    private fun getClientForAccount(account: Account): IBankingClient {
        return getClientForAccount(account.credentials)
    }

    private fun getClientForAccount(credentials: AccountCredentials): IBankingClient {
        clientsForAccounts[credentials]?.let { return it }

        val newClient = Hbci4JavaBankingClient(credentials)
        clientsForAccounts.put(credentials, newClient)

        return newClient
    }


    // TODO: move to router
    fun showAddAccountDialog() {
        find(AddAccountDialog::class.java, mapOf(AddAccountDialog::controller to this)).show()
    }

}