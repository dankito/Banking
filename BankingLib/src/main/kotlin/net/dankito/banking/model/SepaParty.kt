package net.dankito.banking.model


class SepaParty(val name: String,
                val iban: String,
                val bic: String,
                val country: String = "DE",
                // these are not necessarily needed, they are just additional information about the sender
                val blz: String = "",
                val kontoNummer: String = ""
) {

    override fun toString(): String {
        return "$name: $iban"
    }

}