/**
 *  This file is part of RsyncDroid.
 *  http://code.google.com/p/rsyncdroid
 *  
 *  RsyncDroid is open source software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  RsyncDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with TunnelDroid.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  @author Mario Izquierdo (mariodebian) <mariodebian@gmail.com>
 */
package com.thinetic.rsyncdroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.InputStreamReader;
import java.io.OutputStream;
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RsyncDroid extends Activity {
	
	private static final String LOG_TAG = "RsyncDro";
	private static final String RSYNCD_DIR= "/sdcard/rsyncdroid/";
	private static final String RSYNCD_CONF= "/sdcard/rsyncdroid/rsyncd.conf";
	private static final String RSYNCD_BIN= "/system/bin/rsync";
	private Process process;
	private static final String [] DEFAULT_CONF = {"uid=0","gid=0","read only = yes",
	                                               "use chroot = no","","[sdcard]",
	                                               " path = /sdcard/"," comment = SD Card"};
	
	CheckBox CheckboxRunning;
	Button btnStart;
	Button btnStop;
	EditText txtBox;
	TextView conftxt;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        CheckboxRunning = (CheckBox) findViewById(R.id.Running);
        txtBox = (EditText) findViewById(R.id.RsyncdConf);
        conftxt = (TextView) findViewById(R.id.ConfText);
        
		btnStart = (Button) findViewById(R.id.ButtonStart);
		btnStart.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// save conf in file
				saveConf();
				startRsync();
				Log.d(LOG_TAG, "onCreate() rsync started...");
				changeStatus();
				if (statusRsync()) {
					showMsg("rsync started");
				}
				setResult(android.app.Activity.RESULT_OK);
			}

		});
		
		
		btnStop = (Button) findViewById(R.id.ButtonStop);
		btnStop.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if ( stopRsync() ){
					showMsg("rsync stopped");
					setResult(android.app.Activity.RESULT_OK);
				}
				changeStatus();
			}

		});
		
		// load configuration in txtBox
		txtBox.setText( loadConf().toString() );
		
		// change checkbox status
		changeStatus();
    }
    
    public void changeStatus() {
    	if ( statusRsync() ) {
			CheckboxRunning.setChecked(true);
			txtBox.setVisibility(View.INVISIBLE);
			conftxt.setVisibility(View.INVISIBLE);
		}
		else {
			CheckboxRunning.setChecked(false);
			txtBox.setVisibility(View.VISIBLE);
			conftxt.setVisibility(View.VISIBLE);
		}
    	
    }
    
    
    @SuppressWarnings("static-access")
	public void startRsync() {
    	if( ! new File(RSYNCD_BIN).exists() ) {
			showMsg("rsync not installed");
			return;
		}
    	
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
		    		process = Runtime.getRuntime().exec("su -c sh");
		    		//process = Runtime.getRuntime().exec("sh");
		    		OutputStream os = process.getOutputStream();
		    		Log.d(LOG_TAG, "startRsync() cmd='"+RSYNCD_BIN +" --daemon --config " +  RSYNCD_CONF+"'");
		    		writeLine( os, RSYNCD_BIN +" --daemon --config " +  RSYNCD_CONF + " &");
		    		os.flush();
		    		//process.waitFor();
				}
				catch ( IOException e ) {
		    		e.printStackTrace();
		    	}
		    	/*catch (InterruptedException e) {
					e.printStackTrace();
				}*/
			}
		};
		thread.start();
		// sleep to give some time to statusRsync to detect process
		try{
		  Thread.currentThread().sleep(1000);//sleep for 1000 ms
		}
		catch(InterruptedException e){
			e.printStackTrace();
		}
    }
    
    public boolean stopRsync() {
    	String pid;
    	String temp;
    	pid = "";
    	int i;
    	try{
    		Process p = Runtime.getRuntime().exec("ps");
			p.waitFor();
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ( (temp = stdInput.readLine()) != null ) {
				//Log.d(LOG_TAG, "stopRsync() temp='"+temp+"'");
				if ( temp.contains(RSYNCD_BIN) ) {
					Log.d(LOG_TAG, "statusRsync() temp='"+temp+"'");
					String [] cmdArray = temp.split(" +");
					for (i=0; i< cmdArray.length; i++) {
						Log.d(LOG_TAG, "loop i="+ i + " => " + cmdArray[i]);
					}
					pid = cmdArray[1];
				}
			}
    		
    	}
    	catch (IOException e) {
			e.printStackTrace();
		}
	    catch (InterruptedException e) {
			e.printStackTrace();
		}
	    
	    Log.d(LOG_TAG, "statusRsync() pid='"+pid+"'");
	    
	    // if pid
	    if ( pid != "") {
	    	Log.d(LOG_TAG, "statusRsync() killing='"+pid+"' ...");
	    	try {
		    	process = Runtime.getRuntime().exec("su -c sh");
		    	//process = Runtime.getRuntime().exec("sh");
				OutputStream os = process.getOutputStream();
				writeLine( os, "kill -9 " + pid); os.flush();
				writeLine( os, "exit \n"); os.flush();
				process.waitFor();
				return true;
	    	}
	    	catch (IOException e) {
				e.printStackTrace();
			}
		    catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
	    Log.d(LOG_TAG, "statusRsync() pid empty='"+pid+"'");
	    return false;
    }
    
    public boolean statusRsync() {
    	boolean run;
    	String temp;
    	
    	run=false;
    	Log.d(LOG_TAG, "statusRsync() init");
    	
    	try {
			Process p = Runtime.getRuntime().exec("ps");
			p.waitFor();
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ( (temp = stdInput.readLine()) != null ) {
				if ( temp.contains(RSYNCD_BIN) ) {
					Log.d(LOG_TAG, "statusRsync() temp='"+temp+"'");
					run = true;
				}
			}
	    }
	    catch (IOException e) {
			e.printStackTrace();
		}
	    catch (InterruptedException e) {
			e.printStackTrace();
		}
    	return run;
    }
    
	public String loadConf() {
		String Script="";
		File rsyncd_conf_folder = new File(RSYNCD_DIR);
		
		if(!rsyncd_conf_folder.exists())
		{
			Log.d(LOG_TAG, "Creating "+RSYNCD_DIR+" folder");
			rsyncd_conf_folder.mkdir();
			showMsg("Created "+RSYNCD_DIR+" folder");
		}
		else {
			Log.d(LOG_TAG, RSYNCD_DIR+" exists");
		}
		
		// create rsyncd.conf
		if( ! new File(RSYNCD_CONF).exists() ) {
			showMsg("rsyncd.conf no exists");
			for (int i=0; i< DEFAULT_CONF.length; i++){
				Script += DEFAULT_CONF[i] + "\n";
			}
			return Script;
		}
		
		
		try {
	        BufferedReader in = new BufferedReader(new FileReader(RSYNCD_CONF));
	        String str;
	        
	        while ((str = in.readLine()) != null) {
	        	Script += str + "\n";
	        }
	        in.close();
		}
		catch (Exception ex) {
			showMsg("Can't read rsyncd.conf");
			return Script;
		}
		
		return Script;
	}
	
	
	public void saveConf() {
		Writer output = null;
		Log.d(LOG_TAG, "saveConf() init");
	    try {
	    	output = new BufferedWriter(new FileWriter(RSYNCD_CONF));
	    	output.write( txtBox.getText().toString() );
	    	output.close();
	    	Log.d(LOG_TAG, "saveConf() saved and closed");
	    }
	    catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void showMsg(String txt) {
		CharSequence text = txt;
		int duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(this, text, duration);
		toast.show();
		Log.d(LOG_TAG, "showMsg() txt='"+txt+"'");
	}
	
	
	public static void writeLine(OutputStream os, String value) throws IOException
	{
		String line = value + "\n";
		os.write( line.getBytes() );
	}
	

	
	
	
  
}

