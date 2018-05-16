package org.jooby.assets;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class RJsTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  @Test
  public void zero() throws Exception {
    assertEquals("define('scripts/zero',[],function () {\n" +
        "  return 'Zero';\n" +
        "});", new Rjs()
        .set(engineFactory)
        .process("/scripts/zero.js", f("zero.js"), ConfigFactory.empty()).trim());
  }

  @Test
  public void absRoot() throws Exception {
    assertEquals("define('scripts/one',[],function () {\n" +
        "  return 1;\n" +
        "});\n" +
        "\n" +
        "define('scripts/abs',['./one'], function (one) {\n" +
        "  return one;\n" +
        "});", new Rjs()
        .set(engineFactory)
        .process("/scripts/abs.js", f("abs.js"), ConfigFactory.empty()).trim());
  }

  @Test
  public void emptyJquery() throws Exception {
    assertEquals("define('scripts/empty',['jquery'], function ($) {\n" +
        "  return $;\n" +
        "});", new Rjs()
        .set(engineFactory)
        .set("paths", ImmutableMap.of("jquery", "empty:"))
        .process("scripts/empty.js", f("empty.js"), ConfigFactory.empty()).trim());
  }

  @Test(expected = AssetException.class)
  public void fileNotFound() throws Exception {
    assertEquals("define('scripts/empty',['jquery'], function ($) {\n" +
        "  return $;\n" +
        "});\n" +
        "", new Rjs()
        .set(engineFactory)
        .process("scripts/empty.js", f("empty.js"), ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void syntaxError() throws Exception {
    assertEquals("", new Rjs()
        .set(engineFactory)
        .process("scripts/syntax.js", f("syntax.js"), ConfigFactory.empty()));
  }

  @Test
  public void zeroWithBaseUrl() throws Exception {
    assertEquals("define('zero',[],function () {\n" +
        "  return 'Zero';\n" +
        "});", new Rjs()
        .set(engineFactory)
        .set("baseUrl", "scripts")
        .set("name", "zero")
        .process("scripts/zero.js", f("zero.js"), ConfigFactory.empty()).trim());
  }

  @Test
  public void depth1() throws Exception {
    assertEquals("define('d2',[],function () {\n" +
        "  return 2;\n" +
        "});\n" +
        "\n" +
        "define('d1',['d2'], function (d2) {\n" +
        "  return d2;\n" +
        "});", new Rjs()
        .set(engineFactory)
        .set("baseUrl", "scripts")
        .set("name", "d1")
        .process("scripts/d1.js", f("d1.js"), (ConfigFactory.empty())).trim());
  }

  @Test
  public void depth1WithPath() throws Exception {
    assertEquals("define('scripts/d2',[],function () {\n" +
        "  return 2;\n" +
        "});\n" +
        "\n" +
        "define('scripts/d1path',['scripts/d2'], function (d2) {\n" +
        "  return d2;\n" +
        "});\n" +
        "", new Rjs()
        .set(engineFactory)
        .process("scripts/d1path.js", f("d1path.js"), (ConfigFactory.empty())));
  }

  @Test
  public void text() throws Exception {
    assertEquals("/**\n"
        + " * @license RequireJS text 2.0.14 Copyright (c) 2010-2014, The Dojo Foundation All Rights Reserved.\n"
        + " * Available via the MIT or new BSD license.\n"
        + " * see: http://github.com/requirejs/text for details\n"
        + " */\n"
        + "/*jslint regexp: true */\n"
        + "/*global require, XMLHttpRequest, ActiveXObject,\n"
        + "  define, window, process, Packages,\n"
        + "  java, location, Components, FileUtils */\n"
        + "\n"
        + "define('text',['module'], function (module) {\n"
        + "    'use strict';\n"
        + "\n"
        + "    var text, fs, Cc, Ci, xpcIsWindows,\n"
        + "        progIds = ['Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'],\n"
        + "        xmlRegExp = /^\\s*<\\?xml(\\s)+version=[\\'\\\"](\\d)*.(\\d)*[\\'\\\"](\\s)*\\?>/im,\n"
        + "        bodyRegExp = /<body[^>]*>\\s*([\\s\\S]+)\\s*<\\/body>/im,\n"
        + "        hasLocation = typeof location !== 'undefined' && location.href,\n"
        + "        defaultProtocol = hasLocation && location.protocol && location.protocol.replace(/\\:/, ''),\n"
        + "        defaultHostName = hasLocation && location.hostname,\n"
        + "        defaultPort = hasLocation && (location.port || undefined),\n"
        + "        buildMap = {},\n"
        + "        masterConfig = (module.config && module.config()) || {};\n"
        + "\n"
        + "    text = {\n"
        + "        version: '2.0.14',\n"
        + "\n"
        + "        strip: function (content) {\n"
        + "            //Strips <?xml ...?> declarations so that external SVG and XML\n"
        + "            //documents can be added to a document without worry. Also, if the string\n"
        + "            //is an HTML document, only the part inside the body tag is returned.\n"
        + "            if (content) {\n"
        + "                content = content.replace(xmlRegExp, \"\");\n"
        + "                var matches = content.match(bodyRegExp);\n"
        + "                if (matches) {\n"
        + "                    content = matches[1];\n"
        + "                }\n"
        + "            } else {\n"
        + "                content = \"\";\n"
        + "            }\n"
        + "            return content;\n"
        + "        },\n"
        + "\n"
        + "        jsEscape: function (content) {\n"
        + "            return content.replace(/(['\\\\])/g, '\\\\$1')\n"
        + "                .replace(/[\\f]/g, \"\\\\f\")\n"
        + "                .replace(/[\\b]/g, \"\\\\b\")\n"
        + "                .replace(/[\\n]/g, \"\\\\n\")\n"
        + "                .replace(/[\\t]/g, \"\\\\t\")\n"
        + "                .replace(/[\\r]/g, \"\\\\r\")\n"
        + "                .replace(/[\\u2028]/g, \"\\\\u2028\")\n"
        + "                .replace(/[\\u2029]/g, \"\\\\u2029\");\n"
        + "        },\n"
        + "\n"
        + "        createXhr: masterConfig.createXhr || function () {\n"
        + "            //Would love to dump the ActiveX crap in here. Need IE 6 to die first.\n"
        + "            var xhr, i, progId;\n"
        + "            if (typeof XMLHttpRequest !== \"undefined\") {\n"
        + "                return new XMLHttpRequest();\n"
        + "            } else if (typeof ActiveXObject !== \"undefined\") {\n"
        + "                for (i = 0; i < 3; i += 1) {\n"
        + "                    progId = progIds[i];\n"
        + "                    try {\n"
        + "                        xhr = new ActiveXObject(progId);\n"
        + "                    } catch (e) {}\n"
        + "\n"
        + "                    if (xhr) {\n"
        + "                        progIds = [progId];  // so faster next time\n"
        + "                        break;\n"
        + "                    }\n"
        + "                }\n"
        + "            }\n"
        + "\n"
        + "            return xhr;\n"
        + "        },\n"
        + "\n"
        + "        /**\n"
        + "         * Parses a resource name into its component parts. Resource names\n"
        + "         * look like: module/name.ext!strip, where the !strip part is\n"
        + "         * optional.\n"
        + "         * @param {String} name the resource name\n"
        + "         * @returns {Object} with properties \"moduleName\", \"ext\" and \"strip\"\n"
        + "         * where strip is a boolean.\n"
        + "         */\n"
        + "        parseName: function (name) {\n"
        + "            var modName, ext, temp,\n"
        + "                strip = false,\n"
        + "                index = name.lastIndexOf(\".\"),\n"
        + "                isRelative = name.indexOf('./') === 0 ||\n"
        + "                             name.indexOf('../') === 0;\n"
        + "\n"
        + "            if (index !== -1 && (!isRelative || index > 1)) {\n"
        + "                modName = name.substring(0, index);\n"
        + "                ext = name.substring(index + 1);\n"
        + "            } else {\n"
        + "                modName = name;\n"
        + "            }\n"
        + "\n"
        + "            temp = ext || modName;\n"
        + "            index = temp.indexOf(\"!\");\n"
        + "            if (index !== -1) {\n"
        + "                //Pull off the strip arg.\n"
        + "                strip = temp.substring(index + 1) === \"strip\";\n"
        + "                temp = temp.substring(0, index);\n"
        + "                if (ext) {\n"
        + "                    ext = temp;\n"
        + "                } else {\n"
        + "                    modName = temp;\n"
        + "                }\n"
        + "            }\n"
        + "\n"
        + "            return {\n"
        + "                moduleName: modName,\n"
        + "                ext: ext,\n"
        + "                strip: strip\n"
        + "            };\n"
        + "        },\n"
        + "\n"
        + "        xdRegExp: /^((\\w+)\\:)?\\/\\/([^\\/\\\\]+)/,\n"
        + "\n"
        + "        /**\n"
        + "         * Is an URL on another domain. Only works for browser use, returns\n"
        + "         * false in non-browser environments. Only used to know if an\n"
        + "         * optimized .js version of a text resource should be loaded\n"
        + "         * instead.\n"
        + "         * @param {String} url\n"
        + "         * @returns Boolean\n"
        + "         */\n"
        + "        useXhr: function (url, protocol, hostname, port) {\n"
        + "            var uProtocol, uHostName, uPort,\n"
        + "                match = text.xdRegExp.exec(url);\n"
        + "            if (!match) {\n"
        + "                return true;\n"
        + "            }\n"
        + "            uProtocol = match[2];\n"
        + "            uHostName = match[3];\n"
        + "\n"
        + "            uHostName = uHostName.split(':');\n"
        + "            uPort = uHostName[1];\n"
        + "            uHostName = uHostName[0];\n"
        + "\n"
        + "            return (!uProtocol || uProtocol === protocol) &&\n"
        + "                   (!uHostName || uHostName.toLowerCase() === hostname.toLowerCase()) &&\n"
        + "                   ((!uPort && !uHostName) || uPort === port);\n"
        + "        },\n"
        + "\n"
        + "        finishLoad: function (name, strip, content, onLoad) {\n"
        + "            content = strip ? text.strip(content) : content;\n"
        + "            if (masterConfig.isBuild) {\n"
        + "                buildMap[name] = content;\n"
        + "            }\n"
        + "            onLoad(content);\n"
        + "        },\n"
        + "\n"
        + "        load: function (name, req, onLoad, config) {\n"
        + "            //Name has format: some.module.filext!strip\n"
        + "            //The strip part is optional.\n"
        + "            //if strip is present, then that means only get the string contents\n"
        + "            //inside a body tag in an HTML string. For XML/SVG content it means\n"
        + "            //removing the <?xml ...?> declarations so the content can be inserted\n"
        + "            //into the current doc without problems.\n"
        + "\n"
        + "            // Do not bother with the work if a build and text will\n"
        + "            // not be inlined.\n"
        + "            if (config && config.isBuild && !config.inlineText) {\n"
        + "                onLoad();\n"
        + "                return;\n"
        + "            }\n"
        + "\n"
        + "            masterConfig.isBuild = config && config.isBuild;\n"
        + "\n"
        + "            var parsed = text.parseName(name),\n"
        + "                nonStripName = parsed.moduleName +\n"
        + "                    (parsed.ext ? '.' + parsed.ext : ''),\n"
        + "                url = req.toUrl(nonStripName),\n"
        + "                useXhr = (masterConfig.useXhr) ||\n"
        + "                         text.useXhr;\n"
        + "\n"
        + "            // Do not load if it is an empty: url\n"
        + "            if (url.indexOf('empty:') === 0) {\n"
        + "                onLoad();\n"
        + "                return;\n"
        + "            }\n"
        + "\n"
        + "            //Load the text. Use XHR if possible and in a browser.\n"
        + "            if (!hasLocation || useXhr(url, defaultProtocol, defaultHostName, defaultPort)) {\n"
        + "                text.get(url, function (content) {\n"
        + "                    text.finishLoad(name, parsed.strip, content, onLoad);\n"
        + "                }, function (err) {\n"
        + "                    if (onLoad.error) {\n"
        + "                        onLoad.error(err);\n"
        + "                    }\n"
        + "                });\n"
        + "            } else {\n"
        + "                //Need to fetch the resource across domains. Assume\n"
        + "                //the resource has been optimized into a JS module. Fetch\n"
        + "                //by the module name + extension, but do not include the\n"
        + "                //!strip part to avoid file system issues.\n"
        + "                req([nonStripName], function (content) {\n"
        + "                    text.finishLoad(parsed.moduleName + '.' + parsed.ext,\n"
        + "                                    parsed.strip, content, onLoad);\n"
        + "                });\n"
        + "            }\n"
        + "        },\n"
        + "\n"
        + "        write: function (pluginName, moduleName, write, config) {\n"
        + "            if (buildMap.hasOwnProperty(moduleName)) {\n"
        + "                var content = text.jsEscape(buildMap[moduleName]);\n"
        + "                write.asModule(pluginName + \"!\" + moduleName,\n"
        + "                               \"define(function () { return '\" +\n"
        + "                                   content +\n"
        + "                               \"';});\\n\");\n"
        + "            }\n"
        + "        },\n"
        + "\n"
        + "        writeFile: function (pluginName, moduleName, req, write, config) {\n"
        + "            var parsed = text.parseName(moduleName),\n"
        + "                extPart = parsed.ext ? '.' + parsed.ext : '',\n"
        + "                nonStripName = parsed.moduleName + extPart,\n"
        + "                //Use a '.js' file name so that it indicates it is a\n"
        + "                //script that can be loaded across domains.\n"
        + "                fileName = req.toUrl(parsed.moduleName + extPart) + '.js';\n"
        + "\n"
        + "            //Leverage own load() method to load plugin value, but only\n"
        + "            //write out values that do not have the strip argument,\n"
        + "            //to avoid any potential issues with ! in file names.\n"
        + "            text.load(nonStripName, req, function (value) {\n"
        + "                //Use own write() method to construct full module value.\n"
        + "                //But need to create shell that translates writeFile's\n"
        + "                //write() to the right interface.\n"
        + "                var textWrite = function (contents) {\n"
        + "                    return write(fileName, contents);\n"
        + "                };\n"
        + "                textWrite.asModule = function (moduleName, contents) {\n"
        + "                    return write.asModule(moduleName, fileName, contents);\n"
        + "                };\n"
        + "\n"
        + "                text.write(pluginName, nonStripName, textWrite, config);\n"
        + "            }, config);\n"
        + "        }\n"
        + "    };\n"
        + "\n"
        + "    if (masterConfig.env === 'node' || (!masterConfig.env &&\n"
        + "            typeof process !== \"undefined\" &&\n"
        + "            process.versions &&\n"
        + "            !!process.versions.node &&\n"
        + "            !process.versions['node-webkit'] &&\n"
        + "            !process.versions['atom-shell'])) {\n"
        + "        //Using special require.nodeRequire, something added by r.js.\n"
        + "        fs = require.nodeRequire('fs');\n"
        + "\n"
        + "        text.get = function (url, callback, errback) {\n"
        + "            try {\n"
        + "                var file = fs.readFileSync(url, 'utf8');\n"
        + "                //Remove BOM (Byte Mark Order) from utf8 files if it is there.\n"
        + "                if (file[0] === '\\uFEFF') {\n"
        + "                    file = file.substring(1);\n"
        + "                }\n"
        + "                callback(file);\n"
        + "            } catch (e) {\n"
        + "                if (errback) {\n"
        + "                    errback(e);\n"
        + "                }\n"
        + "            }\n"
        + "        };\n"
        + "    } else if (masterConfig.env === 'xhr' || (!masterConfig.env &&\n"
        + "            text.createXhr())) {\n"
        + "        text.get = function (url, callback, errback, headers) {\n"
        + "            var xhr = text.createXhr(), header;\n"
        + "            xhr.open('GET', url, true);\n"
        + "\n"
        + "            //Allow plugins direct access to xhr headers\n"
        + "            if (headers) {\n"
        + "                for (header in headers) {\n"
        + "                    if (headers.hasOwnProperty(header)) {\n"
        + "                        xhr.setRequestHeader(header.toLowerCase(), headers[header]);\n"
        + "                    }\n"
        + "                }\n"
        + "            }\n"
        + "\n"
        + "            //Allow overrides specified in config\n"
        + "            if (masterConfig.onXhr) {\n"
        + "                masterConfig.onXhr(xhr, url);\n"
        + "            }\n"
        + "\n"
        + "            xhr.onreadystatechange = function (evt) {\n"
        + "                var status, err;\n"
        + "                //Do not explicitly handle errors, those should be\n"
        + "                //visible via console output in the browser.\n"
        + "                if (xhr.readyState === 4) {\n"
        + "                    status = xhr.status || 0;\n"
        + "                    if (status > 399 && status < 600) {\n"
        + "                        //An http 4xx or 5xx error. Signal an error.\n"
        + "                        err = new Error(url + ' HTTP status: ' + status);\n"
        + "                        err.xhr = xhr;\n"
        + "                        if (errback) {\n"
        + "                            errback(err);\n"
        + "                        }\n"
        + "                    } else {\n"
        + "                        callback(xhr.responseText);\n"
        + "                    }\n"
        + "\n"
        + "                    if (masterConfig.onXhrComplete) {\n"
        + "                        masterConfig.onXhrComplete(xhr, url);\n"
        + "                    }\n"
        + "                }\n"
        + "            };\n"
        + "            xhr.send(null);\n"
        + "        };\n"
        + "    } else if (masterConfig.env === 'rhino' || (!masterConfig.env &&\n"
        + "            typeof Packages !== 'undefined' && typeof java !== 'undefined')) {\n"
        + "        //Why Java, why is this so awkward?\n"
        + "        text.get = function (url, callback) {\n"
        + "            var stringBuffer, line,\n"
        + "                encoding = \"utf-8\",\n"
        + "                file = new java.io.File(url),\n"
        + "                lineSeparator = java.lang.System.getProperty(\"line.separator\"),\n"
        + "                input = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), encoding)),\n"
        + "                content = '';\n"
        + "            try {\n"
        + "                stringBuffer = new java.lang.StringBuffer();\n"
        + "                line = input.readLine();\n"
        + "\n"
        + "                // Byte Order Mark (BOM) - The Unicode Standard, version 3.0, page 324\n"
        + "                // http://www.unicode.org/faq/utf_bom.html\n"
        + "\n"
        + "                // Note that when we use utf-8, the BOM should appear as \"EF BB BF\", but it doesn't due to this bug in the JDK:\n"
        + "                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058\n"
        + "                if (line && line.length() && line.charAt(0) === 0xfeff) {\n"
        + "                    // Eat the BOM, since we've already found the encoding on this file,\n"
        + "                    // and we plan to concatenating this buffer with others; the BOM should\n"
        + "                    // only appear at the top of a file.\n"
        + "                    line = line.substring(1);\n"
        + "                }\n"
        + "\n"
        + "                if (line !== null) {\n"
        + "                    stringBuffer.append(line);\n"
        + "                }\n"
        + "\n"
        + "                while ((line = input.readLine()) !== null) {\n"
        + "                    stringBuffer.append(lineSeparator);\n"
        + "                    stringBuffer.append(line);\n"
        + "                }\n"
        + "                //Make sure we return a JavaScript string and not a Java string.\n"
        + "                content = String(stringBuffer.toString()); //String\n"
        + "            } finally {\n"
        + "                input.close();\n"
        + "            }\n"
        + "            callback(content);\n"
        + "        };\n"
        + "    } else if (masterConfig.env === 'xpconnect' || (!masterConfig.env &&\n"
        + "            typeof Components !== 'undefined' && Components.classes &&\n"
        + "            Components.interfaces)) {\n"
        + "        //Avert your gaze!\n"
        + "        Cc = Components.classes;\n"
        + "        Ci = Components.interfaces;\n"
        + "        Components.utils['import']('resource://gre/modules/FileUtils.jsm');\n"
        + "        xpcIsWindows = ('@mozilla.org/windows-registry-key;1' in Cc);\n"
        + "\n"
        + "        text.get = function (url, callback) {\n"
        + "            var inStream, convertStream, fileObj,\n"
        + "                readData = {};\n"
        + "\n"
        + "            if (xpcIsWindows) {\n"
        + "                url = url.replace(/\\//g, '\\\\');\n"
        + "            }\n"
        + "\n"
        + "            fileObj = new FileUtils.File(url);\n"
        + "\n"
        + "            //XPCOM, you so crazy\n"
        + "            try {\n"
        + "                inStream = Cc['@mozilla.org/network/file-input-stream;1']\n"
        + "                           .createInstance(Ci.nsIFileInputStream);\n"
        + "                inStream.init(fileObj, 1, 0, false);\n"
        + "\n"
        + "                convertStream = Cc['@mozilla.org/intl/converter-input-stream;1']\n"
        + "                                .createInstance(Ci.nsIConverterInputStream);\n"
        + "                convertStream.init(inStream, \"utf-8\", inStream.available(),\n"
        + "                Ci.nsIConverterInputStream.DEFAULT_REPLACEMENT_CHARACTER);\n"
        + "\n"
        + "                convertStream.readString(inStream.available(), readData);\n"
        + "                convertStream.close();\n"
        + "                inStream.close();\n"
        + "                callback(readData.value);\n"
        + "            } catch (e) {\n"
        + "                throw new Error((fileObj && fileObj.path || '') + ': ' + e);\n"
        + "            }\n"
        + "        };\n"
        + "    }\n"
        + "    return text;\n"
        + "});\n"
        + "\n"
        + "\n"
        + "define('text!partial.html',[],function () { return '<div>r.js</div>\\n';});\n"
        + "\n"
        + "define('main',['text!partial.html'], function (text) {\n"
        + "  return text;\n"
        + "});", new Rjs()
        .set(engineFactory)
        .set("baseUrl", "scripts")
        .set("name", "main")
        .process("scripts/main.js", f("main.js"), (ConfigFactory.empty())).trim());
  }

  private String f(final String path) throws IOException {
    return Files.readAllLines(Paths.get("src", "test", "resources", "scripts", path))
        .stream()
        .collect(Collectors.joining("\n"));
  }

}
