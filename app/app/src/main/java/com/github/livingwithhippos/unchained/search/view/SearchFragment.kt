package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.databinding.FragmentSearchBinding
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val searchBinding = FragmentSearchBinding.inflate(inflater, container, false)

        return searchBinding.root
    }
}