package com.sinavpuan.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a minimal Word document (landscape A4) with one table per report section. */
object DocxExporter {
    private const val USABLE_TWIPS = 16838 - 2 * 720

    fun export(file: File, title: String, tables: List<ReportTable>) {
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
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""",
            )
            put(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""",
            )
            put(
                "word/_rels/document.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"></Relationships>""",
            )
            put("word/document.xml", document(title, tables))
        }
    }

    private fun run(text: String, bold: Boolean, size: Int) =
        """<w:r><w:rPr>${if (bold) "<w:b/>" else ""}<w:sz w:val="$size"/></w:rPr><w:t xml:space="preserve">${xmlEscape(text)}</w:t></w:r>"""

    private fun paragraph(text: String, bold: Boolean, size: Int, center: Boolean = false) =
        """<w:p><w:pPr>${if (center) "<w:jc w:val=\"center\"/>" else ""}<w:spacing w:after="80"/></w:pPr>${run(text, bold, size)}</w:p>"""

    private fun document(title: String, tables: List<ReportTable>): String {
        val sb = StringBuilder()
        sb.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""",
        )
        sb.append(paragraph(title, true, 32, center = true))
        for (t in tables) {
            val ws = t.weights()
            val widths = ws.map { (it / ws.sum() * USABLE_TWIPS).toInt() }
            val fontSize = if (t.header.size > 14) 16 else 20
            sb.append(paragraph(t.title, true, 24))
            sb.append(
                """<w:tbl><w:tblPr><w:tblW w:w="$USABLE_TWIPS" w:type="dxa"/><w:tblLayout w:type="fixed"/><w:tblBorders>""" +
                    listOf("top", "left", "bottom", "right", "insideH", "insideV")
                        .joinToString("") { """<w:$it w:val="single" w:sz="4" w:space="0" w:color="444444"/>""" } +
                    "</w:tblBorders></w:tblPr><w:tblGrid>" +
                    widths.joinToString("") { """<w:gridCol w:w="$it"/>""" } + "</w:tblGrid>",
            )
            fun row(cells: List<String>, header: Boolean) {
                sb.append("<w:tr>")
                if (header) sb.append("<w:trPr><w:tblHeader/></w:trPr>")
                val bold = header || t.isSummaryRow(cells)
                cells.forEachIndexed { i, c ->
                    val shade = if (header) """<w:shd w:val="clear" w:color="auto" w:fill="E6E6E6"/>""" else ""
                    val jc = if (i == 2 && !header) "left" else "center"
                    sb.append(
                        """<w:tc><w:tcPr><w:tcW w:w="${widths[i]}" w:type="dxa"/>$shade</w:tcPr>""" +
                            """<w:p><w:pPr><w:jc w:val="$jc"/><w:spacing w:after="0"/></w:pPr>${run(c, bold, fontSize)}</w:p></w:tc>""",
                    )
                }
                sb.append("</w:tr>")
            }
            row(t.header, true)
            t.rows.forEach { row(it, false) }
            sb.append("</w:tbl>")
            sb.append(paragraph("", false, 20))
        }
        sb.append(
            """<w:sectPr><w:pgSz w:w="16838" w:h="11906" w:orient="landscape"/>""" +
                """<w:pgMar w:top="720" w:right="720" w:bottom="720" w:left="720" w:header="0" w:footer="0" w:gutter="0"/></w:sectPr>""",
        )
        sb.append("</w:body></w:document>")
        return sb.toString()
    }
}
