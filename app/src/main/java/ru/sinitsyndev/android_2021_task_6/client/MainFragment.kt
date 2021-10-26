package ru.sinitsyndev.android_2021_task_6.client

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import ru.sinitsyndev.android_2021_task_6.MainActivity
import ru.sinitsyndev.android_2021_task_6.R
import ru.sinitsyndev.android_2021_task_6.appComponent
import ru.sinitsyndev.android_2021_task_6.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initMediaBrowserConnector(activity as MainActivity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            playBtn.setOnClickListener {
                viewModel.playPause()
            }

            NextBtn.setOnClickListener {
                viewModel.skipToNext()
            }

            prevBtn.setOnClickListener {
                viewModel.skipToPrevious()
            }
        }

        with(viewModel) {
            state.observe(
                viewLifecycleOwner,
                Observer {
                    it ?: return@Observer
                    onStateChanged(it)
                }
            )
            isLoading.observe(
                viewLifecycleOwner,
                Observer {
                    it ?: return@Observer
                    binding.progressBar.isVisible = it
                    disableEnableButtons(!it)
                }
            )
            metadata.observe(
                viewLifecycleOwner,
                Observer {
                    it ?: return@Observer
                    showMetadata(it)
                }
            )
        }
    }

    private fun onStateChanged(_state: PlaybackStateCompat?) {
        Log.d(LOG_TAG, "!!!!onStateChanged $_state")
        if (_state?.state == PlaybackStateCompat.STATE_PLAYING) {
            binding.playBtn.text = getString(R.string.pause)
        } else {
            binding.playBtn.text = getString(R.string.play)
        }
    }

    private fun disableEnableButtons(value: Boolean) {
        with(binding) {
            playBtn.isEnabled = value
            prevBtn.isEnabled = value
            NextBtn.isEnabled = value
        }
    }

    private fun showMetadata(_metadata: MediaMetadataCompat?) {
        if (_metadata == null) {
            with(binding) {
                artist.text = ""
                trakTitle.text = ""
                imageView.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            with(binding) {
                artist.text = _metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                trakTitle.text = _metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                imageView.setImageBitmap(_metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
