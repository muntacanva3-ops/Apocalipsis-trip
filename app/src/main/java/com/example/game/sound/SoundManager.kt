package com.example.game.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.sin

object SoundManager {
  private val executor = Executors.newFixedThreadPool(2)
  private const val SAMPLE_RATE = 22050
  private var isEnginePlaying = false
  private var currentEngineRpmFreq = 55f

  fun playEngineHum(speedKmh: Float) {
    currentEngineRpmFreq = 45f + (speedKmh * 1.2f).coerceIn(0f, 160f)
    if (!isEnginePlaying) {
      isEnginePlaying = true
      executor.execute {
        try {
          val bufferSize = SAMPLE_RATE / 4
          val track = AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

          track.play()
          val buffer = ShortArray(bufferSize)
          var phase = 0.0

          while (isEnginePlaying) {
            val freq = currentEngineRpmFreq.toDouble()
            for (i in buffer.indices) {
              val sample1 = sin(phase)
              val sample2 = sin(phase * 0.5) * 0.5
              val sample3 = ((phase % (2 * Math.PI)) / Math.PI - 1.0) * 0.3
              val mixed = ((sample1 + sample2 + sample3) / 1.8) * 0.28
              buffer[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
              phase += 2.0 * Math.PI * freq / SAMPLE_RATE
              if (phase > 2000.0 * Math.PI) phase -= 2000.0 * Math.PI
            }
            track.write(buffer, 0, buffer.size)
            Thread.sleep(20)
          }
          track.stop()
          track.release()
        } catch (_: Exception) {
          isEnginePlaying = false
        }
      }
    }
  }

  fun stopEngine() {
    isEnginePlaying = false
  }

  fun playItemChime() {
    playToneSequence(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f), 0.08f)
  }

  fun playCashChime() {
    playToneSequence(floatArrayOf(987.77f, 1318.51f), 0.12f)
  }

  fun playZombieHit() {
    playNoiseBurst(durationSec = 0.18f, freqStart = 200f, freqEnd = 60f)
  }

  fun playPunchWhoosh() {
    playNoiseBurst(durationSec = 0.12f, freqStart = 450f, freqEnd = 120f)
  }

  fun playZombieGroan() {
    playToneSequence(floatArrayOf(110f, 95f, 85f), 0.25f)
  }

  fun playBrakeSqueal() {
    playNoiseBurst(durationSec = 0.2f, freqStart = 2200f, freqEnd = 1800f, isHighPitch = true)
  }

  fun playDoorClick() {
    playToneSequence(floatArrayOf(300f, 150f), 0.06f)
  }

  private fun playToneSequence(frequencies: FloatArray, durationPerNote: Float) {
    executor.execute {
      try {
        val totalSamples = (SAMPLE_RATE * durationPerNote * frequencies.size).toInt()
        val buffer = ShortArray(totalSamples)
        val noteSamples = (SAMPLE_RATE * durationPerNote).toInt()

        for (n in frequencies.indices) {
          val freq = frequencies[n].toDouble()
          val startIdx = n * noteSamples
          for (i in 0 until noteSamples) {
            val idx = startIdx + i
            if (idx >= buffer.size) break
            val envelope = 1.0 - (i.toDouble() / noteSamples)
            val sample = sin(2.0 * Math.PI * freq * i / SAMPLE_RATE) * envelope * 0.4
            buffer[idx] = (sample * Short.MAX_VALUE).toInt().toShort()
          }
        }
        playRawPcm(buffer)
      } catch (_: Exception) {}
    }
  }

  private fun playNoiseBurst(durationSec: Float, freqStart: Float, freqEnd: Float, isHighPitch: Boolean = false) {
    executor.execute {
      try {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        val random = java.util.Random()

        for (i in 0 until count) {
          val progress = i.toDouble() / count
          val currentFreq = freqStart + (freqEnd - freqStart) * progress
          val envelope = (1.0 - progress) * (if (isHighPitch) 0.3 else 0.5)
          val osc = sin(phase)
          val noise = (random.nextDouble() * 2.0 - 1.0) * (if (isHighPitch) 0.5 else 0.7)
          val mixed = (osc * 0.4 + noise * 0.6) * envelope
          buffer[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
          phase += 2.0 * Math.PI * currentFreq / SAMPLE_RATE
        }
        playRawPcm(buffer)
      } catch (_: Exception) {}
    }
  }

  private fun playRawPcm(buffer: ShortArray) {
    val track = AudioTrack.Builder()
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      .setAudioFormat(
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(SAMPLE_RATE)
          .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
          .build()
      )
      .setBufferSizeInBytes(buffer.size * 2)
      .setTransferMode(AudioTrack.MODE_STATIC)
      .build()

    track.write(buffer, 0, buffer.size)
    track.play()
    Thread.sleep((buffer.size * 1000L / SAMPLE_RATE) + 50)
    track.release()
  }
}
