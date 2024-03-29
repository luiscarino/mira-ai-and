package io.luiscarino.miraai.pager.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class MainPagerAdapter(fragmentManager: FragmentManager,
                       private val fragments: List<Fragment>) : FragmentPagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment  = fragments[position]

    override fun getCount(): Int = fragments.size

}