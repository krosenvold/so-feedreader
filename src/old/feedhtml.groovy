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
import java.text.SimpleDateFormat;

// args[0]: Feed file name or URL
// Feed site
// HTML output file name
// Stylesheet URL
// Advertising
// navbar contents

System.setProperty("http.agent", "Goober")

SyndFeed feed = feedget.readit(args[0])
def homedir = args[1]
def feedSiteURL = args[2]
def styleSheetURL = args[3]


writeFeedToPages(feed, 10, new File(homedir), feedSiteURL, styleSheetURL)

def writeFeedToPages(SyndFeed feed, int numPerPage, File dir, feedSiteURL, styleSheetURLs) {
    if (numPerPage < 0 || feed.entries.size() <= numPerPage) {
        writeFeedRangeToPage(feed, (Range)null, numPerPage, 0, new File(dir, "index.html"), feedSiteURL, styleSheetURLs);
    } else {
        def pageno = 0;
        def nEntries = feed.entries.size();
        def current = 0;
        while (current < nEntries) {
            writeFeedRangeToPage(
                feed, 
                (current..((current+numPerPage) > feed.entries.size() ? feed.entries.size() - 1 : current+numPerPage-1)), 
                numPerPage,
                pageno,
                new File(dir, pageno == 0 ? "index.html" : "page${pageno}.html"),
                feedSiteURL,
                styleSheetURLs
            );
            current += numPerPage;
            pageno++;
        }
    }
}

