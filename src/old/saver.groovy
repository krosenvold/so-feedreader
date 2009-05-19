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
import com.sun.syndication.io.XmlReader;
import java.io.PrintWriter;

System.setProperty("http.agent", "Goober")

def inFrom = args[0]
def saveTo = args[1]
def saveType = args[2]

SyndFeed feed = feedget.readit(args[0])

saveToFile(feed, saveTo, saveType);

static void saveToAtom(SyndFeedImpl feed, String fileName) {
    saveToFile(feed, new File(fileName), "atom_1.0")
}

static void saveToAtom(SyndFeedImpl feed, File saveTo) {
    saveToFile(feed, saveTo, "atom_1.0")
}

static void saveToRSS(SyndFeedImpl feed, String fileName) {
    saveToFile(feed, new File(fileName), "rss_2.0")
}

static void saveToRSS(SyndFeedImpl feed, File saveTo) {
    saveToFile(feed, saveTo, "rss_2.0")
}

static void saveToFile(SyndFeedImpl feed, String fileName, String saveType) {
    saveToFile(feed, new File(fileName), saveType)
}

static void saveToFile(SyndFeedImpl feed, File saveTo, String saveType) {
    def wr = new PrintWriter(saveTo)
    feed.feedType = saveType;
    new SyndFeedOutput().output(feed, wr);
    wr.flush();
}



