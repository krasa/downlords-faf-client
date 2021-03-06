package com.faforever.client.legacy.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.legacy.htmlparser.HtmlParser;
import com.faforever.client.map.MapVaultHtmlContentHandler;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class LegacyMapVaultParser implements MapVaultParser {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  HtmlParser htmlParser;

  @Override
  public List<MapInfoBean> parseMapVault(int page, int maxEntries) throws IOException {
    MapVaultHtmlContentHandler mapVaultHtmlContentHandler = new MapVaultHtmlContentHandler();

    String urlString = environment.getProperty("vault.mapQueryUrl");
    String params = String.format(environment.getProperty("vault.mapQueryParams"), page, maxEntries);

    URL url = new URL(urlString + "?" + params);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    logger.debug("Reading maps from {}", url);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
      JsonReader jsonReader = new JsonReader(reader);
      jsonReader.beginObject();

      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        if (!"layout".equals(key)) {
          jsonReader.skipValue();
          continue;
        }

        String layout = jsonReader.nextString();

        return htmlParser.parse(layout, mapVaultHtmlContentHandler);
      }

      jsonReader.endObject();
    }

    throw new IllegalStateException("Map vault could not be read from " + url);
  }
}
