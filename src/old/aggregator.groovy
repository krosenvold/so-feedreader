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
import java.io.PrintWriter

def rssFileName = "rss.xml";
def atomFileName = "atom.xml";

System.setProperty("http.agent", "Goober")
System.setProperty("sun.net.client.defaultConnectTimeout", "${60*1000}")
System.setProperty("sun.net.client.defaultReadTimeout", "${60*1000}")

def topPath = args[0]
File topDir = new File(topPath);

// Remove the top OPML file because we're going to be generating it below
File topOPML = new File(topDir, "opml.xml")
if (topOPML.exists()) {
    topOPML.delete()
}

//def sitemeta = new XmlParser().parse(new File(topDir, "sitemeta.xml"));

def smeta = new sitemeta(new File(topDir, "sitemeta.xml"))

def siteURL         = smeta.siteurl()
def styleSheetURLs  = []
def mediaPlayerURL  = smeta.mediaplayer()
def siteTitle       = smeta.title()
def siteDescription = smeta.description()

def fh = new feedhtml()

// TODO: In merge.groovy we can have the OPML with a pair of attributes to use feedarchive
//     archive= is the file to store the feed in
//     days= is the number of days to keep the data
// feedarchive needs refactoring

// TODO: Explore automagic video conversion to FLV

// TODO: What about solitary feeds for which we just use feedarchive?

// TODO: For podcast feeds, the mediaplayer can use the feed file directly
//   as the playlist.  So why not have a button that launches a window which
//   has the mediaplayer loaded with the playlist (feed)?

topDir.eachFileRecurse {
    if (it.path =~ /opml.xml$/) {
        println "******* MERGING $it.path"
        File parent = it.parentFile
        def opml = new XmlParser().parse(it)
        SyndFeed merged = merge.mergeFeedsFromOPML(opml);
        
        // Filters
        feedsort.sortFeedReverse(merged);
        cleaner.cleanFeed(merged)
        
        // Save
        saver.saveToRSS(merged, new File(parent, rssFileName))  
        saver.saveToAtom(merged, new File(parent, atomFileName))
        
        // post-save Filters
        media.addMediaViewers(merged, mediaPlayerURL);
        
        // Format
        styleSheetURLs = []
        opml.meta.style.each {
            styleSheetURLs.add(it.'@url')
        }
        fh.writeFeedToPages(merged, 20, parent, opml.meta.oururl[0].'@home', styleSheetURLs)
    }
}

// generate front page from the aggregated data

def writer = new PrintWriter(new File(topDir, "index.html"))
def builder = new groovy.xml.StreamingMarkupBuilder()

def m = {
    html {
        head {
            title siteTitle
            smeta.stylesheets().stylesheet.each {
                link(href: it.'@href', rel: "stylesheet")
            }
            if (smeta != null && smeta.analyticsid() != null) {
                mkp.yieldUnescaped analytics.theScript(smeta.analyticsid())
            }
        }
        body {
            div(class: "index-header") {
                h1 siteTitle
                p siteDescription
            }
            table(class: "index-table") {
                topDir.eachFileRecurse { 
                    if (it.path =~ /opml.xml$/) {
                        def fOPML = it;
                        File parent = fOPML.parentFile
                        def opml = new XmlParser().parse(it)
                        File feed = new File(parent, "atom.xml")
                        if (! feed.exists()) {
                            feed = new File(parent, "rss.xml")
                        }
                        File index = new File(parent, "index.html")
                        SyndFeed thefeed = feedget.readit(feed)
                        tr {
                            td(valign:"top") {
                                h2 {
                                    a(href: index.path.substring(topPath.length()), opml.head.title[0].text())
                                }
                                p "${thefeed.entries.size()} items from ${opml.body.outline.size()} feeds"
                                p {
                                    def atom = new File(parent, "atom.xml")
                                    if (atom.exists()) {
                                        a(href: atom.path.substring(topPath.length()), "ATOM")
                                        mkp.yieldUnescaped '&nbsp;'
                                    }
                                    def rss = new File(parent, "rss.xml")
                                    if (rss.exists()) {
                                        a(href: rss.path.substring(topPath.length()), "RSS")
                                        mkp.yieldUnescaped '&nbsp;'
                                    }
                                    a(href: fOPML.path.substring(topPath.length()), "OPML")
                                    mkp.yieldUnescaped '&nbsp;'
                                }
                            }
                            td(valign:"top") {
                                if (opml.head.description != null && opml.head.description[0] != null) {
                                    p opml.head.description[0].text()
                                }
                            }
                            //td(valign:"top") {
                            //    def itemcount = 0;
                            //    thefeed.entries.each { entry ->
                            //        if (itemcount <= 5) {
                            //            p {
                            //                a(href: entry.link, entry.title)
                            //            }
                            //        }
                            //        itemcount++
                            //    }
                            //}
                        }
                    }
                }
            }
        }
    }
}


writer << builder.bind(m)
writer.flush()
    
