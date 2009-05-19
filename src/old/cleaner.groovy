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

static void cleanFeed(SyndFeed feed) {
    feed.entries.each { entry ->
        entry.title = cleaner.cleanTitle(entry.title)
        entry.contents.each { content ->
            if (content.value != null) {
                def v = content.value
                content.value = cleaner.htmlCleanup(v)
            }
        }
        if (entry.description != null) {
            def v = entry.description.value
            entry.description.value = cleaner.htmlCleanup(v)
        }
        entry.categories.each { cat ->
            cat.name = cleaner.cleanTitle(cat.name)
        }
    }
}

static String htmlCleanup(String html) {
    def bytes = html.getBytes()
    def obytes = []
    bytes.each { b ->
        if (b > 0 && b <= 127) { obytes.add(b as byte) }
    }
    def out = new String(obytes as byte[])
    //def aa = new StringBuilder("")
    //html.getBytes().each { b -> 
    //    if (b > 0 && b <= 127) { aa.append(Byte.toString(b as byte)) }
    //}
    //def out = aa.toString()
    //String out = html;
    while (out =~ /<object/) {
        // Find the first <object
        // From there find the next </object>
        // New string is based on the stuff prior to <object and following </object>
        def nstring = out.substring(0, out.indexOf("<object")) + out.substring(out.indexOf("</object>") + "</object>".length())
        out = nstring;
    }
    while (out =~ /<embed/) {
        def nstring = out.substring(0, out.indexOf("<embed")) + out.substring(out.indexOf("</embed>") + "</embed>".length())
        out = nstring;
    }
    while (out =~ /<script/) {
        def nstring = out.substring(0, out.indexOf("<script")) + out.substring(out.indexOf("</script>") + "</script>".length())
        out = nstring;
    }
    out = out.replaceAll(/<[iI][mM][gG] [^>]*[sS][rR][cC]=[^>]*blogsmithmedia.com\/[^>]*>/, '')
    out = out.replaceAll(/<[iI][mM][gG] [^>]*[sS][rR][cC]=[^>]*doubleclick.net\/[^>]*>/, '')
    return out;
}

static String cleanTitle(String title) {
    //def chars = title.toCharArray()
    //(0..chars.size() - 1).each { i ->
    //    if (chars[i] == 0325) { chars[i] = 047 }
    //    //chars[i] = (chars[i] & 0x7f);
    //}
    def bytes = title.getBytes()
    def obytes = []
    bytes.each { b ->
        if (b > 0 && b <= 127) { obytes.add(b as byte) }
    }
    def out = new String(obytes as byte[])
    out = out.replaceAll(/&quot;/, '"')
    out = out.replaceAll(/&lt;b&gt;/, '')
    out = out.replaceAll(/<b>/, '')
    out = out.replaceAll(/&lt;\/b&gt;/, '')
    out = out.replaceAll(/<\/b>/, '')
    out = out.replaceAll(/&lt;/, '')
    out = out.replaceAll(/</, '')
    out = out.replaceAll(/&#39;/, '')
    out = out.replaceAll(/&amp;/, '&')
    //def aa = new StringBuilder()
    //title.getBytes().each { b -> 
    //    if (b > 0 && b <= 127) { aa.append(new Byte(b as byte)) }
    //}
    //def out = aa.toString()
    //def out = title.replace(0325 as char, 047 as char)
    
    //Process tr = "tr '\\325' '\\47'".execute()
    //tr << title
    //tr.out.flush()
    //String out = tr.text
    return out
}


