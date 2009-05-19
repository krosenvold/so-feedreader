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

// THIS PROGRAM IS DEPRECATED, IT IS HERE FOR HISTORICAL PURPOSES AND WILL
// PROBABLY BE DELETED LATER

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

System.setProperty("http.agent", "Goober")

println "****** OPML: " + args[0];

def fOPML = new File(args[0])
def fDir  = fOPML.getParentFile()
def opml = new XmlParser().parse(fOPML)

def fnSiteMeta = opml.meta.sitemeta.'@url'[0]
def fSiteMeta = new File(fDir, fnSiteMeta)
def sitemeta = new XmlParser().parse(fSiteMeta)

URL pageURL = new URL(opml.meta.oururl.'@url'[0])

def pageTitle = opml.head.title[0].text();
def pageDescription = opml.head.description != null && opml.head.description[0] != null ? opml.head.description[0].text() : pageTitle;

//println opml

// This is so we can build RSS/ATOM feeds for the aggregation
SyndFeed aggrFeed = new SyndFeedImpl();
// rss_2.0
// atom_1.0
// opml_1.0
// opml_2.0
aggrFeed.setFeedType("rss_2.0");

aggrFeed.setTitle(pageTitle);
aggrFeed.setDescription(pageDescription);
if (opml.head.ownerName != null && opml.head.ownerName[0] != null) {
    aggrFeed.setAuthor(opml.head.ownerName[0].text());
}
aggrFeed.link = pageURL.toExternalForm()

// A directory tree of OPML XML files for different aggregations, that include
//    aggregation name and a list of sites.  The directory tree would have prebuilt
//    HTML files that map the topical hierarchy of the aggregations.
// Need a "no adverts" flag for each aggregation
// After building page, do we ping some service?
// Do we register each page with technorati?  Or just some pages?

def sites = []

opml.body.outline.each { outline ->
    sites += [[
        name: outline.'@text',
        feedurl: outline.'@xmlUrl',
        siteurl: outline.'@siteurl'
    ]]
}

def entries = [];  // List of entries in the feed
def entrylist = [] // a kind of index to let us find data about feeds based on the feedurl
sites.each { site ->
    
    //println site.feedurl
    URL feedUrl = new URL(site.feedurl)
    println "Fetching " + feedUrl.toExternalForm()

    try {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedUrl));
    
        println "..." + feed.entries.size() + " entries"
    
        feed.entries.each { entry ->
            entries.add(entry)
            entrylist += [[ feedname: site.name, siteurl: site.siteurl, feedurl: site.feedurl, entryurl: entry.link ]]
        }
    } catch (Exception e) {
        println "COULD NOT PARSE FEED " + e.message
        e.printStackTrace();
    }
}

aggrFeed.setEntries(entries)

// Sort in reverse chronological order by publishedDate
// I don't understand the expression but it works.
entries.sort {a,b-> (a.publishedDate != null && b.publishedDate != null && a.publishedDate.after(b.publishedDate)) ? 0 : 1 }

try {
    // Output RSS
    aggrFeed.setFeedType("rss_2.0")
    def wrRSS = new PrintWriter(new File(fDir, "rss.xml"))
    new SyndFeedOutput().output(aggrFeed, wrRSS);
    wrRSS.flush()
} catch (Exception e) {
    println "COULD NOT WRITE RSS " + e.message
    e.printStackTrace();
}

try {
    // Output Atom
    aggrFeed.setFeedType("atom_1.0")
    def wrATOM = new PrintWriter(new File(fDir, "atom.xml"))
    new SyndFeedOutput().output(aggrFeed, wrATOM);
    wrATOM.flush();
} catch (Exception e) {
    println "COULD NOT WRITE ATOM " + e.message
    e.printStackTrace();
}

def dateFormatter = new SimpleDateFormat("yyyy MMMMM dd");

// Output HTML of the page
def writer = new PrintWriter(new File(fDir, "index.html"))
def builder = new groovy.xml.StreamingMarkupBuilder()

