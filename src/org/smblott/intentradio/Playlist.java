package org.smblott.intentradio;

import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.text.TextUtils;
import java.util.List;
import android.os.AsyncTask;

public abstract class Playlist extends AsyncTask<String, Void, String>
{
   private static final String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";

   private IntentPlayer player = null;
   private int then = 0;

   Playlist(IntentPlayer the_player)
   {
      super();
      player = the_player;
      then = player.now();
   }

   // A playlist contains a sequence of lines.  Only some of those lines may
   // contain URLs.  Other lines such as comments may also contain URLs, but
   // they aren't part of the playlist.
   //
   // Implementations of filter() must return a line, possibly edited, if it
   // may contain a URL.  For example, it may return the empty string if the
   // line contains a comment.
   //
   abstract String filter(String line);

   /* ********************************************************************
    * Asynchronous task to fetch playlist...
    */

   protected String doInBackground(String... args)
   {
      String url = fetch_url(args[0]);

      if ( url == null )
      {
         log("Playlist: failed to extract URL from playlist.");
         return null;
      }

      if ( url.endsWith(PlaylistPls.suffix) || url.endsWith(PlaylistM3u.suffix) )
      {
         log("Playlist: another playlist!");
         return null;
      }

      return url;
   }

   // This runs on the main thread...
   //
   protected void onPostExecute(String url) {
      if ( url != null && ! isCancelled() )
         player.play(url, then);
   }

   /* ********************************************************************
    * Fetch a single (random) url from a playlist...
    */

   private String fetch_url(String url)
   {
      List<String> lines = HttpGetter.httpGet(url);

      for (int i=0; i<lines.size(); i+= 1)
         lines.set(i, filter(lines.get(i).trim()));

      List<String> links = getLinks(TextUtils.join("\n", lines));
      if ( links.size() == 0 )
         return null;

      return links.get(new Random().nextInt(links.size()));
   }

   /* ********************************************************************
    * Extract list of urls from string...
    *
    * source: http://blog.houen.net/java-get-url-from-string/
    */

   private List<String> getLinks(String text)
   {
      ArrayList links = new ArrayList<String>();
    
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(text);

      while( m.find() )
      {
         String str = m.group();
         if (str.startsWith("(") && str.endsWith(")"))
            str = str.substring(1, str.length() - 1);
         links.add(str);
      }

      return links;
   }

   /* ********************************************************************
    * Logging...
    */

   private static void log(String... msg)
      { Logger.log(msg); }
}
