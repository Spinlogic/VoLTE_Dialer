
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

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


import android.os.Environment;
import android.util.Log;

public class SP_Logger {
	  static private FileHandler fileTxt;
	  static private SimpleFormatter formatterTxt;
	  
	  public static final String LOGGER_NAME	= "at.a1.volte_dialer";
	  public static final String FN_LOGDIR		= "volte_dialer";

	  static public void setup() throws IOException {

	    // get the global logger to configure it
//	    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		Logger logger = Logger.getLogger(LOGGER_NAME);

	    // suppress the logging output to the console
//	    Logger rootLogger = Logger.getLogger("");
//	    Handler[] handlers = rootLogger.getHandlers();
//	    if (handlers[0] instanceof ConsoleHandler) {
//	      rootLogger.removeHandler(handlers[0]);
//	    }
	    
//		We have to select which log file to use.
	    String logpath = "";
	    String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File path = new File(Environment.getExternalStorageDirectory() + 
								File.separator + FN_LOGDIR);
			
			//	Make directory if it does not exist
			if(!path.exists()) {
				path.mkdir();
			}
			logpath = path.getPath();
		}

	    logger.setLevel(Level.INFO);
	    fileTxt = new FileHandler(logpath + "/logging_%u_%g.log", 5242880, 5);

	    // create a TXT formatter
	    formatterTxt = new SimpleFormatter();
	    fileTxt.setFormatter(formatterTxt);
	    logger.addHandler(fileTxt);
	  }

}
