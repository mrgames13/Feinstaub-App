/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mrgames13.jimdo.feintaubapp.ui.fragment.RankingFragment

class RankingAdapter(
    fm: FragmentManager,
    l: Lifecycle,
    private val listener: OnRankingLoadingEventListener
) : FragmentStateAdapter(fm, l) {

    // Interfaces
    interface OnRankingLoadingEventListener {
        fun onPageLoaded()
    }

    override fun createFragment(pos: Int): Fragment {
        return RankingFragment(pos, listener)
    }

    override fun getItemCount() = 2
}