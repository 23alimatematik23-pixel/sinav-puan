package com.sinavpuan.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a minimal Office Open XML spreadsheet: one sheet per table. */
object XlsxExporter {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    fun export(file: File, title: String, tables: List<ReportTable>) {
        val names = sheetNames(tables)
        ZipOutputStream(FileOutputStream(file)).use { z ->
            fun put(name: String, content: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(content.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            put(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
""" + tables.indices.joinToString("\n") {
                    """<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
                } + "\n</Types>",
            )
            put(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="$REL_NS/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""",
            )
            put(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="$NS" xmlns:r="$REL_NS"><sheets>""" +
                    names.mapIndexed { i, n -> """<sheet name="${xmlEscape(n)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }
                        .joinToString("") + "</sheets></workbook>",
            )
            put(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    tables.indices.joinToString("") {
                        """<Relationship Id="rId${it + 1}" Type="$REL_NS/worksheet" Target="worksheets/sheet${it + 1}.xml"/>"""
                    } +
                    """<Relationship Id="rId${tables.size + 1}" Type="$REL_NS/styles" Target="styles.xml"/></Relationships>""",
            )
            put("xl/styles.xml", STYLES)
            tables.forEachIndexed { i, t -> put("xl/worksheets/sheet${i + 1}.xml", sheet(title, t)) }
        }
    }

    private fun sheetNames(tables: List<ReportTable>): List<String> {
        val used = mutableSetOf<String>()
        return tables.mapIndexed { i, t ->
            var n = t.title.replace(Regex("""[\\/?*\[\]:]"""), " ").take(31).trim()
            if (n.isEmpty() || !used.add(n)) n = "Sayfa${i + 1}".also { used.add(it) }
            n
        }
    }

    fun colName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val m = (n - 1) % 26
            sb.insert(0, 'A' + m)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun cell(ref: String, value: String, style: Int): String {
        val num = value.replace(',', '.').toDoubleOrNull()
        return if (num != null && !(value.length > 1 && value.startsWith("0") && !value.contains(','))) {
            """<c r="$ref" s="$style"><v>${value.replace(',', '.')}</v></c>"""
        } else {
            """<c r="$ref" s="$style" t="inlineStr"><is><t xml:space="preserve">${xmlEscape(value)}</t></is></c>"""
        }
    }

    private fun sheet(title: String, t: ReportTable): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="$NS"><cols>""")
        t.header.indices.forEach { i ->
            val w = when (i) {
                0 -> 6
                1 -> 9
                2 -> 28
                else -> 8
            }
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
        }
        sb.append("</cols><sheetData>")
        sb.append("""<row r="1">${cell("A1", "$title - ${t.title}", 1)}</row>""")
        sb.append("""<row r="3">""")
        t.header.forEachIndexed { i, h -> sb.append(cell("${colName(i)}3", h, 2)) }
        sb.append("</row>")
        t.rows.forEachIndexed { ri, row ->
            val r = ri + 4
            val style = if (t.isSummaryRow(row)) 2 else 3
            sb.append("""<row r="$r">""")
            row.forEachIndexed { i, v -> sb.append(cell("${colName(i)}$r", v, style)) }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color auto="1"/></left><right style="thin"><color auto="1"/></right><top style="thin"><color auto="1"/></top><bottom style="thin"><color auto="1"/></bottom><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="4"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/><xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/></cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
}
