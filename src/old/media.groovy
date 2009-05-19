/* Copyright 2007, David S. Herron <davidh at 7gen dot com>

This file is part of feed-aggregation-tools.

feed-aggregation-tools is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

feed-aggregation-tools is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with feed-aggregation-tools; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA */

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;

static void addMediaViewers(SyndFeed feed, String mediaPlayerURL) {
    feed.entries.each { entry ->
        
        // If there's an enclosure, generate FLASH code for a player 
        // ...and... generate a link
        if (entry.enclosures.size() > 0) {
            entry.enclosures.each { enclosure ->
                def url = enclosure.getUrl()
                def type = enclosure.getType()
                if (type == "audio/mpeg" || type == "x-audio/mp3") {
                    def desc = entry.description.value
                    desc += """
<p style='clear:both'><embed src=\"$mediaPlayerURL\" width=320 height=20 allowfullscreen=false 
     flashvars=\"&file=$url&height=20&width=320\" /></p>
""";
                    entry.description.value = desc;
                }
                if (type == "video/quicktime" || type == "video/vnd.objectvideo") {
                    // This is what Apple calls a "Poster Movie".  The idea is to
                    // autoplay a short clip that says "Click Here to begin movie".
                    // This short clip loads fast without bothering the user, and then
                    // once they're ready to play the movie just the one movie plays.
                    // Otherwise the typical aggregated video podcast will include
                    // several movies on the page, each of which will start downloading
                    // and the browser will be very unpleasant.
                    def desc = entry.description.value
                    desc += """
<p style='clear:both'><script language=\"javascript\">
    QT_WriteOBJECT('/ClickHere.mov' , '320', '260' , '', 'CONTROLLER', 'True', 'AUTOPLAY', 'True', 'LOOP', 'True', 'HREF', '$url', 'TARGET', 'Myself');
</script></p>
""";
                    entry.description.value = desc;
                }
                // if (type == ...video...) { ... }
            }
        }
    }
}

