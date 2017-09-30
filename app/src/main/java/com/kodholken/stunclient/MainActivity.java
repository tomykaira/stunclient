/*    
    This file is part of the STUN Client.
    
    Copyright (C) 2010  Magnus Eriksson <eriksson.mag@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.kodholken.stunclient;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();
	private final Object LOCK = new Object();
	private TextView mLogView;
	private DiscoveryTask mCurrentTask = null;
	private Handler mMainHandler;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		mLogView = findViewById(R.id.logtext);

		mMainHandler = new Handler(Looper.getMainLooper());
        
        final Spinner hostSpinner = (Spinner) findViewById(R.id.stun_servers);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.hosts, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hostSpinner.setAdapter(adapter);

		Button goButton = (Button) findViewById(R.id.go_button);
        goButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                appendLog("Performing STUN discovery.");
                restartNewTask((String) hostSpinner.getSelectedItem());
			}
        });
    }

	private void restartNewTask(String hostName) {
        synchronized (LOCK) {
			if (mCurrentTask != null) {
				mCurrentTask.cancel(true);
			}
			mCurrentTask = new DiscoveryTask();
			mCurrentTask.execute(hostName);
		}
	}

	private void appendLog(String log) {
		Log.d(TAG, log);
        mLogView.append(new Date().toString() + ": " + log + "\n");
	}

	private static class Result {
		String host;
		DiscoveryInfo info;
		String error;
	}

	private class DiscoveryTask extends AsyncTask<String, Void, Result> {
		@Override
		protected Result doInBackground(String... strings) {
			String error = null;
			Result result = new Result();
            result.host = strings[0];

			try {
				DiscoveryTest dt = new DiscoveryTest(InetAddress
						.getByName("0.0.0.0"), result.host,
						getHostPort(result.host));
				result.info = dt.test();
				return result;
			} catch (UnknownHostException ex) {
				result.error = "Could not resolve STUN server. Make sure that a network connection is available.";
			} catch (Exception ex) {
				Log.e(TAG, "DiscoveryTest exception", ex);
				result.error = "Discovery error: " + ex.getMessage();
			}

			return result;
		}

		@Override
		protected void onPostExecute(final Result result) {
			super.onPostExecute(result);

            if (result.error != null) {
				appendLog("Error: " + result.error);
			}

			appendLog(result.info.toString());

			mMainHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					restartNewTask(result.host);
				}
			}, 60 * 1000);
		}
	}

    private static int getHostPort(String host) {
    	if (host.equals("stun.sipgate.net")) {
    		return 10000;
    	}
    	
    	return 3478;
    }
}