def m = { html {
    head {
        title pageTitle
        // Make a style sheet
        // aggr-date: for each date
        // aggr-entry-title: for each entry title
        // aggr-entry
        // aggr-entry-byline
        // aggr-entry-description
        // aggr-entry-categories
        // aggr-entry-category-item
        // aggr-entry-cat-link
        // aggr-entry-enclosures
        // aggr-enclosure-item
        // aggr-enclosure-link
        // aggr-enclosure-type
        // aggr-feedlist
        // aggr-feeditem
        // aggr-sitelist
        // aggr-site-item
        //
        // Incorporate .. analytics? snap?
        
        if (opml.meta != null && opml.meta.style != null) {
            link(href: opml.meta.style.'@url'[0], rel: "stylesheet")
        }
        link(
            rel: "alternate", 
            type: "application/atom+xml", 
            title: sitemeta.title.text() + " - Atom", 
            href: pageURL.toExternalForm() + "atom.xml"
        );
        link(
            rel: "alternate", 
            type: "application/rss+xml", 
            title: sitemeta.title.text() + " - RSS", 
            href: pageURL.toExternalForm() + "rss.xml"
        );
    }
    
    body {
        div(id: "wrapper") {
            div(id: "header", style: "text-align: center") {
                h1(class: "logo", pageTitle)
                p pageDescription
                if (sitemeta.adsheader != null) {
                    mkp.yieldUnescaped sitemeta.adsheader[0].text()
                }
            }
                    
            // Add the "Feeds" section here
            // Add a search box here
        
            // adsense links strip goes here
            // from opml.meta .. a filename for headeradverts
        
            table(class: "container") {
                tr {
                    td(valign: "top", class: "content") {
                        // content
                        def lastYear = null
                        def lastMonth = null
                        def lastDay = null
                        def cal = Calendar.getInstance()
                        
                        def lastHost = null
                    
                        aggrFeed.entries.each { entry ->
                            // Detect when an entry is from a different day than the previous
                            // If so, output an H1(class: aggr-date) with the date
                        
                            if (entry.publishedDate != null) { cal.setTime(entry.publishedDate) }
                            if (lastYear == null || cal.get(Calendar.YEAR) != lastYear
                            || lastMonth == null || cal.get(Calendar.MONTH) != lastMonth
                            || lastDay == null || cal.get(Calendar.DAY_OF_MONTH) != lastDay) {
                                h1(class: "aggr-date", style: "clear: both", 
                                    dateFormatter.format(entry.publishedDate)
                                )
                                lastYear = cal.get(Calendar.YEAR)
                                lastMonth = cal.get(Calendar.MONTH)
                                lastDay = cal.get(Calendar.DAY_OF_MONTH)
                            }
                        
                            // There could be a feed icon to display
                            // There could be a way to do urlicon (download favicon)
                        
                            div(class: "aggr-entry-wrapper") {
                                h2(class: "aggr-entry-title", style: "clear: both") {
                                    a(href: entry.link, entry.title != null ? entry.title : entry.link) 
                                }
                                def ofeedname = entrylist.find { item -> item.entryurl == entry.link}.feedname
                                def ositeurl  = entrylist.find { item -> item.entryurl == entry.link}.siteurl
                                //def both = { siteurl, feedname ->  }
                                //def url = { siteurl ->  }
                                span(class: "aggr-entry") {
                                    span(class: "aggr-entry-byline") {
                                        span "[" 
                                        if (ositeurl != null && ofeedname != null) {
                                            a(href: ositeurl, ofeedname)
                                        } else if (ofeedname != null) {
                                            span ofeedname
                                        } else if (ositeurl != null) {
                                            a(href: ositeurl, ositeurl)
                                        }
                                        span " " + (entry.publishedDate != null ? entry.publishedDate.toString() : "")  + "] "
                                    }
                                    if (entry.contents.size() > 0) {
                                        entry.contents.each { content ->
                                            //p "entry.contents.each"
                                            //println content
                                            //println content.value
                                            //println content.getValue()
                                            if (content.value != null) {
                                                //span(class: "aggr-entry-description", content.getValue())
                                                def v = content.value
                                                //println v
                                                //v = v.replaceAll(/\&lt;/, '<')
                                                //v = v.replaceAll(/\&gt;/, '>')
                                                //println v
                                                //v = v.replaceAll(matchObject, '')
                                                //println v
                                                //if (v =~ matchObject) {
                                                //    println "HAS OBJECT: " + v
                                                //    v.eachMatch(matchObject) { println ">>> " + it }
                                                //}
                                                mkp.yieldUnescaped htmlCleanup(v)//.replaceAll(matchObject, '')
                                            }
                                        }
                                    } else {
                                        if (entry.description != null) {
                                            //p "entry.description"
                                            //println entry.description
                                            //println entry.description.value
                                            //println entry.description.getValue()
                                            //span(class: "aggr-entry-description", entry.description.getValue())
                                            def v = entry.description.value
                                            //println v
                                            //v = v.replaceAll(/\&lt;/, '<')
                                            //v = v.replaceAll(/\&gt;/, '>')
                                            //println v
                                            ////println v
                                            //v = v.replaceAll(matchObject, '')
                                            //println v
                                            //if (v =~ matchObject) {
                                            //    println "HAS OBJECT: " + v
                                            //    v.eachMatch(matchObject) { println ">>> " + it }
                                            //}
                                            mkp.yieldUnescaped htmlCleanup(v)//.replaceAll(matchObject, '')
                                        }
                                    }
                                }
                                if (entry.categories.size() > 0) {
                                    ul(class: "aggr-entry-categories") {
                                        entry.categories.each { cat ->
                                            li(class: "aggr-entry-category-item") {
                                                if (cat.taxonomyUri != null) {
                                                    a(href: cat.taxonomyUri,
                                                      class: "aggr-entry-cat-link",
                                                      cat.name != null ? cat.name : cat.taxonomyUri)
                                                } else {
                                                    span cat.name != null ? cat.name : ""
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // If there's an enclosure, generate FLASH code for a player 
                            // ...and... generate a link
                            if (entry.enclosures.size() > 0) {
                                ul(class: "aggr-entry-enclosures") {
                                    entry.enclosures.each { enclosure ->
                                        //println enclosure
                                        li(class: "aggr-enclosure-item") {
                                            def url = enclosure.getUrl()
                                            def type = enclosure.getType()
                                            a(href: url, class: "aggr-enclosure-link", url)
                                            span(class: "aggr-enclosure-type", type)
                                            
                                            // MP3
                                            // <embed src="mediaplayer.swf" width="320" height="20" 
                                            // allowfullscreen="true" 
                                            // flashvars="&file=/upload/peterjones_sunshine_lofi.mp3&height=20&width=320" />
                                            if (type == "audio/mpeg") {
                                                embed(src: "/mediaplayer.swf", width: "320", height: " 20",
                                                    allowfullscreen: "false",
                                                    flashvars: "&file="+url+"&height=20&width=320");
                                            }
                                            
                                            // FLV
                                            // <embed src="mediaplayer.swf" width="320" height="180" 
                                            // allowfullscreen="true" 
                                            // flashvars="&file=http://www.jeroenwijering.com/upload/afraid.flv&height=180&width=320" />
                                        }
                                    }
                                }
                            }
                        }
                    }
                    td(valign: "top") {
                        div(class: "navigation") {
                            div(class: "sidebar-block") {
                                a(href: opml.meta.oururl.'@home'[0], "Home");
                            }
                            div(class: "sidebar-block") {
                                h2(class: "navheader", "Feeds");
                                ul(class: "aggr-feedlist", align: "right") {
                                    li(class: "aggr-feeditem") { a(href: "rss.xml", "RSS") }
                                    li(class: "aggr-feeditem") { a(href: "atom.xml", "ATOM") }
                                    li(class: "aggr-feeditem") { a(href: "opml.xml", "OPML") }
                                }                        
                            }
                            // navigation
                            div(class: "sidebar-block") {
                                h2(class: "navheader", "Sources");
                                sites.each { site ->
                                    p(class: "aggr-site-item") { a(href: site.feedurl, site.name) }
                                }
                            }
                            // Add a "Links" section that isn't just the sites from the OPML
                            // but other sites
                            // Advertising blocks go here
                            // from opml.meta .. a filename for navbaradverts
                        }
                    }
                }
            }
        }
    }
}}

writer << builder.bind(m)
writer.flush()


String htmlCleanup(String html) {
    String out = html;
    while (out =~ /<object/) {
        // Find the first <object
        // From there find the next </object>
        // New string is based on the stuff prior to <object and following </object>
        def nstring = out.substring(0, out.indexOf("<object"))
                + out.substring(out.indexOf("</object>") + "</object>".length())
        out = nstring;
    }
    while (out =~ /<embed/) {
        def nstring = out.substring(0, out.indexOf("<embed"))
                + out.substring(out.indexOf("</embed>") + "</embed>".length())
        out = nstring;
    }
    while (out =~ /<script/) {
        def nstring = out.substring(0, out.indexOf("<script"))
                + out.substring(out.indexOf("</script>") + "</script>".length())
        out = nstring;
    }
    out = out.replaceAll(/<[iI][mM][gG] [^>]*[sS][rR][cC]=[^>]*blogsmithmedia.com\/[^>]*>/, '')
    out = out.replaceAll(/<[iI][mM][gG] [^>]*[sS][rR][cC]=[^>]*doubleclick.net\/[^>]*>/, '')
    return out;
}



