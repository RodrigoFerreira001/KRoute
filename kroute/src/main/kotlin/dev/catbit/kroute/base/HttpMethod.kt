package dev.catbit.kroute.base

data class HttpMethod(val value: String) {
    companion object {
        val Get: HttpMethod = HttpMethod("GET")
        val Post: HttpMethod = HttpMethod("POST")
        val Put: HttpMethod = HttpMethod("PUT")
        val Patch: HttpMethod = HttpMethod("PATCH")
        val Delete: HttpMethod = HttpMethod("DELETE")
        val Head: HttpMethod = HttpMethod("HEAD")
        val Options: HttpMethod = HttpMethod("OPTIONS")
    }
}