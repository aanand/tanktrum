include FileUtils::Verbose

task :default => :compile

directory 'classes'
directory 'tmp'
directory 'lib'

def jar_files dir, names
  names.map{ |name| "#{dir}/#{name}.jar" }
end

LIB_JAR_NAMES = %w(scala-library slick jbox2d lwjgl sbinary natives-linux natives-mac natives-win32 natives-win64)
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

SOURCE = Dir['src/**/*.scala']
TARGETS = SOURCE.map{|f| f.sub(/^src/, 'classes').sub(/scala$/, 'class')}

TARGETS.each do |target|
  #This depends on each scala file actually containing a class by the name of
  #the file in the correct package, which may not always be the case.  If this
  #insists on building every time, that might be the problem.
  file target => SOURCE do
    sh "fsc -deprecation -classpath #{CLASSPATH} src/**/*.scala -d classes ##{target}"
  end
end

desc "compile .class files"
task :compile => (['classes'] + TARGETS)

desc "start the game"
task :run => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} shared.Main"
end

desc "start the game from the jar file"
task :run_jar => :jar do
  sh "java -classpath #{GAME_JAR_FILE}:#{CLASSPATH} -Djava.library.path=#{LIBPATH} shared.Main"
end

desc "start a metaserver"
task :run_metaserver => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} metaserver.MetaServerMain"
end

desc "start a server on the default port"
task :run_server => :compile do
  sh "java -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} server.ServerMain"
end

desc "profile the game"
task :profile => :compile do
  sh "java -agentpath:#{ENV['AGENTPATH']} -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} shared.Main"
end

desc "profile the server"
task :profile_server => :compile do
  sh "java -agentpath:#{AGENTPATH} -classpath classes:#{CLASSPATH} -Djava.library.path=#{LIBPATH} server.ServerMain"
end

desc "build #{GAME_JAR_NAME}.jar"
task :jar => GAME_JAR_FILE

file GAME_JAR_FILE => [:compile] + Dir['media/**'] do
  sh "jar -cf #{GAME_JAR_FILE} -C classes ."
  sh "jar -uf #{GAME_JAR_FILE} media "
  sh "jar -uf #{GAME_JAR_FILE} config.properties "
end

directory "dist/webstart"
jarsigner_passphrase = nil

file "dist/www" do
  ln_s "../packaging/www/build", "dist/www"
end

WEBSTART_JAR_FILES.each do |target|
  source = "lib/#{File.basename(target)}"
  
  file target => source do
    cp source, target
    
    require 'highline'
    jarsigner_passphrase ||= HighLine.new.ask("Enter jarsigner passphrase: ") { |q| q.echo = false }
    unless system "jarsigner -keystore tanktrum.ks -storepass '#{jarsigner_passphrase}' #{target} boomtrapezoid"
      rm target
      raise "jarsigner failed"
    end
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
  
  desc "build website"
  task :www => ["deps:www", "dist/www"] do
    Dir.chdir("packaging/www") do
      sh "middleman build"
    end
  end
  
  desc "build webstart"
  task :webstart => ["dist/webstart"] + WEBSTART_JAR_FILES do
    cp_r "packaging/webstart/.", "dist/webstart"
  end

  task :natives do
    %w(linux mac win32 win64).each do |suffix|
      dir_name = "lib/natives-#{suffix}"
      jar_name = "lib/natives-#{suffix}.jar"
      flag = File.exist?(jar_name) ? "u" : "c"
      sh "jar -#{flag}f #{jar_name} -C #{dir_name}/ ."
    end
  end
end

namespace :serve do
  desc "serve website locally"
  task :www => "deps:www" do
    Dir.chdir("packaging/www") do
      sh "middleman"
    end
  end
end

namespace :deps do
  task :www do
    Dir.chdir("packaging/www") do
      sh "bundle check || bundle install"
    end
  end
end

namespace :upload do
  desc "upload website"
  task :www do
    upload Dir["dist/www/*"]
  end
  
  desc "upload #{GAME_JAR_NAME}.jar and webstart files"
  task :webstart => ["build:webstart", :game, :files]
  
  desc "upload #{GAME_JAR_NAME}.jar"
  task :game do
    upload "dist/webstart/#{GAME_JAR_NAME}.jar", "webstart"
  end
  
  desc "upload webstart files - .jnlp, index.html, etc"
  task :files do
    upload(Dir["dist/webstart/*"] - WEBSTART_JAR_FILES, "webstart")
  end
  
  desc "upload dependencies"
  task :libs do
    upload(WEBSTART_JAR_FILES - ["dist/webstart/#{GAME_JAR_NAME}.jar"], "webstart")
  end
  
  def upload(files, path="")
    files = [files].flatten

    full_path = "/var/www/boomtrapezoid.com/htdocs/#{path}"
    full_path += "/" unless full_path =~ /\/$/

    sh "scp -r #{files.join(" ")} boomtrapezoid@norgg.org:#{full_path}"
  end
end
