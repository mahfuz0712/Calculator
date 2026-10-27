package com.mahfuz0712.calculator.engine

sealed interface BNode
data class BVar(val c: Char) : BNode
data class BConst(val v: Boolean) : BNode
data class BNot(val a: BNode) : BNode
data class BBin(val op: Char, val a: BNode, val b: BNode) : BNode

data class KMap(
    val corner: String,
    val rowLabels: List<String>,
    val colLabels: List<String>,
    val cells: List<List<Boolean>>
)

data class BoolResult(
    val vars: List<Char>,
    val table: List<Pair<Int, Boolean>>,
    val minterms: List<Int>,
    val sop: String,
    val pos: String,
    val kmap: KMap?
)

object BoolEngine {

    private class BParser(src: String) {
        private val s = src.filter { !it.isWhitespace() }
        private var p = 0

        fun parse(): BNode {
            if (s.isEmpty()) throw CalcException("Enter an expression")
            val n = or()
            if (p < s.length) throw CalcException("Unexpected '${s[p]}'")
            return n
        }

        private fun peek(): Char? = if (p < s.length) s[p] else null

        private fun or(): BNode {
            var l = xor()
            while (peek() == '+' || peek() == '|' || peek() == '∨') {
                p++
                l = BBin('|', l, xor())
            }
            return l
        }

        private fun xor(): BNode {
            var l = and()
            while (peek() == '^' || peek() == '⊕') {
                p++
                l = BBin('^', l, and())
            }
            return l
        }

        private fun and(): BNode {
            var l = not()
            while (true) {
                val c = peek() ?: break
                if (c == '*' || c == '·' || c == '&' || c == '∧') {
                    p++
                    l = BBin('&', l, not())
                } else if (startsPrimary(c)) {
                    l = BBin('&', l, not())
                } else break
            }
            return l
        }

        private fun startsPrimary(c: Char) =
            c.isLetter() || c == '0' || c == '1' || c == '(' || c == '!' || c == '~' || c == '¬'

        private fun not(): BNode {
            val c = peek()
            if (c == '!' || c == '~' || c == '¬') {
                p++
                return BNot(not())
            }
            return post()
        }

        private fun post(): BNode {
            var n = primary()
            while (peek() == '\'' || peek() == '’') {
                p++
                n = BNot(n)
            }
            return n
        }

        private fun primary(): BNode {
            val c = peek() ?: throw CalcException("Unexpected end")
            return when {
                c == '(' -> {
                    p++
                    val n = or()
                    if (peek() == ')') p++ else if (peek() != null) throw CalcException("Expected ')'")
                    n
                }
                c == '0' -> { p++; BConst(false) }
                c == '1' -> { p++; BConst(true) }
                c.isLetter() -> { p++; BVar(c.uppercaseChar()) }
                else -> throw CalcException("Unexpected '$c'")
            }
        }
    }

    private fun eval(n: BNode, env: Map<Char, Boolean>): Boolean = when (n) {
        is BVar -> env[n.c] ?: false
        is BConst -> n.v
        is BNot -> !eval(n.a, env)
        is BBin -> {
            val x = eval(n.a, env)
            val y = eval(n.b, env)
            when (n.op) {
                '&' -> x && y
                '|' -> x || y
                else -> x != y
            }
        }
    }

    private fun collect(n: BNode, out: MutableSet<Char>) {
        when (n) {
            is BVar -> out.add(n.c)
            is BConst -> {}
            is BNot -> collect(n.a, out)
            is BBin -> { collect(n.a, out); collect(n.b, out) }
        }
    }

    // ---- Quine-McCluskey ----
    private data class Imp(val bits: Int, val mask: Int)

    private fun covers(i: Imp, m: Int) = (m and i.mask.inv()) == i.bits

