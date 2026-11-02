package it.matato.dietreminder.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.format
import it.matato.dietreminder.data.model.resId
import java.io.OutputStream
import java.time.format.TextStyle
import java.util.Locale

internal data class DietPdfLine(val text: String, val size: Float = 11f, val bold: Boolean = false, val indent: Float = 0f)

object DietPdfExporter {
    /** This projection intentionally keeps every alternative, including empty/free meals. */
    internal fun lines(context: Context, diet: DietExport): List<DietPdfLine> = buildList {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        add(DietPdfLine(diet.name, 22f, true))
        if (diet.meals.isEmpty()) add(DietPdfLine(context.getString(R.string.pdf_no_meals)))
        diet.meals.sortedWith(compareBy({ it.day.value }, { it.time.substringBefore(":").toInt() * 60 + it.time.substringAfter(":").toInt() })).groupBy { it.day }.forEach { (day, meals) ->
            add(DietPdfLine(""))
            add(DietPdfLine(day.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }, 17f, true))
            meals.forEach { meal ->
                add(DietPdfLine("${meal.time} · ${meal.customTypeLabel ?: context.getString(meal.type.resId)}", 13f, true))
                if (meal.description.isNotBlank()) add(DietPdfLine(meal.description))
                meal.courses.sortedBy { it.order }.forEach { course ->
                    val options = meal.courses.sortedBy { it.order }.filter { it.alternativeGroup == course.alternativeGroup }
                    val prefix = if (course.alternativeGroup == null) "" else context.getString(R.string.pdf_alternative, options.indexOf(course) + 1) + " · "
                    add(DietPdfLine(prefix + course.name, 12f, true))
                    if (course.description.isNotBlank()) add(DietPdfLine(course.description))
                    course.items.sortedBy { it.order }.forEach { item ->
                        val quantity = item.unit.format(item.amount, context)
                        add(DietPdfLine(item.name + if (quantity.isBlank()) "" else " — $quantity", indent = 12f))
                    }
                    add(DietPdfLine("", 5f))
                }
            }
        }
    }

    internal data class PositionedLine(val text: String, val size: Float, val bold: Boolean, val x: Float, val y: Float)

    /** Measure and paginate before drawing, so no line can run beyond the page. */
    internal fun layout(context: Context, diet: DietExport): List<List<PositionedLine>> {
        val pages = mutableListOf(mutableListOf<PositionedLine>())
        var y = 44f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun newPage() { pages.add(mutableListOf()); y = 44f }
        for (line in lines(context, diet)) {
            paint.textSize = line.size
            paint.typeface = if (line.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val height = paint.fontSpacing + 4f
            val width = 507f - line.indent
            if (line.bold && y + height * 2 > 792f) newPage()
            for (paragraph in line.text.replace("\r\n", "\n").replace('\r','\n').split('\n')) {
                var remaining = paragraph
                if (remaining.isEmpty()) {
                    if (y + height > 792f) newPage()
                    y += height
                }
                while (remaining.isNotEmpty()) {
                    var count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
                    if (count < remaining.length && count > 1 && Character.isHighSurrogate(remaining[count - 1])) count--
                    if (count < remaining.length) {
                        val space = remaining.substring(0,count).indexOfLast { it.isWhitespace() }
                        if (space > 0) count = space
                    }
                    if (y + height > 792f) newPage()
                    pages.last().add(PositionedLine(remaining.substring(0,count), line.size, line.bold, 44f + line.indent, y - paint.fontMetrics.ascent))
                    y += height
                    remaining = remaining.substring(count).trimStart()
                }
            }
        }
        return pages
    }

    fun write(context: Context, diet: DietExport, output: OutputStream) {
        val pages = layout(context, diet)
        val document = PdfDocument()
        var current: PdfDocument.Page? = null
        try {
            pages.forEachIndexed { index, lines ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(595,842,index+1).create())
                current = page
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
                lines.forEach { line ->
                    paint.textSize = line.size
                    paint.typeface = if (line.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    page.canvas.drawText(line.text,line.x,line.y,paint)
                }
                paint.textSize = 9f; paint.color = Color.DKGRAY
                page.canvas.drawText((index+1).toString(),290f,812f,paint)
                document.finishPage(page)
                current = null
            }
            document.writeTo(output)
        } finally {
            current?.let { document.finishPage(it) }
            document.close()
        }
    }
}
