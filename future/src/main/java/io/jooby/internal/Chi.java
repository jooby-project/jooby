package io.jooby.internal;

import io.jooby.Handler;
import io.jooby.RootHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Chi {

  public static final int mSTUB = 1;
  public static final int mCONNECT = 2;
  public static final int mDELETE = 3;
  public static final int mGET = 4;
  public static final int mHEAD = 5;
  public static final int mOPTIONS = 6;
  public static final int mPATCH = 7;
  public static final int mPOST = 8;
  public static final int mPUT = 9;
  public static final int mTRACE = 10;

  static final int ntStatic = 0;// /home
  static final int ntRegexp = 1;                // /{id:[0-9]+}
  static final int ntParam = 2;                // /{user}
  static final int ntCatchAll = 3;               // /api/v1/*

  int mALL = mCONNECT | mDELETE | mGET | mHEAD | mOPTIONS | mPATCH | mPOST | mPUT | mTRACE;

  Map<String, Integer> methodMap = new HashMap<>();

  {
    methodMap.put("CONNECT", mCONNECT);
    methodMap.put("DELETE", mDELETE);
    methodMap.put("GET", mGET);
    methodMap.put("HEAD", mHEAD);
    methodMap.put("OPTIONS", mOPTIONS);
    methodMap.put("PATCH", mPATCH);
    methodMap.put("POST", mPOST);
    methodMap.put("PUT", mPUT);
    methodMap.put("TRACE", mTRACE);
  }

  void registerMethod(String method) {
    if (method == null || method.length() == 0) {
      return;
    }
    String key = method.toUpperCase();
    if (methodMap.get(key) != null) {
      return;
    }
    int mt = methodMap.size();
    methodMap.put(key, mt);
    mALL |= mt;
  }

  static class Context {
    public boolean methodNotAllowed;
    String[] keys;
    String[] values;

    Map<String, String> toMap() {
      if (keys == null || keys.length == 0) {
        return Collections.emptyMap();
      }
      Map<String, String> hash = new HashMap<>(keys.length);
      for (int i = 0; i < keys.length; i++) {
        hash.put(keys[i], values[i]);
      }
      return hash;
    }

    void key(String key) {
      this.keys = append(this.keys, key);
    }

    void key(String[] keys) {
      if (keys != null) {
        if (this.keys == null) {
          this.keys = keys;
        } else {
          String[] src = keys;
          this.keys = new String[src.length + keys.length];
          System.arraycopy(src, 0, this.keys, 0, src.length);
          System.arraycopy(keys, 0, this.keys, src.length, keys.length);
        }
      }
    }

    void value(String value) {
      this.values = append(this.values, value);
    }
  }

  class Node implements Comparable<Node> {
    // node type: static, regexp, param, catchAll
    int typ;

    // first byte of the prefix
    char label;

    // first byte of the child prefix
    char tail;

    // prefix is the common prefix we ignore
    String prefix;

    // regexp matcher for regexp nodes
    Pattern rex;

    // HTTP handler endpoints on the leaf node
    Map<Integer, Endpoint> endpoints;

    // subroutes on the leaf node
    //Routes subroutes;

    // child nodes should be stored in-order for iteration,
    // in groups of the node type.
    Node[][] children = new Node[ntCatchAll + 1][];

    public Node typ(int typ) {
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

    public Node prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    Node insertRoute(int method, String pattern, Handler handler) {
      Node n = this;
      Node parent;
      String search = pattern;

      while (true) {
        // Handle key exhaustion
        if (search.length() == 0) {
          // Insert or update the node's leaf handler
          n.setEndpoint(method, handler, pattern);
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
          prefix = "";
        }

        // Look for the edge to attach to
        parent = n;
        n = n.getEdge(seg.nodeType, label, seg.tail, prefix);

        // No edge, create one
        if (n == null) {
          Node child = new Node().label(label).tail(seg.tail).prefix(search);
          Node hn = parent.addChild(child, search);
          hn.setEndpoint(method, handler, pattern);
          return hn;
        }

        // Found an edge to match the pattern

        if (n.typ > ntStatic) {
          // We found a param node, trim the param from the search path and continue.
          // This param/wild pattern segment would already be on the tree from a previous
          // call to addChild when creating a new node.
          search = search.substring(seg.endIndex);
          continue;
        }

        // Static nodes fall below here.
        // Determine longest prefix of the search key on match.
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
          child.setEndpoint(method, handler, pattern);
          return child;
        }

        // Create a new edge for the node
        Node subchild = new Node().typ(ntStatic).label(search.charAt(0)).prefix(search);
        Node hn = child.addChild(subchild, search);
        hn.setEndpoint(method, handler, pattern);
        return hn;
      }
    }

    // addChild appends the new `child` node to the tree using the `pattern` as the trie key.
    // For a URL router like chi's, we split the static, param, regexp and wildcard segments
    // into different nodes. In addition, addChild will recursively call itself until every
    // pattern segment is added to the url pattern tree as individual nodes, depending on type.
    Node addChild(Node child, String prefix) {
      Node n = this;
      String search = prefix;

      // handler leaf node added to the tree is the child.
      // this may be overridden later down the flow
      Node hn = child;

      // Parse next segment
      //      segTyp, _, segRexpat, segTail, segStartIdx, segEndIdx := patNextSegment(search)
      Segment seg = patNextSegment(search);
      int segTyp = seg.nodeType;
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
            rex = Pattern.compile(seg.rexPat);
            child.prefix = seg.rexPat;
            child.rex = rex;
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

    void setEndpoint(int method, Handler handler, String pattern) {
      Node n = this;
      // Set the handler for the method type on the node
      if (n.endpoints == null) {
        n.endpoints = new HashMap<>();
      }

      String[] paramKeys = patParamKeys(pattern);

      if ((method & mSTUB) == mSTUB) {
        n.endpoints.put(mSTUB, new Endpoint(handler));
      }
      if ((method & mALL) == mALL) {
        Endpoint h = n.endpoints.get(mALL);
        h.handler = handler;
        h.pattern = pattern;
        h.paramKeys = paramKeys;
        for (Integer m : methodMap.values()) {
          h = n.endpoints.computeIfAbsent(m, k -> new Endpoint(handler));
          h.handler = handler;
          h.pattern = pattern;
          h.paramKeys = paramKeys;
        }
      } else {
        Endpoint h = n.endpoints.computeIfAbsent(method, k -> new Endpoint(handler));
        h.handler = handler;
        h.pattern = pattern;
        h.paramKeys = paramKeys;
      }
    }

    // Recursive edge traversal by checking all nodeTyp groups along the way.
    // It's like searching through a multi-dimensional radix trie.
    Node findRoute(Context rctx, int method, String path) {
      Node n = this;
      Node nn = n;
      String search = path;

      for (int ntyp = 0; ntyp < nn.children.length; ntyp++) {
        Node[] nds = nn.children[ntyp];
        if (nds == null || nds.length == 0) {
          continue;
        }

        Node xn = null;
        String xsearch = search;

        char label = (char) 0;
        if (search.length() > 0) {
          label = search.charAt(0);
        }

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

              if (p <= 0) {
                if (xn.tail == '/') {
                  p = xsearch.length();
                } else {
                  continue;
                }
              }

              if (ntyp == ntRegexp) {
                if (!xn.rex.matcher(xsearch.substring(0, p)).matches()) {
                  continue;
                }
              } else if (xsearch.substring(0, p).indexOf('/') != -1) {
                // avoid a match across path segments
                continue;
              }

              // TODO: rctx.routeParams.Values = append(rctx.routeParams.Values, xsearch[:p])
              rctx.value(xsearch.substring(0, p));
              xsearch = xsearch.substring(p);
              break;
            }
            break;

          default:
            // catch-all nodes
            //TODO: rctx.routeParams.Values = append(rctx.routeParams.Values, search)
            rctx.value(search);
            xn = nds[0];
            xsearch = "";
        }

        if (xn == null) {
          continue;
        }

        // did we find it yet?
        if (xsearch.length() == 0) {
          if (xn.isLeaf()) {
            Endpoint h = xn.endpoints.get(method);
            if (h != null && h.handler != null) {
              // TODO: rctx.routeParams.Keys = append(rctx.routeParams.Keys, h.paramKeys...)
              rctx.key(h.paramKeys);
              return xn;
            }

            // flag that the routing context found a route, but not a corresponding
            // supported method
            rctx.methodNotAllowed = true;
          }
        }

        // recursively find the next node..
        Node fin = xn.findRoute(rctx, method, xsearch);
        if (fin != null) {
          return fin;
        }

        // Did not find final handler, let's remove the param here if it was set
        if (xn.typ > ntStatic) {
          // TODO:
          //          if len(rctx.routeParams.Values) > 0 {
          //            rctx.routeParams.Values = rctx.routeParams.Values[:len(rctx.routeParams.Values) - 1]
          //          }
        }

      }

      return null;
    }

    Node findEdge(int ntyp, char label) {
      Node n = this;
      Node[] nds = n.children[ntyp];
      int num = nds.length;
      int idx = 0;

      switch (ntyp) {
        case ntStatic:
        case ntParam:
        case ntRegexp:
          int j = num - 1;
          for (int i = 0; i < j; i++) {
            idx = i + (j - i) / 2;
            if (label > nds[idx].label) {
              i = idx + 1;
            } else if (label < nds[idx].label) {
              j = idx - 1;
            } else {
              i = num; // breaks cond
            }
          }
          if (nds[idx].label != label) {
            return null;
          }
          return nds[idx];

        default: // catch all
          return nds[idx];
      }
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

    // longestPrefix finds the length of the shared prefix of two strings
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

    String[] patParamKeys(String pattern) {
      String pat = pattern;
      String[] paramKeys = null;
      while (true) {
        //        ptyp, paramKey, _, _, _, e :=patNextSegment(pat)
        Segment s = patNextSegment(pat);
        if (s.nodeType == ntStatic) {
          return paramKeys;
        }
        for (int i = 0; paramKeys != null && i < paramKeys.length; i++) {
          if (paramKeys[i] == s.key) {
            throw new IllegalArgumentException(String
                .format("chi: routing pattern '%s' contains duplicate param key, '%s'", pattern,
                    s.key));
          }
        }
        paramKeys = Chi.append(paramKeys, s.key);
        pat = pat.substring(s.endIndex);
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

    class Segment {
      int nodeType;
      String key = "";
      String rexPat = "";
      char tail;
      int startIndex;
      int endIndex;

      public Segment() {
      }

      public Segment(int nodeType, String key, String regex, char tail, int startIndex,
          int endIndex) {
        this.nodeType = nodeType;
        this.key = key;
        this.rexPat = regex;
        this.tail = tail;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
      }
    }

    // patNextSegment returns the next segment details from a pattern:
    // node type, param key, regexp string, param tail byte, param starting index, param ending index
    Segment patNextSegment(String pattern) {
      int ps = pattern.indexOf("{");
      int ws = pattern.indexOf("*");

      if (ps < 0 && ws < 0) {
        return new Segment(ntStatic, "", "", (char) 0, 0,
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
        int nt = ntParam;

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
          throw new IllegalArgumentException("chi: route param closing delimiter '}' is missing");
        }

        String key = pattern.substring(ps + 1, pe);
        pe++; // set end to next position

        if (pe < pattern.length()) {
          tail = pattern.charAt(pe);
        }

        String rexpat = "";
        int idx = key.indexOf(":");
        if (idx >= 0) {
          nt = ntRegexp;
          rexpat = key.substring(idx + 1);
          key = key.substring(0, idx);
        }

        if (rexpat.length() > 0) {
          if (rexpat.charAt(0) != '^') {
            rexpat = "^" + rexpat;
          }
          if (rexpat.charAt(rexpat.length() - 1) != '$') {
            rexpat = rexpat + "$";
          }
        }

        return new Segment(nt, key, rexpat, tail, ps, pe);
      }

      // Wildcard pattern as finale
      // TODO: should we panic if there is stuff after the * ???
      return new Segment(ntCatchAll, "*", "", (char) 0, ws, pattern.length());
    }
  }

  // endpoints is a mapping of http method constants to handlers
  // for a given route.
  Map<Integer, Endpoint> endpoints;

  class Endpoint {
    // endpoint handler
    Handler handler;

    public Endpoint(Handler handler) {
      this.handler = handler;
    }

    // pattern is the routing pattern for handler nodes
    String pattern;

    // parameter keys recorded on handler nodes
    String[] paramKeys;
  }

  Node root = new Node();

  public void insertRoute(String method, String pattern, Handler handler) {
    // TODO: check int vs Integer in methodMap
    root.insertRoute(methodMap.get(method), pattern, handler);
  }

  public Match findRoute(String method, String path) {
    Context ctx = new Context();
    Integer m = methodMap.get(method);
    Node node = root.findRoute(ctx, m, path);
    if (node != null) {
      Endpoint endpoint = node.endpoints.get(m);
      if (endpoint != null) {
        return new Match(endpoint.handler, ctx.toMap());
      }
    }
    Handler h = path.equals("/favicon.ico") ? Handler.FAVICON : Handler.NOT_FOUND;
    return new Match(h, ctx.toMap());
  }

  public static class Match {
    public final Handler handler;
    public final Map<String, String> params;

    public Match(Handler handler, Map<String, String> params) {
      this.handler = handler;
      this.params = params;
    }
  }

  private static String[] append(String[] src, String child) {
    if (src == null) {
      return new String[]{child};
    }
    String[] result = new String[src.length + 1];
    System.arraycopy(src, 0, result, 0, src.length);
    result[result.length - 1] = child;
    return result;
  }
}
