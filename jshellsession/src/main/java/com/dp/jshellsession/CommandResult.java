/*
 * Copyright (c) 2017 Darshan Parajuli
 */

package com.dp.jshellsession;

import java.util.Arrays;
import java.util.Set;

public class CommandResult {

    private int mExitCode;
    private String[] mStdOut, mErrOut;
    private Set<Integer> mSuccessExitValues;

    CommandResult(int exitCode, Set<Integer> successExitValues) {
        this(exitCode, successExitValues, new String[]{}, new String[]{});
    }

    CommandResult(int exitCode, Set<Integer> successExitValues, String[] stdOut, String[] errOut) {
        mExitCode = exitCode;
        mSuccessExitValues = successExitValues;
        mStdOut = stdOut;
        mErrOut = errOut;
    }

    public String[] stdOut() {
        return mStdOut;
    }

    public String[] errOut() {
        return mErrOut;
    }

    public int exitCode() {
        return mExitCode;
    }

    public boolean exitSuccess() {
        if (mSuccessExitValues.isEmpty()) {
            return mExitCode == 0;
        } else {
            return mSuccessExitValues.contains(mExitCode);
        }
    }

    public boolean exitFailure() {
        return !exitSuccess();
    }

    @Override
    public String toString() {
        return "exit code: " + mExitCode +
                ", stdout: " + Arrays.toString(mStdOut) +
                ", errout: " + Arrays.toString(mErrOut);
    }
}
