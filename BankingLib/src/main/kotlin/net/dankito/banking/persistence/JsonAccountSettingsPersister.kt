package net.dankito.banking.persistence

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.dankito.banking.model.BankInfo
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


class JsonAccountSettingsPersister : IAccountSettingsPersister {


    private val objectMapper = ObjectMapper()


    init {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        // only serialize fields
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    }


    override fun persistAccounts(destinationFile: File, bankInfos: List<BankInfo>) {
        val json = objectMapper.writeValueAsString(bankInfos) // TODO: encrypt, passwords as well as file as whole

        val writer = BufferedWriter(FileWriter(destinationFile))

        writer.write(json)
        writer.flush()
        writer.close()
    }

    override fun getPersistedAccounts(file: File): List<BankInfo> {
        return objectMapper.readValue<List<BankInfo>>(file, objectMapper.typeFactory.constructParametricType(List::class.java, BankInfo::class.java))
    }

}