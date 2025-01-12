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

package cz.lastaapps.menza.ui.dests.panels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.lastaapps.menza.ui.dests.others.crashes.CrashesViewModel
import cz.lastaapps.menza.ui.dests.others.whatsnew.WhatsNewPanel
import cz.lastaapps.menza.ui.dests.others.whatsnew.WhatsNewViewModel
import cz.lastaapps.menza.ui.dests.others.whatsnew.whatsNewPanelState
import cz.lastaapps.menza.ui.dests.settings.SettingsViewModel
import cz.lastaapps.menza.ui.root.locals.rememberActivityViewModel

@Composable
fun Panels(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = rememberActivityViewModel<SettingsViewModel>().value,
    crashesViewModel: CrashesViewModel = rememberActivityViewModel<CrashesViewModel>().value,
    whatsNewViewModel: WhatsNewViewModel = rememberActivityViewModel<WhatsNewViewModel>().value,
) {
    Box(modifier.animateContentSize()) {
        val showPrice = priceTypeUnspecifiedState(settingsViewModel)
        val showCrash = crashReportState(crashesViewModel)
        val showWhatsNew = whatsNewPanelState(whatsNewViewModel)
        val showAprils = aprilFoolsState()

        val items = remember(showPrice, showCrash, showAprils) {
            listOf(
                PanelItem(showPrice) { PriceTypeUnspecified(settingsViewModel, it) },
                PanelItem(showCrash) { CrashReport(crashesViewModel, it) },
                PanelItem(showWhatsNew) { WhatsNewPanel(whatsNewViewModel, it) },
                PanelItem(showAprils) { AprilFools(it) },
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.animateContentSize(),
        ) {
            items.firstOrNull { it.shouldShow.value }?.content?.let { content ->
                content(
                    Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

private data class PanelItem(
    val shouldShow: State<Boolean>,
    val content: @Composable (Modifier) -> Unit
)