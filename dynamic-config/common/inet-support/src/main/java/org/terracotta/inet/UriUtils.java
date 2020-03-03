/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.inet;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Anthony Dahanne on 2015-12-01.
 *
 * @author Anthony Dahanne
 * @author Mathieu Carbou
 */
public class UriUtils {

  private UriUtils() {
  }

  public static List<String> parseSegments(String uri) {
    try {
      List<String> segments = new ArrayList<>();
      String path = parsePath(uri);
      if (path != null) {
        for (String segment : path.split("/")) {
          if (segment.length() > 0)
            segments.add(URLDecoder.decode(segment, "UTF-8"));
        }
      }
      return segments;
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(); // if utf-8 is not supported, call 911.
    }
  }

  public static List<InetSocketAddress> parseInetSocketAddresses(String uri) {
    return parseInetSocketAddresses(uri, 9410);
  }

  public static List<InetSocketAddress> parseInetSocketAddresses(String uri, int defaultPort) {
    String auth = parseAuthority(uri);
    if (auth == null) {
      return Collections.emptyList();
    }
    return InetSocketAddressConvertor.getInetSocketAddresses(auth.split(","), defaultPort);
  }

  public static String parseScheme(String uri) {
    if (uri == null) {
      return null;
    }
    int index = uri.indexOf(":");
    // let the standard URI parser parse our scheme
    return index <= 0 ? null : URI.create(uri.substring(0, index) + "://fake").getScheme();
  }

  public static String parseAuthority(String uri) {
    if (uri == null) {
      return null;
    }
    int index = uri.indexOf("://");
    if (index <= 0) {
      return null;
    }
    String schemeLess = uri.substring(index + 3);
    String authority;
    if (schemeLess.isEmpty()) {
      return null;
    } else {
      int beginningOfPath = schemeLess.indexOf("/");
      int beginningOfQuery = schemeLess.indexOf("?");
      int beginningOfFragment = schemeLess.indexOf("#");
      if (beginningOfPath != -1) {
        authority = schemeLess.substring(0, beginningOfPath);
      } else if (beginningOfQuery != -1) {
        authority = schemeLess.substring(0, beginningOfQuery);
      } else if (beginningOfFragment != -1) {
        authority = schemeLess.substring(0, beginningOfFragment);
      } else {
        authority = schemeLess;
      }
    }
    return authority.isEmpty() ? null : authority;
  }

  public static String parseUserInfo(String uri) {
    String authority = parseAuthority(uri);
    if (authority == null) {
      return null;
    } else {
      String[] servers = authority.split(",");
      if (servers.length > 1) {
        return null;
      } else {
        String server = servers[0];
        int indexOfAlpha = server.indexOf("@");
        if (indexOfAlpha == -1) {
          return null;
        } else {
          String userInfo = server.substring(0, indexOfAlpha);
          return userInfo.isEmpty() ? null : userInfo;
        }
      }
    }
  }

  public static String parsePath(String uri) {
    if (uri == null) {
      return null;
    }
    int schemePosition = uri.indexOf("://");
    if (schemePosition == 0) {
      return null;
    }

    String schemeLess;
    if (schemePosition != -1) {
      //We can have a path in the absence of a scheme
      schemeLess = uri.substring(schemePosition + 3);
    } else {
      schemeLess = uri;
    }
    String authority = parseAuthority(uri);
    String path;
    if (authority == null) {
      //In the absence of an authority, a relative path is assumed to be present in the URI
      int beginningOfQuery = schemeLess.indexOf("?");
      int beginningOfFragment = schemeLess.indexOf("#");
      if (beginningOfQuery != -1) {
        path = schemeLess.substring(0, beginningOfQuery);
      } else if (beginningOfFragment != -1) {
        path = schemeLess.substring(0, beginningOfFragment);
      } else {
        path = schemeLess;
      }
    } else {
      String authorityLess = schemeLess.replace(authority, "");
      int beginningOfQuery = authorityLess.indexOf("?");
      int beginningOfFragment = authorityLess.indexOf("#");
      if (beginningOfQuery != -1) {
        path = authorityLess.substring(0, beginningOfQuery);
      } else if (beginningOfFragment != -1) {
        path = authorityLess.substring(0, beginningOfFragment);
      } else {
        path = authorityLess;
      }
    }
    return path;
  }

  public static String parseQuery(String uri) {
    if (uri == null) {
      return null;
    }
    int indexOfQuestionMark = uri.indexOf("?");
    if (indexOfQuestionMark == -1) {
      return null;
    }
    int indexOfHash = uri.indexOf("#");
    String query;
    if (indexOfHash != -1) {
      query = uri.substring(indexOfQuestionMark + 1, indexOfHash);
    } else {
      query = uri.substring(indexOfQuestionMark + 1);
    }
    return query.isEmpty() ? null : query;
  }

  public static String appendSegmentToPath(String path, String segment) {
    if (path == null) {
      path = "";
    }
    if (segment == null) {
      segment = "";
    }
    if (path.isEmpty()) {
      return segment;
    } else if (segment.isEmpty()) {
      return path;
    } else if (path.charAt(path.length() - 1) == '/' && segment.startsWith("/")) {
      return path.substring(0, path.length() - 1) + segment;
    } else if (path.charAt(path.length() - 1) == '/' || segment.startsWith("/")) {
      return path + segment;
    } else {
      return path + "/" + segment;
    }
  }

}
