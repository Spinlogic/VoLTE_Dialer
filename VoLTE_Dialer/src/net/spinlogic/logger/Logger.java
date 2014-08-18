
package net.spinlogic.logger;
/**
 *  Part of the dialer for testing VoLTE network side KPIs.
 *  
 *   Copyright (C) 2014  Spinlogic
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class Logger {
	public static final String TAG 	= "Logger";
	
	static File 			logFile;
	public static long	 	KMaxLogfileSize = 10485760;	// 10 MB
	
	public static final boolean isLogActive = true;
	
	// Log file directory and name
	public static final String FN_LOGDIR 	= "volte_dialer";
	public static final String FN_LOGFILE1 	= "log1.txt";
	public static final String FN_LOGFILE2 	= "log2.txt";
	
	/**
	 * Selects the file in which to log data initially.
	 * This method should be called when the application starts before 
	 * it logs any data.
	 */
	public static void initializeValues() {
		String state = Environment.getExternalStorageState();
		
		//	We have to select which log file to use.
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File path = new File(Environment.getExternalStorageDirectory() + 
								File.separator + FN_LOGDIR);
			
			//	Make directory if it does not exist
			if(!path.exists()) {
				path.mkdir();
			}
			
			//	Try first with FN_SOSLOG
//			String imei = HWFunctionsContainer.getIMEI();
//			logFile = new File(path, imei + "_" + GlobalConstants.FN_SOSLOG);
			logFile = new File(path, FN_LOGFILE1);
			if(logFile.exists()) {
				if(logFile.length() >= (KMaxLogfileSize / 2)) {
					logFile = null;		// Destroy current object
//					logFile = new File(path, imei + "_" + GlobalConstants.FN_SOSLOG2);
					logFile = new File(path, FN_LOGFILE2);
				}
			}
		}
	}
	
	
	/**
	 * Switch log files between FN_SOSLOG and FN_SOSLOG2 
	 */
	private static void switchLogFiles() {
		String filename = logFile.getName();
		String path 	= logFile.getParent();
//		String imei = HWFunctionsContainer.getIMEI();
//		if(filename.equalsIgnoreCase(imei + "_" + GlobalConstants.FN_SOSLOG)) {
		if(filename.equalsIgnoreCase(FN_LOGFILE1)) {
			logFile = null;
//			logFile = new File(path, imei + "_" + GlobalConstants.FN_SOSLOG2);
			logFile = new File(path, FN_LOGFILE2);
			if(logFile.exists()) {
				//	if the file exist, then delete it
				logFile.delete();
			}
		}
		else {
			logFile = null;
//			logFile = new File(path, imei + "_" + GlobalConstants.FN_SOSLOG);
			logFile = new File(path, FN_LOGFILE1);
			if(logFile.exists()) {
				logFile.delete();
			}
		}
	}

	
	public static void Log(String header, String content) {
		final String METHOD = "::appendLog   ";
		if(isLogActive) {
			if(KMaxLogfileSize > 0) {
			   if (!logFile.exists()) {
			      try {
			        logFile.createNewFile();
			      }
			      catch (IOException e) {
			    	  Log.d(TAG + METHOD, "appendLog: " + e.getMessage());
			      }
			   }
			   try {
				   //	Prepend date and time information to the log data
				   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
				   String currentDateandTime = sdf.format(new Date());
				   String logline = currentDateandTime + "\t" + header + "\t" + content;
				   //BufferedWriter for performance, true to set append to file flag
				   BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
				   buf.append(logline);
				   buf.newLine();
				   buf.close();
			   }
			   catch (IOException e) {
				   Log.d(TAG + METHOD, "appendLog: " + e.getMessage());
			   }
			   if(logFile.length() >= (KMaxLogfileSize / 2)) {
				   switchLogFiles();
			   }
			}
		}
	}
}
