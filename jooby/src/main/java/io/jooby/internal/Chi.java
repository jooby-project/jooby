/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

/** Sync: May 22, 2020 Commit: 5704d7ee98edd3fe55169b506531bdd061667c70 */
class Chi implements RouteTree {
  private static final String EMPTY_STRING = "";
  private static final Slice EMPTY_SLICE = new Slice(EMPTY_STRING);
  private static final byte ntStatic = 0; // /home
  private static final byte ntRegexp = 1; // /{id:[0-9]+}
  private static final byte ntParam = 2; // /{user}
  private static final byte ntCatchAll = 3; // /api/v1/*

  private static final int NODE_SIZE = ntCatchAll + 1;

  static final char ZERO_CHAR = Character.MIN_VALUE;
  private MessageEncoder encoder;

  /** Avoid string allocation */
  private static class Slice implements CharSequence {
    private final String base;
    private final int startIndex;

    private final int endIndex;

    public Slice(String base, int startIndex, int endIndex) {
      this.base = base;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    public Slice(String base, int startIndex) {
      this(base, startIndex, base.length());
    }

    public Slice(String base) {
      this(base, 0);
    }

    @Override
    public int length() {
      return endIndex - startIndex;
    }

    @Override
    public char charAt(int index) {
      return base.charAt(startIndex + index);
    }

    @Override
    public Slice subSequence(int start, int end) {
      return new Slice(base, startIndex + start, startIndex + end);
    }

    @Override
    public String toString() {
      return base.substring(startIndex, endIndex);
    }

    public Slice substring(int start) {
      return substring(start, length());
    }

    public Slice substring(int start, int end) {
      return subSequence(start, end);
    }

    public int indexOf(int ch) {
      for (int i = startIndex; i < endIndex; i++) {
        if (base.charAt(i) == ch) {
          return i - startIndex;
        }
      }
      return -1;
    }

    public boolean startsWith(String prefix) {
      int len = prefix.length();
      if (len <= length()) {
        for (int i = 0; i < len; i++) {
          if (base.charAt(i + startIndex) != prefix.charAt(i)) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
  }

  private interface StaticMap {
    StaticMap INIT =
        new StaticMap() {
          @Override
          public StaticRoute get(String path) {
            return null;
          }

          @Override
          public StaticMap put(String path, StaticRoute staticRoute) {
            return new StaticMap1(path, staticRoute);
          }
        };

    StaticRoute get(String path);

    StaticMap put(String path, StaticRoute staticRoute);
  }

  private static class StaticMap1 implements StaticMap {
    private String pattern;
    private StaticRoute route;

    public StaticMap1(String pattern, StaticRoute route) {
      this.pattern = pattern;
      this.route = route;
    }

    @Override
    public StaticMap put(String path, StaticRoute route) {
      if (pattern.equals(path)) {
        // Override
        this.route = route;
        return this;
      }
      return new StaticMap2(this, path, route);
    }

    public void release() {
      this.pattern = null;
      this.route = null;
    }

    public StaticRoute get(String path) {
      if (pattern.equals(path)) {
        return route;
      } else {
        return null;
      }
    }
  }

  private static class StaticMap2 implements StaticMap {
    private String pattern1;
    private StaticRoute route1;
    private String pattern2;
    private StaticRoute route2;

    public StaticMap2(StaticMap1 staticMap, String path, StaticRoute route) {
      this.pattern1 = staticMap.pattern;
      this.route1 = staticMap.route;
      this.pattern2 = path;
      this.route2 = route;
      staticMap.release();
    }

    public void release() {
      this.pattern1 = null;
      this.route1 = null;
      this.pattern2 = null;
      this.route2 = null;
    }

    @Override
    public StaticMap put(String path, StaticRoute route) {
      if (pattern1.equals(path)) {
        route1 = route;
        return this;
      } else if (pattern2.equals(path)) {
        route2 = route;
        return this;
      }
      return new StaticMap3(this, path, route);
    }

    public StaticRoute get(String path) {
      if (pattern1.equals(path)) {
        return route1;
      } else if (pattern2.equals(path)) {
        return route2;
      } else {
        return null;
      }
    }
  }

  private static class StaticMap3 implements StaticMap {
    private String pattern1;
    private StaticRoute route1;
    private String pattern2;
    private StaticRoute route2;
    private String pattern3;
    private StaticRoute route3;

    public StaticMap3(StaticMap2 staticMap, String path, StaticRoute route) {
      this.pattern1 = staticMap.pattern1;
      this.route1 = staticMap.route1;
      this.pattern2 = staticMap.pattern2;
      this.route2 = staticMap.route2;
      this.pattern3 = path;
      this.route3 = route;
      staticMap.release();
    }

    public void release() {
      this.pattern1 = null;
      this.route1 = null;
      this.pattern2 = null;
      this.route2 = null;
      this.pattern3 = null;
      this.route3 = null;
    }

    @Override
    public StaticMap put(String path, StaticRoute route) {
      if (pattern1.equals(path)) {
        route1 = route;
        return this;
      } else if (pattern2.equals(path)) {
        route2 = route;
        return this;
      } else if (pattern3.equals(path)) {
        route3 = route;
        return this;
      }
      return new StaticMap4(this, path, route);
    }

    public StaticRoute get(String path) {
      if (pattern1.equals(path)) {
        return route1;
      } else if (pattern2.equals(path)) {
        return route2;
      } else if (pattern3.equals(path)) {
        return route3;
      } else {
        return null;
      }
    }
  }

  private static class StaticMap4 implements StaticMap {
    private String pattern1;
    private StaticRoute route1;
    private String pattern2;
    private StaticRoute route2;
    private String pattern3;
    private StaticRoute route3;
    private String pattern4;
    private StaticRoute route4;

    public StaticMap4(StaticMap3 staticMap, String path, StaticRoute route) {
      this.pattern1 = staticMap.pattern1;
      this.route1 = staticMap.route1;
      this.pattern2 = staticMap.pattern2;
      this.route2 = staticMap.route2;
      this.pattern3 = staticMap.pattern3;
      this.route3 = staticMap.route3;
      this.pattern4 = path;
      this.route4 = route;
      staticMap.release();
    }

    public void release() {
      this.pattern1 = null;
      this.route1 = null;
      this.pattern2 = null;
      this.route2 = null;
      this.pattern3 = null;
      this.route3 = null;
      this.pattern4 = null;
      this.route4 = null;
    }

    @Override
    public StaticMap put(String path, StaticRoute route) {
      if (pattern1.equals(path)) {
        route1 = route;
        return this;
      } else if (pattern2.equals(path)) {
        route2 = route;
        return this;
      } else if (pattern3.equals(path)) {
        route3 = route;
        return this;
      } else if (pattern4.equals(path)) {
        route4 = route;
        return this;
      }
      return new StaticMap5(this, path, route);
    }

    public StaticRoute get(String path) {
      if (pattern1.equals(path)) {
        return route1;
      } else if (pattern2.equals(path)) {
        return route2;
      } else if (pattern3.equals(path)) {
        return route3;
      } else if (pattern4.equals(path)) {
        return route4;
      } else {
        return null;
      }
    }
  }

  private static class StaticMap5 implements StaticMap {
    private String pattern1;
    private StaticRoute route1;
    private String pattern2;
    private StaticRoute route2;
    private String pattern3;
    private StaticRoute route3;
    private String pattern4;
    private StaticRoute route4;
    private String pattern5;
    private StaticRoute route5;

    public StaticMap5(StaticMap4 staticMap, String path, StaticRoute route) {
      this.pattern1 = staticMap.pattern1;
      this.route1 = staticMap.route1;
      this.pattern2 = staticMap.pattern2;
      this.route2 = staticMap.route2;
      this.pattern3 = staticMap.pattern3;
      this.route3 = staticMap.route3;
      this.pattern4 = staticMap.pattern4;
      this.route4 = staticMap.route4;
      this.pattern5 = path;
      this.route5 = route;
      staticMap.release();
    }

    public void release() {
      this.pattern1 = null;
      this.route1 = null;
      this.pattern2 = null;
      this.route2 = null;
      this.pattern3 = null;
      this.route3 = null;
      this.pattern4 = null;
      this.route4 = null;
      this.pattern5 = null;
      this.route5 = null;
    }

    @Override
    public StaticMap put(String path, StaticRoute route) {
      if (pattern1.equals(path)) {
        route1 = route;
        return this;
      } else if (pattern2.equals(path)) {
        route2 = route;
        return this;
      } else if (pattern3.equals(path)) {
        route3 = route;
        return this;
      } else if (pattern4.equals(path)) {
        route4 = route;
        return this;
      } else if (pattern5.equals(path)) {
        route5 = route;
        return this;
      }
      return new StaticMap6(this, path, route);
    }

    public StaticRoute get(String path) {
      if (pattern1.equals(path)) {
        return route1;
      } else if (pattern2.equals(path)) {
        return route2;
      } else if (pattern3.equals(path)) {
        return route3;
      } else if (pattern4.equals(path)) {
        return route4;
      } else if (pattern5.equals(path)) {
        return route5;
      } else {
        return null;
      }
    }
  }

  private static class StaticMap6 implements StaticMap {
    private String pattern1;
    private StaticRoute route1;
    private String pattern2;
    private StaticRoute route2;
    private String pattern3;
    private StaticRoute route3;
    private String pattern4;
    private StaticRoute route4;
    private String pattern5;
    private StaticRoute route5;
    private String pattern6;
    private StaticRoute route6;

    public StaticMap6(StaticMap5 staticMap, String path, StaticRoute route) {
      this.pattern1 = staticMap.pattern1;
      this.route1 = staticMap.route1;
      this.pattern2 = staticMap.pattern2;
      this.route2 = staticMap.route2;
      this.pattern3 = staticMap.pattern3;
      this.route3 = staticMap.route3;
      this.pattern4 = staticMap.pattern4;
      this.route4 = staticMap.route4;
      this.pattern5 = staticMap.pattern5;
      this.route5 = staticMap.route5;
      this.pattern6 = path;
      this.route6 = route;
      staticMap.release();
    }

    public void release() {
      this.pattern1 = null;
      this.route1 = null;
      this.pattern2 = null;
      this.route2 = null;
      this.pattern3 = null;
      this.route3 = null;
      this.pattern4 = null;
      this.route4 = null;
      this.pattern5 = null;
      this.route5 = null;
      this.pattern6 = null;
      this.route6 = null;
    }

    @Override
    public StaticMap put(String path, StaticRoute route) {
      if (pattern1.equals(path)) {
        route1 = route;
        return this;
      } else if (pattern2.equals(path)) {
        route2 = route;
        return this;
      } else if (pattern3.equals(path)) {
        route3 = route;
        return this;
      } else if (pattern4.equals(path)) {
        route4 = route;
        return this;
      } else if (pattern5.equals(path)) {
        route5 = route;
        return this;
      } else if (pattern6.equals(path)) {
        route6 = route;
      }
      return new StaticMapN(this, path, route);
    }

    public StaticRoute get(String path) {
      if (pattern1.equals(path)) {
        return route1;
      } else if (pattern2.equals(path)) {
        return route2;
      } else if (pattern3.equals(path)) {
        return route3;
      } else if (pattern4.equals(path)) {
        return route4;
      } else if (pattern5.equals(path)) {
        return route5;
      } else if (pattern6.equals(path)) {
        return route6;
      } else {
        return null;
      }
    }
  }

  private static class StaticMapN implements StaticMap {
    private final Map<String, StaticRoute> paths = new HashMap<>(10);

    public StaticMapN(StaticMap6 staticMap, String path, StaticRoute staticRoute) {
      put(staticMap.pattern1, staticMap.route1);
      put(staticMap.pattern2, staticMap.route2);
      put(staticMap.pattern3, staticMap.route3);
      put(staticMap.pattern4, staticMap.route4);
      put(staticMap.pattern5, staticMap.route5);
      put(staticMap.pattern6, staticMap.route6);
      put(path, staticRoute);

      staticMap.release();
    }

    @Override
    public StaticRoute get(String path) {
      return paths.get(path);
    }

    @Override
    public StaticMap put(String path, StaticRoute staticRoute) {
      paths.computeIfAbsent(path, k -> staticRoute);
      return this;
    }
  }

  private interface MethodMatcher {
    StaticRouterMatch get(String method);

    void put(String method, StaticRouterMatch route);
  }

  private static class SingleMethodMatcher implements MethodMatcher {
    private String method;
    private StaticRouterMatch route;

    @Override
    public void put(String method, StaticRouterMatch route) {
      this.method = method;
      this.route = route;
    }

    @Override
    public StaticRouterMatch get(String method) {
      return this.method.equals(method) ? route : null;
    }

    public void clear() {
      this.method = null;
      this.route = null;
    }
  }

  private static class MultipleMethodMatcher implements MethodMatcher {
    private final Map<String, StaticRouterMatch> methods = new HashMap<>();

    public MultipleMethodMatcher(SingleMethodMatcher matcher) {
      methods.put(matcher.method, matcher.route);
      matcher.clear();
    }

    @Override
    public StaticRouterMatch get(String method) {
      return methods.get(method);
    }

    @Override
    public void put(String method, StaticRouterMatch route) {
      methods.put(method, route);
    }
  }

  static class StaticRoute {
    private MethodMatcher matcher;

    public void put(String method, Route route) {
      if (matcher == null) {
        matcher = new SingleMethodMatcher();
      } else if (matcher instanceof SingleMethodMatcher) {
        matcher = new MultipleMethodMatcher((SingleMethodMatcher) matcher);
      }
      matcher.put(method, new StaticRouterMatch(route));
    }
  }

  static class Segment {
    byte nodeType;
    //    String key = "";
    String rexPat = EMPTY_STRING;
    char tail;
    int startIndex;
    int endIndex;

    public Segment() {}

    public Segment(
        byte nodeType, /*String key,*/ String regex, char tail, int startIndex, int endIndex) {
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
    String prefix;

    // regexp matcher for regexp nodes
    Pattern rex;

    // HTTP handler endpoints on the leaf node
    Map<String, Route> endpoints;

    // subroutes on the leaf node
    // Routes subroutes;

    // child nodes should be stored in-order for iteration,
    // in groups of the node type.
    Node[][] children = new Node[NODE_SIZE][];

    public Node typ(byte typ) {
      this.typ = typ;
      return this;
    }

    @Override
    public int compareTo(Node o) {
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

    public Node prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    @Override
    public String toString() {
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
      String nodes =
          Stream.of(children)
              .filter(Objects::nonNull)
              .flatMap(Stream::of)
              .filter(Objects::nonNull)
              .map(Node::toString)
              .collect(Collectors.joining(", ", "[", "]"));
      node.append(", children: ").append(nodes);
      node.append("}");
      return node.toString();
    }

    Node insertRoute(String method, String pattern, Route route, boolean failOnDuplicateRoutes) {
      Node n = this;
      Node parent;
      String search = pattern;

      while (true) {
        // Handle key exhaustion
        if (search.isEmpty()) {
          // Insert or update the node's leaf handler
          n.setEndpoint(method, route, failOnDuplicateRoutes);
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

        String prefix;
        if (seg.nodeType == ntRegexp) {
          prefix = seg.rexPat;
        } else {
          prefix = EMPTY_STRING;
        }

        // Look for the edge to attach to
        parent = n;
        n = n.getEdge(seg.nodeType, label, seg.tail, prefix);

        // No edge, create one
        if (n == null) {
          Node child = new Node().label(label).tail(seg.tail).prefix(search);
          Node hn = parent.addChild(child, search);
          hn.setEndpoint(method, route, failOnDuplicateRoutes);
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
        if (search.isEmpty()) {
          child.setEndpoint(method, route, failOnDuplicateRoutes);
          return child;
        }

        // Create a new edge for the node
        Node subchild = new Node().typ(ntStatic).label(search.charAt(0)).prefix(search);
        Node hn = child.addChild(subchild, search);
        hn.setEndpoint(method, route, failOnDuplicateRoutes);
        return hn;
      }
    }

    // addChild appends the new `child` node to the tree using the `pattern` as the trie key.
    // For a URL router like chi's, we split the static, param, regexp and wildcard segments
    // into different nodes. In addition, addChild will recursively call itself until every
    // pattern segment is added to the url pattern tree as individual nodes, depending on type.
    Node addChild(Node child, String search) {
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
            child.rex = Pattern.compile(seg.rexPat);
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

              Node nn = new Node().typ(ntStatic).label(search.charAt(0)).prefix(search);
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

    Node getEdge(int ntyp, char label, char tail, String prefix) {
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

    void setEndpoint(String method, Route route, boolean failOnDuplicateRoutes) {
      Node n = this;
      // Set the handler for the method type on the node
      if (n.endpoints == null) {
        n.endpoints = new HashMap<>();
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
      if (failOnDuplicateRoutes) {
        var existing = n.endpoints.get(method);
        if (existing != null) {
          throw new IllegalArgumentException(
              "Route already exists: "
                  + method
                  + " "
                  + existing.getPattern()
                  + " at "
                  + existing.getLocation().filename()
                  + ":"
                  + existing.getLocation().line());
        }
      }
      n.endpoints.put(method, route);
      //        Endpoint h = n.endpoints.computeIfAbsent(method, k -> new Endpoint(handler));
      //        h.handler = handler;
      //        h.pattern = pattern;
      //        h.paramKeys = paramKeys;
      // }
    }

    // Recursive edge traversal by checking all nodeTyp groups along the way.
    // It's like searching through a multi-dimensional radix trie.
    Route findRoute(RouterMatch rctx, String method, Slice path) {

      for (int ntyp = 0; ntyp < NODE_SIZE; ntyp++) {
        Node[] nds = this.children[ntyp];
        if (nds != null) {
          Node xn = null;
          Slice xsearch = path;

          char label = xsearch.isEmpty() ? ZERO_CHAR : xsearch.charAt(0);

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
              if (xsearch.isEmpty()) {
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
                  // 12, ar, page, arx
                  if (!xn.rex.matcher(xsearch.substring(0, p)).matches()) {
                    continue;
                  }
                } else if (xsearch.substring(0, p).indexOf('/') > 0) {
                  // avoid a newRuntimeRoute across path segments
                  continue;
                }

                // rctx.routeParams.Values = append(rctx.routeParams.Values, xsearch[:p])
                int prevlen = rctx.vars.size();
                rctx.value(xsearch.substring(0, p).toString());
                xsearch = xsearch.substring(p);

                if (xsearch.isEmpty()) {
                  if (xn.isLeaf()) {
                    Route h = xn.endpoints.get(method);
                    if (h != null) {
                      rctx.key(h.getPathKeys());
                      return h;
                    }
                    rctx.methodNotAllowed(xn.endpoints.keySet());
                  }
                }

                // recursively find the next node on this branch
                Route fin = xn.findRoute(rctx, method, xsearch);
                if (fin != null) {
                  return fin;
                }

                // not found on this branch, reset vars
                rctx.truncate(prevlen);
                xsearch = path;
              }
              break;
            default:
              // catch-all nodes
              // rctx.routeParams.Values = append(rctx.routeParams.Values, search)
              if (!xsearch.isEmpty()) {
                rctx.value(xsearch.toString());
              }
              xn = nds[0];
              xsearch = EMPTY_SLICE;
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
            //            rctx.routeParams.Values =
            // rctx.routeParams.Values[:len(rctx.routeParams.Values) - 1]
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
    int longestPrefix(String k1, String k2) {
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
        return new Node[] {child};
      }
      Node[] result = new Node[src.length + 1];
      System.arraycopy(src, 0, result, 0, src.length);
      result[result.length - 1] = child;
      return result;
    }

    // patNextSegment returns the next segment details from a pattern:
    // node type, param key, regexp string, param tail byte, param starting index, param ending
    // index
    Segment patNextSegment(String pattern) {
      int ps = pattern.indexOf('{');
      int ws = pattern.indexOf('*');

      if (ps < 0 && ws < 0) {
        return new Segment(
            ntStatic, EMPTY_STRING, ZERO_CHAR, 0, pattern.length()); // we return the entire thing
      }

      // Sanity check
      if (ws >= 0 && ws < ps) {
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
        String range = pattern.substring(ps);
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

        String key = pattern.substring(ps + 1, pe);
        pe++; // set end to next position

        if (pe < pattern.length()) {
          tail = pattern.charAt(pe);
        }

        String rexpat = "";
        int idx = key.indexOf(':');
        if (idx >= 0) {
          nt = ntRegexp;
          rexpat = key.substring(idx + 1);
          //          key = key.substring(0, idx);
        }

        if (!rexpat.isEmpty()) {
          if (rexpat.charAt(0) != '^') {
            rexpat = "^" + rexpat;
          }
          if (rexpat.charAt(rexpat.length() - 1) != '$') {
            rexpat = rexpat + "$";
          }
        }

        return new Segment(nt, rexpat, tail, ps, pe);
      }

      // Wildcard pattern as finale
      // EDIT: should we panic if there is stuff after the * ???
      // We allow naming a wildcard: *path
      // String key = ws == pattern.length() - 1 ? "*" : pattern.substring(ws + 1).toString();
      return new Segment(ntCatchAll, EMPTY_STRING, ZERO_CHAR, ws, pattern.length());
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

  private static final String BASE_CATCH_ALL = "/?*";

  private final Node root = new Node();

  private StaticMap staticPaths = StaticMap.INIT;

  boolean failOnDuplicateRoutes;

  public Chi(boolean failOnDuplicateRoutes) {
    this.failOnDuplicateRoutes = failOnDuplicateRoutes;
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
      StaticRoute staticRoute = new StaticRoute();
      staticPaths = staticPaths.put(pattern, staticRoute);
      staticRoute.put(method, route);
    }
    root.insertRoute(method, pattern, route, failOnDuplicateRoutes);
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
    return find(method, path).matches();
  }

  @Override
  public Router.Match find(String method, String path) {
    StaticRoute staticRoute = staticPaths.get(path);
    if (staticRoute == null) {
      return findInternal(method, path);
    } else {
      StaticRouterMatch match = staticRoute.matcher.get(method);
      return match == null ? findInternal(method, path) : match;
    }
  }

  private Router.Match findInternal(String method, String path) {
    // use radix tree
    RouterMatch result = new RouterMatch();
    Route route = root.findRoute(result, method, new Slice(path));
    if (route == null) {
      return result.missing(method, path, encoder);
    }
    return result.found(route);
  }

  public void setEncoder(MessageEncoder encoder) {
    this.encoder = encoder;
  }
}
