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

package cz.lastaapps.menza.ui.dests.today

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import cz.lastaapps.entity.allergens.Allergen
import cz.lastaapps.entity.allergens.AllergenId
import cz.lastaapps.entity.day.Dish
import cz.lastaapps.entity.day.IssueLocation
import cz.lastaapps.menza.R
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.max

@Composable
fun NoDishSelected(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.today_no_dish))
    }
}

@Composable
fun TodayInfo(
    dish: Dish,
    todayViewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DishImage(dish = dish, Modifier.fillMaxWidth())

        Header(dish = dish)
        PriceView(dish = dish, Modifier.fillMaxWidth())
        IssueLocationList(list = dish.issuePlaces)
        AllergenList(dish = dish, todayViewModel = todayViewModel)
    }
}

@Composable
private fun Header(dish: Dish, modifier: Modifier = Modifier) {
    Text(
        text = dish.name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier,
    )
}

@Composable
private fun PriceView(dish: Dish, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text(text = dish.amount?.amount ?: "")
        Text(
            text = "${dish.priceStudent?.price ?: "???"} / ${dish.priceNormal?.price ?: "???"} Kč",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IssueLocationList(
    list: ImmutableList<IssueLocation>,
    modifier: Modifier = Modifier,
) {
    if (list.isEmpty()) return
    Column(modifier) {
        Row {
            Text(
                text = stringResource(R.string.today_info_location),
                style = MaterialTheme.typography.titleMedium,
            )
            /*Text(
                text = stringResource(R.string.today_info_window),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )*/
        }
        list.forEach {
            Text(text = it.name)
        }
    }
}

@Composable
private fun AllergenList(
    dish: Dish,
    todayViewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val context = LocalContext.current
        val data by remember(context) {
            // used in case server fucks up and returns allergens like 79 or 1011 (',' forgotten)
            val unknownAllergen = Allergen(
                AllergenId(Int.MAX_VALUE),
                context.getString(R.string.today_info_unknown_allergen_title),
                context.getString(R.string.today_info_unknown_allergen_description),
            )
            todayViewModel.getAllergenForIds(dish.allergens, unknownAllergen)
        }.collectAsState(emptyList())

        Text(
            stringResource(R.string.today_info_allergens_title),
            style = MaterialTheme.typography.titleLarge
        )

        if (data.isEmpty()) {
            Text(stringResource(R.string.today_info_allergens_none))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.forEach {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = CircleShape,
                            ) {
                                /*val density = LocalDensity.current
                                val minSize = remember(density) {
                                    with(density) { 24.dp.roundToPx() }
                                }*/
                                Layout(
                                    content = {
                                        Text(
                                            "${it.id.id}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(
                                                start = 6.dp, end = 6.dp,
                                                top = 2.dp, bottom = 2.dp
                                            ),
                                        )
                                    }
                                ) { measurable, constrains ->
                                    val placeable = measurable[0].measure(constrains)
                                    //val h = max(placeable.height, minSize)
                                    val h = placeable.height
                                    val w = max(placeable.width, h)
                                    layout(w, h) {
                                        placeable.place(
                                            (w - placeable.width) / 2,
                                            (h - placeable.height) / 2,
                                        )
                                    }
                                }
                            }
                            Text(it.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(it.description)
                    }
                }
            }
        }
    }
}

@Composable
private fun DishImage(dish: Dish, modifier: Modifier = Modifier) {
    Box(modifier.animateContentSize()) {

        if (dish.imageUrl != null) {

            //temporary solution for refreshing
            var retryHash by remember { mutableStateOf(0) }

            val imageRequest = with(ImageRequest.Builder(LocalContext.current)) {
                data(dish.imageUrl)
                diskCacheKey(dish.imageUrl)
                memoryCacheKey(dish.imageUrl)
                crossfade(true)
                setParameter("retry_hash", retryHash)
            }.build()

            SubcomposeAsyncImage(
                imageRequest, dish.name,
                loading = {
                    Box(
                        Modifier
                            .aspectRatio(4f / 3f)
                            .placeholder(
                                true, color = MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.extraSmall,
                                highlight = PlaceholderHighlight.fade(
                                    highlightColor = MaterialTheme.colorScheme.primary,
                                )
                            )
                            .clickable { retryHash++ }
                    )
                },
                error = {
                    Box(
                        Modifier
                            .aspectRatio(4f / 3f)
                            .clickable { retryHash++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            stringResource(R.string.today_info_image_load_failed)
                        )
                    }
                },
            )
        } else {
            Box(Modifier.aspectRatio(4f / 1f))
        }
    }
}
