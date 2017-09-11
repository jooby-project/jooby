grammar StatusCode;

text: (statusCodes|.)*?;

statusCodes:
      statusCode=CODE       #htmlCode
    | statusCode=CODE_A     #markdownCode
    | statusCode=CODE_CURLY #jsonCode
    ;

CODE: '<code>' .*? '</code>';

CODE_A: '`' .*? '`';

CODE_CURLY: '{' .*? '}';

ANY: . -> skip;
