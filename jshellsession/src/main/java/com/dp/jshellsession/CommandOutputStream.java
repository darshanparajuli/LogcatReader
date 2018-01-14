/*
 * Copyright (c) 2017 Darshan Parajuli
 */

package com.dp.jshellsession;

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

public class CommandOutputStream implements Closeable {

    private JShellSession mShell;
    private Config mConfig;

    public CommandOutputStream(@NonNull Config config) throws IOException {
        mConfig = config.builder()
                .setRunExitOnClose(false)
                .build();
        mShell = null;
    }

    public void start(@NonNull OnCommandOutputListener listener) throws IOException {
        if (isAlive()) {
            return;
        }

        mShell = new JShellSession(mConfig, listener);
    }

    public boolean isAlive() {
        return mShell != null && mShell.isRunning();
    }

    @Override
    public void close() {
        if (mShell != null) {
            mShell.close();
        }
    }
}
