/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sync: 20-01-20
 * Commit: 17fb1065d2b256d20f68bed0b7bca6c2942aff49
 */
class Chi implements RouteTree {
  private static final byte ntStatic = 0;// /home
  private static final byte ntRegexp = 1;                // /{id:[0-9]+}
  private static final byte ntParam = 2;                // /{user}
  private static final byte ntCatchAll = 3;               // /api/v1/*

  private static int NODE_SIZE = ntCatchAll + 1;

  static final StaticRoute NO_MATCH = new StaticRoute(Collections.emptyMap());

  static final char ZERO_CHAR = (char) 0;
  private MessageEncoder encoder;

  static class StaticRoute {
    private final Map<String, StaticRouterMatch> methods;

    public StaticRoute(Map<String, StaticRouterMatch> methods) {
      this.methods = methods;
    }

    public StaticRoute() {
      this(newMap(Router.METHODS.size()));
    }

    public void put(String method, Route route) {
      methods.put(method, new StaticRouterMatch(route));
    }
  }

  static class ZeroCopyString {
    public static final ZeroCopyString EMPTY = new ZeroCopyString(new char[0], 0, 0);

    private int offset;
    private int length;
    private int hash = 0;
    private char[] value;

    public ZeroCopyString(String source) {
      this.offset = 0;
      this.length = source.length();
      this.value = source.toCharArray();
    }

    @Override public boolean equals(Object anObject) {
      if (this == anObject) {
        return true;
      }
      if (anObject instanceof ZeroCopyString) {
        ZeroCopyString anotherString = (ZeroCopyString) anObject;
        int n = length;
        if (n == anotherString.length) {
          char v1[] = value;
          char v2[] = anotherString.value;
          int i = 0;
          while (n-- != 0) {
            if (v1[i + offset] != v2[i + anotherString.offset])
              return false;
            i++;
          }
          return true;
        }
      }
      return false;
    }

    public int hashCode() {
      int h = hash;
      if (h == 0 && length > 0) {
        char val[] = value;
        int len = offset + length;
        for (int i = offset; i < len; i++) {
          h = 31 * h + val[i];
        }
        hash = h;
      }
      return h;
    }

    protected ZeroCopyString(char[] source, int offset, int length) {
      this.offset = offset;
      this.length = length;
      this.value = source;
    }

    public int length() {
      return length;
    }

    public ZeroCopyString substring(int beginIndex) {
      return (beginIndex == 0)
          ? this
          : new ZeroCopyString(value, offset + beginIndex, length - beginIndex);
    }

    public ZeroCopyString substring(int beginIndex, int endIndex) {
      int len = endIndex - beginIndex;
      return (beginIndex == 0 && len == length)
          ? this
          : new ZeroCopyString(value, offset + beginIndex, endIndex - beginIndex);
    }

    public char charAt(int index) {
      return value[offset + index];
    }

    public int indexOf(int ch) {
      int fromIndex = offset;
      final int max = Math.min(value.length, offset + length);

      final char[] value = this.value;
      for (int i = fromIndex; i < max; i++) {
        if (value[i] == ch) {
          return i - offset;
        }
      }
      return -1;
    }

    public boolean startsWith(ZeroCopyString prefix) {
      char ta[] = value;
      int to = offset;
      char pa[] = prefix.value;
      int po = prefix.offset;
      int pc = prefix.length;
      // Note: toffset might be near -1>>>1.
      if (pc > length) {
        return false;
      }
      while (--pc >= 0) {
        if (ta[to++] != pa[po++]) {
          return false;
        }
      }
      return true;
    }

    @Override public String toString() {
      return new String(value, offset, length);
    }
  }

  static class Segment {
    byte nodeType;
    //    String key = "";
    ZeroCopyString rexPat = ZeroCopyString.EMPTY;
    char tail;
    int startIndex;
    int endIndex;

    public Segment() {
    }

    public Segment(byte nodeType, /*String key,*/ ZeroCopyString regex, char tail, int startIndex,
        int endIndex) {
      this.nodeType = nodeType;
      //      this.key = key;
      this.rexPat = regex;
      this.tail = tail;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }
  }

  private static class Node implements Comparable<Node> {
    // node type: static, regexp, param, catchAll
    byte typ;

    // first byte of the prefix
    char label;

    // first byte of the child prefix
    char tail;

    // prefix is the common prefix we ignore
    ZeroCopyString prefix;

    // regexp matcher for regexp nodes
    Pattern rex;

    // HTTP handler endpoints on the leaf node
    Map<String, Route> endpoints;

    // subroutes on the leaf node
    //Routes subroutes;