    private fun primes(terms: List<Int>): List<Imp> {
        var cur: Set<Imp> = terms.map { Imp(it, 0) }.toSet()
        val result = mutableSetOf<Imp>()
        while (cur.isNotEmpty()) {
            val used = mutableSetOf<Imp>()
            val next = mutableSetOf<Imp>()
            val list = cur.toList()
            for (a in list.indices) {
                for (b in a + 1 until list.size) {
                    val x = list[a]
                    val y = list[b]
                    if (x.mask != y.mask) continue
                    val d = x.bits xor y.bits
                    if (d != 0 && (d and (d - 1)) == 0) {
                        next.add(Imp(x.bits and d.inv(), x.mask or d))
                        used.add(x)
                        used.add(y)
                    }
                }
            }
            result.addAll(cur - used)
            cur = next
        }
        return result.toList()
    }

    private fun select(terms: List<Int>, primes: List<Imp>): List<Imp> {
        val chosen = mutableListOf<Imp>()
        for (m in terms) {
            val c = primes.filter { covers(it, m) }
            if (c.size == 1 && c[0] !in chosen) chosen.add(c[0])
        }
        val remaining = terms.toMutableSet()
        for (ch in chosen) remaining.removeAll { covers(ch, it) }
        while (remaining.isNotEmpty()) {
            val best = primes.maxByOrNull { p -> remaining.count { covers(p, it) } } ?: break
            chosen.add(best)
            remaining.removeAll { covers(best, it) }
        }
        return chosen.sortedBy { it.bits }
    }

    private fun sopTerm(i: Imp, vars: List<Char>): String {
        val n = vars.size
        val sb = StringBuilder()
        for (k in 0 until n) {
            val bit = 1 shl (n - 1 - k)
            if ((i.mask and bit) != 0) continue
            sb.append(vars[k])
            if ((i.bits and bit) == 0) sb.append('\'')
        }
        return if (sb.isEmpty()) "1" else sb.toString()
    }

    private fun posTerm(i: Imp, vars: List<Char>): String {
        val n = vars.size
        val lits = mutableListOf<String>()
        for (k in 0 until n) {
            val bit = 1 shl (n - 1 - k)
            if ((i.mask and bit) != 0) continue
            lits.add(if ((i.bits and bit) != 0) "${vars[k]}'" else "${vars[k]}")
        }
        return if (lits.size == 1) lits[0] else lits.joinToString(" + ", "(", ")")
    }

    private fun gray(k: Int): List<Int> = if (k == 1) listOf(0, 1) else listOf(0, 1, 3, 2)

    fun analyze(expr: String): BoolResult {
        val node = BParser(expr).parse()
        val set = sortedSetOf<Char>()
        collect(node, set)
        val vars = set.toList()
        val n = vars.size
        if (n > 6) throw CalcException("Maximum 6 variables")
        val rows = 1 shl n
        val outs = BooleanArray(rows) { idx ->
            val env = HashMap<Char, Boolean>()
            vars.forEachIndexed { k, v -> env[v] = ((idx shr (n - 1 - k)) and 1) == 1 }
            eval(node, env)
        }
        val minterms = (0 until rows).filter { outs[it] }
        val zeros = (0 until rows).filter { !outs[it] }

        val sop = when {
            minterms.isEmpty() -> "0"
            minterms.size == rows -> "1"
            else -> select(minterms, primes(minterms)).joinToString(" + ") { sopTerm(it, vars) }
        }
        val pos = when {
            zeros.isEmpty() -> "1"
            zeros.size == rows -> "0"
            else -> select(zeros, primes(zeros)).joinToString("") { posTerm(it, vars) }
        }

        var kmap: KMap? = null
        if (n in 2..4) {
            val rv = n / 2
            val cv = n - rv
            val ro = gray(rv)
            val co = gray(cv)
            kmap = KMap(
                corner = vars.take(rv).joinToString("") + "\\" + vars.drop(rv).joinToString(""),
                rowLabels = ro.map { Integer.toBinaryString(it).padStart(rv, '0') },
                colLabels = co.map { Integer.toBinaryString(it).padStart(cv, '0') },
                cells = ro.map { r -> co.map { c -> outs[(r shl cv) or c] } }
            )
        }
        return BoolResult(vars, (0 until rows).map { it to outs[it] }, minterms, sop, pos, kmap)
    }
}
