package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.TrackStats;

public class TrackStatsStringDump {
	public static List<String> stringDescription(TrackStats stats){
		List<String> dump = new LinkedList<String>();
			dump.add("--------------------------TrackStats dump");
			dump.add("-------------------------------getNumOfPoints()" + stats.getNumOfPoints());
			dump.add("-------------------------------getStartTime()" + stats.getStartTime());
			dump.add("-------------------------------getStopTime()" + stats.getStopTime());
			dump.add("-------------------------------getTotalTime()" + stats.getTotalTime());
			dump.add("-------------------------------getTotalLength()" + stats.getTotalLength());
		return dump;
	}
}