    // child nodes should be stored in-order for iteration,
    // in groups of the node type.
    Node[][] children = new Node[NODE_SIZE][];

    public Node typ(byte typ) {
      this.typ = typ;
      return this;
    }

    @Override public int compareTo(Node o) {
      return label - o.label;
    }

    public Node label(char label) {
      this.label = label;
      return this;
    }

    public Node tail(char tail) {
      this.tail = tail;
      return this;
    }

    public Node prefix(ZeroCopyString prefix) {
      this.prefix = prefix;
      return this;
    }

    @Override public String toString() {
      StringBuilder node = new StringBuilder();
      if (prefix != null) {
        node.append(prefix);
      }
      node.append("{type: ");
      switch (typ) {
        case ntStatic:
          node.append("static");
          break;
        case ntParam:
          node.append("param");
          break;
        case ntRegexp:
          node.append("regex");
          break;
        default:
          node.append("catch-all");
      }
      String nodes = Stream.of(children).filter(Objects::nonNull)
          .flatMap(Stream::of)
          .filter(Objects::nonNull)
          .map(Node::toString)
          .collect(Collectors.joining(", ", "[", "]"));
      node.append(", children: ").append(nodes);
      node.append("}");
      return node.toString();
    }

    Node insertRoute(String method, ZeroCopyString pattern, Route route) {
      Node n = this;
      Node parent;
      ZeroCopyString search = pattern;

      while (true) {
        // Handle key exhaustion
        if (search.length() == 0) {
          // Insert or update the node's leaf handler
          n.setEndpoint(method, route);
          return n;
        }

        // We're going to be searching for a wild node next,
        // in this case, we need to get the tail
        char label = search.charAt(0);
        //        char segTail;
        //        int segEndIdx;
        //        int segTyp;
        Segment seg;
        if (label == '{' || label == '*') {
          //          segTyp, _, segRexpat, segTail, _, segEndIdx = patNextSegment(search)
          seg = patNextSegment(search);
        } else {
          seg = new Segment();
        }

        ZeroCopyString prefix;
        if (seg.nodeType == ntRegexp) {
          prefix = seg.rexPat;
        } else {
          prefix = ZeroCopyString.EMPTY;
        }

        // Look for the edge to attach to
        parent = n;
        n = n.getEdge(seg.nodeType, label, seg.tail, prefix);

        // No edge, create one
        if (n == null) {
          Node child = new Node().label(label).tail(seg.tail).prefix(search);
          Node hn = parent.addChild(child, search);
          hn.setEndpoint(method, route);
          return hn;
        }

        // Found an edge to newRuntimeRoute the pattern

        if (n.typ > ntStatic) {
          // We found a param node, trim the param from the search path and continue.
          // This param/wild pattern segment would already be on the tree from a previous
          // call to addChild when creating a new node.
          search = search.substring(seg.endIndex);
          continue;
        }

        // Static nodes fall below here.
        // Determine longest prefix of the search key on newRuntimeRoute.
        int commonPrefix = longestPrefix(search, n.prefix);
        if (commonPrefix == n.prefix.length()) {
          // the common prefix is as long as the current node's prefix we're attempting to insert.
          // keep the search going.
          search = search.substring(commonPrefix);
          continue;
        }

        // Split the node
        Node child = new Node().typ(ntStatic).prefix(search.substring(0, commonPrefix));
        parent.replaceChild(search.charAt(0), seg.tail, child);

        // Restore the existing node
        n.label = n.prefix.charAt(commonPrefix);
        n.prefix = n.prefix.substring(commonPrefix);
        child.addChild(n, n.prefix);

        // If the new key is a subset, set the method/handler on this node and finish.
        search = search.substring(commonPrefix);
        if (search.length() == 0) {
          child.setEndpoint(method, route);
          return child;
        }

        // Create a new edge for the node
        Node subchild = new Node().typ(ntStatic).label(search.charAt(0)).prefix(search);
        Node hn = child.addChild(subchild, search);
        hn.setEndpoint(method, route);
        return hn;
      }
    }

