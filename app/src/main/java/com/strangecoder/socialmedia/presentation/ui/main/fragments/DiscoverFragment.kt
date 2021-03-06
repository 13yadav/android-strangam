package com.strangecoder.socialmedia.presentation.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.strangecoder.socialmedia.R
import com.strangecoder.socialmedia.databinding.FragmentDiscoverBinding
import com.strangecoder.socialmedia.commons.EventObserver
import com.strangecoder.socialmedia.presentation.ui.MainActivity
import com.strangecoder.socialmedia.presentation.ui.main.adapters.UserAdapter
import com.strangecoder.socialmedia.presentation.ui.main.viewmodels.DiscoverViewModel
import com.strangecoder.socialmedia.presentation.viewutils.snackBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoverViewModel by viewModels()

    @Inject
    lateinit var discoverAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        setUpRecyclerView()

        viewModel.getAllUsers()

        discoverAdapter.setOnUserClickListener { user ->
            if (user.uid == FirebaseAuth.getInstance().uid!!) {
                (requireActivity() as MainActivity).bottomNav.selectedItemId = R.id.profileFragment
                return@setOnUserClickListener
            }
            findNavController()
                .navigate(SearchFragmentDirections.actionGlobalToOthersProfileFragment(user.uid))
        }
    }

    private fun subscribeToObservers() {
        viewModel.users.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.progressBar.isVisible = false
                snackBar(it)
            },
            onLoading = {
                binding.progressBar.isVisible = true
            }
        ) { users ->
            binding.progressBar.isVisible = false
            discoverAdapter.users = users
        })
    }

    private fun setUpRecyclerView() = binding.rvDiscoverPeople.apply {
        adapter = discoverAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}