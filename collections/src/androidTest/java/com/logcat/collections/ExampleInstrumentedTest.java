package com.logcat.collections;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
  @Test
  public void useAppContext() throws Exception {
    // Context of the app under test.
    final Context appContext = InstrumentationRegistry.getTargetContext();

    assertEquals("com.trakam.collections.test", appContext.getPackageName());
  }
}