    // addChild appends the new `child` node to the tree using the `pattern` as the trie key.
    // For a URL router like chi's, we split the static, param, regexp and wildcard segments
    // into different nodes. In addition, addChild will recursively call itself until every
    // pattern segment is added to the url pattern tree as individual nodes, depending on type.
    Node addChild(Node child, ZeroCopyString search) {
      Node n = this;
      //      String search = prefix.toString();

      // handler leaf node added to the tree is the child.
      // this may be overridden later down the flow
      Node hn = child;

      // Parse next segment
      //      segTyp, _, segRexpat, segTail, segStartIdx, segEndIdx := patNextSegment(search)
      Segment seg = patNextSegment(search);
      byte segTyp = seg.nodeType;
      int segStartIdx = seg.startIndex;
      int segEndIdx = seg.endIndex;
      // Add child depending on next up segment
      switch (segTyp) {

        case ntStatic:
          // Search prefix is all static (that is, has no params in path)
          // noop
          break;

        default:
          // Search prefix contains a param, regexp or wildcard

          if (segTyp == ntRegexp) {
            child.prefix = seg.rexPat;
            child.rex = Pattern.compile(seg.rexPat.toString());
          }

          if (segStartIdx == 0) {
            // Route starts with a param
            child.typ = segTyp;

            if (segTyp == ntCatchAll) {
              segStartIdx = -1;
            } else {
              segStartIdx = segEndIdx;
            }
            if (segStartIdx < 0) {
              segStartIdx = search.length();
            }
            child.tail = seg.tail; // for params, we set the tail

            if (segStartIdx != search.length()) {
              // add static edge for the remaining part, split the end.
              // its not possible to have adjacent param nodes, so its certainly
              // going to be a static node next.

              search = search.substring(segStartIdx); // advance search position

              Node nn = new Node().typ(ntStatic).label(search.charAt(0))
                  .prefix(search);
              hn = child.addChild(nn, search);
            }

          } else if (segStartIdx > 0) {
            // Route has some param

            // starts with a static segment
            child.typ = ntStatic;
            child.prefix = search.substring(0, segStartIdx);
            child.rex = null;

            // add the param edge node
            search = search.substring(segStartIdx);

            Node nn = new Node().typ(segTyp).label(search.charAt(0)).tail(seg.tail);
            hn = child.addChild(nn, search);
          }
      }

      n.children[child.typ] = append(n.children[child.typ], child);
      tailSort(n.children[child.typ]);
      return hn;
    }

    void replaceChild(char label, char tail, Node child) {
      Node n = this;
      Node[] children = n.children[child.typ];
      for (int i = 0; children != null && i < children.length; i++) {
        if (children[i].label == label && children[i].tail == tail) {
          children[i] = child;
          children[i].label = label;
          children[i].tail = tail;
          return;
        }
      }
      throw new IllegalArgumentException("chi: replacing missing child");
    }

    Node getEdge(int ntyp, char label, char tail, ZeroCopyString prefix) {
      Node n = this;
      Node[] nds = n.children[ntyp];
      for (int i = 0; nds != null && i < nds.length; i++) {
        if (nds[i].label == label && nds[i].tail == tail) {
          if (ntyp == ntRegexp && !nds[i].prefix.equals(prefix)) {
            continue;
          }
          return nds[i];
        }
      }
      return null;
    }

    void setEndpoint(String method, Route route) {
      Node n = this;
      // Set the handler for the method type on the node
      if (n.endpoints == null) {
        n.endpoints = new ConcurrentHashMap<>();
      }

      //      if ((method & mSTUB) == mSTUB) {
      //        n.endpoints.put(mSTUB, new Endpoint(handler));
      //      }
      //      if ((method & mALL) == mALL) {
      //        Endpoint h = n.endpoints.get(mALL);
      //        h.handler = handler;
      //        h.pattern = pattern;
      //        h.paramKeys = paramKeys;
      //        for (Integer m : methodMap.values()) {
      //          h = n.endpoints.computeIfAbsent(m, k -> new Endpoint(handler));
      //          h.handler = handler;
      //          h.pattern = pattern;
      //          h.paramKeys = paramKeys;
      //        }
      //      } else {
      n.endpoints.put(method, route);
      //        Endpoint h = n.endpoints.computeIfAbsent(method, k -> new Endpoint(handler));
      //        h.handler = handler;
      //        h.pattern = pattern;
      //        h.paramKeys = paramKeys;
      //}
    }

