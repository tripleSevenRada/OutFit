package radim.outfit.debugdumps.FitSDKDebugDumps;

import java.util.LinkedList;
import java.util.List;

import com.garmin.fit.CourseMesg;
import com.garmin.fit.CoursePointMesg;
import com.garmin.fit.EventMesg;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.RecordMesg;

public class Dumps {
    public static List<String> banner() {
        List<String> banner = new LinkedList<String>();
        banner.add("__________________________________________________________________________________________________________");
        return banner;
    }

    public static List<String> fileIdMessageDump(FileIdMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("=====FileIdMesg dump");
        data.add("==========getName()   " + message.getName());
        data.add("==========getTimeCreated()   " + message.getTimeCreated());
        data.add("==========getNumber()   " + message.getNumber());
        data.add("==========getSerialNumber()   " + message.getSerialNumber());
        data.add("==========getType()   " + message.getType());
        return data;
    }

    public static List<String> courseMessageDump(CourseMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("=====CourseMesg dump");
        data.add("==========getName()   " + message.getName());
        data.add("==========getCapabilities()   " + message.getCapabilities());
        data.add("==========getSport()   " + message.getSport());
        return data;
    }

    public static List<String> lapMessageDump(LapMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("=====LapMesg dump");
        data.add("==========getName()   " + message.getName());
        data.add("==========getStartPositionLat()   " + message.getStartPositionLat());
        data.add("==========getStartPositionLong()   " + message.getStartPositionLong());
        data.add("==========getEndPositionLat()   " + message.getEndPositionLat());
        data.add("==========getEndPositionLong()   " + message.getEndPositionLong());
        data.add("==========getStartTime()   " + message.getStartTime());
        data.add("==========getTotalElapsedTime()   " + message.getTotalElapsedTime());
        data.add("==========getTimestamp().toString()   " + message.getTimestamp().toString());
        data.add("==========getMaxAltitude()   " + message.getMaxAltitude());
        data.add("==========getMinAltitude()   " + message.getMinAltitude());
        data.add("==========getTotalDescent()   " + message.getTotalDescent());
        data.add("==========getTotalAscent()   " + message.getTotalAscent());
        return data;
    }

    public static List<String> eventMessageDump(EventMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("=====EventMesg dump");
        data.add("==========getName()   " + message.getName());
        data.add("==========getEvent().name()   " + message.getEvent().name());
        data.add("==========getEventType().name()   " + message.getEventType().name());
        data.add("==========getTimestamp().toString()   " + message.getTimestamp().toString());
        return data;
    }

    public static List<String> recordMessageDump(RecordMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("=====RecordMesg dump");
        data.add("==========getName()   " + message.getName());
        data.add("==========getPositionLat()   " + message.getPositionLat());
        data.add("==========getPositionLong()   " + message.getPositionLong());
        data.add("==========getAltitude()   " + message.getAltitude());
        data.add("==========getDistance()   " + message.getDistance());
        data.add("==========getTimestamp().toString()   " + message.getTimestamp().toString());
        return data;
    }

    public static List<String> recordMessageDumpLine(RecordMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("Rcrd: "  + message.getName() + ", "+ message.getPositionLat() + ", "+ message.getPositionLong() + ", " + message.getAltitude()+ ", " + message.getDistance() + ", " + message.getTimestamp().toString() + ", " + message.getSpeed());
        return data;
    }

    public static List<String> coursePointMessageDumpLine(CoursePointMesg message) {
        List<String> data = new LinkedList<String>();
        if(message == null){
            data.add("NULL");
            return data;
        }
        data.add("CP: "  + message.getName() + ", "
                + message.getType() + ", "
                + message.getPositionLat() + ", "+ message.getPositionLong() + ", "
                + message.getDistance() + ", " + message.getTimestamp().toString());
        return data;
    }

}
