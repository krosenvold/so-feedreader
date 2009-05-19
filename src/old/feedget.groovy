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

import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.XmlReader;

SyndFeed feed = feedget.readit(args[0])
saver.saveToAtom(feed, "atom.xml")

static SyndFeed readit(spec) {

    boolean isFile = ! (spec =~ /^http:\//) 

    URL feedURL = null;
    File feedFile = null;
    XmlReader reader = null;

    if (isFile) {
        feedFile = new File(spec);
        reader = new XmlReader(feedFile);
    } else {
        feedURL = new URL(spec);
        reader = new XmlReader(feedURL);
    }

    SyndFeedInput input = new SyndFeedInput();
    SyndFeed feed = input.build(reader)
    return feed;
}

static SyndFeed readit(File feedFile) {
    XmlReader reader = new XmlReader(feedFile);
    SyndFeedInput input = new SyndFeedInput();
    SyndFeed feed = input.build(reader)
    return feed;
}


