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

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import cz.lastaapps.entity.common.CourseType
import cz.lastaapps.entity.day.Dish
import cz.lastaapps.entity.menza.MenzaId
import cz.lastaapps.menza.R
import cz.lastaapps.menza.ui.CollectErrors
import cz.lastaapps.menza.ui.LocalConnectivityProvider
import cz.lastaapps.menza.ui.LocalSnackbarProvider
import cz.lastaapps.menza.ui.dests.settings.SettingsViewModel
import cz.lastaapps.menza.ui.dests.settings.store.PriceType
import cz.lastaapps.menza.ui.dests.settings.store.getPrice
import cz.lastaapps.menza.ui.dests.settings.store.imagesOnMetered
import cz.lastaapps.menza.ui.dests.settings.store.priceType
import cz.lastaapps.menza.ui.isMetered
import cz.lastaapps.menza.ui.layout.menza.MenzaNotSelected

@Composable
fun TodayDishList(
    navController: NavController,
    menzaId: MenzaId?,
    onDishSelected: (Dish) -> Unit,
    viewModel: TodayViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val priceType by settingsViewModel.sett.priceType.collectAsState()
    val onPriceType = { type: PriceType -> settingsViewModel.setPriceType(type) }
    val downloadOnMetered by settingsViewModel.sett.imagesOnMetered.collectAsState()

    if (menzaId == null) {
        MenzaNotSelected(navController, modifier)
    } else {

        //show error snackBars
        val snackbarHostState = LocalSnackbarProvider.current
        CollectErrors(snackbarHostState, viewModel.errors)

        //getting locale for food sorting
        val config = LocalContext.current.resources.configuration
        val locale = remember(config) {
            @Suppress("DEPRECATION")
            if (Build.VERSION_CODES.N >= Build.VERSION.SDK_INT)
                config.locale else config.locales[0]
        }

        //getting data
        val data by remember(menzaId) { viewModel.getData(menzaId, locale) }.collectAsState()

        val isRefreshing by remember(menzaId) {
            viewModel.isRefreshing(menzaId)
        }.collectAsState()

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.refresh(menzaId, locale) },
            modifier = modifier,
        ) {
            DishContent(
                data = data,
                onDishSelected,
                priceType,
                onPriceType,
                downloadOnMetered,
                Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DishContent(
    data: List<DishTypeList>,
    onDishSelected: (Dish) -> Unit,
    priceType: PriceType,
    onPriceType: (PriceType) -> Unit,
    downloadOnMetered: Boolean,
    modifier: Modifier = Modifier
) {

    //no data handling
    if (data.isEmpty()) {
        Box(
            modifier = modifier.verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.today_list_none))
        }
        return
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // showing items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            data.forEach { dishType ->
                stickyHeader {
                    Surface(Modifier.fillMaxWidth()) {
                        DishHeader(courseType = dishType.first, Modifier.padding(bottom = 8.dp))
                    }
                }
                items(dishType.second) { dish ->
                    DishItem(
                        dish, onDishSelected,
                        priceType, downloadOnMetered,
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            return@LazyColumn
        }
        PriceTypeUnspecified(priceType, onPriceType, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PriceTypeUnspecified(
    priceType: PriceType,
    onPriceType: (PriceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (priceType == PriceType.Unset) {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = modifier) {
            Column(
                Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.today_price_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(IntrinsicSize.Max)
                ) {
                    Button(
                        onClick = { onPriceType(PriceType.Discounted) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            stringResource(R.string.today_price_discounted),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = { onPriceType(PriceType.Normal) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            stringResource(R.string.today_price_normal),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DishHeader(courseType: CourseType, modifier: Modifier = Modifier) {
    Text(text = courseType.type, modifier = modifier, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DishItem(
    dish: Dish,
    onDishSelected: (Dish) -> Unit,
    priceType: PriceType,
    downloadOnMetered: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.clickable { onDishSelected(dish) },
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            DishImageWithBadge(dish, priceType, downloadOnMetered)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DishNameRow(dish)
                DishInfoRow(dish)
            }
        }
    }
}

@Composable
private fun DishNameRow(dish: Dish, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier,
    ) {
        Text(dish.name, Modifier.weight(1f))
        Column(
            Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dish.issuePlaces.forEach {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${it.abbrev} ${it.windowsId}",
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun DishInfoRow(dish: Dish, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(dish.amount?.amount ?: "")
        Text(
            dish.allergens.map { it.id }.joinToString(separator = ","),
            Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DishImageWithBadge(
    dish: Dish,
    priceType: PriceType,
    downloadOnMetered: Boolean,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier) {
        val (imgConst, priceConst) = createRefs()

        DishImage(dish = dish, downloadOnMetered, Modifier.constrainAs(imgConst) {
            centerHorizontallyTo(parent)
            centerVerticallyTo(parent)
        })

        DishBadge(dish = dish, priceType, Modifier.constrainAs(priceConst) {
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        })
    }
}

@Composable
private fun DishBadge(dish: Dish, priceType: PriceType, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.tertiary) {
        Text(
            text = "${dish.getPrice(priceType).price} Kč",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(2.dp)
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun DishImage(dish: Dish, downloadOnMetered: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        val size = 64.dp
        val imageModifier = Modifier
            .size(size)
            .padding(bottom = 8.dp, end = 8.dp)

        if (dish.imageUrl != null) {

            var retryHash by remember { mutableStateOf(0) }
            val isMetered = LocalConnectivityProvider.current
            var userAllowed by rememberSaveable(dish.imageUrl) { mutableStateOf(false) }
            val canDownload = remember(isMetered, userAllowed) {
                downloadOnMetered || !isMetered.isMetered() || userAllowed
            }

            val imageRequest = with(ImageRequest.Builder(LocalContext.current)) {
                diskCacheKey(dish.imageUrl)
                memoryCacheKey(dish.imageUrl)
                crossfade(true)
                setParameter("retry_hash", retryHash)
                // if user is no a metered network, images are going to be loaded from cache
                if (canDownload)
                    data(dish.imageUrl)
                else
                    data("https://userisonmeterednetwork.su/")
            }
                .build()

            SubcomposeAsyncImage(
                imageRequest, dish.name,
                loading = {
                    Box(
                        imageModifier
                            .placeholder(
                                true, color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(4.dp),
                                highlight = PlaceholderHighlight.fade(
                                    highlightColor = MaterialTheme.colorScheme.primary,
                                )
                            )
                            .clickable { retryHash++ }
                    )
                },
                error = {
                    Box(
                        imageModifier.clickable { retryHash++; userAllowed = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (canDownload)
                            Icon(
                                Icons.Default.Refresh,
                                stringResource(R.string.today_list_image_load_failed)
                            )
                        else
                            Icon(
                                Icons.Default.Download,
                                stringResource(R.string.today_list_image_metered)
                            )
                    }
                },
                success = {
                    SubcomposeAsyncImageContent(
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier,
                    )
                },
            )

        } else {
            Box(imageModifier, contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = dish.name,
                )
            }
        }
    }
}