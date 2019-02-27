package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.TrackStats;

public class TrackStatsStringDump {
    public static List<String> stringDescription(TrackStats stats) {
        List<String> dump = new LinkedList<String>();
        dump.add("--------------------------TrackStats dump");
        if(stats == null){
            dump.add("NULL");
            return dump;
        }
        dump.add("-------------------------------getNumOfPoints()" + stats.getNumOfPoints());
        dump.add("-------------------------------getStartTime()" + stats.getStartTime());
        dump.add("-------------------------------getStopTime()" + stats.getStopTime());
        dump.add("-------------------------------getTotalTime()" + stats.getTotalTime());
        dump.add("-------------------------------getTotalLength()" + stats.getTotalLength());
        dump.add("-------------------------------getAltitudeMax()" + stats.getAltitudeMax());
        dump.add("-------------------------------getAltitudeMin()" + stats.getAltitudeMin());
        dump.add("-------------------------------getEleNegativeDistance()" + stats.getEleNegativeDistance());
        dump.add("-------------------------------getEleNegativeHeight()" + stats.getEleNegativeHeight());
        dump.add("-------------------------------getElePositiveDistance()" + stats.getElePositiveDistance());
        dump.add("-------------------------------getElePositiveHeight()" + stats.getElePositiveHeight());
        return dump;
    }
}
