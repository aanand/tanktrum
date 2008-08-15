include FileUtils::Verbose

task :default => :compile

directory 'classes'
directory 'tmp'
directory 'lib'

def jar_files dir, names
  names.map{ |name| "#{dir}/#{name}.jar" }
end

LIB_JAR_NAMES = %w(scala-library slick phys2d lwjgl sbinary jogg vorbisspi jorbis tritonus_share natives-linux natives-mac natives-win32)
LIB_JAR_FILES = jar_files('lib', LIB_JAR_NAMES)

GAME_JAR_NAME = 'tank'
GAME_JAR_FILE = "lib/#{GAME_JAR_NAME}.jar"

WEBSTART_JAR_FILES = jar_files('dist/webstart', LIB_JAR_NAMES + [GAME_JAR_NAME])

CLASSPATH = LIB_JAR_FILES.join(":")

case `uname`
when /Darwin/i
  SUBDIR = 'macosx'
when /Linux/i
  SUBDIR = 'linux'
else
  SUBDIR = 'win32'
end
LIBPATH = "lib/lwjgl/#{SUBDIR}"

TARGETS = Dir['src/**.scala'].map{|f| f.sub(/^src/, 'classes').sub(/scala$/, 'class')}

TARGETS.each do |target|
  file target => Dir['src/**.scala'] do
    sh "scalac -deprecation -classpath #{CLASSPATH} src/**.scala -d classes"
  end
end

task :compile => TARGETS

task :run => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} Main"
end

task :run_server => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} ServerMain"
end

task :jar => GAME_JAR_FILE

file GAME_JAR_FILE => [:compile] + Dir['media/**'] do
  sh "jar -cf #{GAME_JAR_FILE} -C classes ."
  sh "jar -uf #{GAME_JAR_FILE} media "
end

namespace :install do
  desc "install dependencies"
  task :deps => [:scala, :slick, :phys2d, :lwjgl, :sbinary, :vorbisspi, :native_lwjgl_libs]

  task :clobber do
    rm_rf 'lib'
    rm_rf 'tmp'
  end
  
  task :scala => :lib do
    cp File.join(File.dirname(`which scala`), '..', 'lib', 'scala-library.jar'), 'lib/'
  end
  
  task :slick do
    download_file 'lib/slick.jar', 'http://slick.cokeandcode.com/downloads/slick.jar'
  end
  
  task :phys2d do
    download_file 'lib/phys2d.jar', 'http://www.cokeandcode.com/phys2d/source/builds/phys2d-060408.jar'
  end

  task :sbinary do
    download_file 'lib/sbinary.jar', 'http://sbinary.googlecode.com/files/sbinary-0.2.1.jar'
  end

  task :vorbisspi do
    tmp_dir = "tmp/vorbisspi"
    rm_rf tmp_dir
    
    download_file "tmp/vorbisspi.zip", 'http://www.javazoom.net/vorbisspi/sources/vorbisspi1.0.3.zip'
    raise "extraction of vorbisspi.zip failed" unless sh "unzip tmp/vorbisspi.zip -d #{tmp_dir}"

    vorbis_dir = Dir.glob("#{tmp_dir}/*").first

    vorbis_jar = Dir.glob("#{vorbis_dir}/vorbisspi*.jar").first
    cp vorbis_jar, "lib/vorbisspi.jar"

    jogg_jar = Dir.glob("#{vorbis_dir}/lib/jogg*.jar").first
    cp jogg_jar, "lib/jogg.jar"

    jorbis_jar = Dir.glob("#{vorbis_dir}/lib/jorbis*.jar").first
    cp jorbis_jar, "lib/jorbis.jar"

    tritonus_share_jar = Dir.glob("#{vorbis_dir}/lib/tritonus_share*.jar").first
    cp tritonus_share_jar, "lib/tritonus_share.jar"
  end
  
  task :lwjgl => ['tmp', 'lib'] do
    tmp_dir = "tmp/lwjgl"
    
    rm_rf tmp_dir

    download_file "tmp/lwjgl.zip", 'http://kent.dl.sourceforge.net/sourceforge/java-game-lib/lwjgl-2.0rc1.zip'
    raise "extraction of lwjgl.zip failed" unless sh "unzip tmp/lwjgl.zip -d #{tmp_dir}"
    
    lwjgl_dir = Dir.glob("#{tmp_dir}/*").first
    
    cp "#{lwjgl_dir}/jar/lwjgl.jar", "lib"
    cp_r "#{lwjgl_dir}/native", "lib/lwjgl"
  end

  task :native_lwjgl_libs => ['tmp', 'lib'] do
    %w{linux mac win32}.each do |os|
      download_file "lib/natives-#{os}.jar", "http://slick.cokeandcode.com/demos/natives-#{os}.jar"
      system "zip -q -d lib/natives-#{os}.jar *.SF *.RSA *.DSA"
    end
  end
end

def download_file path, url
  raise "download failed" unless File.exist?(path) or sh "curl -o #{path} #{url}"
end

directory "dist/webstart"
jarsigner_passphrase = nil

WEBSTART_JAR_FILES.each do |target|
  source = "lib/#{File.basename(target)}"
  
  file target => source do
    cp source, target
    
    require 'highline'
    jarsigner_passphrase ||= HighLine.new.ask("Enter jarsigner passphrase: ") { |q| q.echo = false }
    
    raise "jarsigner failed" unless system "jarsigner -keystore deathtank.ks -storepass '#{jarsigner_passphrase}' #{target} mykey"
  end
end

namespace :build do
  desc "build Mac OS X app"
  task :mac do
    app_dir = "dist/mac/Tank.app"
    contents_dir = "#{app_dir}/Contents"
    executable_dir = "#{contents_dir}/MacOS"
    resources_dir = "#{contents_dir}/Resources"
    java_dir = "#{resources_dir}/Java"

    app_stub_path = "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub"

    mkdir_p [app_dir, contents_dir, executable_dir, resources_dir, java_dir]
  
    cp app_stub_path, executable_dir
    cp_r "packaging/mac/.", contents_dir

    cp_r "lib", java_dir

    sh "/Developer/Tools/SetFile -a B #{app_dir}"
    
    chmod 0755, "#{executable_dir}/JavaApplicationStub"
  end
  
  task :webstart => ["dist/webstart"] + WEBSTART_JAR_FILES do
    cp_r "packaging/webstart/.", "dist/webstart"
  end
end

namespace :upload do
  task :webstart => [:game, :files]
  
  task :game do
    upload "dist/webstart/#{GAME_JAR_NAME}.jar"
  end
  
  task :files do
    upload(Dir["dist/webstart/*"] - WEBSTART_JAR_FILES)
  end
  
  task :libs do
    upload(WEBSTART_JAR_FILES - ["dist/webstart/#{GAME_JAR_NAME}.jar"])
  end
  
  def upload files
    files = [files].flatten
    
    sh "scp #{files.join(" ")} deathtank@norgg.org:/var/www/norgg.org/htdocs/deathtank"
  end
end
