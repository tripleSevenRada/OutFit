package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.Location;

import static radim.outfit.core.export.work.GeoCalcKt.toSemiCirclesJava;

public class LocationStringDump {
    public static List<String> stringDescription(Location loc) {
        List<String> dump = new LinkedList<String>();
        dump.add("---------------------Location dump");
        dump.add("--------------------------latitude   " + loc.latitude);
        dump.add("--------------------------latitudeSemicircles   " + toSemiCirclesJava(loc.latitude));
        dump.add("--------------------------longitude   " + loc.longitude);
        dump.add("--------------------------longitudeSemicircles   " + toSemiCirclesJava(loc.longitude));
        dump.add("--------------------------hasSpeed()   " + loc.hasSpeed());
        dump.add("--------------------------getSpeed()   " + loc.getSpeed());
        dump.add("--------------------------getTime()   " + loc.getTime());
        dump.add("--------------------------hasAltitude()   " + loc.hasAltitude());
        dump.add("--------------------------getAltitude()   " + loc.getAltitude());
        return dump;
    }
}