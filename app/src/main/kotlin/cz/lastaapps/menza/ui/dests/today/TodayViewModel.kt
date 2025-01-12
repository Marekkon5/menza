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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lastaapps.entity.allergens.Allergen
import cz.lastaapps.entity.allergens.AllergenId
import cz.lastaapps.entity.common.CourseType
import cz.lastaapps.entity.day.Dish
import cz.lastaapps.entity.menza.MenzaId
import cz.lastaapps.menza.compareToLocal
import cz.lastaapps.menza.di.TodayRepoFactory
import cz.lastaapps.storage.repo.AllergenRepo
import cz.lastaapps.storage.repo.MenzaError
import cz.lastaapps.storage.repo.TodayRepo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

typealias DishTypeList = Pair<CourseType, ImmutableList<Dish>>

class TodayViewModel constructor(
    private val todayRepoFactory: TodayRepoFactory,
    private val allergenRepo: AllergenRepo,
) : ViewModel() {

    val selectedDish: StateFlow<Dish?>
        get() = mSelectedDish

    private val mSelectedDish = MutableStateFlow<Dish?>(null)

    /**
     * Called when a menza id is spotted
     * If the id doesn't correspond with the id of the selected dish,
     * the dish is unselected
     */
    fun menzaSpotted(menzaId: MenzaId?) {
        if (menzaId != mSelectedDish.value?.menzaId)
            mSelectedDish.value = null
    }

    fun selectDish(dish: Dish?) {
        mSelectedDish.value = dish
    }

    // I was lazy to pass AllergenViewModel in the whole hierarchy
    // so there is the important method
    fun getAllergenForIds(
        list: ImmutableList<AllergenId>,
        unknownAllergen: Allergen
    ): Flow<List<Allergen>> {
        return flow {
            emit(list.map {
                allergenRepo.getAllergenInfo(it).first() ?: unknownAllergen.copy(id = it)
            })
        }
    }


    private val repos = HashMap<MenzaId, TodayRepo>()
    private val cache = HashMap<MenzaId, MutableStateFlow<ImmutableList<DishTypeList>>>()

    val errors = Channel<MenzaError>(Channel.BUFFERED)

    fun getData(menzaId: MenzaId, locale: Locale): StateFlow<ImmutableList<DishTypeList>> {

        val data = cache[menzaId]
        if (data != null) return data

        return MutableStateFlow<ImmutableList<DishTypeList>>(persistentListOf()).also {
            cache[menzaId] = it
            refresh(menzaId, locale)
        }
    }

    fun isRefreshing(menzaId: MenzaId): StateFlow<Boolean> {
        return repos.getOrCreate(menzaId).requestInProgress
    }

    fun refresh(menzaId: MenzaId, locale: Locale) {
        viewModelScope.launch {

            val repo = repos.getOrCreate(menzaId)

            val data = repo.getData()

            if (data != null) {
                cache.getOrPut(menzaId) { MutableStateFlow(persistentListOf()) }.value =
                    data.toDishTypeList().toDishList(locale)
            }
            while (true)
                repo.errors.tryReceive().takeIf { it.isSuccess }
                    ?.let { errors.send(it.getOrThrow()) } ?: break
        }
    }

    private fun HashMap<MenzaId, TodayRepo>.getOrCreate(menzaId: MenzaId) = getOrPut(menzaId) {
        todayRepoFactory.create(menzaId)
    }

    private fun Collection<Dish>.toDishTypeList(): ImmutableList<DishTypeList> {
        val map = HashMap<CourseType, MutableList<Dish>>()
        this.forEach { dish ->
            map.getOrPut(dish.courseType) { mutableListOf() }.add(dish)
        }
        return map.entries.map { it.key to it.value.toImmutableList() }.toImmutableList()
    }

    private fun Collection<DishTypeList>.toDishList(locale: Locale): ImmutableList<DishTypeList> {
        return this.sortedWith { d1, d2 ->
            d1.first.webOrder.compareTo(d2.first.webOrder)
                .takeIf { it != 0 }?.let { return@sortedWith it }
            d1.first.type.compareToLocal(d2.first.type, locale)
                .takeIf { it != 0 }?.let { return@sortedWith it }
            0
        }.map {
            it.first to it.second.sortedWith { d1, d2 ->
                d1.name.compareToLocal(d2.name, locale)
            }.toImmutableList()
        }.toImmutableList()
    }
}