package com.example.appmanagement.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formatter for dd/MM/yyyy dates using the device locale
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

// Safely parse the string into a LocalDate using the shared formatter
fun String?.toDateOrNull(): LocalDate? = try {
    if (this.isNullOrBlank()) null else LocalDate.parse(this, DATE_FORMATTER)
} catch (_: Exception) { null }

// Format the provided date or today's date into the standard string format
fun toDayString(date: LocalDate = LocalDate.now()): String =
    date.format(DATE_FORMATTER)
