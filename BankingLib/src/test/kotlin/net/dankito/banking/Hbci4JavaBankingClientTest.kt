package net.dankito.banking

import net.dankito.banking.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class Hbci4JavaBankingClientTest {

    companion object {
        // set your account details here
        private const val Bankleitzahl = ""
        private const val CustomerId = ""
        private const val Pin = ""

        private const val NameForCashTransfer = ""
        private const val IbanForCashTransfer = ""
        private const val BicForCashTransfer = ""
    }


    private lateinit var underTest: Hbci4JavaBankingClient


    @Before
    fun setUp() {
        underTest = Hbci4JavaBankingClient(AccountCredentials(Bankleitzahl, CustomerId, Pin), File("testDir"))
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
        assertThat(getAccountsResult).isNotNull
        assertThat(getAccountsResult?.successful).isTrue()
        assertThat(getAccountsResult?.error).isNull()
        assertThat(getAccountsResult?.bankInfo).isNotNull
        assertThat(getAccountsResult?.bankInfo?.accounts).isNotEmpty
    }

    @Test
    fun testGetUmsaetze() {
        val result = AtomicReference<AccountingEntries?>()
        val countDownLatch = CountDownLatch(1)

        underTest.getAccountsAsync { getAccountsResult ->
            val bankInfo = getAccountsResult.bankInfo
            if(getAccountsResult.successful == false || bankInfo == null) {
                countDownLatch.countDown()
            }
            else {
                underTest.getAccountingEntriesAsync(bankInfo.accounts[0], null) {
                    result.set(it)
                    countDownLatch.countDown()
                }
            }
        }

        try { countDownLatch.await() } catch(ignored: Exception) { }

        val accountingEntries = result.get()

        assertThat(accountingEntries).isNotNull
        assertThat(accountingEntries?.saldo).isNotNull
        assertThat(accountingEntries?.saldo?.longValue).isNotEqualTo(0L)
        assertThat(accountingEntries?.entries).isNotNull
        assertThat(accountingEntries?.entries).isNotEmpty
    }

    @Test
    fun testCashTransfer() {
        // given
        val source = SepaParty(NameForCashTransfer, IbanForCashTransfer, BicForCashTransfer)
        val destination = SepaParty(NameForCashTransfer, IbanForCashTransfer, BicForCashTransfer)

        // when
        // let's transfer 1 Cent to same account
        val result = underTest.transferCash(CashTransfer(source, destination, BigDecimal("0.01"), "Test cash transfer"))

        // then
        assertThat(result).isNotNull
        assertThat(result.successful).isTrue()
        assertThat(result.message).isNotBlank()
        assertThat(result.exception).isNull()
    }

}