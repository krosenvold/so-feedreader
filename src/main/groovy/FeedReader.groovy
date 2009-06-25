import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.XmlReader
import com.sun.syndication.feed.synd.SyndEntryImpl
import com.sun.syndication.io.ParsingFeedException;

static SyndFeed readit(spec) {
  URL feedURL = new URL(spec);
  XmlReader reader =  new XmlReader(feedURL);
  SyndFeedInput input = new SyndFeedInput();
  SyndFeed feed = input.build(reader)
  return feed;
}

boolean running = true;
boolean frequencyEstablished = false;
new Thread({
  while (running) {
    new BufferedReader(new InputStreamReader(System.in)).readLine();
    for (int i = 0; i < 10; i++)
      System.out.println("");
  }
}).start();

Set<SyndEntryImpl> seen = new HashSet<SyndEntryImpl>();


Date first = null;
Date second = null;
long interval = 10000;
Date currentDate;
Date previous;
boolean intervalEstablished = false;
for (int i = 0; i < 100000; i++) {
  try {
    SyndFeed feed = readit("http://stackoverflow.com/feeds");
    currentDate = feed.getPublishedDate();
    if (first == null){
      first = currentDate; 
    } else if (second == null && currentDate != first) {
        second = currentDate;
        if (first.getTime() != second.getTime() && !intervalEstablished){
          intervalEstablished = true;
          interval = second.getTime() - first.getTime();
          interval = (int) interval/ 2;
          println("Frequency established at " + interval)
        }
    }

//    println(feed.getPublishedDate());
    for (SyndEntryImpl entry in feed.getEntries()) {
      if (entry.publishedDate == entry.updatedDate && !seen.contains(entry)) {
        seen.add(entry);
        println(entry.title)
      }
    }
    if (frequencyEstablished &&  i % 10 == 9){
      println("Re-establishing frequency")
      first = second  = null;
       frequencyEstablished = false;
    }
  } catch (ParsingFeedException e) {
      print("E");
  }
  Thread.sleep(interval)
  previous = currentDate;
}
running = false;
