/*
 *    Copyright 2023, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Menza.
 *
 *     Menza is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Menza is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Menza.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.lastaapps.scraping

import cz.lastaapps.entity.info.Contact
import cz.lastaapps.entity.info.Email
import cz.lastaapps.entity.info.Name
import cz.lastaapps.entity.info.PhoneNumber
import cz.lastaapps.entity.info.Role
import cz.lastaapps.entity.menza.MenzaId
import io.ktor.client.request.get
import io.ktor.http.decodeURLPart
import it.skrape.core.htmlDocument
import it.skrape.selects.Doc
import it.skrape.selects.DocElement
import it.skrape.selects.html5.a
import it.skrape.selects.html5.td

object ContactsScraperImpl : ContactsScraper {

    override suspend fun createRequest() =
        agataClient.get("kontakty.php?lang=cs")

    override fun scrape(html: String): Set<Contact> {
        return htmlDocument(html) { parseHtml() }
    }

    private fun Doc.parseHtml(): Set<Contact> {
        return emptySet()

        val set = mutableSetOf<Contact>()

        findFirst("#otdoby") {
            tryFindAllAndCycle("section") {

                val menzaId = id.removePrefix("section").takeIf { it.isNotBlank() }?.toInt()
                    ?: error("Invalid menza id")

                tryFindAllAndCycle("tbody tr") {

                    td {
                        val role = findByIndex(0) {
                            text.takeIf { it.removeSpaces().isNotBlank() }?.let { Role(it) }
                        }
                        val name = findByIndex(1) {
                            text.takeIf { it.removeSpaces().isNotBlank() }?.let { Name(it) }
                        }
                        val phoneNumber = findByIndex(2) {
                            parsePhoneNumber()?.let { PhoneNumber(it) }
                        }
                        val email = findByIndex(3) {
                            parseEmail()?.let { Email(it) }
                        }

                        if (role != null || name != null || phoneNumber != null || email != null)
                            set += Contact(MenzaId(menzaId), name, role, phoneNumber, email)
                    }
                }
            }
        }

        return set
    }

    private val phoneNumberRegex = "^\\+\\d{12}$".toRegex()

    private fun DocElement.parsePhoneNumber(): String? {
        return a {
            tryFindFirst {
                var number = attributes["href"]?.takeIf {
                    it.removeSpaces().isNotBlank()
                } ?: return@tryFindFirst null

                number = number.removePrefix("tel:")

                if (!number.startsWith("+"))
                    number = "+420$number"

                if (!phoneNumberRegex.matches(number))
                    error("Invalid phone number")

                number
            }
        }
    }

    private fun DocElement.parseEmail(): String? {
        return tryFindFirst("a") {
            attribute("href").takeIf {
                it.removeSpaces().isNotBlank()
            }?.removePrefix("mailto:")?.decodeURLPart()
                ?.takeIf { it.removeSpaces().isNotBlank() }
        }
    }
}