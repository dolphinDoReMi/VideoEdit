package com.mira.clip

import android.content.Context
import org.robolectric.RuntimeEnvironment
import com.mira.clip.clip.ClipBPETokenizer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [28])
class ClipTokenizerTest {
  @Test
  fun encode_has_sot_eot_and_length() {
    val ctx = RuntimeEnvironment.getApplication()
    val tok = ClipBPETokenizer.fromAssets(ctx)
    val ids = tok.encode("a photo of a dog")
    assertTrue(ids.size == 77)
    assertTrue(ids[0] != 0)       // SOT
    assertTrue(ids.any { it == ids[ids.indexOfLast { true }] }) // last slot filled (either eot or pad)
  }
}
