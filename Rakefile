include FileUtils::Verbose

task :default => :compile

directory 'classes'
directory 'tmp'
directory 'lib'

task :compile => 'classes' do
  sh "scalac -classpath lib/scala-library.jar:lib/slick.jar:lib/phys2d.jar:lib/lwjgl.jar src/**.scala -d classes"
end

task :jar => ['lib', 'classes'] do
  sh "jar -cf lib/tank.jar -C classes ."
end

namespace :install do
  desc "install dependencies"
  task :deps => [:scala, :slick, :phys2d, :lwjgl]

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
  
  task :lwjgl => ['tmp', 'lib'] do
    tmp_dir = "tmp/lwjgl"
    
    rm_rf tmp_dir

    download_file "tmp/lwjgl.zip", 'http://kent.dl.sourceforge.net/sourceforge/java-game-lib/lwjgl-2.0rc1.zip'
    raise "extraction of lwjgl.zip failed" unless sh "unzip tmp/lwjgl.zip -d #{tmp_dir}"
    
    lwjgl_dir = Dir.glob("#{tmp_dir}/*").first
    
    cp "#{lwjgl_dir}/jar/lwjgl.jar", "lib"
    cp_r "#{lwjgl_dir}/native", "lib/lwjgl"
  end
end

def download_file path, url
  raise "download failed" unless File.exist?(path) or sh "curl -o #{path} #{url}"
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

    raise "SetFile failed" unless system "/Developer/Tools/SetFile -a B #{app_dir}"
    
    chmod 0755, "#{executable_dir}/JavaApplicationStub"
  end
end
