package com.tinah.transactify.ui.clients

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.tinah.transactify.R
import com.tinah.transactify.databinding.FragmentClientsBinding
import com.tinah.transactify.di.appContainer
import kotlinx.coroutines.launch

/** Répertoire des clients : liste triée par volume, recherche par nom/numéro. */
class ClientsFragment : Fragment() {

    private var _binding: FragmentClientsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClientViewModel by viewModels {
        ClientViewModel.Factory(requireContext().appContainer)
    }

    private val adapter = ClientAdapter { client ->
        findNavController().navigate(
            R.id.action_clientsFragment_to_clientDetailFragment,
            ClientDetailFragment.argsFor(client.phoneNumber),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentClientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clientsList.adapter = adapter

        binding.searchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setQuery(s?.toString().orEmpty())
                }
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.clients.collect { clients ->
                    adapter.submitList(clients)
                    binding.emptyStateText.visibility = if (clients.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.clientsList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
