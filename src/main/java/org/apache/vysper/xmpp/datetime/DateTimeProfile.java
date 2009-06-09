package org.apache.vysper.xmpp.datetime;

import org.apache.vysper.compliance.SpecCompliant;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.IN_PROGRESS;

import java.util.TimeZone;
import java.util.Date;
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

    static {
        TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
        utcDateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        utcDateTimeFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC
        utcDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        utcDateFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC
        utcTimeFormatter = new SimpleDateFormat("HH:mm:ss'Z'");
        utcTimeFormatter.setTimeZone(TIME_ZONE_UTC); // convert to UTC
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

}
