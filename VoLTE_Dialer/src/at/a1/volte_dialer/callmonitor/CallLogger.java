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

package at.a1.volte_dialer.callmonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spinlogic.logger.SP_Logger;
import android.os.Environment;

public class CallLogger {
	final static String TAG = "CallLogger";
	private final static Logger LOGGER = Logger.getLogger(SP_Logger.LOGGER_NAME);
	
	public static final String	CSV_CHAR	= ",";	// character to separate entries in log
	
	// Log file directory and name
	public static final String FN_VDDIR = "volte_dialer";
	public static final String FN_VDLOG = "vd_calllog.txt";
	
	private static File logFile;
	
	/**
	 * Selects the file in which to log data initially.
	 * This method should be called when the application starts before 
	 * it logs any data.
	 */
	public static void initializeValues() {
		LOGGER.setLevel(Level.INFO);
		String state = Environment.getExternalStorageState();
		//	We have to select which log file to use.
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File path = new File(Environment.getExternalStorageDirectory() + File.separator + FN_VDDIR);
			//	Make directory if it does not exist
			if(!path.exists()) {
				path.mkdir();
			}
			logFile = new File(path, FN_VDLOG);
		}
	}

	
	public static void appendLog(String text, long date) {
		String METHOD = "::appendLog ";
		
		if (!logFile.exists()) {
			try {
				if(logFile.createNewFile()) {
					// Write the header. Must correspond with logged fields in 
					// at.a1.volte_dialer_dialer.CallDescription
					String logline = "DATE,START TIME,DIRECTION,PREFIX,DURATION,ALERTING,CONNECTED,DISCONNECTION SIDE," +
									 "DISCONNECT CAUSE,START CID,START SIGNAL,END CID,END SIGNAL,SRVCC";
					insertLine(logline);
//					Logger.Log(TAG + METHOD, "appendLog: Creating new logfile");
					LOGGER.info(TAG + METHOD + "appendLog: Creating new logfile");
				}
			}
			catch (IOException e) {
//				Logger.Log(TAG, METHOD + e.getMessage());
				LOGGER.info(TAG + METHOD + "appendLog: Creating new logfile");
			}
		}
		try {
			//	Prepend date and time information to the log data
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
			String currentDateandTime = sdf.format(new Date(date));
			String logline = currentDateandTime + "," + text;
			insertLine(logline);
		}
		catch (IOException e) {
//			Logger.Log(TAG, METHOD + e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	public static void deleteLog() {
		logFile.delete();
	}
	
	private static void insertLine(String logline) throws IOException {
		BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
		buf.append(logline);
		buf.newLine();
		buf.close();
	}
}
