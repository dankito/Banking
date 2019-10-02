package net.dankito.banking.tan


class SelectTanProcedure(val procedure: TanProcedure,

                         /**
                          * Name to display to user.
                          * Value provided by bank
                          */
                         val displayName: String,

                         /**
                          * Bank internal code for this TAN procedure.
                          */
                         val procedureCode: String

) {

    override fun toString(): String {
        return "$displayName ($procedure)"
    }

}