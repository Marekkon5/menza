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

package cz.lastaapps.menza.ui.root

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.compose.dialog
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cz.lastaapps.entity.menza.MenzaId
import cz.lastaapps.menza.init.InitDecision
import cz.lastaapps.menza.init.InitViewModel
import cz.lastaapps.menza.navigation.Dest
import cz.lastaapps.menza.ui.WithConnectivity
import cz.lastaapps.menza.ui.dests.info.InfoLayout
import cz.lastaapps.menza.ui.dests.info.InfoViewModel
import cz.lastaapps.menza.ui.dests.others.license.LicenseLayout
import cz.lastaapps.menza.ui.dests.others.osturak.OsturakLayout
import cz.lastaapps.menza.ui.dests.others.privacy.PrivacyCheck
import cz.lastaapps.menza.ui.dests.others.privacy.PrivacyDialogContent
import cz.lastaapps.menza.ui.dests.others.privacy.PrivacyViewModel
import cz.lastaapps.menza.ui.dests.settings.SettingsLayout
import cz.lastaapps.menza.ui.dests.settings.SettingsViewModel
import cz.lastaapps.menza.ui.dests.settings.store.darkMode
import cz.lastaapps.menza.ui.dests.settings.store.resolveShouldUseDark
import cz.lastaapps.menza.ui.dests.settings.store.systemTheme
import cz.lastaapps.menza.ui.dests.today.TodayDest
import cz.lastaapps.menza.ui.dests.today.TodayViewModel
import cz.lastaapps.menza.ui.dests.week.WeekLayout
import cz.lastaapps.menza.ui.dests.week.WeekViewModel
import cz.lastaapps.menza.ui.layout.main.WithDrawerListStateProvider
import cz.lastaapps.menza.ui.layout.menza.MenzaViewModel
import cz.lastaapps.menza.ui.root.locals.*
import cz.lastaapps.menza.ui.theme.AppTheme

@Composable
fun AppRoot(
    activity: Activity,
    viewModel: RootViewModel,
    viewModelStoreOwner: ViewModelStoreOwner,
) {

    val useDark by viewModel.sett.darkMode.collectAsState()
    val useSystem by viewModel.sett.systemTheme.collectAsState()

    val privacyViewModel: PrivacyViewModel by rememberActivityViewModel()
    val initViewModel: InitViewModel by rememberActivityViewModel()
    val menzaViewModel: MenzaViewModel by rememberActivityViewModel()
    val settingsViewModel: SettingsViewModel by rememberActivityViewModel()

    AppTheme(
        darkTheme = useDark.resolveShouldUseDark(),
        useCustomTheme = !useSystem,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ApplyLocalProviders(
                activity = activity,
                viewModelStoreOwner = viewModelStoreOwner,
            ) {
                //checks if privacy policy has been accepted
                PrivacyCheck(privacyViewModel) {

                    //Download default data
                    InitDecision(initViewModel) {

                        //show app if ready
                        AppContent(menzaViewModel, settingsViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyLocalProviders(
    activity: Activity,
    viewModelStoreOwner: ViewModelStoreOwner,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalActivityViewModelOwner provides viewModelStoreOwner) {
        WithLocalWindowSizes(activity) {
            WithFoldingFeature(activity) {
                WithConnectivity {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun AppContent(viewModel: MenzaViewModel, settingsViewModel: SettingsViewModel) {

    val menzaId by viewModel.selectedMenza.collectAsState()
    val onMenzaSelected: (MenzaId?) -> Unit = { viewModel.selectMenza(it) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawableLazyListState = rememberLazyListState()
    val navHostState = rememberAnimatedNavController()

    //drawer should auto open, if there is no menza selected and user hasn't touched the drawer
    val drawerState =
        rememberDrawerState(if (menzaId == null) DrawerValue.Open else DrawerValue.Closed)

    WithSnackbarProvider(snackbarHostState = snackbarHostState) {
        WithDrawerListStateProvider(drawableLazyListState) {

            ChooseLayout(
                navController = navHostState,
                menzaId = menzaId,
                onMenzaSelected = onMenzaSelected,
                menzaViewModel = viewModel,
                settingsViewModel = settingsViewModel,
                snackbarHostState = snackbarHostState,
                drawerState = drawerState
            ) {
                AnimatedNavHost(
                    navController = navHostState,
                    startDestination = Dest.R.start,
                    modifier = Modifier.fillMaxSize()
                ) {

                    composable(
                        Dest.R.today,
                    ) {
                        TodayDest(
                            navController = navHostState,
                            menzaId = menzaId,
                            todayViewModel = rememberActivityViewModel<TodayViewModel>().value,
                            settingsViewModel = settingsViewModel,
                        )
                    }
                    composable(Dest.R.week) {
                        WeekLayout(
                            navController = navHostState,
                            menzaId = menzaId,
                            weekViewModel = rememberActivityViewModel<WeekViewModel>().value,
                        )
                    }
                    composable(Dest.R.info) {
                        InfoLayout(
                            navController = navHostState,
                            snackbarHostState = snackbarHostState,
                            menzaId = menzaId,
                            infoViewModel = rememberActivityViewModel<InfoViewModel>().value,
                        )
                    }
                    composable(Dest.R.settings) {
                        SettingsLayout(
                            navController = navHostState,
                            menzaViewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                        )
                    }
                    composable(Dest.R.license) {
                        LicenseLayout()
                    }
                    composable(Dest.R.osturak) {
                        OsturakLayout(
                            navController = navHostState
                        )
                    }
                    dialog(Dest.R.privacyPolicy) {
                        PrivacyDialogContent(showAccept = false, onAccept = {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseLayout(
    navController: NavController,
    menzaId: MenzaId?,
    onMenzaSelected: (MenzaId?) -> Unit,
    menzaViewModel: MenzaViewModel,
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    when (LocalWindowWidth.current) {
        WindowSizeClass.COMPACT -> {
            AppLayoutCompact(
                navController = navController,
                menzaId = menzaId,
                onMenzaSelected = onMenzaSelected,
                menzaViewModel = menzaViewModel,
                settingsViewModel = settingsViewModel,
                snackbarHostState = snackbarHostState,
                drawerState = drawerState,
                content = content,
            )
        }
        WindowSizeClass.MEDIUM -> {
            AppLayoutMedium(
                navController = navController,
                menzaId = menzaId,
                onMenzaSelected = onMenzaSelected,
                menzaViewModel = menzaViewModel,
                settingsViewModel = settingsViewModel,
                snackbarHostState = snackbarHostState,
                drawerState = drawerState,
                content = content,
            )
        }
        WindowSizeClass.EXPANDED -> {
            AppLayoutExpanded(
                navController = navController,
                menzaId = menzaId,
                onMenzaSelected = onMenzaSelected,
                menzaViewModel = menzaViewModel,
                settingsViewModel = settingsViewModel,
                snackbarHostState = snackbarHostState,
                drawerState = drawerState,
                content = content,
            )
        }
    }
}


