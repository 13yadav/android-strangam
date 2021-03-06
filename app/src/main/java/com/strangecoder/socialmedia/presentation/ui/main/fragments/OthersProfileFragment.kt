package com.strangecoder.socialmedia.presentation.ui.main.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.strangecoder.socialmedia.AuthNavGraphDirections
import com.strangecoder.socialmedia.R
import com.strangecoder.socialmedia.data.entities.User
import com.strangecoder.socialmedia.commons.EventObserver

class OthersProfileFragment : ProfileFragment() {

    private val args: OthersProfileFragmentArgs by navArgs()

    override val uid: String
        get() = args.uid

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnEditProfile.isVisible = false
        binding.btnToggleFollow.isVisible = true
        binding.btnMessage.isVisible = true

        subscribeToObservers()
        binding.btnToggleFollow.setOnClickListener {
            viewModel.toggleFollowForUser(uid)
        }
        binding.btnMessage.setOnClickListener {
            findNavController().navigate(AuthNavGraphDirections.actionGlobalToChattingFragment(uid))
        }
    }

    private fun subscribeToObservers() {
        viewModel.profileMeta.observe(viewLifecycleOwner, EventObserver {
            setupToggleFollowBtn(it)
            currentUser = it
        })
        viewModel.followStatus.observe(viewLifecycleOwner, EventObserver {
            currentUser?.isFollowing = it
            setupToggleFollowBtn(currentUser ?: return@EventObserver)
        })
    }

    private fun setupToggleFollowBtn(user: User) {
        binding.btnToggleFollow.apply {
            if (user.isFollowing) {
                text = requireContext().getString(R.string.unfollow)
//                setTextColor(Color.RED)
            } else {
                text = requireContext().getString(R.string.follow)
            }
        }
    }
}