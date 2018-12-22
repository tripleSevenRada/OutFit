package radim.outfit.core.export.work.locusapiextensions.stringdumps;

import java.util.LinkedList;
import java.util.List;

import locus.api.objects.extra.Point;

public class PointStringDump {
    public static List<String> stringDescription(Point point) {
        List<String> dump = new LinkedList<String>();
        dump.add("----------Point dump");
        if(point == null){
            dump.add("NULL");
            return dump;
        }
        dump.add("---------------getId()   " + point.getId());
        dump.add("---------------getName()   " + point.getName());
        dump.add("---------------getTimeCreated() " + point.getTimeCreated());
        dump.add("---------------getParameterRteAction() " + point.getParameterRteAction());
        dump.add("---------------getParameterDescription() " + point.getParameterDescription());
        dump.add("---------------getParameterStyleName() " + point.getParameterStyleName());
        dump.add("---------------getParameterSource() " + point.getParameterSource());
        dump.add("---------------getParamRteIndex() " + point.getParamRteIndex());
        return dump;
    }
}