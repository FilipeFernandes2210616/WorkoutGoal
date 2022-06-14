package com.philopes.workoutgoal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.philopes.workoutgoal.data.models.User
import com.philopes.workoutgoal.databinding.FragmentProfileBinding
import com.philopes.workoutgoal.helpers.Constants

class ProfileFragment : Fragment() {

    private lateinit var binding : FragmentProfileBinding
    private lateinit var username : TextView
    private lateinit var useremail : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = arguments?.get(Constants.USER) as User
        username = view.findViewById(R.id.username)!!
        useremail = view.findViewById(R.id.useremail)!!
        username.text = user.displayName
        useremail.text = user.email

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

}

