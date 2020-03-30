package generator;

import io.jooby.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateHTML {
  private Path basedir;
  private Path output;

  public GenerateHTML(Path basedir, Path output) {
    this.basedir = basedir;
    this.output = output;
  }

  public Path doEnvDetails() throws IOException {
    String filename = "env_details.html";
    Path output = output(filename, basedir);
    StringBuilder lines = new StringBuilder();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php /* List data-table values, i.e: $_SERVER, $_GET, .... */ ?>", "")
          .replace("<?php foreach ($tables as $label => $data): ?>", "{% for scope in env %}")
          .replace("<?php echo $tpl->escape($tpl->slug($label)) ?>", "{{scope.key}}")
          .replace("<?php if (!empty($data)): ?>", "{% if scope is not empty %}")
          .replace("<?php echo $tpl->escape($label) ?>", "{{scope.key}}")
          .replace("<?php foreach ($data as $k => $value): ?>", "{% for data in scope.value %}")
          .replace("<?php echo $tpl->escape($k) ?>", "{{data.key}}")
          .replace("<?php echo $tpl->dump($value) ?>", "{{data.value}}")
          .replace("<?php endforeach ?>", "{% endfor %}")
          .replace("<?php else: ?>", "{% else %}")
          .replace("<?php echo $tpl->escape($label) ?>", "{{scope.key}}")
          .replace("<?php endif ?>", "{% endif %}")
          .replace("<?php endforeach ?>", "{% endfor %}")
          // TODO
          .replace("<?php /* List registered handlers, in order of first to last registered */ ?>", "")
          .replace("<?php foreach ($handlers as $i => $h): ?>", "{% for h in handlers %}")
          .replace("<?php echo ($h === $handler) ? 'active' : ''?>", "")
          .replace("<?php echo $i ?>. <?php echo $tpl->escape(get_class($h)) ?>", "{{h}}")
          .replace("<?php endforeach ?>", "{% endfor %}")
      ;
      if (line.trim().length() > 0) {
        lines.append(line).append(System.getProperty("line.separator"));
      }
    }
    Document doc = Jsoup.parse(lines.toString());
    doc.select("#handlers").remove();
    Document.OutputSettings settings = new Document.OutputSettings();
    settings.prettyPrint(false);
    settings.indentAmount(2);
    settings.outline(true);
    Files.write(output, Arrays.asList(doc.outputSettings(settings).body().children().toString()));
    return output;
  }

  public Path doFrameCode() throws IOException {
    String filename = "frame_code.html";
    Path output = output(filename, basedir);
    StringBuilder lines = new StringBuilder();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php /* Display a code block for all frames in the stack.", "")
          .replace("       * @todo: This should PROBABLY be done on-demand, lest", "")
          .replace("       * we get 200 frames to process. */ ?>", "")
          .replace("<?php echo (!$has_frames ? 'empty' : '') ?>",
              "{% if frames.empty %}empty{% endif %}")
          .replace("foreach ($frames as $i => $frame):", "{% for frame in frames %}")
          .replace("$line = $frame->getLine();", "")
          .replace("<?php echo ($i == 0 ) ? 'active' : '' ?>\" id=\"frame-code-<?php echo $i ?>",
              "{% if loop.index == 0 %}active{% endif %}\" id=\"frame-code-{{loop.index}}")
          .replace("echo ($handler->getEditorAjax($filePath, (int) $line) ? ' data-ajax' : '')", "")
          .replace("<?php $filePath = $frame->getFile(); ?>", "{% if frame.open %}")
          .replace(
              "<?php if ($filePath && $editorHref = $handler->getEditorHref($filePath, (int) $line)): ?>",
              "")
          .replace("<?php else: ?>", "{% else %}")
          .replace(
              "<?php echo $tpl->breakOnDelimiter('/', $tpl->escape($filePath ?: '<#unknown>')) ?>",
              "{{frame.fileName}}")
          .replace("<?php endif ?>", "{% endif %}")
          //                .replace("<?php", "")
          .replace("// Do nothing if there's no line to work off", "")
          .replace("if ($line !== null):", "{% if frame.source != null %}")
          .replace("// the $line is 1-indexed, we nab -1 where needed to account for this", "")
          .replace("$range = $frame->getFileLines($line - 20, 40);", "")
          .replace("// getFileLines can return null if there is no source code", "")
          .replace("if ($range):", "")
          .replace(
              "$range = array_map(function ($line) { return empty($line) ? ' ' : $line;}, $range);",
              "")
          .replace("$start = key($range) + 1;", "")
          .replace("$code  = join(\"\\n\", $range);", "")
          //          .replace("?>", "")
          .replace("<?=$i?>", "{{loop.index}}")
          .replace("<?php echo $start ?>", "{{frame.lineStart}}")
          .replace("<?php echo $tpl->escape($code) ?>", "{{frame.source}}")
          .replace("<?php $frameArgs = $tpl->dumpArgs($frame); ?>", "")
          .replace("<?php if ($frameArgs): ?>", "{% if frame.args != null %}")
          .replace("<?php echo $frameArgs; ?>", "{{frame.args}}")
          .replace("// Append comments for this frame", "")
          .replace("$comments = $frame->getComments();", "")
          .replace("<?php echo empty($comments) ? 'empty' : '' ?>",
              "{% if frame.comments.size == 0 %}empty{% endif %}")
          .replace("<?php foreach ($comments as $commentNo => $comment): ?>",
              "{% for comment in frame.comments %}")
          .replace("<?php extract($comment) ?>", "")
          .replace("<?php echo $i . '-' . $commentNo ?>", "{{loop.index}}")
          .replace("<?php echo $tpl->escape($context) ?>", "{{comment.class.name}}")
          .replace("<?php echo $tpl->escapeButPreserveUris($comment) ?>", "{{comment.message}}")
          .replace("<?php endforeach ?>", "{% endfor %}")
          .replace("<?php", "")
          .replace("?>", "")
      ;
      if (line.trim().length() > 0) {
        lines.append(line).append(System.getProperty("line.separator"));
      }
    }
    Files.write(output, Arrays.asList(lines.toString().replace("{% endif %}\n"
        + "        {% if frame.args != null %}", "{% if frame.args != null %}")));
    return output;
  }

  public Path doFrameList() throws IOException {
    String filename = "frame_list.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php /* List file names & line numbers for all stack frames;", "")
          .replace("         clicking these links/buttons will display the code view", "")
          .replace("         for that particular frame */ ?>", "")
          .replace("<?php foreach ($frames as $i => $frame): ?>", "{% for frame in frames %}")
          .replace(
              "<?php echo ($i == 0 ? 'active' : '') ?> <?php echo ($frame->isApplication() ? 'frame-application' : '') ?>\" id=\"frame-line-<?php echo $i ?>\"",
              "{% if loop.index == 0 %}active{% endif %}{% if frame.source != null %} source{% endif %}\" id=\"frame-line-{{loop.index}}\"")
          .replace("<?php echo (count($frames) - $i - 1) ?>", "{{ loop.length - loop.index - 1 }}")
          .replace(
              "<?php echo $tpl->breakOnDelimiter('\\\\', $tpl->escape($frame->getClass() ?: '')) ?>",
              "{{frame.className}}")
          .replace(
              "<?php echo $tpl->breakOnDelimiter('\\\\', $tpl->escape($frame->getFunction() ?: '')) ?>",
              "{{frame.method}}")
          .replace(
              "<?php echo $frame->getFile() ? $tpl->breakOnDelimiter('/', $tpl->shorten($tpl->escape($frame->getFile()))) : '<#unknown>' ?>",
              "{{frame.location}}")
          .replace("<?php echo (int) $frame->getLine() ?>", "{{frame.line}}")
          .replace("<?php endforeach;", "{% endfor %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doFramesContainer() throws IOException {
    String filename = "frames_container.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace(
              "<?php echo $active_frames_tab == 'application' ? 'frames-container-application' : '' ?>",
              "{{activeFramesTab}}")
          .replace("<?php $tpl->render($frame_list) ?>", "{% include \"frame_list\" %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doFramesDescription() throws IOException {
    String filename = "frames_description.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace(
              "<?php echo $has_frames_tabs ? 'frames-description-application' : '' ?>",
              "{{framesDescriptionTab}}")
          .replace("<?php if ($has_frames_tabs): ?>", "{% if hasFrameTabs %}")
          .replace("<?php echo $active_frames_tab == 'application' ? 'frames-tab-active' : '' ?>",
              "{{activeFramesTab}}")
          .replace("<?php echo $active_frames_tab == 'all' ? 'frames-tab-active' : '' ?>",
              "{{activeFramesTab}}")
          .replace("<?php echo count($frames) ?>", "{{frames.size}}")
          .replace("<?php else: ?>", "{% else %}")
          .replace("<?php endif; ?>", "{% endif %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doHeader() throws IOException {
    String filename = "header.html";
    Path output = output(filename, basedir);
    StringBuilder lines = new StringBuilder();
    for (String line : phpFile(filename)) {
      line = line
          .replace(
              "<?php foreach ($name as $i => $nameSection): ?>", "{% for name in causeName %}")
          .replace("<?php if ($i == count($name) - 1): ?>", "{% if loop.index == causeName.size - 1 %}")
          .replace("<?php echo $tpl->escape($nameSection) ?>", "{{name}}")
          .replace("<?php else: ?>", "{% else %}")
          .replace("<?php echo $tpl->escape($nameSection) . ' \\\\' ?>",
              "{{name}}.")
          .replace("<?php endif ?>", "{% endif %}")
          .replace("<?php endforeach ?>", "{% endfor %}")
          .replace("<?php if ($code): ?>", "{%if code != null %}")
          .replace("(<?php echo $tpl->escape($code) ?>)", ": {{code}}")
          .replace("<?php if (!empty($message)): ?>", "{% if cause.message != null %}")
          .replace("<?php echo $tpl->escape($message) ?>", "<span>{{cause.message}}</span>")
          .replace("<?php if (count($previousMessages)): ?>", "{% if previousMessages is not empty %}")
          .replace("<?php foreach ($previousMessages as $i => $previousMessage): ?>",
              "{% for previousMessage in previousMessages %}")
          .replace("<?php echo $tpl->escape($previousMessage) ?>", "{{previousMessage}}")
          .replace("<?php echo $previousCodes[$i] ?>", "")
          .replace("<?php endforeach; ?>", "{% endfor %}")
          .replace("<?php if (!empty($docref_url)): ?>", "{% if docref_url != null %}")
          .replace("<?php echo $tpl->escape($plain_exception) ?>", "{{stacktrace}}")
          .replace("", "")

      ;
      if (line.trim().length() > 0) {
        lines.append(line).append(System.getProperty("line.separator"));
      }
    }
    String html = lines.toString();
    Document doc = Jsoup.parse(html);
    doc.select(".search-for-help").remove();
    doc.select("#hide-error").remove();
    Document.OutputSettings settings = new Document.OutputSettings();
    settings.prettyPrint(false);
    settings.indentAmount(2);
    settings.outline(true);
    Files.write(output, Arrays.asList(doc.outputSettings(settings).body().children().toString()));
    return output;
  }

  public Path doHeaderOuter() throws IOException {
    String filename = "header_outer.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php $tpl->render($header) ?>", "{% include \"header\" %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doLayout() throws IOException {
    String filename = "layout.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php echo $preface; ?>", "")
          .replace("<?php echo $tpl->escape($page_title) ?>", "{{title}}")
          .replace("<style><?php echo $stylesheet ?></style>",
              "<link rel=\"stylesheet\" type=\"text/css\" href=\"{{stylesheet}}\">")
          .replace("<?php $tpl->render($panel_left_outer) ?>", "{% include\"panel_left_outer\" %}")
          .replace("<?php $tpl->render($panel_details_outer) ?>",
              "{% include \"panel_details_outer\" %}")
          .replace("<script><?php echo $prettify ?></script>",
              "<script src=\"{{prettify}}\"></script>")
          .replace("<script><?php echo $zepto ?></script>", "<script src=\"{{zepto}}\"></script>")
          .replace("<script><?php echo $clipboard ?></script>",
              "<script src=\"{{clipboard}}\"></script>")
          .replace("<script><?php echo $javascript ?></script>",
              "<script src=\"{{javascript}}\"></script>")
          .replace("<?php", "")
          .replace("/**", "")
          .replace("* Layout template file for Whoops's pretty error output.", "")
          .replace("*/", "")
          .replace("?>", "")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doPanelDetails() throws IOException {
    String filename = "panel_details.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php $tpl->render($frame_code) ?>", "{% include \"frame_code\" %}")
          .replace("<?php $tpl->render($env_details) ?>", "{% include \"env_details\" %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doPanelDetailsOuter() throws IOException {
    String filename = "panel_details_outer.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php $tpl->render($panel_details) ?>", "{% include \"panel_details\" %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doPanelLeft() throws IOException {
    String filename = "panel_left.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php", "")
          .replace("$tpl->render($header_outer);", "{% include \"header_outer\" %}")
          .replace("$tpl->render($frames_description);", "{% include \"frames_description\" %}")
          .replace("$tpl->render($frames_container);", "{% include \"frames_container\" %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public Path doPanelLeftOuter() throws IOException {
    String filename = "panel_left_outer.html";
    Path output = output(filename, basedir);
    List<String> lines = new ArrayList<>();
    for (String line : phpFile(filename)) {
      line = line
          .replace("<?php $tpl->render($panel_left) ?>", "{% include \"panel_left\" %}")
      ;
      if (line.trim().length() > 0) {
        lines.add(line);
      }
    }
    Files.write(output, lines);
    return output;
  }

  public void generate() {
    try {
      doEnvDetails();
      doFrameCode();
      doFrameList();
      doFramesContainer();
      doFramesDescription();
      doHeader();
      doHeaderOuter();
      doLayout();
      doPanelDetails();
      doPanelDetailsOuter();
      doPanelLeft();
      doPanelLeftOuter();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public static void main(String[] args) {
    create().generate();
  }

  public static GenerateHTML create() {
    return new GenerateHTML(
        Paths.get(basedir(), "src", "test", "resources", "whoops", "views"),
        Paths.get(basedir(), "src", "main", "resources", "io", "jooby", "whoops", "views"));
  }

  private static String basedir() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (!basedir.getFileName().toString().equals("jooby-whoops")) {
      // IDE vs Maven
      basedir = basedir.resolve("modules").resolve("jooby-whoops");
    }
    return basedir.toString();
  }

  private List<String> phpFile(String filename) throws IOException {
    return Files.readAllLines(basedir.resolve(filename + ".php"));
  }

  private Path output(String filename, Path basedir) {
    return output.resolve(filename);
  }
}
