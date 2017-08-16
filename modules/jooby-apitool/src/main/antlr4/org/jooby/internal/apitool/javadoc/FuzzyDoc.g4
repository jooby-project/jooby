grammar FuzzyDoc;

source: .*? (routes .*?)+;

routes: route | use | script | clazz | mvcRoute;

use:
     doc=DOC 'use' '(' pattern=STRING ')'
   | doc=DOC 'use' '(' method=STRING comma+=',' pattern=STRING comma+=','
   | doc=DOC 'use' '(' pattern=STRING comma+=',';

route: doc=DOC 'route' '(' pattern=STRING ')' routeBody;

routeBody: '{' (routeBody | script | .)*? '}';

clazz: doc=DOC annotations+=annotation+ (isClass='class'|.)*? '{' classBody  '}';

classBody: (mvcRoute | .)*?;

script:
        doc=DOC dot='.'?     method=METHOD '{'
      | doc=DOC dot='.'? method=METHOD '(' pattern=STRING?;


mvcRoute: doc=DOC annotations+=annotation+;

annotation:
    name=ANNOTATION '(' 'value' '=' value=STRING ')'
  | name=ANNOTATION '(' value=STRING ')'
  | name=ANNOTATION '(' ')'
  | name=ANNOTATION;

DOC: '/**' .*? '*/';

STRING : '"' ( '\\"' | . )*? '"';

ANNOTATION: '@' [a-zA-Z]+;

CLASS: 'class';

METHOD: 'get' | 'post' | 'put' | 'patch' | 'delete' | 'connect' | 'head' | 'options' | 'trace' | 'all';

WS : [ \r\t\n]+ -> skip;

ANY: . -> skip;

