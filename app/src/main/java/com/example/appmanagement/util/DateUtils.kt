package com.example.appmanagement.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

fun String?.toDateOrNull(): LocalDate? = try {
    if (this.isNullOrBlank()) null else LocalDate.parse(this, DATE_FORMATTER)
} catch (_: Exception) { null }

fun toDayString(date: LocalDate = LocalDate.now()): String =
    date.format(DATE_FORMATTER)
