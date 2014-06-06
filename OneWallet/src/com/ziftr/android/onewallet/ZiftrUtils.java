package com.ziftr.android.onewallet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ZiftrUtils {
	
	
	private static final ExecutorService threadService = Executors.newCachedThreadPool();
	
	
	private static final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US); //formatting for date written like "2013-08-26T13:59:30-04:00"
	private static final DateFormat formatterExpanded = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss Z", Locale.US); //formatting for date written like "Tue, 19 Nov 2013 10:28:31 -0500"
	private static final DateFormat formatterNoTimeZone = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //formatting for date written like "2013-12-11 10:59:48"

	
	
	
	/**
	 * Converts a date string in a format such as "2013-08-26T13:59:30-04:00" into milliseconds
	 * @param dateString string representing the date
	 * @return time in milliseconds, or -1 if the string cannot be parsed to a date
	 */
	public static long getTimeMillis(String dateString) {
		
		try {
			Date date = formatter.parse(dateString);
			return date.getTime();
		} 
		catch (ParseException e) {
			ZLog.log("Exception trying to parse date: ", dateString, " - ", e);
		}
		
		return -1;
	}
	
	
	/**
	 * Converts a date string in a format such as "Tue, 19 Nov 2013 10:28:31 -0500" into milliseconds
	 * @param dateString string representing the date
	 * @return time in milliseconds, or -1 if the string cannot be parsed to a date
	 */
	public static long getTimeMillisExpanded(String expandedDateString) {
		
		try {
			Date date = formatterExpanded.parse(expandedDateString);
			return date.getTime();
		} 
		catch (ParseException e) {
			ZLog.log("Exception trying to parse date: ", expandedDateString, " - ", e);
		}
		
		return -1;
	}
	
	
	/**
	 * Converts a date string in a format such as "2013-12-11 10:59:48" into milliseconds.
	 * Assumes users current time zone.
	 * @param noTimeZoneDateString string representing the date
	 * @return time in milliseconds, or -1 if string cannot be parsed to a date
	 */
	public static long getTimeMillisNoTimeZone(String noTimeZoneDateString) {
		try {
			Date date = formatterNoTimeZone.parse(noTimeZoneDateString);
			return date.getTime();
		}
		catch(ParseException e) {
			ZLog.log("Exception trying to parse date: ", noTimeZoneDateString, " - ", e);
		}
		
		return -1;
	}
	
	
	public static int convertDpToPixels(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}
	
	
	/**
	 * takes a timestamp in milliseconds and changes it to a string format the server expects, such as "2013-08-26T13:59:30-04:00"
	 * @param timestamp time in ms
	 * @return formatted string representing the date and time of the timestamp
	 */
	public static String formatTimestamp(long timestamp) {
		
		Date date = new Date(timestamp);
		return formatter.format(date);
	}
	
	
	
	/**
	 * Pipes the data in an input stream to an output stream.
	 * Both streams will be wrapped in buffered streams before piping.
	 * The input stream will be closed when finished.
	 * The output stream will be flushed and closed when finished.
	 * Note: this call is done on the calling thread and will block if
	 * the streams are file or network streams
	 * @param input an {@link InputStream} containing the data to be read
	 * @param output an {@link OutputStream} to write the data to
	 */
	public static void pipeStreams(InputStream input, OutputStream output) {
		
		BufferedInputStream inStream = new BufferedInputStream(input);
		BufferedOutputStream outStream = new BufferedOutputStream(output);
		
		try {
			byte[] buffer = new byte[4096];
			int read = inStream.read(buffer);
			while(read >= 0) {
				
				outStream.write(buffer, 0, read);
				read = inStream.read(buffer);
			}
			outStream.flush();
			
		} 
		catch (IOException e) {
			ZLog.log("Exception piping streams: ", e);
		}
		finally {
			try {
				inStream.close();
			} 
			catch (IOException e) {
				ZLog.log(e);
			}
			
			try {
				outStream.close();
			} 
			catch (IOException e) {
				ZLog.log(e);
			}
		}
		
	}
	
	
	
	/**
	 * takes an input stream, reads it into a string, then closes the stream
	 * @param stream An {@link InputStream} to be read and closed.
	 * @return a String of whatever was contained in the input stream, returns a blank strink if there was an error
	 */
	public static String streamToString(InputStream stream) {
		
		StringBuilder builder = new StringBuilder("");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		try {
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
		} 
		catch (IOException e) {
			ZLog.log("Exception reading stream to string: \n", e);
		}
		finally {
			try {
				reader.close();
			}
			catch(Exception e) {}
		}
		
		return builder.toString();
	}
	
	
	/**
	 * a simple method meant to run an anonymous inner class {@link Runnable} as soon as it's called
	 * @param runnable the runnable to be run
	 */
	public static void runOnNewThread(Runnable runnable) {
		threadService.execute(runnable);
	}
	
	
	
	public static String arrayToString(Collection<String> strings, String delimiter) {
		
		StringBuilder stringBuilder = new StringBuilder();
		
		if(strings != null && strings.size() > 0) {
			for(String string : strings) {
				stringBuilder.append(string).append(delimiter);
			}
			
			if(stringBuilder.length() > 0 && delimiter.length() > 0) {
				stringBuilder.deleteCharAt(stringBuilder.length() -1);
			}
		}
		
		return stringBuilder.toString();
	}


	public static void closeKeyboard(Activity activity) {
		InputMethodManager inputMan = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		View currentFocus = activity.getCurrentFocus();
		inputMan.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
	}
	
	
	/**
	 * Gets the locations on the device where app specific files should be installed.
	 * @return {@link File} for the directory 
	 */
	/**
	public static File getZiftrBaseDirectory() {
		
		ZiftrApplication.getAppContext().getExternalFilesDir(null);
		
		Environment.getDataDirectory();
		
		return null;
	}
	**/
	
	

}
