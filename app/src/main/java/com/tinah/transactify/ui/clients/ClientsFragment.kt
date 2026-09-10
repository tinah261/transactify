package com.tinah.transactify.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tinah.transactify.R
import com.tinah.transactify.databinding.FragmentPlaceholderBinding

/**
 * Répertoire des clients (VIP / REGULAR / ONE_TIME). UI détaillée hors du scope
 * de ce prompt (Phases 1-6) — placeholder de navigation en attendant la Phase 7.
 */
class ClientsFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.placeholderText.setText(R.string.nav_clients)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
