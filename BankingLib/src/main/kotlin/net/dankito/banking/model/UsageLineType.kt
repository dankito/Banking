package net.dankito.banking.model


enum class UsageLineType {

    EREF, // Ende-zu-Ende Referenz
    KREF, // Kundenreferenz
    MREF, // Mandatsreferenz
    CRED, // Creditor Identifier
    DEBT, // für Originators Identification Code
    COAM, // Compensation Amount
    OAMT, // Original Amount
    SVWZ, // für SEPA-Verwendungszweck
    ABWA, // für Abweichender Auftraggeber
    ABWE, // Abweichender Zahlungsempfänger

    NoSpecialType,
    ContinuationFromLastLine

}