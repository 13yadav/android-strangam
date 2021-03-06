package com.strangecoder.socialmedia.presentation.ui.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.strangecoder.socialmedia.R
import com.strangecoder.socialmedia.databinding.FragmentRegisterBinding
import com.strangecoder.socialmedia.commons.EventObserver
import com.strangecoder.socialmedia.presentation.ui.auth.viewmodels.AuthViewModel
import com.strangecoder.socialmedia.presentation.viewutils.snackBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToObservers()
        binding.btnRegister.setOnClickListener {
            viewModel.registerUser(
                email = binding.etEmail.text.toString(),
                username = binding.etUsername.text.toString(),
                password = binding.etPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString()
            )
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun subscribeToObservers() {
        viewModel.registerStatus.observe(viewLifecycleOwner, EventObserver(
            onError = {
                binding.registerProgressBar.isVisible = false
                binding.btnRegister.isVisible = true
                snackBar(it)
            },
            onLoading = {
                binding.registerProgressBar.isVisible = true
                binding.btnRegister.isVisible = false
            },
            onSuccess = {
                binding.registerProgressBar.isVisible = false
                snackBar(getString(R.string.success_registration))
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
        ))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}