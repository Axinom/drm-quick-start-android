package com.axinom.drm.quickstart.requests.additional;

import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;

public class NoRetryPolicy implements RetryPolicy {

	private static final int DEFAULT_TIMEOUT = 10000;
	/** The current timeout in milliseconds. */
    private int mCurrentTimeoutMs;
	
    /**
     * Constructs a new retry policy with retry count 0.
     */
    public NoRetryPolicy() {
        this(DEFAULT_TIMEOUT);
    }
    
    /**
     * Constructs a new retry policy with retry count 0.
     * @param initialTimeoutMs The initial timeout for the policy.
     */
    public NoRetryPolicy(int initialTimeoutMs) {
        mCurrentTimeoutMs = initialTimeoutMs;
    }
	
	@Override
	public int getCurrentTimeout() {
		return mCurrentTimeoutMs;
	}

	@Override
	public int getCurrentRetryCount() {
		return 0;
	}

	@Override
	public void retry(VolleyError error) throws VolleyError {
		throw error;
	}

}
