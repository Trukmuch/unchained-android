package com.github.livingwithhippos.unchained.downloaddetails.view

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadDetailsBinding
import com.github.livingwithhippos.unchained.downloaddetails.model.AlternativeDownloadAdapter
import com.github.livingwithhippos.unchained.downloaddetails.model.StreamingAdapter
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of showing the details of a [DownloadItem]
 */
@AndroidEntryPoint
class DownloadDetailsFragment : UnchainedFragment(), DownloadDetailsListener {

    private val viewModel: DownloadDetailsViewModel by viewModels()

    private val args: DownloadDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val detailsBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)

        detailsBinding.details = args.details
        detailsBinding.listener = this
        detailsBinding.yatseInstalled = isYatseInstalled()

        viewModel.streamLiveData.observe(viewLifecycleOwner, {
            val streams = mutableListOf<Pair<String,String>>()
            streams.add(Pair("apple",it.apple.link))
            streams.add(Pair("dash",it.dash.link))
            streams.add(Pair("liveMP4",it.liveMP4.link))
            streams.add(Pair("h264WebM",it.h264WebM.link))
            val streamingAdapter = StreamingAdapter(this)
            detailsBinding.rvStreaming.adapter = streamingAdapter
            streamingAdapter.submitList(streams)
            detailsBinding.cvStreaming.visibility = View.VISIBLE
        })


        if (!args.details.alternative.isNullOrEmpty()) {
            val alternativeAdapter = AlternativeDownloadAdapter(this)
            detailsBinding.rvAlternativeList.adapter = alternativeAdapter
            alternativeAdapter.submitList(args.details.alternative)
        }

        viewModel.deletedDownloadLiveData.observe(viewLifecycleOwner, EventObserver {
                // todo: check returned value (it)
                activity?.baseContext?.showToast(R.string.download_removed)
                // if deleted go back
                activity?.onBackPressed()
                activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_DOWNLOAD)
        })

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            // the delete operation is observed from the viewModel
            if (bundle.getBoolean("deleteConfirmation"))
                viewModel.deleteDownload(args.details.id)
        }

        if (args.details.streamable == 1)
            viewModel.fetchStreamingInfo(args.details.id)

        return detailsBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_details_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                val dialog = DeleteDialogFragment()
                dialog.show(parentFragmentManager, "DeleteDialogFragment")
                true
            }
            R.id.share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                val shareLink = args.details.download
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareLink)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("Real-Debrid Download Link", text)
        context?.showToast(R.string.link_copied)
    }

    override fun onOpenClick(url: String) {
        openExternalWebPage(url)
    }

    override fun onOpenWith(url: String) {

        val yatseIntent = Intent().apply {
            action = "org.leetzone.android.yatsewidget.ACTION_MEDIA_PLAYURI"
            component = ComponentName(
                "org.leetzone.android.yatsewidgetfree",
                "org.leetzone.android.yatsewidget.service.core.YatseCommandService"
            )
            putExtra("org.leetzone.android.yatsewidget.EXTRA_STRING_PARAMS", url)
        }

        // we already check once if it is installed, but this also takes care if yatse get uninstalled while this fragment is active
        if (isYatseInstalled()) {
            try {
                ContextCompat.startForegroundService(requireContext(), yatseIntent)
            } catch (e: IllegalStateException) {
                context?.showToast(R.string.limitations)
            }
        } else
            context?.showToast(R.string.app_not_installed)
    }

    //already added the query
    @SuppressLint("QueryPermissionsNeeded")
    private fun isYatseInstalled(): Boolean {
        return context?.packageManager
            ?.getInstalledPackages(PackageManager.GET_META_DATA)
            ?.firstOrNull { it.packageName == "org.leetzone.android.yatsewidgetfree" } != null
    }
}

interface DownloadDetailsListener {
    fun onCopyClick(text: String)
    fun onOpenClick(url: String)
    fun onOpenWith(url: String)
}