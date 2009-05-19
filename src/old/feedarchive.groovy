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

// Given a "feed" (RSS or ATOM) and makes a local archive of the postings.
// It should remember the postings over an 'n' month period, removing anything
// older than the threshold.
//
// FEED URL
//
// ATOM file to hold the archive
//
// max age (months)

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

System.setProperty("http.agent", "Goober")

URL feedURL = new URL(args[0])
def feedURLstring = feedURL.toExternalForm()
println "****** URL: $feedURLstring";

File archiveFile = new File(args[1])
File archiveDir  = archiveFile.parentFile
println "****** Feed File: $archiveFile.path"

def maxAge = 6; // Store for 6 months, TODO: Make this configurable

SyndFeedInput input = new SyndFeedInput();
SyndFeed feed = input.build(new XmlReader(feedURL))

SyndFeed archive = null;
if (archiveFile.exists()) {
    archive = input.build(new XmlReader(archiveFile));
} else {
    archive = new SyndFeedImpl();
    archive.author = feed.author;
    archive.authors = feed.authors;
    archive.categories = feed.categories;
    archive.contributors = feed.contributors;
    archive.copyright = feed.copyright;
    archive.description = feed.description;
    archive.descriptionEx = feed.descriptionEx;
    archive.encoding = feed.encoding;
    // Skip entries, they're treated specially below
    // Skip feedType, we're forcing it to atom below
    archive.foreignMarkup = feed.foreignMarkup;
    archive.image = feed.image;
    archive.language = feed.language;
    archive.link = feed.link;
    archive.links = feed.links;
    // Skip module and modules, not settable
    archive.publishedDate = feed.publishedDate;
    // skip supportedFeedTypes
    archive.title = feed.title;
    archive.titleEx = feed.titleEx;
    archive.uri = feed.uri;
}

// 1)
// For each entry in feed, 
//     check if the entry is in archive, 
//          and if not add it to archive
// 2)
// For each entry in archive,
//     if older than maxAge,
//          remove from archive
// 3)
// write back to original file (ATOM)
// write RSS file

// NOTE: uri is the GUID
// SEE: http://wiki.java.net/bin/view/Javawsxml/Rome04URIMapping

// Step 1: Add new entries to archive
feed.entries.each { entry ->
    
    def archiveEntry = archive.entries.find { item -> item.uri == entry.uri }
    if (archiveEntry == null) {
        println "Entry not in archive, adding"
        archive.entries.add(entry)
    } else {
        println "....skipping"
    }
}

// Step 2: Remove old entries

def today = new Date()
def cutoff = today - (maxAge * 30)
def oldies = archive.entries.find { item -> item.publishedDate.before(cutoff) }
oldies.each { feed.entries.remove(it) }

// Step 3: Save to disk

saver.saveToAtom(archive, new File(archiveDir))

