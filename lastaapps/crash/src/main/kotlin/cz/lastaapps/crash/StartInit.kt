/*
 *    Copyright 2022, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.crash

import android.content.Context
import androidx.annotation.Keep
import androidx.startup.Initializer
import org.kodein.di.android.closestDI
import org.kodein.di.instance
import org.lighthousegames.logging.logging

@Keep
internal class StartInit : Initializer<Unit> {

    companion object {
        private val log = logging()
    }

    override fun create(context: Context) {
        val di by closestDI(context)
        val database: CrashDatabase by di.instance()

        log.i { "Initializing crash storage" }
        Catcher.register(database)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
