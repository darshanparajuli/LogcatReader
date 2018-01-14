/*
 * Copyright (c) 2017 Darshan Parajuli
 */
package com.dp.jshellsession;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JShellSession implements Closeable {

    private static final String END_MARKER = "[>>END<<]:";

    private Lock mLock;
    private Condition mReachedEndMarkerCondition;
    private volatile boolean mReachedEndMarker;
    private Process mProcess;
    private BufferedWriter mWriter;
    private BufferedReader mStdOutReader;
    private BufferedReader mStdErrReader;

    private List<String> mStdOut;
    private List<String> mStdErr;
    private int mExitCode;
    private Set<Integer> mSuccessExitValues;

    private Thread mThreadStdOut;
    private Thread mThreadStdErr;

    private boolean mRunExitOnClose;

    private OnCommandOutputListener mOnCommandOutputListener;

    public JShellSession(Config config) throws IOException {
        this(config, null);
    }

    JShellSession(Config config, OnCommandOutputListener listener) throws IOException {
        mStdOut = new ArrayList<>();
        mStdErr = new ArrayList<>();
        mLock = new ReentrantLock(true);
        mReachedEndMarkerCondition = mLock.newCondition();
        mReachedEndMarker = false;
        mExitCode = 0;
        mOnCommandOutputListener = listener;
        mSuccessExitValues = new HashSet<>(config.mSuccessExitValues);
        mRunExitOnClose = config.mRunExitOnClose;

        mProcess = createProcess(config);
        mWriter = new BufferedWriter(new OutputStreamWriter(mProcess.getOutputStream()));
        mStdOutReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
        mStdErrReader = new BufferedReader(new InputStreamReader(mProcess.getErrorStream()));

        mThreadStdOut = new Thread(new Runnable() {
            @Override
            public void run() {
                processStdOutput();
            }
        });
        mThreadStdOut.start();

        if (config.mRedirectErrorStream) {
            mThreadStdErr = null;
        } else {
            mThreadStdErr = new Thread(new Runnable() {
                @Override
                public void run() {
                    processErrOutput();
                }
            });
            mThreadStdErr.start();
        }
    }

    private Process createProcess(Config config) throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder(config.mShellCommand);
        processBuilder.redirectErrorStream(config.mRedirectErrorStream);
        processBuilder.environment().putAll(config.mEnv);
        return processBuilder.start();
    }

    public CommandResult run(String cmd) throws IOException {
        return run(cmd, 0);
    }

    private String validateCommand(String cmd) {
        return (cmd == null || cmd.trim().isEmpty()) ? ":" : cmd;
    }

    public static CommandResult quickRun(String cmd) throws IOException {
        return quickRun(Config.defaultConfig(), cmd);
    }

    public static CommandResult quickRun(Config config, String cmd) throws IOException {
        final JShellSession jShellSession = new JShellSession(config);
        try {
            return jShellSession.run(cmd);
        } finally {
            jShellSession.close();
        }
    }

    public CommandResult run(String cmd, long timeout) throws IOException {
        mLock.lock();
        mReachedEndMarker = false;
        try {
            if (mProcess == null) {
                throw new IllegalStateException("session has been closed");
            }

            mStdOut.clear();
            mStdErr.clear();

            mWriter.write(String.format("%s; echo \"%s\"$?", validateCommand(cmd), END_MARKER));
            mWriter.newLine();
            mWriter.flush();

            waitForEndMarker(timeout);

//            MyLogger.logDebug("JShell", "reached return");
            return new CommandResult(mExitCode, mSuccessExitValues, mStdOut.toArray(new String[mStdOut.size()]),
                    mStdErr.toArray(new String[mStdErr.size()]));
        } finally {
            mLock.unlock();
        }
    }

    private void waitForEndMarker(long timeout) {
        if (timeout == 0) {
            while (!mReachedEndMarker) {
                try {
//                    MyLogger.logDebug("JShell", "waiting");
                    mReachedEndMarkerCondition.await();
                } catch (InterruptedException ignored) {
                }
//                MyLogger.logDebug("JShell", "signalled");
            }
//            MyLogger.logDebug("JShell", "done waiting");
        } else {
            long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
            while (!mReachedEndMarker) {
                if (nanos <= 0L) {
                    break;
                }
                try {
//                    MyLogger.logDebug("JShell", "waiting");
                    nanos = mReachedEndMarkerCondition.awaitNanos(nanos);
                } catch (InterruptedException ignored) {
                }
//                MyLogger.logDebug("JShell", "signalled");
            }
//            MyLogger.logDebug("JShell", "done waiting");
        }
    }

    private void processErrOutput() {
        try {
            for (String line = mStdErrReader.readLine(); line != null; line = mStdErrReader.readLine()) {
                line = line.trim();
                mStdErr.add(line);
                if (mOnCommandOutputListener != null) {
                    mOnCommandOutputListener.onNewErrOutLine(line);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void processStdOutput() {
        try {
            for (String line = mStdOutReader.readLine(); line != null; line = mStdOutReader.readLine()) {
                line = line.trim();
                if (line.contains(END_MARKER)) {
                    final int endMarkerIndex = line.indexOf(END_MARKER);
                    if (!line.startsWith(END_MARKER)) {
                        mStdOut.add(line.substring(0, endMarkerIndex).trim());
                    }
                    mExitCode = Integer.parseInt(line.substring(endMarkerIndex + END_MARKER.length()));
                    signal();
                } else {
                    mStdOut.add(line);
                    if (mOnCommandOutputListener != null) {
                        mOnCommandOutputListener.onNewStdOutLine(line);
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (mProcess != null) {
                try {
                    mExitCode = mProcess.waitFor();
                } catch (InterruptedException ignored) {
                    mExitCode = 1;
                }
            }
            signal();
        }
    }

    private void signal() {
        mLock.lock();
        try {
            mReachedEndMarker = true;
            mReachedEndMarkerCondition.signal();
        } finally {
            mLock.unlock();
        }
    }

    public boolean isRunning() {
        if (mProcess == null) {
            return false;
        }
        try {
            mProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException ignored) {
            return true;
        }
    }

    public int getExitCode() {
        if (isRunning()) {
            throw new IllegalStateException("JShellSession is still running");
        }
        return mExitCode;
    }

    @Override
    public void close() {
        if (mProcess != null) {
            if (mRunExitOnClose) {
                try {
                    run("exit");
                } catch (IOException ignored) {
                }
            }

            mProcess.destroy();
            mThreadStdOut.interrupt();
            mProcess = null;
        }

        if (mStdOutReader != null) {
            try {
                mStdOutReader.close();
            } catch (IOException ignored) {
            }
        }

        if (mStdErrReader != null) {
            try {
                mStdErrReader.close();
            } catch (IOException ignored) {
            }
        }

        if (mWriter != null) {
            try {
                mWriter.close();
            } catch (IOException ignored) {
            }
        }

        try {
            mThreadStdOut.join(1000);
        } catch (InterruptedException ignored) {
        }

        if (mThreadStdErr != null) {
            try {
                mThreadStdErr.join(1000);
            } catch (InterruptedException ignored) {
            }
        }

        mOnCommandOutputListener = null;
    }
}