    // Recursive edge traversal by checking all nodeTyp groups along the way.
    // It's like searching through a multi-dimensional radix trie.
    Route findRoute(RouterMatch rctx, String method, ZeroCopyString path) {
      Node n = this;
      Node nn = n;

      ZeroCopyString search = path;

      for (int ntyp = 0; ntyp < NODE_SIZE; ntyp++) {
        Node[] nds = nn.children[ntyp];
        if (nds != null) {
          Node xn = null;
          ZeroCopyString xsearch = search;

          char label = search.length() > 0 ? search.charAt(0) : ZERO_CHAR;

          switch (ntyp) {
            case ntStatic:
              xn = findEdge(nds, label);
              if (xn == null || !xsearch.startsWith(xn.prefix)) {
                continue;
              }
              xsearch = xsearch.substring(xn.prefix.length());
              break;

            case ntParam:
            case ntRegexp:
              // short-circuit and return no matching route for empty param values
              if (xsearch.length() == 0) {
                continue;
              }
              // serially loop through each node grouped by the tail delimiter
              for (int idx = 0; idx < nds.length; idx++) {
                xn = nds[idx];

                // label for param nodes is the delimiter byte
                int p = xsearch.indexOf(xn.tail);

                if (p < 0) {
                  if (xn.tail == '/') {
                    p = xsearch.length();
                  } else {
                    continue;
                  }
                }

                if (ntyp == ntRegexp && xn.rex != null) {
                  if (!xn.rex.matcher(xsearch.substring(0, p).toString()).matches()) {
                    continue;
                  }
                } else if (xsearch.substring(0, p).indexOf('/') != -1) {
                  // avoid a newRuntimeRoute across path segments
                  continue;
                }

                // rctx.routeParams.Values = append(rctx.routeParams.Values, xsearch[:p])
                rctx.value(xsearch.substring(0, p));
                xsearch = xsearch.substring(p);
                break;
              }
              break;

            default:
              // catch-all nodes
              // rctx.routeParams.Values = append(rctx.routeParams.Values, search)
              if (xsearch.length() > 0) {
                rctx.value(xsearch);
              }
              xn = nds[0];
              xsearch = ZeroCopyString.EMPTY;
          }

          if (xn == null) {
            continue;
          }

          // did we returnType it yet?
          if (xsearch.length() == 0) {
            if (xn.isLeaf()) {
              Route h = xn.endpoints.get(method);
              if (h != null) {
                // rctx.routeParams.Keys = append(rctx.routeParams.Keys, h.paramKeys...)
                rctx.key(h.getPathKeys());
                return h;
              }

              // flag that the routing context found a route, but not a corresponding
              // supported method
              rctx.methodNotAllowed(xn.endpoints.keySet());
            }
          }

          // recursively returnType the next node..
          Route fin = xn.findRoute(rctx, method, xsearch);
          if (fin != null) {
            return fin;
          }

          // Did not returnType final handler, let's remove the param here if it was set
          if (xn.typ > ntStatic) {
            //          if len(rctx.routeParams.Values) > 0 {
            //            rctx.routeParams.Values = rctx.routeParams.Values[:len(rctx.routeParams.Values) - 1]
            //          }
            rctx.pop();
          }
        }
      }

      return null;
    }

    Node findEdge(Node[] ns, char label) {
      int num = ns.length;
      int idx = 0;
      int i = 0, j = num - 1;
      while (i <= j) {
        idx = i + (j - i) / 2;
        if (label > ns[idx].label) {
          i = idx + 1;
        } else if (label < ns[idx].label) {
          j = idx - 1;
        } else {
          i = num; // breaks cond
        }
      }
      if (ns[idx].label != label) {
        return null;
      }
      return ns[idx];
    }

    boolean isLeaf() {
      return endpoints != null;
    }

    // longestPrefix finds the filesize of the shared prefix of two strings
    int longestPrefix(ZeroCopyString k1, ZeroCopyString k2) {
      int len = Math.min(k1.length(), k2.length());
      for (int i = 0; i < len; i++) {
        if (k1.charAt(i) != k2.charAt(i)) {
          return i;
        }
      }
      return len;
    }

    void tailSort(Node[] ns) {
      if (ns != null && ns.length > 1) {
        Arrays.sort(ns);
        for (int i = ns.length - 1; i >= 0; i--) {
          if (ns[i].typ > ntStatic && ns[i].tail == '/') {
            Node tmp = ns[i];
            ns[i] = ns[ns.length - 1];
            ns[ns.length - 1] = tmp;
            return;
          }
        }
      }
    }

    private Node[] append(Node[] src, Node child) {
      if (src == null) {
        return new Node[]{child};
      }
      Node[] result = new Node[src.length + 1];
      System.arraycopy(src, 0, result, 0, src.length);
      result[result.length - 1] = child;
      return result;
    }

