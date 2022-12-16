package com.hybcode.maplayer.songs

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.hybcode.maplayer.MainActivity
import com.hybcode.maplayer.R
import com.hybcode.maplayer.common.data.model.Song
import com.hybcode.maplayer.databinding.FragmentEditSongBinding
import java.io.FileNotFoundException
import java.io.IOException

class EditSongFragment : Fragment() {

    private var _binding: FragmentEditSongBinding? = null
    private val binding get() = _binding!!
    private var song: Song? = null
    private var newArtwork: Bitmap? = null
    private lateinit var callingActivity: MainActivity

    private val registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                val selectedImageUri = result.data?.data ?: return@registerForActivityResult
                val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedImageUri)
                newArtwork = ImageDecoder.decodeBitmap(source)
                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(binding.editSongArtwork)
            } catch (ignore: FileNotFoundException) {
            } catch (ignore: IOException) { }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = EditSongFragmentArgs.fromBundle(it)
            song = safeArgs.song
        }
        _binding = FragmentEditSongBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        callingActivity = activity as MainActivity
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var editable: Editable = SpannableStringBuilder(song!!.title)
        binding.editSongTitle.text = editable
        editable = SpannableStringBuilder(song!!.artist)
        binding.editSongArtist.text = editable
        editable = SpannableStringBuilder(song!!.track.toString().substring(0, 1))
        binding.editSongDisc.text = editable
        editable = SpannableStringBuilder(song!!.track.toString().substring(1, 4).toInt().toString())
        binding.editSongTrack.text = editable
        editable = SpannableStringBuilder(song!!.year)
        binding.editSongYear.text = editable
// Retrieve the song's album artwork
        callingActivity.insertArtwork(song!!.albumID, binding.editSongArtwork)

        binding.editSongArtwork.setOnClickListener {
            registerResult.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI))
        }
        binding.editSongArtworkIcon.setOnClickListener {
                    registerResult.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI))
                }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.findItem(R.id.search).isVisible = false
        menu.findItem(R.id.save).isVisible = true
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                val newTitle = binding.editSongTitle.text.toString()
                val newArtist = binding.editSongArtist.text.toString()
                val newDisc = binding.editSongDisc.text.toString()
                val newTrack = binding.editSongTrack.text.toString()
                val newYear = binding.editSongYear.text.toString()

                for (string in listOf(newTitle, newArtist, newDisc, newTrack, newYear)) {
                    if (string.isBlank()) {
                        Toast.makeText(activity, getString(R.string.check_fields_not_empty), Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
                val completeTrack = when (newTrack.length) {
                    3 -> newDisc + newTrack
                    2 -> newDisc + "0" + newTrack
                    else -> newDisc + "00" + newTrack
                }.toInt()

// Save the new album artwork if the artwork has been changed
                if (newArtwork != null){
                    val file = callingActivity.getArtworkFile(song?.albumID!!)
                    callingActivity.saveImage(newArtwork!!, file)
                }
                // Check if any metadata fields require updating
                if (newTitle != song!!.title || newArtist != song!!.artist || completeTrack != song!!.track || newYear != song!!.year) {
                    song!!.title = newTitle
                    song!!.artist = newArtist
                    song!!.track = completeTrack
                    song!!.year = newYear
                    callingActivity.updateSongInfo(song!!)
                }
                Toast.makeText(activity, getString(R.string.details_saved), Toast.LENGTH_SHORT).show()
                requireView().findNavController().popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}