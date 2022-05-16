package com.philopes.workoutgoal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.philopes.workoutgoal.databinding.FragmentMainScreenBinding

class MainScreenFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentMainScreenBinding>(inflater,R.layout.fragment_main_screen,container,false)
        return binding.root
    }
}