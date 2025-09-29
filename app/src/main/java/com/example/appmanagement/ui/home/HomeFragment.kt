package com.example.appmanagement.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.appmanagement.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ----- Dữ liệu mẫu cho progress Priority
        val done = 3
        val total = 5
        val percent = (done.toFloat() / total * 100f)

        b.tvPrioritySub.text = "$done/$total is completed"
        b.progress.max = 100
        b.progress.setProgress(percent.toInt(), true)
        b.tvPercent.text = String.format("%.2f%%", percent)

        // ----- Số liệu mẫu cho 3 thẻ
        b.tvTotalTask.text = "16"
        b.tvCompleted.text = "32"
        b.tvProjects.text = "8"

        // ----- Click card (tạm thời: Toast)
        b.cardTotalTask.setOnClickListener { toast("Open: Total Task") }
        b.cardCompleted.setOnClickListener { toast("Open: Completed") }
        b.cardProjects.setOnClickListener { toast("Open: Total Projects") }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
