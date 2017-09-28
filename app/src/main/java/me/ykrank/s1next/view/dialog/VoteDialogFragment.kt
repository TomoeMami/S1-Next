package me.ykrank.s1next.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.github.ykrank.androidautodispose.AndroidRxDispose
import com.github.ykrank.androidlifecycle.event.FragmentEvent
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiFlatTransformer
import me.ykrank.s1next.data.api.app.AppService
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.databinding.LayoutVoteBinding
import me.ykrank.s1next.util.L
import me.ykrank.s1next.util.RxJavaUtil
import me.ykrank.s1next.view.adapter.simple.SimpleRecycleViewAdapter
import me.ykrank.s1next.viewmodel.ItemVoteViewModel
import me.ykrank.s1next.viewmodel.VoteViewModel
import javax.inject.Inject


/**
 * A dialog lets the user vote thread.
 */
class VoteDialogFragment : BaseDialogFragment() {
    @Inject
    lateinit var appService: AppService
    @Inject
    lateinit var mUser: User

    private lateinit var tid: String
    private lateinit var mVote: Vote
    private lateinit var binding: LayoutVoteBinding
    private lateinit var adapter: SimpleRecycleViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        App.getAppComponent().inject(this)
        super.onCreate(savedInstanceState)
        tid = arguments.getString(ARG_THREAD_ID)
        mVote = arguments.getParcelable(ARG_VOTE)
        adapter = SimpleRecycleViewAdapter(context, R.layout.item_vote)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LayoutVoteBinding.inflate(inflater, container, false)

        val model = VoteViewModel(mVote)
        binding.model = model

        binding.recycleView.adapter = adapter
        binding.recycleView.layoutManager = LinearLayoutManager(context)
        mVote.voteOptions?.let {
            adapter.swapDataSet(it.values.map {
                val vm = ItemVoteViewModel(model)
                vm.option.set(it)
                vm
            })
        }

        loadData()

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun loadData() {
        appService.getPollInfo(mUser.appSecureToken, tid)
                .compose(ApiFlatTransformer.apiErrorTransformer())
                .compose(RxJavaUtil.iOTransformer())
                .to(AndroidRxDispose.withObservable(this, FragmentEvent.DESTROY))
                .subscribe({ binding.model.appVote.set(it.data) }, L::e)
    }

    companion object {

        val ARG_VOTE = "vote"
        val ARG_THREAD_ID = "thread_id"
        val TAG: String = VoteDialogFragment::class.java.name

        fun newInstance(threadId: String, vote: Vote): VoteDialogFragment {
            val fragment = VoteDialogFragment()
            val bundle = Bundle()
            bundle.putString(ARG_THREAD_ID, threadId)
            bundle.putParcelable(ARG_VOTE, vote)
            fragment.arguments = bundle

            return fragment
        }
    }
}
