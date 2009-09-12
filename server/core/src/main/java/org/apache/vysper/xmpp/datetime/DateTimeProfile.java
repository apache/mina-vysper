package org.apache.vysper.xmpp.datetime;

import org.apache.vysper.compliance.SpecCompliant;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * provides dates and times in XMPP conform formats
 */
@SpecCompliant(spec = "XEP-0082", status = IN_PROGRESS, coverage = COMPLETE)
public class DateTimeProfile {

    protected static final TimeZone TIME_ZONE_UTC;
    
    protected static final SimpleDateFormat utcDateFormatter;
    protected static final SimpleDateFormat utcDateTimeFormatter;
    protected static final SimpleDateFormat utcTimeFormatter;

    protected static final SimpleDateFormat utcDateParser;
    protected static final SimpleDateFormat utcDateTimeParser;
    protected static final SimpleDateFormat utcTimeParser;

    
    static {
        TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
        utcDateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        utcDateTimeFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC
        utcDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        utcDateFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC
        utcTimeFormatter = new SimpleDateFormat("HH:mm:ss'Z'");
        utcTimeFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC

        utcDateTimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        utcDateParser = new SimpleDateFormat("yyyy-MM-dd");
        utcTimeParser = new SimpleDateFormat("HH:mm:ss");
    }
    
    private final static DateTimeProfile SINGLETON = new DateTimeProfile();

    public static DateTimeProfile getInstance() {
        return SINGLETON;     
    }
    
    protected DateTimeProfile() {
        // empty
    }

    public String getDateTimeInUTC(Date time) {
        return utcDateTimeFormatter.format(time);
    }

    public String getDateInUTC(Date time) {
        return utcDateFormatter.format(time);
    }

    public String getTimeInUTC(Date time) {
        return utcTimeFormatter.format(time);
    }

    public Date fromDateTime(String time) throws ParseException {
        return parseWithTz(utcDateTimeParser, time, false);
    }

    public Date fromTime(String time) throws ParseException {
        return parseWithTz(utcTimeParser, time, true);
    }

    public Date fromDate(String time) throws ParseException {
        return utcDateParser.parse(time);
    }

    
    private Date parseWithTz(DateFormat format, String time, boolean optionalTz) throws ParseException {
        int tzOffset;
        String timeWithoutTz;
        // tz is required for datetimes and optional for times by XEP-0082
        if(time.endsWith("Z")) {
            timeWithoutTz = time.substring(0, time.length() - 1);
            tzOffset = 0;
        } else {
            Pattern tzPattern = Pattern.compile("([+-])(\\d\\d):(\\d\\d)$");
            
            Matcher matcher = tzPattern.matcher(time);
            
            if(matcher.find()) {
                timeWithoutTz = time.substring(0, time.length() - 6);
                
                String sign = matcher.group(1);
                int hours = Integer.parseInt(matcher.group(2));
                int min = Integer.parseInt(matcher.group(3));
                
                tzOffset = hours * 60 + min;
                if(sign.equals("-")) tzOffset = -tzOffset;
            } else {
                if(optionalTz) {
                    timeWithoutTz = time;
                    tzOffset = 0;
                } else {
                    throw new IllegalArgumentException("Time zone required for date time: " + time);
                }
            }
        }
        
        // parse without time zone
        Date actual = format.parse(timeWithoutTz);
        
        // correct for time zone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(actual);
        cal.add(Calendar.MINUTE, tzOffset);
        return cal.getTime();
        
    }
}
