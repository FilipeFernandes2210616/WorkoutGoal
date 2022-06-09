package com.philopes.workoutgoal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.philopes.workoutgoal.databinding.FragmentProfileBinding
import com.philopes.workoutgoal.databinding.FragmentRankingBinding

class RankingFragment : Fragment() {

    private lateinit var binding : FragmentRankingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }
}