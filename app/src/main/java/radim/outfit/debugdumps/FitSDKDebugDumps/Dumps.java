import java.util.LinkedList;
import java.util.List;

import com.garmin.fit.CourseMesg;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.RecordMesg;

public class Dumps {
	public static List<String> banner(){
		List<String> banner = new LinkedList<String>();
			banner.add("==============================================================");
			banner.add("==============================================================");
			banner.add("==============================================================");
		return banner;
	}
	public static List<String> fileIdMessageDump(FileIdMesg message){
		List<String> data = new LinkedList<String>();
			data.add("=====FileIdMesg dump");
			data.add("==========getName()   " + message.getName());
			data.add("==========getTimeCreated()   " + message.getTimeCreated());
		return data;
	}
	public static List<String> courseMessageDump(CourseMesg message){
		List<String> data = new LinkedList<String>();
			data.add("=====CourseMesg dump");
			data.add("==========getName()   " + message.getName());
			data.add("==========getCapabilities()   " + message.getCapabilities());
		return data;
	}
	public static List<String> lapMessageDump(LapMesg message){
		List<String> data = new LinkedList<String>();
			data.add("=====LapMesg dump");
			data.add("==========getName()   " + message.getName());
			data.add("==========getStartPositionLat()   " + message.getStartPositionLat());
			data.add("==========getStartPositionLong()   " + message.getStartPositionLong());
			data.add("==========getEndPositionLat()   " + message.getEndPositionLat());
			data.add("==========getEndPositionLong()   " + message.getEndPositionLong());
			data.add("==========getStartTime()   " + message.getStartTime());
			data.add("==========getTotalElapsedTime()   " + message.getTotalElapsedTime());
			data.add("==========getTimestamp().toString()   " + message.getTimestamp().toString());
		return data;
	}
	public static List<String> recordMessageDump(RecordMesg message){
		List<String> data = new LinkedList<String>();
			data.add("=====RecordMesg dump");
			data.add("==========getName()   " + message.getName());
			data.add("==========getPositionLat()   " + message.getPositionLat());
			data.add("==========getPositionLong()   " + message.getPositionLong());
			data.add("==========getAltitude()   " + message.getAltitude());
			data.add("==========getDistance()   " + message.getDistance());
			data.add("==========getTimestamp().toString()   " + message.getTimestamp().toString());
		return data;
	}
}
