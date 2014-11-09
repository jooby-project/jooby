=begin
  Jekyll tag to include Markdown text from _includes directory preprocessing with Liquid.
  Usage:
    {% markdown <filename> %}
  Dependency:
    - redcarpet
=end
module Jekyll
  class MarkdownTag < Liquid::Tag
    VARIABLE_SYNTAX = /(?<variable>[^{]*\{\{\s*(?<name>[\w\-\.]+)\s*(\|.*)?\}\}[^\s}]*)(?<params>.*)/

    def initialize(tag_name, file, tokens)
      super
      @tag = tag_name
      @file = file.strip
    end

    # Render the variable if required
    def render_variable(context)
      if @file.match(VARIABLE_SYNTAX)
        partial = Liquid::Template.parse(@file)
        partial.render!(context)
      end
    end

    require "redcarpet"
    def render(context)
      file = render_variable(context) || @file

      file = file.sub! ".md", ".toc.md" if @tag_name == "toc"

      tmpl = File.read File.join Dir.pwd, "_includes", file
      site = context.registers[:site]
      tmpl = (Liquid::Template.parse tmpl).render site.site_payload
      md   = Redcarpet::Markdown.new(Redcarpet::Render::HTML)
      html =  md.render(tmpl)
    end
  end
end
Liquid::Template.register_tag('markdown', Jekyll::MarkdownTag)
Liquid::Template.register_tag('toc', Jekyll::MarkdownTag)
