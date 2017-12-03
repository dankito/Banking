package net.dankito.banking.persistence

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

abstract class JsonPersisterBase {


    protected val objectMapper = ObjectMapper()


    init {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        // only serialize fields
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    }


    protected open fun saveObjectToFile(destinationFile: File, objectToPersist: Any) {
        val json = objectMapper.writeValueAsString(objectToPersist)

        val writer = BufferedWriter(FileWriter(destinationFile))

        writer.write(json)
        writer.flush()
        writer.close()
    }

    protected open fun <T> deserializePersistedObject(file: File, type: Class<T>): T {
        return objectMapper.readValue<T>(file, type)
    }

    protected open fun <T> deserializePersistedObject(file: File, type: JavaType): T {
        return objectMapper.readValue<T>(file, type)
    }

}