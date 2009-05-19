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
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

// List of feeds to merge
//   take from OPML 

// Create an ATOM feed for output
// For each item from each feed
//    Add a description of the item to the output feed
//    The description should make it clear it was aggregated 
//    from a specific feed...


System.setProperty("http.agent", "Goober")
System.setProperty("sun.net.client.defaultConnectTimeout", 60*1000)

println "****** OPML: " + args[0];

def fOPML = new File(args[0])
def fDir  = fOPML.getParentFile()
def opml = new XmlParser().parse(fOPML)

// TODO: File names for rss and atom feed files generated from this.
def rssFileName = "rss.xml";
def atomFileName = "atom.xml";

SyndFeed feed = merge.mergeFeedsFromOPML(opml);
feedsort.sortFeedReverse(feed);

try {
    saver.saveToRSS(feed, new File(fDir, rssFileName))  
} catch (Exception e) {
    println "COULD NOT WRITE RSS " + e.message
    e.printStackTrace();
}
try {
    saver.saveToAtom(feed, new File(fDir, atomFileName))
} catch (Exception e) {
    println "COULD NOT WRITE ATOM " + e.message
    e.printStackTrace();
}

static SyndFeed mergeFeedsFromOPML(opml) {
                           
    def outputFeedTitle = opml.head.title[0].text();
    def outputFeedDescription = null
    if (opml.head.description != null && opml.head.description[0] != null) {
        outputFeedDescription = opml.head.description[0].text()
    } else {
        outputFeedDescription = outputFeedTitle;
    }
    def outputFeedOwner = null;
    if (opml.head.ownerName != null && opml.head.ownerName[0] != null) {
        outputFeedOwner = opml.head.ownerName[0].text()
    } else {
        outputFeedOwner = "";
    }
    URL pageURL = new URL(opml.meta.oururl.'@url'[0])
    def outputFeedLink = pageURL.toExternalForm();

    // This is so we can build RSS/ATOM feeds for the aggregation
    SyndFeed aggrFeed = new SyndFeedImpl();
    // rss_2.0
    // atom_1.0
    // opml_1.0
    // opml_2.0
    aggrFeed.setFeedType("rss_2.0");
    aggrFeed.setTitle(outputFeedTitle);
    aggrFeed.setDescription(outputFeedDescription);
    aggrFeed.setAuthor(outputFeedOwner);
    aggrFeed.link = outputFeedLink;

    def entries = [];  // List of entries in the feed

    opml.body.outline.each { site ->
        
        def siteName    = site.'@text'
        def siteFeedUrl = site.'@xmlUrl'
        def siteUrl     = site.'@siteurl'

        println "Fetching $siteFeedUrl"

        try {
            SyndFeed feed = feedget.readit(siteFeedUrl)
    
            println "..." + feed.entries.size() + " entries"
    
            feed.entries.each { entry ->
                
                if (entry.description != null && entry.description.value != null) {
                    boolean found = false;
                    entry.contents.each { content ->
                        if (content.value != null && content.value == entry.description.value) {
                            found = true;
                        }
                    }
                    if (found) {
                        entry.description.value = "";
                    }
                }
            
                if (feed.link != null && feed.title != null) {
                    def nc = new SyndContentImpl()
                    nc.type = "text/plain"
                    nc.value = "[<a href=\"$feed.link\">$feed.title</a> ${entry.publishedDate != null ? entry.publishedDate.toString() : ""}]"
                    entry.contents.add(0, nc)
                } else if (feed.title != null) {
                    def nc = new SyndContentImpl()
                    nc.type = "text/plain"
                    nc.value = "[$feed.title ${entry.publishedDate != null ? entry.publishedDate.toString() : ""}] "
                    entry.contents.add(0, nc)
                } else if (feed.link != null) {
                    def nc = new SyndContentImpl()
                    nc.type = "text/plain"
                    nc.value = "[<a href=\"$feed.link\">$feed.link</a> ${entry.publishedDate != null ? entry.publishedDate.toString() : ""}] "
                    entry.contents.add(0, nc)
                } else {
                    def nc = new SyndContentImpl()
                    nc.type = "text/plain"
                    nc.value = "[${entry.publishedDate != null ? entry.publishedDate.toString() : ""}] "
                    entry.contents.add(0, nc)
                }
                    
                def etitle = (entry.title != null) ? entry.title : ""
                entry.title = feed.title +": "+ etitle
                
                entries.add(entry)
            }
        } catch (Exception e) {
            println "COULD NOT PARSE FEED " + e.message
            e.printStackTrace();
        }
    }
    
    aggrFeed.setEntries(entries)
    
    return aggrFeed;
    
}


