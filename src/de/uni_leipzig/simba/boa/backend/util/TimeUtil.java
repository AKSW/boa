/**
 * 
 */
package de.uni_leipzig.simba.boa.backend.util;

import java.util.concurrent.TimeUnit;


/**
 * @author gerb
 *
 */
public class TimeUtil {

    /**
     * Converts a time span in ms to something like this:
     * "12 min, 5s" 
     * 
     * @param millis
     * @return
     */
    public static String convertMilliSeconds(long millis) {
        
        return String.format("%d min, %d sec", 
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - 
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
    
    /**
     * Convert a millisecond duration to a string format
     * 
     * @param millis
     *            A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis) {

        if (millis < 0) throw new IllegalArgumentException("Duration must be greater than zero!");

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(days);
        sb.append(" Days ");
        sb.append(hours);
        sb.append(" Hours ");
        sb.append(minutes);
        sb.append(" Minutes ");
        sb.append(seconds);
        sb.append(" Seconds");

        return (sb.toString());
    }
}
