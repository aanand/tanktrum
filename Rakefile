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
  OS = 'mac'
when /Linux/i
  OS = 'linux'
else
  OS = 'win32'
end
LIBPATH = "lib/natives-#{OS}"

TARGETS = Dir['src/**.scala'].map{|f| f.sub(/^src/, 'classes').sub(/scala$/, 'class')}

TARGETS.each do |target|
  file target => Dir['src/**.scala'] do
    sh "scalac -deprecation -classpath #{CLASSPATH} src/**.scala -d classes"
  end
end

desc "compile .class files"
task :compile => (['install:deps', 'classes'] + TARGETS)

desc "start the game"
task :run => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} Main"
end

desc "start a server on the default port"
task :run_server => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} ServerMain"
end

desc "build #{GAME_JAR_NAME}.jar"
task :jar => GAME_JAR_FILE

file GAME_JAR_FILE => [:compile] + Dir['media/**'] do
  sh "jar -cf #{GAME_JAR_FILE} -C classes ."
  sh "jar -uf #{GAME_JAR_FILE} media "
end

namespace :install do
  desc "install dependencies"
  task :deps => LIB_JAR_FILES

  desc "remove dependencies and temporary files"
  task :clobber do
    rm_rf 'lib'
    rm_rf 'tmp'
  end
  
  file 'lib/scala-library.jar' => :lib do
    cp File.join(File.dirname(`which scala`), '..', 'lib', 'scala-library.jar'), 'lib/'
  end
  
  file 'lib/slick.jar' => :lib do
    download_file 'lib/slick.jar', 'http://slick.cokeandcode.com/demos/slick.jar'
    system "zip -q -d lib/slick.jar *.SF *.RSA *.DSA"
  end
  
  file 'lib/phys2d.jar' => :lib do
    download_file 'lib/phys2d.jar', 'http://www.cokeandcode.com/phys2d/source/builds/phys2d-060408.jar'
  end

  file 'lib/sbinary.jar' => :lib do
    download_file 'lib/sbinary.jar', 'http://sbinary.googlecode.com/files/sbinary-0.2.1.jar'
  end

  def vorbis_dir 
    Dir.glob("tmp/vorbisspi/*").first
  end

  file 'tmp/vorbisspi' do
    rm_rf 'tmp/vorbisspi'
    download_file "tmp/vorbisspi.zip", 'http://www.javazoom.net/vorbisspi/sources/vorbisspi1.0.3.zip'
    raise "extraction of vorbisspi.zip failed" unless sh "unzip tmp/vorbisspi.zip -d tmp/vorbisspi"
  end

  file 'lib/vorbisspi.jar' => 'tmp/vorbisspi' do
    vorbis_jar = Dir.glob("#{vorbis_dir}/vorbisspi*.jar").first
    cp vorbis_jar, "lib/vorbisspi.jar"
  end

  file 'lib/jogg.jar' => 'tmp/vorbisspi' do
    jogg_jar = Dir.glob("#{vorbis_dir}/lib/jogg*.jar").first
    cp jogg_jar, "lib/jogg.jar"
  end

  file 'lib/jorbis.jar' => 'tmp/vorbisspi' do
    jorbis_jar = Dir.glob("#{vorbis_dir}/lib/jorbis*.jar").first
    cp jorbis_jar, "lib/jorbis.jar"
  end

  file 'lib/tritonus_share.jar' => 'tmp/vorbisspi' do
    tritonus_share_jar = Dir.glob("#{vorbis_dir}/lib/tritonus_share*.jar").first
    cp tritonus_share_jar, "lib/tritonus_share.jar"
  end
  
  file 'lib/lwjgl.jar' => ['tmp', 'lib'] do
    download_file 'lib/lwjgl.jar', 'http://slick.cokeandcode.com/demos/lwjgl.jar'
    system "zip -q -d lib/lwjgl.jar *.SF *.RSA *.DSA"
  end

  %w{linux mac win32}.each do |os|
    file "lib/natives-#{os}.jar" => :lib do
      download_file "lib/natives-#{os}.jar", "http://slick.cokeandcode.com/demos/natives-#{os}.jar"
      system "zip -q -d lib/natives-#{os}.jar *.SF *.RSA *.DSA"
      system "unzip lib/natives-#{os}.jar -d lib/natives-#{os}"
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
  
  desc "build webstart"
  task :webstart => ["dist/webstart"] + WEBSTART_JAR_FILES do
    cp_r "packaging/webstart/.", "dist/webstart"
  end
end

namespace :upload do
  desc "upload #{GAME_JAR_NAME}.jar and webstart files"
  task :webstart => [:game, :files]
  
  desc "upload #{GAME_JAR_NAME}.jar"
  task :game do
    upload "dist/webstart/#{GAME_JAR_NAME}.jar"
  end
  
  desc "upload webstart files - .jnlp, index.html, etc"
  task :files do
    upload(Dir["dist/webstart/*"] - WEBSTART_JAR_FILES)
  end
  
  desc "upload dependencies"
  task :libs do
    upload(WEBSTART_JAR_FILES - ["dist/webstart/#{GAME_JAR_NAME}.jar"])
  end
  
  def upload files
    files = [files].flatten
    sh "scp #{files.join(" ")} deathtank@norgg.org:/var/www/norgg.org/htdocs/deathtank"
  end
end
