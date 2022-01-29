package ru.rofleksey.sunnyway.util.csv

import java.io.BufferedReader
import java.io.Reader
import kotlin.collections.*

class CsvReader(reader: Reader): BufferedReader(reader) {
    private val splitRegex = Regex(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")
    private var header: List<String>? = null

    private fun readRowInternal(): List<String>? {
        val line = readLine() ?: return null
        return line.split(splitRegex).map {
            it.trim().removeSurrounding("\"")
        }
    }

    fun readHeader() {
        val row = readRowInternal()
        header = row
    }

    fun readRow(): Map<String, String>? {
        val curHeader = header ?: throw IllegalStateException("Header was not read")
        val map = mutableMapOf<String, String>()
        val row = readRowInternal() ?: return null
        for (i in row.indices) {
            map[curHeader[i]] = row[i]
        }
        return map
    }

    fun sequence(): RowSequence = RowSequence(this)

    class RowSequence(private val reader: CsvReader): Sequence<Map<String, String>> {
        override fun iterator(): Iterator<Map<String, String>> = object: Iterator<Map<String, String>> {
            private var nextRow: Map<String, String>? = null
            private var eof = false

            override fun hasNext(): Boolean {
                if (nextRow == null && !eof) {
                    nextRow = reader.readRow()
                    if (nextRow == null) {
                        eof = true
                    }
                }
                return nextRow != null
            }

            override fun next(): Map<String, String> {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                val result = nextRow
                nextRow = null
                return result!!
            }
        }
    }
}
