package com.madchan.glidegifcompressor.library;

import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Drops input frames to respect the output frame rate.
 */
public abstract class VideoFrameDropper {

    private final static String TAG = VideoFrameDropper.class.getSimpleName();

    private VideoFrameDropper() {}

    public abstract boolean shouldRenderFrame(long presentationTimeUs);

    @NonNull
    public static VideoFrameDropper newDropper(int inputFrameRate, int outputFrameRate) {
        return new Dropper1(inputFrameRate, outputFrameRate);
    }

    /**
     * A simple and more elegant dropper.
     * Reference: https://stackoverflow.com/questions/4223766/dropping-video-frames
     */
    private static class Dropper1 extends VideoFrameDropper {

        private double mInFrameRateReciprocal;
        private double mOutFrameRateReciprocal;
        private double mFrameRateReciprocalSum;
        private int mFrameCount;

        private Dropper1(int inputFrameRate, int outputFrameRate) {
            mInFrameRateReciprocal = 1.0d / inputFrameRate;
            mOutFrameRateReciprocal = 1.0d / outputFrameRate;
            Log.i(TAG, "inFrameRateReciprocal:" + mInFrameRateReciprocal + " outFrameRateReciprocal:" + mOutFrameRateReciprocal);
        }

        @Override
        public boolean shouldRenderFrame(long presentationTimeUs) {
            mFrameRateReciprocalSum += mInFrameRateReciprocal;
            if (mFrameCount++ == 0) {
                Log.v(TAG, "RENDERING (first frame) - frameRateReciprocalSum:" + mFrameRateReciprocalSum);
                return true;
            } else if (mFrameRateReciprocalSum > mOutFrameRateReciprocal) {
                mFrameRateReciprocalSum -= mOutFrameRateReciprocal;
                Log.v(TAG, "RENDERING - frameRateReciprocalSum:" + mFrameRateReciprocalSum);
                return true;
            } else {
                Log.v(TAG, "DROPPING - frameRateReciprocalSum:" + mFrameRateReciprocalSum);
                return false;
            }
        }
    }
}
