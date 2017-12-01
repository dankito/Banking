package net.dankito.banking.util


class ExceptionHelper {

    fun getInnerException(exception: Exception, maxDepth: Int = 3): Exception {
        var innerException = exception
        var depth = 0

        while(innerException.cause is Exception && depth < maxDepth) {
            innerException = innerException.cause as Exception
            depth++
        }

        return innerException
    }

}