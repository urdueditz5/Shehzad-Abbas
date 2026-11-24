package com.example.features.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.data.models.UserNote
import com.example.data.models.UserTodo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

object ToolCalculators {
    private class ExprParser(private val clean: String) {
        private var pos = 0

        fun parseAll(): Double {
            val result = parseExpr()
            require(pos == clean.length) { "Invalid expression syntax" }
            return result
        }

        private fun parseExpr(): Double {
            var value = parseTerm()
            while (pos < clean.length && (clean[pos] == '+' || clean[pos] == '-')) {
                val op = clean[pos++]
                val rhs = parseTerm()
                value = if (op == '+') value + rhs else value - rhs
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (pos < clean.length && (clean[pos] == '*' || clean[pos] == '/')) {
                val op = clean[pos++]
                val rhs = parseFactor()
                if (op == '/') {
                    require(rhs != 0.0) { "Division by zero" }
                    value /= rhs
                } else {
                    value *= rhs
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            if (pos < clean.length && clean[pos] == '-') {
                pos++
                return -parseFactor()
            }
            if (pos < clean.length && clean[pos] == '+') {
                pos++
                return parseFactor()
            }
            if (pos < clean.length && clean[pos] == '(') {
                pos++
                val inside = parseExpr()
                require(pos < clean.length && clean[pos] == ')') { "Mismatched parentheses" }
                pos++
                return inside
            }
            val start = pos
            while (pos < clean.length && (clean[pos].isDigit() || clean[pos] == '.')) pos++
            require(pos > start) { "Unexpected token" }
            return clean.substring(start, pos).toDouble()
        }
    }

    fun evaluateArithmetic(expression: String): Result<Double> {
        val clean = expression.replace(" ", "").replace("×", "*").replace("÷", "/")
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Enter an expression"))
        return runCatching { ExprParser(clean).parseAll() }
    }

    // Supports Pakistan land measurements (1 Marla = 272.25 Sq.Ft standard Lahore/Pakistan) + Metric/Imperial
    fun convertUnit(value: Double, fromUnit: String, toUnit: String): Double {
        val areaToSqFt = mapOf(
            "Sq.Ft" to 1.0,
            "Marla (272.25)" to 272.25,
            "Marla (225)" to 225.0,
            "Kanal (20 Marla)" to 5445.0,
            "Sq.Yd (Gaz)" to 9.0,
            "Sq.Meter" to 10.7639104
        )
        val lengthToMeters = mapOf(
            "Meter" to 1.0,
            "Kilometer" to 1000.0,
            "Centimeter" to 0.01,
            "Foot" to 0.3048,
            "Inch" to 0.0254,
            "Mile" to 1609.344
        )
        val massToKg = mapOf(
            "Kilogram" to 1.0,
            "Gram" to 0.001,
            "Maund (40 Kg)" to 40.0,
            "Pound" to 0.45359237
        )
        return when {
            fromUnit in areaToSqFt && toUnit in areaToSqFt ->
                value * areaToSqFt.getValue(fromUnit) / areaToSqFt.getValue(toUnit)
            fromUnit in lengthToMeters && toUnit in lengthToMeters ->
                value * lengthToMeters.getValue(fromUnit) / lengthToMeters.getValue(toUnit)
            fromUnit in massToKg && toUnit in massToKg ->
                value * massToKg.getValue(fromUnit) / massToKg.getValue(toUnit)
            fromUnit == "Celsius" && toUnit == "Fahrenheit" -> (value * 9.0 / 5.0) + 32.0
            fromUnit == "Fahrenheit" && toUnit == "Celsius" -> (value - 32.0) * 5.0 / 9.0
            else -> value
        }
    }

    fun calculateAge(birthDateStr: String): Result<String> {
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val dobDate = sdf.parse(birthDateStr.trim()) ?: error("Use YYYY-MM-DD format")
            val dob = Calendar.getInstance().apply { time = dobDate }
            val today = Calendar.getInstance()
            require(!dob.after(today)) { "Birth date cannot be in the future" }

            var years = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH)
            var days = today.get(Calendar.DAY_OF_MONTH) - dob.get(Calendar.DAY_OF_MONTH)
            if (days < 0) {
                months -= 1
                val prevMonth = today.clone() as Calendar
                prevMonth.add(Calendar.MONTH, -1)
                days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            if (months < 0) {
                years -= 1
                months += 12
            }
            "$years years, $months months, $days days"
        }
    }

    fun daysBetween(date1Str: String, date2Str: String): Result<Long> {
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val d1 = sdf.parse(date1Str.trim()) ?: error("Invalid first date (use YYYY-MM-DD)")
            val d2 = sdf.parse(date2Str.trim()) ?: error("Invalid second date (use YYYY-MM-DD)")
            val diffMs = abs(d2.time - d1.time)
            diffMs / (1000L * 60L * 60L * 24L)
        }
    }

    fun generatePassword(length: Int, includeSymbols: Boolean, includeDigits: Boolean): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"
        val digits = "23456789"
        val symbols = "@#$%&*!?"
        val pool = buildString {
            append(letters)
            if (includeDigits) append(digits)
            if (includeSymbols) append(symbols)
        }
        val rng = SecureRandom()
        return (1..length.coerceIn(8, 32))
            .map { pool[rng.nextInt(pool.length)] }
            .joinToString("")
    }
}

class ToolsViewModel(
    private val deps: ApnaDependencies,
    private val currentUserId: String
) : ViewModel() {

    val notes: StateFlow<List<UserNote>> = deps.toolsRepository.observeNotes(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "users/$currentUserId/notes")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val todos: StateFlow<List<UserTodo>> = deps.toolsRepository.observeTodos(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "users/$currentUserId/todos")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addNote(title: String, content: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(deps.toolsRepository.addNote(title, content))
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            deps.toolsRepository.deleteNote(noteId)
        }
    }

    fun addTodo(title: String, reminderLabel: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(deps.toolsRepository.addTodo(title, reminderLabel))
        }
    }

    fun toggleTodo(todoId: String, completed: Boolean) {
        viewModelScope.launch {
            deps.toolsRepository.toggleTodo(todoId, completed)
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            deps.toolsRepository.deleteTodo(todoId)
        }
    }
}
