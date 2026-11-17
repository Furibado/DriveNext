package ru.mtuci.drivenext.domain

object RegistrationValidation {
    fun isEmailValid(value: String): Boolean =
        Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.(?:com|ru)$",
            RegexOption.IGNORE_CASE).matches(value.trim())

    fun dateForStorage(value: String, format: String): String =
        java.time.LocalDate.parse(
            value,
            java.time.format.DateTimeFormatter.ofPattern(format)
                .withResolverStyle(java.time.format.ResolverStyle.STRICT)
        ).toString()

    fun passwordsMatch(password: String, confirmation: String): Boolean =
        password.isNotEmpty() && password == confirmation
}
