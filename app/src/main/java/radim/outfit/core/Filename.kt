package radim.outfit.core

import locus.api.objects.extra.Track

class Filename{

    // TODO

    fun getFilename(track: Track): String{
        return "${track.name}.fit"
    }

}