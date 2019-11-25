package io.luiscarino.miraai.image

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import io.luiscarino.miraai.R
import io.luiscarino.miraai.pager.PagerFragmentDirections
import kotlinx.android.synthetic.main.base_detail_fragment.*

class BaseDetailFragment : Fragment() {

    companion object {
        fun newInstance(type: FragmentType): Fragment {
            val baseDetailFragment = BaseDetailFragment()
            baseDetailFragment.arguments = Bundle().apply {
                putSerializable(ARGS_TYPE, type)
            }
            return baseDetailFragment
        }

        const val ARGS_TYPE = "args.type"
    }

    private lateinit var viewModel: BaseDetailViewModel
    private lateinit var outputFileUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.base_detail_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BaseDetailViewModel::class.java)
        val fragmentType = arguments?.getSerializable(ARGS_TYPE) as? FragmentType
        frameLayout.setBackgroundResource(fragmentType?.background ?: R.drawable.orange_gradient)
        button.setBackgroundResource(fragmentType?.button ?: R.drawable.orange_rounded_btn)
        titleTv.text = fragmentType?.title
        subtitleTv.text = fragmentType?.subtitle
        image.setImageResource(fragmentType?.imageSrc ?: R.drawable.icon_people)

        button.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.host_fragment).navigate(
                PagerFragmentDirections.actionPagerToCamera()
            )
        }
    }
}

enum class FragmentType(
    val title: String,
    val subtitle: String,
    val background: Int,
    val button: Int,
    val imageSrc: Int
) {
    PEOPLE(
        "People",
        "Recognize People",
        R.drawable.orange_gradient,
        R.drawable.orange_rounded_btn,
        R.drawable.icon_people
    ),
    DOCS(
        "Documents",
        "Read document",
        R.drawable.purple_gradient,
        R.drawable.purple_rounded_btn,
        R.drawable.icon_docs
    ),
    COLORS(
        "Color",
        "Recognize Colors",
        R.drawable.aqua_gradient,
        R.drawable.aqua_rounded_btn,
        R.drawable.ic_light
    ),

}
