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

package cz.lastaapps.menza.starting.ui.downloaddata

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.lastaapps.core.ui.vm.HandleAppear
import cz.lastaapps.menza.R
import cz.lastaapps.menza.ui.HandleError
import cz.lastaapps.menza.ui.theme.MenzaPadding
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DownloadScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = koinViewModel(),
    hostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    DownloadEffects(viewModel, hostState, onDone)

    DownloadContent(
        state = viewModel.flowState.value,
        onRefresh = viewModel::retry,
        hostState = hostState,
        modifier = modifier,
    )
}

@Composable
private fun DownloadEffects(
    viewModel: DownloadViewModel,
    hostState: SnackbarHostState,
    onDone: () -> Unit,
) {
    HandleAppear(viewModel)

    HandleError(viewModel, hostState)

    val isDone = viewModel.flowState.value.isDone
    LaunchedEffect(isDone) {
        if (isDone) {
            onDone()
            viewModel.dismissDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadContent(
    state: DownloadDataState,
    onRefresh: () -> Unit,
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    MenzaPadding.Medium,
                    Alignment.CenterVertically,
                ),
                modifier = Modifier
                    .sizeIn(maxWidth = DownloadUI.maxWidth)
                    .fillMaxWidth()
                    .padding(MenzaPadding.Small)
                    .animateContentSize()
            ) {
                Text(
                    stringResource(R.string.init_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                )

                Crossfade(
                    targetState = state.isLoading to state.isReady,
                    modifier = Modifier.fillMaxWidth(),
                ) { (isLoading, isReady) ->
                    when {
                        isLoading && isReady -> {
                            Column(verticalArrangement = Arrangement.spacedBy(MenzaPadding.Small)) {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = state.downloadProgress.progress,
                                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MenzaPadding.Small),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        animatedProgress,
                                        modifier = Modifier.weight(1f),
                                    )

                                    val percentValue = state.downloadProgress.progress * 100
                                    Text(
                                        "%3.0f %%".format(percentValue),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(state.downloadProgress.text)
                            }
                        }
                        !isLoading && isReady -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MenzaPadding.Small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = onRefresh) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                                Text(stringResource(R.string.init_error))
                            }
                        }
                        else -> {
                            Box(Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}

private object DownloadUI {
    val maxWidth = 256.dp
}