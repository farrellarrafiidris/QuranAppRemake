package com.faizul.quranapp.presentation.quran

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.faizul.quranapp.R
import com.faizul.quranapp.adapter.ListAyahAdapter
import com.faizul.quranapp.core.data.network.quran.AyahsItem
import com.faizul.quranapp.core.data.network.quran.SurahItem
import com.faizul.quranapp.databinding.ActivityDetailSurahBinding
import com.faizul.quranapp.databinding.CustomViewAlertdialogBinding

class DetailSurahActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailSurahBinding
    private val viewModel: QuranViewModel by viewModels()

//    Media Player for Audio
    private var _mediaPlayer: MediaPlayer? = null
    private val mediaPlayer get() = _mediaPlayer as MediaPlayer

    companion object {
        const val EXTRA_DATA = "extra_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailSurahBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val surah = intent.getParcelableExtra<SurahItem>(EXTRA_DATA)
//        Log.i("DetailActivity", data?.name.toString())

        binding.apply {
            tvDetailAyah.text = "${surah?.revelationType} - ${surah?.numberOfAyahs} Ayahs"
            tvDetailName.text = surah?.name
            tvDetailSurah.text = surah?.englishName
            tvDetailNameTranslation.text = surah?.englishNameTranslation
        }

        surah?.number?.let { viewModel.getListAyah(it)}

        val mAdapter = ListAyahAdapter()
        mAdapter.setOnItemClickCallback(object : ListAyahAdapter.OnItemClickCallback{
            override fun onItemClicked(data: AyahsItem) {
                showCustomDialog(data, surah)
            }

        })

        viewModel.listAyah.observe(this@DetailSurahActivity) { ayah ->
            binding.rvSurah.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(this@DetailSurahActivity)
            }
            mAdapter.setData(ayah.quranEdition.get(0).listAyahs, ayah.quranEdition)
        }
    }

    private fun showCustomDialog(dataAudio: AyahsItem, surah: SurahItem?) {
        val progressDialog = AlertDialog.Builder(this@DetailSurahActivity).create()
        val view = CustomViewAlertdialogBinding.inflate(layoutInflater)
        progressDialog.setView(view.root)

        view.apply {
            tvSurah.text = surah?.englishName
            tvName.text = surah?.name
            tvNumberAyah.text = "Ayah ${dataAudio?.numberInSurah}"
        }

        progressDialog.setOnShowListener {
            view.btnPlay.visibility = View.GONE
            view.loadingView.visibility = View.VISIBLE
            view.loadingView.isEnabled = false
            dataAudio?.audio?.let {sound -> loadAudio(sound, view, progressDialog)}
        }

        progressDialog.setOnDismissListener {
            binding.progressBar.visibility = View.GONE
        }

        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    private fun loadAudio(
        sound: String,
        view: CustomViewAlertdialogBinding,
        progressDialog: AlertDialog
    ) {
        _mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            )
            try {
                setDataSource(sound)
                prepareAsync()
                setOnPreparedListener{
                    view.btnPlay.isEnabled = true
                    view.loadingView.visibility = View.GONE
                    view.btnPlay.visibility = View.VISIBLE
                    view.btnPlay.text = getString(R.string.play_audio)
                    view.btnPlay.setOnClickListener {btnPlay->
                        btnPlay.isEnabled = false
                        view.btnPlay.text = getString(R.string.playing_audio)
                        start()
                    }

                    view.btnCancel.setOnClickListener {
                        stop()
                        progressDialog.dismiss()
                    }

                    setOnCompletionListener {
                        progressDialog.dismiss()
                    }


                    binding.progressBar.visibility = View.GONE
                    binding.root.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialog.dismiss()
            }
        }
    }
}