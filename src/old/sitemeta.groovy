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

class sitemeta {
def data = null;
           
sitemeta(String path) {
    init(new File(path))
}
                  
sitemeta(File file) {
    data = new XmlParser().parse(file)
}

String siteurl() {
    return data.siteurl[0].'@href'
}

String mediaplayer() {
    return data.mediaplayer[0].'@href'
}

String title() {
    return data.title != null ? data.title.text() : ""
}

String description() {
    return data.description != null ? data.description.text() : ""
}

def stylesheets() {
    return data.stylesheets
}

String analyticsid() {
    return data.analytics != null ? data.analytics.'@id'[0] : null;
}
}

