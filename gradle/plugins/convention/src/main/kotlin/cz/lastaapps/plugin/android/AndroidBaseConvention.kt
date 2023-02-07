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

package cz.lastaapps.plugin.android

import cz.lastaapps.extensions.implementation
import cz.lastaapps.extensions.libs
import cz.lastaapps.plugin.BasePlugin
import cz.lastaapps.plugin.common.ArrowKtConvention
import cz.lastaapps.plugin.common.DetektConvention
import cz.lastaapps.plugin.common.JavaConvention
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class AndroidBaseConvention : BasePlugin({

    apply<JavaConvention>()
    apply<DetektConvention>()
    apply<ArrowKtConvention>()

    dependencies {
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.lifecycle.viewModel)

        implementation(platform(libs.arrowkt.bom))
        implementation(libs.arrowkt.core)
        implementation(libs.arrowkt.fx.coroutines)
        implementation(libs.arrowkt.fx.stm)
    }
})
