package com.example.util

import java.util.Locale
import kotlin.math.roundToLong

object NumberToWords {
    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(amount: Double): String {
        if (amount == 0.0) return "Zero Rupees Only"
        val rounded = (amount * 100.0).roundToLong()
        val rupees = rounded / 100
        val paise = rounded % 100

        val words = StringBuilder()
        if (rupees > 0) {
            words.append(convertNumber(rupees))
            words.append(" Rupees")
        }
        if (paise > 0) {
            if (rupees > 0) {
                words.append(" and ")
            }
            words.append(convertNumber(paise))
            words.append(" Paise")
        }
        words.append(" Only")
        return words.toString().trim()
    }

    fun convertUpperCase(amount: Double): String {
        return convert(amount).uppercase(Locale.ROOT)
    }

    private fun convertNumber(n: Long): String {
        if (n < 0) return "Minus " + convertNumber(-n)
        if (n < 20) return units[n.toInt()]
        if (n < 100) {
            val rem = n % 10
            return tens[(n / 10).toInt()] + if (rem > 0) " " + units[rem.toInt()] else ""
        }
        if (n < 1000) {
            val rem = n % 100
            return units[(n / 100).toInt()] + " Hundred" + if (rem > 0) " " + convertNumber(rem) else ""
        }
        if (n < 100000) {
            val rem = n % 1000
            return convertNumber(n / 1000) + " Thousand" + if (rem > 0) " " + convertNumber(rem) else ""
        }
        if (n < 10000000) {
            val rem = n % 100000
            return convertNumber(n / 100000) + " Lakh" + if (rem > 0) " " + convertNumber(rem) else ""
        }
        val rem = n % 10000000
        return convertNumber(n / 10000000) + " Crore" + if (rem > 0) " " + convertNumber(rem) else ""
    }
}
