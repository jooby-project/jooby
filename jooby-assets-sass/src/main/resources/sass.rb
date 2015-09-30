require 'rubygems'
require 'sass/plugin'
require 'sass/engine'
require 'base64'

class ClasspathImport < Sass::Importers::Base

  attr_accessor :root

  def initialize(root, loader)
    @root = _no_leading_dot(File.dirname(root))
    @loader = loader
    @ext = File.extname(root)
    @cache = Hash.new
  end

  # @see Base#find_relative
  def find_relative(name, base, options)
    if (base)
      _find(File.dirname(base), name, options)
    else
      _find(@root, name, options)
    end
  end

  # @see Base#find
  def find(name, options)
    _find(@root, name, options)
  end

  def public_url(name, sourcemap_directory)
    filename = name
    if (!filename.start_with?('/'))
      filename = '/' + filename;
    end
    return filename
  end

  # @see Base#key
  def key(name, options)
    [self.class.name + ":" + File.dirname(File.expand_path(name)), File.basename(name)]
  end

  # @see Base#to_s
  def to_s
    @root
  end

  def hash
    @root.hash
  end

  def eql?(other)
    root.eql?(other.root)
  end

  def _no_leading_dot(path)
    if (path.start_with?('.'))
        return path[1, path.length]
    end
    return path
  end

  def _find(dir, name, options)
    if (dir.length > 0)
      engine = _findOne(dir + '/' + name, options)
      if (engine)
        return engine
      end
    end
    return _findOne(name, options)
  end

  def _readFile(path)
    stream = @loader.getResourceAsStream(path)
    if (stream.nil?)
      return nil
    end

    begin
      return String.from_java_bytes(com::google::common::io::ByteStreams.toByteArray(stream), "UTF-8");
    ensure
      stream.close()
    end
  end

  def _findOne(name, options)
    fullname = _no_leading_dot(name)
    if (!(fullname.end_with?('.scss') || fullname.end_with?('.sass')))
      fullname = fullname + @ext
    end
    fullname = fullname.gsub(/\/+/, '/')

    contents = @cache[fullname]

    if (contents.nil?)
      contents = _readFile(fullname)
      if (contents.nil?)
        return nil
      else
        @cache[fullname] = contents
      end
    end

    ext = _no_leading_dot(File.extname(fullname))

    options[:syntax] = ext.to_sym
    options[:filename] = fullname
    options[:importer] = self

    return Sass::Engine.new(contents, options)
  end
end

def to_symbol value
  begin
    value.to_sym
  rescue NoMethodError
    value
  end
end

# Convert a Java HashMap to a ruby Hash
def convert map
  hsh = Hash.new

  map.each { |key, value| hsh[to_symbol(key)] = to_symbol(value)}

  return hsh
end

def source_map(filename, type)
  source_map = Hash.new
  source_map[:sourcemap_path] = filename + '.map'
  source_map[:css_path] = filename
  source_map[:type] = type
  return source_map
end

def render(source, options, filename, loader, is_map)
  begin
    options = convert(options)
    importer = ClasspathImport.new(filename, loader)
    options[:importer] = importer
    options[:filename] = filename
    options[:cache_location] = options[:cache_location].to_s if options[:cache_location]
    options[:template_location] = options[:template_location].to_s if options[:template_location]
    options[:css_location] = options[:css_location].to_s if options[:css_location]
    options[:cache_location] = options[:cache_location].to_s if options[:cache_location]

    is_inline = options[:sourcemap] == :inline

    engine = Sass::Engine.new(source, options)

    if (options[:sourcemap])
      css, sourcemap = engine.to_tree.render_with_sourcemap

      json = sourcemap.to_json(source_map(filename, options[:sourcemap]))

      if (is_inline)
        css << "\n" if css[-1] != ?\n
        css << "/*# sourceMappingURL=data:application/json;base64,"
        css << Base64.strict_encode64(json)
        css << " */\n"
        return css
      else
        if (is_map)
          return json
        else
          css << "\n" if css[-1] != ?\n
          css << "/*# sourceMappingURL="
          css << Sass::Util.escape_uri(filename + '.map')
          css << " */\n"
          return css
        end
      end
    end
    return engine.render
  rescue Sass::SyntaxError => ex
    problem = org::jooby::assets::AssetProblem.new(ex.sass_filename, ex.sass_line, -1, ex.message)
    return problem
  end
end
