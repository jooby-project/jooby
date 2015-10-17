/**
 * prim 0.0.7 Copyright (c) 2012-2013, The Dojo Foundation All Rights Reserved.
 * Available via the MIT or new BSD license.
 * see: http://github.com/requirejs/prim for details
 */

/*global setImmediate, process, setTimeout, define, module */
var prim;
(function () {
    'use strict';

    var waitingId, nextTick,
        waiting = [];

    function callWaiting() {
        waitingId = 0;
        var w = waiting;
        waiting = [];
        while (w.length) {
            w.shift()();
        }
    }

    function asyncTick(fn) {
        waiting.push(fn);
        if (!waitingId) {
            waitingId = setTimeout(callWaiting, 0);
        }
    }

    function syncTick(fn) {
        fn();
    }

    function isFunObj(x) {
        var type = typeof x;
        return type === 'object' || type === 'function';
    }

    //Use setImmediate.bind() because attaching it (or setTimeout directly
    //to prim will result in errors. Noticed first on IE10,
    //issue requirejs/alameda#2)
    nextTick = typeof setImmediate === 'function' ? setImmediate.bind() :
        (typeof process !== 'undefined' && process.nextTick ?
            process.nextTick : (typeof setTimeout !== 'undefined' ?
                asyncTick : syncTick));

    function notify(ary, value) {
        prim.nextTick(function () {
            ary.forEach(function (item) {
                item(value);
            });
        });
    }

    function callback(p, ok, yes) {
        if (p.hasOwnProperty('v')) {
            prim.nextTick(function () {
                yes(p.v);
            });
        } else {
            ok.push(yes);
        }
    }

    function errback(p, fail, no) {
        if (p.hasOwnProperty('e')) {
            prim.nextTick(function () {
                no(p.e);
            });
        } else {
            fail.push(no);
        }
    }

    prim = function prim(fn) {
        var promise, f,
            p = {},
            ok = [],
            fail = [];

        function makeFulfill() {
            var f, f2,
                called = false;

            function fulfill(v, prop, listeners) {
                if (called) {
                    return;
                }
                called = true;

                if (promise === v) {
                    called = false;
                    f.reject(new TypeError('value is same promise'));
                    return;
                }

                try {
                    var then = v && v.then;
                    if (isFunObj(v) && typeof then === 'function') {
                        f2 = makeFulfill();
                        then.call(v, f2.resolve, f2.reject);
                    } else {
                        p[prop] = v;
                        notify(listeners, v);
                    }
                } catch (e) {
                    called = false;
                    f.reject(e);
                }
            }

            f = {
                resolve: function (v) {
                    fulfill(v, 'v', ok);
                },
                reject: function(e) {
                    fulfill(e, 'e', fail);
                }
            };
            return f;
        }

        f = makeFulfill();

        promise = {
            then: function (yes, no) {
                var next = prim(function (nextResolve, nextReject) {

                    function finish(fn, nextFn, v) {
                        try {
                            if (fn && typeof fn === 'function') {
                                v = fn(v);
                                nextResolve(v);
                            } else {
                                nextFn(v);
                            }
                        } catch (e) {
                            nextReject(e);
                        }
                    }

                    callback(p, ok, finish.bind(undefined, yes, nextResolve));
                    errback(p, fail, finish.bind(undefined, no, nextReject));

                });
                return next;
            },

            catch: function (no) {
                return promise.then(null, no);
            }
        };

        try {
            fn(f.resolve, f.reject);
        } catch (e) {
            f.reject(e);
        }

        return promise;
    };

    prim.resolve = function (value) {
        return prim(function (yes) {
            yes(value);
        });
    };

    prim.reject = function (err) {
        return prim(function (yes, no) {
            no(err);
        });
    };

    prim.cast = function (x) {
        // A bit of a weak check, want "then" to be a function,
        // but also do not want to trigger a getter if accessing
        // it. Good enough for now.
        if (isFunObj(x) && 'then' in x) {
            return x;
        } else {
            return prim(function (yes, no) {
                if (x instanceof Error) {
                    no(x);
                } else {
                    yes(x);
                }
            });
        }
    };

    prim.all = function (ary) {
        return prim(function (yes, no) {
            var count = 0,
                length = ary.length,
                result = [];

            function resolved(i, v) {
                result[i] = v;
                count += 1;
                if (count === length) {
                    yes(result);
                }
            }

            if (!ary.length) {
                yes([]);
            } else {
                ary.forEach(function (item, i) {
                    prim.cast(item).then(function (v) {
                        resolved(i, v);
                    }, function (err) {
                        no(err);
                    });
                });
            }
        });
    };

    prim.nextTick = nextTick;

    if (typeof define === 'function' && define.amd) {
        define(function () { return prim; });
    } else if (typeof module !== 'undefined' && module.exports) {
        module.exports = prim;
    }
}());
