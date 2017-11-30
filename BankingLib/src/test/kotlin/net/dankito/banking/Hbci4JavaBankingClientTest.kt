package net.dankito.banking

import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class Hbci4JavaBankingClientTest {

    private lateinit var underTest: Hbci4JavaBankingClient


    @Before
    fun setUp() {
        underTest = Hbci4JavaBankingClient("", "", "") // set your account details here
    }


    @Test
    fun testGetAccounts() {
        val getAccountsResult = underTest.getAccounts()

        assertThat(getAccountsResult.successful, `is`(true))
        assertThat(getAccountsResult.error, nullValue())
        assertThat(getAccountsResult.accounts.size, `is`(not(0)))
    }

    @Test
    fun testGetUmsaetze() {
        val getAccountsResult = underTest.getAccounts()
        val accountingEntries = underTest.getAccountingEntries(getAccountsResult.accounts[0])

        assertThat(accountingEntries, notNullValue())
        assertThat(accountingEntries.saldo?.longValue, `is`(not(0L)))
        assertThat(accountingEntries.entries.size, `is`(not(0)))
    }

}