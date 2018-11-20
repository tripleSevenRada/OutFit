package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.Track;

public class TrackStringDump {
	public static List<String> stringDescription(Track track){
		List<String> dump = new LinkedList<String>();
			dump.add("Track dump");
			dump.add("-----Name: " + track.getName());
			dump.add("-----Activity type: " + track.getActivityType());
			dump.add("-----Points count: " + track.getPointsCount());
			dump.add("-----Waypoints count: " + track.getWaypoints().size());
		return dump;
	}
	public static List<String> stringDescriptionDeep(Track track){
		List<String> dump = new LinkedList<String>();
		dump.addAll(stringDescription(track));
		dump.addAll(TrackStatsStringDump.stringDescription(track.getStats()));
		for(Location point : track.getPoints()) {
			dump.addAll(LocationStringDump.stringDescription(point));
		}
		for(Point point : track.getWaypoints()) {
			dump.addAll(PointStringDump.stringDescription(point));
		}
		return dump;
	}
}