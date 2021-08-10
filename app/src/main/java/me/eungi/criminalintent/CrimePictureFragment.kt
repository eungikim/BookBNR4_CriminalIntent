package me.eungi.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_PATH = "path"

class CrimePictureFragment : DialogFragment() {

    private lateinit var crimePhotoImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = layoutInflater.inflate(R.layout.dialog_crime_picture, container, false)
        crimePhotoImageView = view.findViewById(R.id.crime_photo_image_view)
        crimePhotoImageView.setOnClickListener {
            this@CrimePictureFragment.dismiss()
        }
        val photoPath = arguments?.getString(ARG_PATH)
        val bitmap = photoPath?.let { getScaledBitmap(it, requireActivity()) }
        crimePhotoImageView.setImageBitmap(bitmap)

        return view
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }


    companion object {
        fun newInstance(path: String): CrimePictureFragment {
            val args = Bundle().apply {
                putString(ARG_PATH, path)
            }
            return CrimePictureFragment().apply {
                arguments = args
            }
        }
    }

}