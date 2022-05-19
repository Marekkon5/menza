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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.compose.dialog
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cz.lastaapps.entity.menza.MenzaId
import cz.lastaapps.menza.init.InitDecision
import cz.lastaapps.menza.navigation.Dest
import cz.lastaapps.menza.ui.*
import cz.lastaapps.menza.ui.dests.info.InfoLayout
import cz.lastaapps.menza.ui.dests.others.license.LicenseLayout
import cz.lastaapps.menza.ui.dests.others.osturak.OsturakLayout
import cz.lastaapps.menza.ui.dests.others.privacy.PrivacyCheck
import cz.lastaapps.menza.ui.dests.others.privacy.PrivacyDialogContent
import cz.lastaapps.menza.ui.dests.settings.SettingsLayout
import cz.lastaapps.menza.ui.dests.settings.SettingsViewModel
import cz.lastaapps.menza.ui.dests.settings.store.darkMode
import cz.lastaapps.menza.ui.dests.settings.store.resolveShouldUseDark
import cz.lastaapps.menza.ui.dests.settings.store.systemTheme
import cz.lastaapps.menza.ui.dests.today.TodayDest
import cz.lastaapps.menza.ui.dests.week.WeekLayout
import cz.lastaapps.menza.ui.layout.main.WithDrawerScrollProvider
import cz.lastaapps.menza.ui.layout.menza.MenzaViewModel
import cz.lastaapps.menza.ui.theme.AppTheme

@Composable
fun AppRoot(
    activity: Activity,
    viewModel: RootViewModel,
    viewModelStoreOwner: ViewModelStoreOwner,
) {

    val useDark by viewModel.sett.darkMode.collectAsState()
    val useSystem by viewModel.sett.systemTheme.collectAsState()

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
                PrivacyCheck(hiltViewModel()) {

                    //Download default data
                    InitDecision(hiltActivityViewModel()) {

                        //show app if ready
                        AppContent(hiltActivityViewModel())
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
private fun AppContent(viewModel: MenzaViewModel) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val menzaId by viewModel.selectedMenza.collectAsState()
    val onMenzaSelected: (MenzaId?) -> Unit = { viewModel.selectMenza(it) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawableScroll = rememberScrollState()
    val navHostState = rememberAnimatedNavController()

    //drawer should auto open, if there is no menza selected and user hasn't touched the drawer
    val drawerState =
        rememberDrawerState(if (menzaId == null) DrawerValue.Open else DrawerValue.Closed)

    WithSnackbarProvider(snackbarHostState = snackbarHostState) {
        WithDrawerScrollProvider(drawableScroll) {

            // instead of leaving app, open selection menu
            /*BackHandler(drawerState.currentValue == DrawerValue.Closed) {
                scope.launch {
                    drawerState.open()
                }
            }*/

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
                        snackbarHostState = snackbarHostState,
                        drawerState = drawerState,
                        menzaId = menzaId,
                        onMenzaSelected = onMenzaSelected,
                        menzaViewModel = viewModel,
                        todayViewModel = hiltActivityViewModel(),
                        settingsViewModel = settingsViewModel,
                    )
                }
                composable(Dest.R.week) {
                    WeekLayout(
                        navController = navHostState,
                        snackbarHostState = snackbarHostState,
                        drawerState = drawerState,
                        menzaId = menzaId,
                        onMenzaSelected = onMenzaSelected,
                        menzaViewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        weekViewModel = hiltActivityViewModel(),
                    )
                }
                composable(Dest.R.info) {
                    InfoLayout(
                        navController = navHostState,
                        snackbarHostState = snackbarHostState,
                        drawerState = drawerState,
                        menzaId = menzaId,
                        onMenzaSelected = onMenzaSelected,
                        menzaViewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        infoViewModel = hiltActivityViewModel(),
                    )
                }
                composable(Dest.R.settings) {
                    SettingsLayout(
                        navController = navHostState,
                        snackbarHostState = snackbarHostState,
                        drawerState = drawerState,
                        menzaId = menzaId,
                        onMenzaSelected = onMenzaSelected,
                        menzaViewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                    )
                }

                composable(Dest.R.license) {
                    LicenseLayout(
                        navController = navHostState,
                        snackbarHostState = snackbarHostState,
                        drawerState = drawerState,
                        menzaId = menzaId,
                        onMenzaSelected = onMenzaSelected,
                        menzaViewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                    )
                }
                composable(Dest.R.osturak) {
                    OsturakLayout(
                        navController = navHostState,
                        snackbarHostState = snackbarHostState,
                        drawerState = drawerState,
                        menzaId = menzaId,
                        onMenzaSelected = onMenzaSelected,
                        menzaViewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                    )
                }
                dialog(Dest.R.privacyPolicy) {
                    PrivacyDialogContent(showAccept = false, onAccept = {})
                }
            }
        }
    }
}


