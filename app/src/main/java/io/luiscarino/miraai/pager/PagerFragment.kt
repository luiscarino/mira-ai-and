package io.luiscarino.miraai.pager

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import io.luiscarino.miraai.R
import io.luiscarino.miraai.image.BaseDetailFragment
import io.luiscarino.miraai.image.FragmentType
import io.luiscarino.miraai.pager.adapter.MainPagerAdapter
import kotlinx.android.synthetic.main.fragment_pager.*

/**
 * Fragment that display the list of options in a pager
 */
class PagerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_pager, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setPager()
    }

    private fun setPager() {
        val mainPagerAdapter = MainPagerAdapter(
            activity!!.supportFragmentManager, listOf(
                BaseDetailFragment.newInstance(FragmentType.PEOPLE),
                BaseDetailFragment.newInstance(FragmentType.DOCS),
                BaseDetailFragment.newInstance(FragmentType.COLORS)
            )
        )
        viewPager.adapter = mainPagerAdapter
    }

}