def writeFeedRangeToPage(SyndFeed feed, Range range, int numPerPage, int pageno, File output, feedSiteURL, styleSheetURLs) {
    println range +" "+ output.path
    File outputDir = output.parentFile
    File OPML = new File(outputDir, "opml.xml")
    def opml = OPML.exists() ? new XmlParser().parse(OPML) : null
    def feedTitle = feed.title
    def feedDescription = feed.description != null ? feed.description: ""
    def feedLink = feed.link

    //println opml.meta
    //println opml.meta.sitemeta
    //println opml.meta.sitemeta.'@url'
    //println opml.meta.sitemeta.'@url'[0]
    def smeta = opml != null ? new sitemeta(new File(outputDir, opml.meta.sitemeta.'@url'[0])) : null

    def dateFormatter = new SimpleDateFormat("yyyy MMMMM dd");
    def lastDate = null;

    // Output the HTML page
    def writer = new PrintWriter(output)
    def builder = new groovy.xml.StreamingMarkupBuilder()
    
    // num pages = num entries / numPerPage .. then round up if it's a fraction
    def numPages = (feed.entries.size() / numPerPage).intValue();
    //println feed.entries.size() +"/"+ numPerPage +" = "+numPages
    //println feed.entries.size() +"%"+ numPerPage +" = "+ (feed.entries.size() % numPerPage)
    // I don't understand why.  The numPages calculation rounds upward.  But if the
    // modulo remainder is 0 then numPages is one too large.
    if ((feed.entries.size() % numPerPage) == 0) numPages--;

    def m = { html {
        head {
            title feedTitle
            // rss_2.0
            // atom_1.0
            // opml_1.0
            // opml_2.0
            link(
                rel: "alternate",
                type: (feed.feedType =~ /atom/ ? "application/atom+xml" : "application/rss+xml"),
                title: feedTitle,
                href: feedLink
                );
            if (styleSheetURLs != null) {
                styleSheetURLs.each { styleSheetURL ->
                    link(href: styleSheetURL, rel: "stylesheet")
                }
            }
            
            if (smeta != null && smeta.analyticsid() != null) {
                mkp.yieldUnescaped analytics.theScript(smeta.analyticsid())
            }
            script(language: "javascript") {
                mkp.yieldUnescaped """
if (navigator.plugins) {
    for (i=0; i < navigator.plugins.length; i++ ) {
        if (navigator.plugins[i].name.indexOf(\"QuickTime\") >= 0) {
            haveqt = true;
        }
    }
}


if ((navigator.appVersion.indexOf(\"Mac\") > 0)
    && (navigator.appName.substring(0,9) == \"Microsoft\")
    && (parseInt(navigator.appVersion) < 5) ) {
    haveqt = true; 
}
"""
            }
            script(src: "/AC_QuickTime.js", language: "javascript") { }
        }
        body {
            div(class: "header") {
                h1 feedTitle
                p feedDescription
                // TODO: Advertising
            }
            div(class: "page-navigation") {
                b 'Pages: '
                mkp.yieldUnescaped '&nbsp;'
                a(href: feedSiteURL, "home")
                mkp.yieldUnescaped '&nbsp;'
                (0..numPages).each { n ->
                    mkp.yieldUnescaped '&nbsp;'
                    span(class: "page-nav-number") {
                        if (n != pageno) {
                            if (n == 0) {
                                a(href: "index.html", "1")
                            } else {
                                a(href: "page${n}.html", "${n+1}")
                            }
                        } else {
                            span (n+1)
                        }
                    }
                }
                if (OPML.exists()) {
                    mkp.yieldUnescaped '&nbsp;'
                    a(href: "opml.xml", "OPML")
                    mkp.yieldUnescaped '&nbsp;'
                }
            }
            div(class: "content") {
                //println range
                //println feed.entries.size()
            (range != null
              ? feed.entries[range] 
              : feed.entries
              ).each { entry ->
                
                def cal = Calendar.getInstance()
                if (entry.publishedDate != null) { 
                    cal.setTime(entry.publishedDate)
                    def thisDate = dateFormatter.format(entry.publishedDate)
                    if (lastDate == null || lastDate != thisDate) {
                        h1(class: "date-header", thisDate)
                        lastDate = thisDate;
                    }
                }
                
                //def thisDate = dateFormatter.format(entry.publishedDate)
                //h1 thisDate
            
                div(class: "feed-item") {
                    h2(class: "entry-title", style: "clear: both") {
                        a(href: entry.link, entry.title != null ? entry.title : entry.link) 
                    }
                    //span(class: "entry-byline") {
                    //    span "[ "
                    //    a(href: feedSiteURL, feedTitle)
                    //    span " " + (entry.publishedDate != null ? entry.publishedDate.toString() : "") + " ]"
                    //}
                    div(class: "entry-body") {
                        if (entry.contents.size() > 0) {
                            entry.contents.each { content ->
                                if (content.value != null) {
                                    def v = content.value
                                    span(class: "entry-content") { mkp.yieldUnescaped v } //htmlCleanup(v) }
                                }
                            }
                        }
                        if (entry.description != null) {
                            def v = entry.description.value
                            span(class: "entry-description") { mkp.yieldUnescaped v /*htmlCleanup(v)*/ }
                        }
                    }
                    // TODO: What if the entry.links exist ??  Format them all??
                    div(class: "entry-categories") {
                        if (entry.categories.size() > 0) {
                            ul(class: "entry-category-list") {
                                entry.categories.each { cat ->
                                    li(class: "entry-category-item") {
                                        if (cat.taxonomyUri != null) {
                                            a(href: cat.taxonomyUri,
                                              class: "entry-cat-link",
                                              cat.name != null ? cat.name : cat.taxonomyUri)
                                        } else {
                                            span cat.name != null ? cat.name : ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                    div(class: "entry-enclosures") {
                        // If there's an enclosure, generate FLASH code for a player 
                        // ...and... generate a link
                        if (entry.enclosures.size() > 0) {
                            ul(class: "entry-enclosure-list") {
                                entry.enclosures.each { enclosure ->
                                    //println enclosure
                                    li(class: "enclosure-item") {
                                        def url = enclosure.getUrl()
                                        def type = enclosure.getType()
                                        a(href: url, class: "enclosure-link", url)
                                        span(class: "enclosure-type", type)
                                        
                                        // MP3
                                        // <embed src="mediaplayer.swf" width="320" height="20" 
                                        // allowfullscreen="true" 
                                        // flashvars="&file=/upload/peterjones_sunshine_lofi.mp3&height=20&width=320" />
                                        //if (type == "audio/mpeg") {
                                        //    embed(src: "/mediaplayer.swf", width: "320", height: "20",
                                        //        allowfullscreen: "false",
                                        //        flashvars: "&file="+url+"&height=20&width=320");
                                        //}
                                        
                                        // FLV
                                        // <embed src="mediaplayer.swf" width="320" height="180" 
                                        // allowfullscreen="true" 
                                        // flashvars="&file=http://www.jeroenwijering.com/upload/afraid.flv&height=180&width=320" />
                                        // TODO: How to transcode media from other video formats to FLV?
                                    }
                                }
                            }
                        }
                    }
                }
            }
        
            if (OPML.exists()) {
                // Read OPML
                // Format up a list of the items
                def opmlTitle = opml.head.title[0].text();
                
                div(class: "feedlist") {
                    h2 opmlTitle
                    opml.body.outline.each { outline ->
                        p { 
                            a(href: outline.'@siteurl', outline.'@text')
                            span ' ('
                            a(href: outline.'@xmlUrl', 'XML')
                            span ')'
                        }
                    }
                    h2 "Links"
                    p {
                        if (new File(outputDir, "rss.xml").exists()) {
                            p { a(href: "rss.xml", "RSS") }
                        }
                        if (new File(outputDir, "atom.xml").exists()) {
                            p { a(href: "atom.xml", "ATOM") }
                        }
                        p { a(href: "opml.xml", "OPML") }
                        p { a(href: feedSiteURL, "home") }
                    }
                    opml.links.link.each {
                        p { a(href: it.'@url', it.'@text') }
                    }
                }
            }
            }
        }
    }}

    writer << builder.bind(m)
    writer.flush()
}

static String htmlCleanup(String html) {
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




