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
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;
import java.text.SimpleDateFormat;

// TBD: Build a sitemap.xml
// TBD: Googl analytics
// TBD: SNAP

def homedir = new File(args[0])
def homePath = homedir.getPath();

def sitemeta = new XmlParser().parse(new File(homedir, "sitemeta.xml"));

URL siteURL = new URL(sitemeta.siteurl.'@href'[0]);

// This sets up the overall feed we're generating
SyndFeed summaryFeed = new SyndFeedImpl();
summaryFeed.setTitle(sitemeta.title.text());
summaryFeed.setDescription(sitemeta.description[0].text());
summaryFeed.link = siteURL.toExternalForm()
def pages = [];
def entries = [];

// Search the directory hierarchy for OPML files.  The defined structure
// is to have one OPML per directory.  The aggregator2 script generates
// RSS and ATOM files in the same directory.  We can then automatically
// gather all information about every subsection simply by doing recursion.
homedir.eachFileRecurse { file ->
    // Skip over the top level OPML
    if (file.name == "opml.xml" && file.parentFile.path != homePath) {
        def opmlPath = file.path;
        def opml = new XmlParser().parse(file);
        def opmlTitle = opml.head.title.value[0];
        println "****** OPML " + opmlPath + " " + opmlTitle
        def index = new File(file.getParentFile(), "index.html");
        def relPath = index.path.substring(homePath.length() + 1);
        def frss   = new File(file.getParentFile(), "rss.xml");
        def fatom   = new File(file.getParentFile(), "atom.xml");
        
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(frss));
        feed.entries.each { entry ->
            // for each item in every feed, add it to the overall feed
            SyndEntryImpl nentry = new SyndEntryImpl();
            nentry.title = entry.title;
            nentry.link = entry.link;
            nentry.setPublishedDate(entry.publishedDate);
            SyndContentImpl content = new SyndContentImpl();
            content.value = "<a href='"+sitemeta.siteurl.'@href'[0]+"/"+relPath+"'>" + opmlTitle + "</a>";
            content.type  = "text/html";
            nentry.description = content;
            entries.add(nentry);
        }
        
        // Make an index of all the subsites
        // This data becomes the OPML file
        pages.add([[
            title: opmlTitle,
            description: (opml.head.description != null && opml.head.description[0] != null) 
                    ? opml.head.description[0].text() : "",
            nfeeds: opml.body.outline.size(),
            nentries: feed.entries.size(),
            opml: opml,
            opmlPath: opmlPath,
            opmlRelative: opmlPath.substring(homePath.length() + 1),
            index: index.path,
            relative: relPath,
            rss: frss.path,
            rssRelative: frss.path.substring(homePath.length() + 1),
            atom: fatom.path,
            atomRelative: fatom.path.substring(homePath.length() + 1)
        ]]);
        
    }
}

// Sort in reverse chronological order by publishedDate
// I don't understand the expression but it works.
entries.sort {a,b-> (a.publishedDate != null && b.publishedDate != null && a.publishedDate.after(b.publishedDate)) ? 0 : 1 }
summaryFeed.setEntries(entries)


try {
    // Output RSS
    summaryFeed.setFeedType("rss_2.0")
    def wrRSS = new PrintWriter(new File(homedir, "rss.xml"))
    new SyndFeedOutput().output(summaryFeed, wrRSS);
    wrRSS.flush()
} catch (Exception e) {
    println "COULD NOT WRITE RSS " + e.message
    e.printStackTrace();
}

try {
    // Output Atom
    summaryFeed.setFeedType("atom_1.0")
    def wrATOM = new PrintWriter(new File(homedir, "atom.xml"))
    new SyndFeedOutput().output(summaryFeed, wrATOM);
    wrATOM.flush();
} catch (Exception e) {
    println "COULD NOT WRITE ATOM " + e.message
    e.printStackTrace();
}

// Output the OPML file
def writer = new PrintWriter(new File(homedir, "opml.xml"))
def builder = new groovy.xml.StreamingMarkupBuilder()

def o = { opml {
    head {
        title sitemeta.title.text()
    }
    body {
        pages.each { page ->
            outline(
                text: page.title, 
                siteurl: sitemeta.siteurl.'@href'[0] +"/"+ page.relative[0], 
                xmlUrl: sitemeta.siteurl.'@href'[0] +"/"+ page.rssRelative[0]
                );
        }
    }
}}

writer << builder.bind(o)
writer.flush()

def dateFormatter = new SimpleDateFormat("yyyy MMMMM dd");

// Output the HTML page
writer = new PrintWriter(new File(homedir, "index.html"))
builder = new groovy.xml.StreamingMarkupBuilder()

def m = { html {
    head {
        title sitemeta.title.text()
        link(href: 'style.css', rel: "stylesheet")
        link(
            rel: "alternate", 
            type: "application/atom+xml", 
            title: sitemeta.title.text() + " - Atom", 
            href: siteURL.toExternalForm() + "atom.xml"
        );
        link(
            rel: "alternate", 
            type: "application/rss+xml", 
            title: sitemeta.title.text() + " - RSS", 
            href: siteURL.toExternalForm() + "rss.xml"
        );
    }
    body {
        div(id: "wrapper") {
            div(id: "header", style: "text-align: center") {
                h1(class: "logo", sitemeta.title[0].text())
                p sitemeta.description[0].text()
                p { tt sitemeta.siteurl.'@href'[0] }
                if (sitemeta.adsheader != null) {
                    mkp.yieldUnescaped sitemeta.adsheader[0].text()
                }
            }
            // TBD: The sidebar needs javascript to make it stay in place when the user scrolls
            table(border: 0) {
                tr {
                    td(valign: "top") {
                        def lastDate = null;
                        entries.each { entry ->
                            //println entry
                            def cal = Calendar.getInstance()
                        
                            if (entry.publishedDate != null) { 
                                cal.setTime(entry.publishedDate)
                                def thisDate = dateFormatter.format(entry.publishedDate)
                                if (lastDate == null || lastDate != thisDate) {
                                    h1(class: "aggr-date", thisDate)
                                    lastDate = thisDate;
                                }
                            }
                        
                            p {
                                a(href: entry.link, entry.title)
                                font(size: "-1") {
                                    span " ("
                                    mkp.yieldUnescaped entry.description.value
                                    span ")"
                                }
                            }
                        }
                    }
                    td(halign: "right", valign: "top") {
                        div(class: "navigation") {
                            div(class: "sidebar-block") {
                                h2(class: "navheader", "Feeds for " + sitemeta.siteurl.'@href'[0]);
                                p {
                                    a(href: "rss.xml", "RSS")
                                    span ", "
                                    a(href: "atom.xml", "ATOM")
                                    span ", "
                                    a(href: "opml.xml", "OPML")
                                }
                            }
                            div(class: "sidebar-block" ) {
                                h2(class: "navheader", "Subsections");
                                pages.each { page ->
                                    //println page
                                    p {
                                        a(href: page.relative[0], page.title)
                                        font(size: "-1") {
                                            span " (" + page.nentries[0] + " articles from " + page.nfeeds[0] + " feeds "
                                            a(href: page.rssRelative[0], "RSS")
                                            span ", "
                                            a(href: page.atomRelative[0], "ATOM")
                                            span ", "
                                            a(href: page.opmlRelative[0], "OPML")
                                            span ")"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // TBD: footer
        }
    }
}}

writer << builder.bind(m)
writer.flush()

