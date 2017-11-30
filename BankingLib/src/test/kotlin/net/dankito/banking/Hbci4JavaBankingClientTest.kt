package net.dankito.banking

import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.GetAccountsResult
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class Hbci4JavaBankingClientTest {

    private lateinit var underTest: Hbci4JavaBankingClient


    @Before
    fun setUp() {
        underTest = Hbci4JavaBankingClient("", "", "") // set your account details here
    }


    @Test
    fun testGetAccounts() {
        val result = AtomicReference<GetAccountsResult?>()
        val countDownLatch = CountDownLatch(1)

        underTest.getAccountsAsync { getAccountsResult ->
            result.set(getAccountsResult)
            countDownLatch.countDown()
        }

        try { countDownLatch.await() } catch(ignored: Exception) { }

        val getAccountsResult = result.get()
        assertThat(getAccountsResult, notNullValue())
        assertThat(getAccountsResult?.successful, `is`(true))
        assertThat(getAccountsResult?.error, nullValue())
        assertThat(getAccountsResult?.accounts?.size, `is`(not(0)))
    }

    @Test
    fun testGetUmsaetze() {
        val result = AtomicReference<AccountingEntries?>()
        val countDownLatch = CountDownLatch(1)

        underTest.getAccountsAsync { getAccountsResult ->
            if(getAccountsResult.successful == false) {
                countDownLatch.countDown()
            }
            else {
                underTest.getAccountingEntriesAsync(getAccountsResult.accounts[0]) {
                    result.set(it)
                    countDownLatch.countDown()
                }
            }
        }

        try { countDownLatch.await() } catch(ignored: Exception) { }

        val accountingEntries = result.get()

        assertThat(accountingEntries, notNullValue())
        assertThat(accountingEntries?.saldo, notNullValue())
        assertThat(accountingEntries?.saldo?.longValue, `is`(not(0L)))
        assertThat(accountingEntries?.entries, notNullValue())
        assertThat(accountingEntries?.entries?.size, `is`(not(0)))
    }

}