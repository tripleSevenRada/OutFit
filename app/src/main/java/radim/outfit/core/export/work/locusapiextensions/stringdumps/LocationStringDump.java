package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.Location;

import static radim.outfit.core.export.work.GeoCalcKt.toSemiCirclesJava;

public class LocationStringDump {
    public static List<String> stringDescription(Location loc) {
        List<String> dump = new LinkedList<String>();
        dump.add("---------------------Location dump");
        if(loc == null){
            dump.add("NULL");
            return dump;
        }
        dump.add("lat | long: " + loc.latitude + ", " + toSemiCirclesJava(loc.latitude) + " | " + loc.longitude + ", " + toSemiCirclesJava(loc.longitude));
        dump.add("speed: " + loc.hasSpeed() + ", " + loc.getSpeed());
        dump.add("time: " + loc.getTime());
        dump.add("alt: " + loc.hasAltitude() + ", " + loc.getAltitude());
        return dump;
    }
    public static String locationStringDescriptionSimple(Location loc){
        if(loc == null) return "null";
        return "location: " + loc.latitude + " " + loc.longitude;
    }
}