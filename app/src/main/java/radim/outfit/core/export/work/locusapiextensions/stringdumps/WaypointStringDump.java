package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.Point;

public class WaypointStringDump {
	public static List<String> stringDescription(Point point){
		List<String> dump = new LinkedList<String>();
			dump.add("--------------------------Waypoint dump");
			// waypoint is "Point" trackpoint is "Location"
			dump.addAll(PointStringDump.stringDescription(point));
		return dump;
	}
}
