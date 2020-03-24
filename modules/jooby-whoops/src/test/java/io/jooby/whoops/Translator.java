package io.jooby.whoops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Translator {
  private Path basedir;

  public Translator(Path basedir) {
    this.basedir = basedir;
  }

  public Path doEnvDetails() throws IOException {
    String filename = "env_details.html";
    Path output = basedir.resolve(filename);
    List<String> lines = new ArrayList<>();
    for (String line : Files.readAllLines(basedir.resolve(filename + ".php"))) {
      line = line
          .replaceAll("<\\?php\\s*/.*/\\s*\\?>", "")
          .replace("<?php foreach ($tables as $label => $data): ?>", "{% for e in env %}")
          .replace("<?php echo $tpl->escape($tpl->slug($label)) ?>", "{{e.key}}")
          .replace("<?php if (!empty($data)): ?>", "{% if e.value.size > 0 %}")
          .replace("<?php echo $tpl->escape($label) ?>", "{{e.key}}")
          .replace("<?php foreach ($data as $k => $value): ?>", "{% for data in e.value %}")
          .replace("<?php echo $tpl->escape($k) ?>", "{{data.key}}")
          .replace("<?php echo $tpl->dump($value) ?>", "{{data.value}}")
          .replace("<?php endforeach ?>", "{%endfor%}")
          .replace("<?php else: ?>", "{%else%}")
          .replace("<?php echo $tpl->escape($label) ?>", "{{e.key}}")
          .replace("<?php endif ?>", "{%endif%}")
          .replace("<?php endforeach ?>", "{%endfor%}")
          // TODO
          .replace("<?php foreach ($handlers as $i => $h): ?>", "{% for h in handlers %}")
          .replace("<?php echo ($h === $handler) ? 'active' : ''?>", "")
          .replace("<?php echo $i ?>. <?php echo $tpl->escape(get_class($h)) ?>", "{{h}}")
          .replace("<?php endforeach ?>", "\"{%endfor%}\"")
      ;

      lines.add(line);
    }
    Files.write(output, lines);
    return output;
  }

  public Path doFrameCode() throws IOException {
    String filename = "frame_code.html";
    Path output = basedir.resolve(filename);
    List<String> lines = new ArrayList<>();
    for (String line : Files.readAllLines(basedir.resolve(filename + ".php"))) {
      line = line
          .replaceAll("<\\?php\\s*/.*/\\s*\\?>", "")
          .replace("<?php echo (!$has_frames ? 'empty' : '') ?>", "{% if frames.size == 0 %} empty{% endif %}")
          .replace("<?php echo ($i == 0 ) ? 'active' : '' ?>\" id=\"frame-code-<?php echo $i ?>", "{% if loop.index == 0 %} active{% endif %}\" id=\"frame-code-{{loop.index}}")
          .replace("<?php $filePath = $frame->getFile(); ?>\n"
              + "          <?php if ($filePath && $editorHref = $handler->getEditorHref($filePath, (int) $line)): ?>\n"
              + "            <a href=\"<?php echo $editorHref ?>\" class=\"editor-link\"<?php echo ($handler->getEditorAjax($filePath, (int) $line) ? ' data-ajax' : '') ?>>\n"
              + "              Open:\n"
              + "              <strong><?php echo $tpl->breakOnDelimiter('/', $tpl->escape($filePath ?: '<#unknown>')) ?></strong>\n"
              + "            </a>\n"
              + "          <?php else: ?>\n"
              + "            <strong><?php echo $tpl->breakOnDelimiter('/', $tpl->escape($filePath ?: '<#unknown>')) ?></strong>\n"
              + "          <?php endif ?>", "{%if frame.open != '' %}\n"
              + "            Open:\n"
              + "            <a href=\"{{frame.open}}\" class=\"editor-link\">\n"
              + "              <strong>{{frame.fileName}}</strong>\n"
              + "            </a>\n"
              + "          {%else%}\n"
              + "            <strong>{{frame.fileName}}</strong>\n"
              + "          {%endif%}")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
          .replace("", "")
      ;

      lines.add(line);
    }
    Files.write(output, lines);
    return output;
  }

  public static void main(String[] args) throws IOException {
    Translator translator = new Translator(
        Paths.get(basedir(), "src", "main", "resources", "whoops", "views"));
    translator.doEnvDetails();
    translator.doFrameCode();
  }

  private static String basedir() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (!basedir.getFileName().toString().equals("jooby-whoops")) {
      // IDE vs Maven
      basedir = basedir.resolve("modules").resolve("jooby-whoops");
    }
    return basedir.toString();
  }
}
