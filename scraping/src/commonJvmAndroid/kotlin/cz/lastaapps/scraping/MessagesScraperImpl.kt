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

import cz.lastaapps.entity.menza.Message
import io.ktor.client.request.get
import it.skrape.core.htmlDocument
import it.skrape.selects.Doc
import org.lighthousegames.logging.logging

object MessagesScraperImpl : MessagesScraper {

    private val log = logging()

    override suspend fun createRequest() =
        agataClient.get("index.php?lang=cs")

    /**
     * Accepts any url /jidelnicky/index.php???
     */
    override fun scrape(html: String): Set<Message> {
        return htmlDocument(html) { parseHtml() }
    }

    private fun Doc.parseHtml(): Set<Message> {

        val menzas = MenzaScraperImpl.scrape(html).map {
            it.name to it.menzaId
        }.toMap()

        val set = mutableSetOf<Message>()

        tryFindFirst("#aktuality") {
            tryFindAllAndCycle("div div div") {
                runCatching {
                    val menzaId = menzas[children[0].ownText.removeSpaces()]
                        ?: error("Invalid menza id")

                    val text = children[1].run {
                        html
                            .replace("<BR>", "\n")
                            .replace("<br>", "\n")
                            .replace(" +".toRegex(), " ")
                            .replace("^ +".toRegex(RegexOption.MULTILINE), "")
                            .replace(" +$".toRegex(RegexOption.MULTILINE), "")
                            .removeSpaces()
                    }
                    set += Message(menzaId, text)
                }.getOrElse { log.e(it) { "Failed to parse a menza message" } }
            }
        }

        return set
    }
}
