package com.mahfuz0712.calculator.engine

import kotlin.math.abs

object MatrixEngine {

    fun parse(text: String): Array<DoubleArray> {
        val rows = text.trim().split(Regex("[;\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (rows.isEmpty()) throw CalcException("Matrix is empty")
        val m = rows.map { r ->
            r.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.map {
                it.toDoubleOrNull() ?: throw CalcException("Bad number: $it")
            }.toDoubleArray()
        }.toTypedArray()
        if (m.any { it.size != m[0].size }) throw CalcException("Rows must have equal length")
        return m
    }

    private fun square(m: Array<DoubleArray>) {
        if (m.any { it.size != m.size }) throw CalcException("Matrix must be square")
    }

    private fun det(m: Array<DoubleArray>): Double {
        square(m)
        val n = m.size
        val a = Array(n) { m[it].clone() }
        var d = 1.0
        for (c in 0 until n) {
            var p = c
            for (r in c + 1 until n) if (abs(a[r][c]) > abs(a[p][c])) p = r
            if (abs(a[p][c]) < 1e-12) return 0.0
            if (p != c) {
                val t = a[p]; a[p] = a[c]; a[c] = t
                d = -d
            }
            d *= a[c][c]
            for (r in c + 1 until n) {
                val f = a[r][c] / a[c][c]
                for (k in c until n) a[r][k] -= f * a[c][k]
            }
        }
        return d
    }

    private fun inv(m: Array<DoubleArray>): Array<DoubleArray> {
        square(m)
        val n = m.size
        val a = Array(n) { i -> DoubleArray(2 * n) { j -> if (j < n) m[i][j] else if (j - n == i) 1.0 else 0.0 } }
        for (c in 0 until n) {
            var p = c
            for (r in c + 1 until n) if (abs(a[r][c]) > abs(a[p][c])) p = r
            if (abs(a[p][c]) < 1e-12) throw CalcException("Matrix is singular")
            val t = a[p]; a[p] = a[c]; a[c] = t
            val pv = a[c][c]
            for (k in 0 until 2 * n) a[c][k] /= pv
            for (r in 0 until n) {
                if (r == c) continue
                val f = a[r][c]
                if (f != 0.0) for (k in 0 until 2 * n) a[r][k] -= f * a[c][k]
            }
        }
        return Array(n) { i -> DoubleArray(n) { j -> a[i][n + j] } }
    }

    private fun mul(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        if (a[0].size != b.size) throw CalcException("A columns must equal B rows")
        return Array(a.size) { i ->
            DoubleArray(b[0].size) { j ->
                var s = 0.0
                for (k in b.indices) s += a[i][k] * b[k][j]
                s
            }
        }
    }

    private fun trans(a: Array<DoubleArray>) = Array(a[0].size) { j -> DoubleArray(a.size) { i -> a[i][j] } }

    private fun addSub(a: Array<DoubleArray>, b: Array<DoubleArray>, sign: Double): Array<DoubleArray> {
        if (a.size != b.size || a[0].size != b[0].size) throw CalcException("Matrices must have the same size")
        return Array(a.size) { i -> DoubleArray(a[0].size) { j -> a[i][j] + sign * b[i][j] } }
    }

    private fun show(m: Array<DoubleArray>): String {
        val cells = m.map { r -> r.map { ExprEngine.format(if (abs(it) < 1e-12) 0.0 else it) } }
        val w = cells.maxOf { r -> r.maxOf { it.length } }
        return cells.joinToString("\n") { r -> r.joinToString("  ") { it.padStart(w) } }
    }

    fun run(op: String, aText: String, bText: String): String = when (op) {
        "A+B" -> show(addSub(parse(aText), parse(bText), 1.0))
        "A−B" -> show(addSub(parse(aText), parse(bText), -1.0))
        "A×B" -> show(mul(parse(aText), parse(bText)))
        "det A" -> ExprEngine.format(det(parse(aText)))
        "det B" -> ExprEngine.format(det(parse(bText)))
        "A⁻¹" -> show(inv(parse(aText)))
        "B⁻¹" -> show(inv(parse(bText)))
        "Aᵀ" -> show(trans(parse(aText)))
        "Bᵀ" -> show(trans(parse(bText)))
        else -> throw CalcException("Unknown operation")
    }
}
