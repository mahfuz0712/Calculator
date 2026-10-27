package com.mahfuz0712.calculator.engine

import kotlin.math.*

class CalcException(message: String) : Exception(message)

object ExprEngine {

    private val FUNCS = listOf(
        "asin", "acos", "atan", "sinh", "cosh", "tanh", "sin", "cos", "tan",
        "log2", "ln", "log", "sqrt", "cbrt", "exp", "abs", "floor", "ceil",
        "round", "fact", "pow", "min", "max", "mod"
    )

    fun evaluate(src: String, degrees: Boolean, vars: Map<String, Double> = emptyMap()): Double =
        Parser(src, degrees, vars).run()

    fun format(v: Double): String {
        if (v.isNaN()) throw CalcException("Math error")
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        if (v == floor(v) && abs(v) < 1e15) return v.toLong().toString()
        val bd = java.math.BigDecimal(v).round(java.math.MathContext(12)).stripTrailingZeros()
        val a = abs(v)
        return if (a >= 1e-6 && a < 1e12) bd.toPlainString() else bd.toString()
    }

    private class Parser(src: String, val useDeg: Boolean, val vars: Map<String, Double>) {
        private val s = src.replace(" ", "")
        private var i = 0

        fun run(): Double {
            if (s.isEmpty()) throw CalcException("Empty expression")
            val v = expr()
            if (i < s.length) throw CalcException("Unexpected '${s[i]}'")
            return v
        }

        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

        private fun expr(): Double {
            var v = term()
            while (true) {
                v = when (peek()) {
                    '+' -> { i++; v + term() }
                    '-', '−' -> { i++; v - term() }
                    else -> break
                }
            }
            return v
        }

        private fun term(): Double {
            var v = unary()
            while (true) {
                val c = peek()
                v = when {
                    c == '*' || c == '×' -> { i++; v * unary() }
                    c == '/' || c == '÷' -> {
                        i++
                        val d = unary()
                        if (d == 0.0) throw CalcException("Division by zero")
                        v / d
                    }
                    c == '(' || c.isLetter() || c == '√' -> v * unary()
                    else -> break
                }
            }
            return v
        }

        private fun unary(): Double = when (peek()) {
            '-', '−' -> { i++; -unary() }
            '+' -> { i++; unary() }
            else -> power()
        }

        private fun power(): Double {
            val b = postfix()
            if (peek() == '^') {
                i++
                return b.pow(unary())
            }
            return b
        }

        private fun postfix(): Double {
            var v = primary()
            while (true) {
                when (peek()) {
                    '!' -> { i++; v = fact(v) }
                    '%' -> { i++; v /= 100.0 }
                    else -> break
                }
            }
            return v
        }

        private fun primary(): Double {
            val c = peek()
            return when {
                c.isDigit() || c == '.' -> number()
                c == '(' -> { i++; val v = expr(); close(); v }
                c == '√' -> {
                    i++
                    val a = unary()
                    if (a < 0) throw CalcException("Invalid input for √")
                    sqrt(a)
                }
                c.isLetter() -> ident()
                c == '\u0000' -> throw CalcException("Incomplete expression")
                else -> throw CalcException("Unexpected '$c'")
            }
        }

        private fun close() {
            if (peek() == ')') i++
            else if (i < s.length) throw CalcException("Expected ')'")
        }

        private fun number(): Double {
            val st = i
            while (peek().isDigit() || peek() == '.') i++
            if (peek() == 'E') {
                val save = i
                i++
                if (peek() == '+' || peek() == '-') i++
                if (peek().isDigit()) {
                    while (peek().isDigit()) i++
                } else {
                    i = save
                }
            }
            return s.substring(st, i).toDoubleOrNull() ?: throw CalcException("Bad number")
        }

        private fun ident(): Double {
            val start = i
            var j = i
            while (j < s.length && s[j].isLetter()) j++
            val word = s.substring(start, j).lowercase()
            val known = FUNCS + listOf("pi", "π", "e") + vars.keys
            val match = known.filter { word.startsWith(it) }.maxByOrNull { it.length }
                ?: throw CalcException("Unknown '$word'")
            i = start + match.length
            vars[match]?.let { return it }
            if (match == "pi" || match == "π") return PI
            if (match == "e") return E
            val argList = if (peek() == '(') { i++; args() } else listOf(unary())
            return call(match, argList)
        }

        private fun args(): List<Double> {
            val l = mutableListOf<Double>()
            if (peek() == ')') { i++; return l }
            while (true) {
                l.add(expr())
                if (peek() == ',') { i++; continue }
                break
            }
            close()
            return l
        }

        private fun toRad(v: Double) = if (useDeg) Math.toRadians(v) else v
        private fun fromRad(v: Double) = if (useDeg) Math.toDegrees(v) else v
        private fun clean(v: Double) = if (abs(v) < 1e-12) 0.0 else v

        private fun fact(v: Double): Double {
            if (v < 0 || v != floor(v) || v > 170) throw CalcException("Invalid factorial input")
            var r = 1.0
            for (k in 2..v.toInt()) r *= k
            return r
        }

        private fun call(n: String, a: List<Double>): Double {
            fun one(): Double {
                if (a.size != 1) throw CalcException("$n() needs 1 value")
                return a[0]
            }
            fun two(): Pair<Double, Double> {
                if (a.size != 2) throw CalcException("$n() needs 2 values")
                return a[0] to a[1]
            }
            return when (n) {
                "sin" -> clean(sin(toRad(one())))
                "cos" -> clean(cos(toRad(one())))
                "tan" -> {
                    val r = toRad(one())
                    if (abs(cos(r)) < 1e-12) throw CalcException("tan undefined")
                    clean(tan(r))
                }
                "asin" -> { val v = one(); if (abs(v) > 1.0) throw CalcException("Domain error"); fromRad(asin(v)) }
                "acos" -> { val v = one(); if (abs(v) > 1.0) throw CalcException("Domain error"); fromRad(acos(v)) }
                "atan" -> fromRad(atan(one()))
                "sinh" -> sinh(one())
                "cosh" -> cosh(one())
                "tanh" -> tanh(one())
                "ln" -> { val v = one(); if (v <= 0) throw CalcException("Domain error"); ln(v) }
                "log" -> { val v = one(); if (v <= 0) throw CalcException("Domain error"); log10(v) }
                "log2" -> { val v = one(); if (v <= 0) throw CalcException("Domain error"); log2(v) }
                "sqrt" -> { val v = one(); if (v < 0) throw CalcException("Domain error"); sqrt(v) }
                "cbrt" -> cbrt(one())
                "exp" -> exp(one())
                "abs" -> abs(one())
                "floor" -> floor(one())
                "ceil" -> ceil(one())
                "round" -> floor(one() + 0.5)
                "fact" -> fact(one())
                "pow" -> { val (x, y) = two(); x.pow(y) }
                "mod" -> {
                    val (x, y) = two()
                    if (y == 0.0) throw CalcException("Division by zero")
                    x % y
                }
                "min" -> a.minOrNull() ?: throw CalcException("min() needs values")
                "max" -> a.maxOrNull() ?: throw CalcException("max() needs values")
                else -> throw CalcException("Unknown function '$n'")
            }
        }
    }
}