    // patNextSegment returns the next segment details from a pattern:
    // node type, param key, regexp string, param tail byte, param starting index, param ending index
    Segment patNextSegment(ZeroCopyString pattern) {
      int ps = pattern.indexOf('{');
      int ws = pattern.indexOf('*');

      if (ps < 0 && ws < 0) {
        return new Segment(ntStatic, ZeroCopyString.EMPTY, (char) 0, 0,
            pattern.length()); // we return the entire thing
      }

      // Sanity check
      if (ps >= 0 && ws >= 0 && ws < ps) {
        throw new IllegalArgumentException(
            "chi: wildcard '*' must be the last pattern in a route, otherwise use a '{param}'");
      }

      char tail = '/'; // Default endpoint tail to / byte

      if (ps >= 0) {
        // Param/Regexp pattern is next
        byte nt = ntParam;

        // Read to closing } taking into account opens and closes in curl count (cc)
        int cc = 0;
        int pe = ps;
        ZeroCopyString range = pattern.substring(ps);
        for (int i = 0; i < range.length(); i++) {
          char c = range.charAt(i);
          if (c == '{') {
            cc++;
          } else if (c == '}') {
            cc--;
            if (cc == 0) {
              pe = ps + i;
              break;
            }
          }
        }
        if (pe == ps) {
          throw new IllegalArgumentException(
              "Router: route param closing delimiter '}' is missing");
        }

        ZeroCopyString key = pattern.substring(ps + 1, pe);
        pe++; // set end to next position

        if (pe < pattern.length()) {
          tail = pattern.charAt(pe);
        }

        String rexpat = "";
        int idx = key.indexOf(':');
        if (idx >= 0) {
          nt = ntRegexp;
          rexpat = key.substring(idx + 1).toString();
          //          key = key.substring(0, idx);
        }

        if (rexpat.length() > 0) {
          if (rexpat.charAt(0) != '^') {
            rexpat = "^" + rexpat;
          }
          if (rexpat.charAt(rexpat.length() - 1) != '$') {
            rexpat = rexpat + "$";
          }
        }

        return new Segment(nt, new ZeroCopyString(rexpat), tail, ps, pe);
      }

      // Wildcard pattern as finale
      // EDIT: should we panic if there is stuff after the * ???
      // We allow naming a wildcard: *path
      //String key = ws == pattern.length() - 1 ? "*" : pattern.substring(ws + 1).toString();
      return new Segment(ntCatchAll, ZeroCopyString.EMPTY, (char) 0, ws, pattern.length());
    }

    public void destroy() {
      for (int ntyp = 0; ntyp < children.length; ntyp++) {
        Node[] nds = children[ntyp];
        if (nds != null) {
          for (int i = 0; i < nds.length; i++) {
            nds[i].destroy();
            nds[i] = null;
          }
          children[ntyp] = null;
        }
      }
      children = null;
      if (this.endpoints != null) {
        this.endpoints.clear();
        this.endpoints = null;
      }
    }
  }

  private static String BASE_CATCH_ALL = "/?*";

  private Node root = new Node();

  /** Not need to use a concurrent map, due we don't allow to add routes after application started. */
  private Map<Object, StaticRoute> staticPaths = newMap(16);

  static <K, V> Map<K, V> newMap(int size) {
    return new ConcurrentHashMap<>(size);
  }

  public void insert(String method, String pattern, Route route) {
    String baseCatchAll = baseCatchAll(pattern);
    if (baseCatchAll.length() > 1) {
      // Add route pattern: /static/?* => /static
      insert(method, baseCatchAll, route);
      String tail = pattern.substring(baseCatchAll.length() + 2);
      pattern = baseCatchAll + "/" + tail;
    }
    if (pattern.equals(BASE_CATCH_ALL)) {
      pattern = "/*";
    }
    if (Router.pathKeys(pattern).isEmpty()) {
      StaticRoute staticRoute = staticPaths.computeIfAbsent(pattern, k -> new StaticRoute());
      staticRoute.put(method, route);
    }
    root.insertRoute(method, new ZeroCopyString(pattern), route);
  }

  private String baseCatchAll(String pattern) {
    int i = pattern.indexOf(BASE_CATCH_ALL);
    if (i > 0) {
      return pattern.substring(0, i);
    }
    return "";
  }

  public void insert(Route route) {
    insert(route.getMethod(), route.getPattern(), route);
  }

  public void destroy() {
    root.destroy();
  }

  public boolean exists(String method, String path) {
    if (!staticPaths.getOrDefault(path, NO_MATCH).methods.containsKey(method)) {
      return root.findRoute(new RouterMatch(), method, new ZeroCopyString(path)) != null;
    }
    return true;
  }

  @Override public Router.Match find(String method, String path) {
    StaticRouterMatch match = staticPaths.getOrDefault(path, NO_MATCH).methods.get(method);
    if (match == null) {
      // use radix tree
      RouterMatch result = new RouterMatch();
      Route route = root.findRoute(result, method, new ZeroCopyString(path));
      if (route == null) {
        return result.missing(method, path, encoder);
      }
      return result.found(route);
    }
    return match;
  }

  public void setEncoder(MessageEncoder encoder) {
    this.encoder = encoder;
  }
}
