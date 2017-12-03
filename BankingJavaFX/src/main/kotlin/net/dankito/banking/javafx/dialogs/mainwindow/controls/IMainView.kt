package net.dankito.banking.javafx.dialogs.mainwindow.controls

import net.dankito.banking.model.BankInfo


interface IMainView {

    fun showAccounts(bankInfos: List<BankInfo>)

}