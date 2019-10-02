package net.dankito.banking

import net.dankito.banking.callbacks.HbciClientCallback
import net.dankito.banking.model.AccountCredentials
import net.dankito.banking.tan.SelectTanProcedure
import net.dankito.banking.tan.TanData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.kapott.hbci.callback.HBCICallback
import org.kapott.hbci.passport.HBCIPassport
import org.mockito.Mockito.mock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


class HbciCallbackTest {

    private val passportMock: HBCIPassport = mock(HBCIPassport::class.java)


    @Test
    fun selectTanProcedure() {

        // given
        val selectTanProcedureCalled = AtomicBoolean(false)
        val enterTanCalled = AtomicBoolean(false)
        val offeredSelectableTanProcedure = AtomicReference<List<SelectTanProcedure>>(null)

        val underTest = HbciCallback(AccountCredentials("", "", ""), object : HbciClientCallback {

            override fun selectTanProcedure(selectableTanProcedures: List<SelectTanProcedure>): SelectTanProcedure? {
                selectTanProcedureCalled.set(true)

                offeredSelectableTanProcedure.set(selectableTanProcedures)

                return null
            }

            override fun enterTan(tanData: TanData): String? {
                enterTanCalled.set(true)

                return null
            }

        })


        // when
        underTest.callback(passportMock, HBCICallback.NEED_PT_SECMECH, "*** Select a pintan method from the list", HBCICallback.TYPE_TEXT,
                StringBuffer("999:Einschritt-Verfahren|900:iTAN|910:chipTAN manuell|911:chipTAN optisch|912:chipTAN-USB|913:chipTAN-QR|920:smsTAN|921:pushTAN"))

        // then
        assertThat(selectTanProcedureCalled.get()).isTrue()

        assertThat(offeredSelectableTanProcedure.get()).hasSize(6) // Einschritt-Verfahren and iTan should get filtered out

        assertThat(enterTanCalled.get()).isFalse()
    }

}