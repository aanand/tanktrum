#!/usr/bin/ruby

require 'rubygems'
require 'simple-rss'
require 'irc'
require 'pp'
require 'open-uri'

class Feed
  def initialize url
    @olditems = []
    @url = url
  end

  def new_items
    new_items_list = []
    rss = nil
    rss = SimpleRSS.parse open(@url)

    if rss.nil?
      raise "Couldn't fetch rss"
      return []
    end

    if @olditems.empty?
      item = rss.items.first
      new_items_list << item
    else
      rss.items.reverse.each do |item|
        unless @olditems.find { |old_item| item.link == old_item.link }
          new_items_list << item
        end
      end
    end

    if rss.items and rss.items.size >= @olditems.size
      @olditems = rss.items
    end
    new_items_list
  end
end

class RssBot
  IRC_USER = 'boombot'
  IRC_CHANS = ['#boomtrapezoid', '#groovecats']
  IRC_SERVER = 'irc.synirc.net'
  IRC_PORT = 6667

  LH_FEED  = Feed.new 'http://boomtrapezoid.lighthouseapp.com/events.atom'
  GH_FEED  = Feed.new 'http://github.com/feeds/aanand/commits/boomtrapezoid/master'

  SERVER_LOG = File.open('../server.log')

  def initialize
    SERVER_LOG.seek SERVER_LOG.stat.size
    @irc = IrcConnection.new(IRC_USER, IRC_USER, IRC_SERVER, IRC_PORT)
    IRC_CHANS.each do |chan|
      @irc.join(chan)
    end
    send("Yo.  Followin' some trapezoids.")
  end

  def process_rss
    LH_FEED.new_items.each{|item| send_lh_item item}
    GH_FEED.new_items.each{|item| send_gh_item item}
    rescue Exception => e
      $stderr.puts("Error processing RSS: " + e)
  end

  def send message
    IRC_CHANS.each do |chan|
      @irc.send chan, message
    end
    puts message
  end

  def check_log
    line = SERVER_LOG.gets
    return unless line

    if line.match(/has joined the game\.$/) ||
       line.match(/has left the game\.$/) ||
       line.match(/timed out\.$/)
      send("Server: " + line)
    end

    rescue Exception => e
      $stderr.puts("Error reading from server log: " + e)
  end


  def send_lh_item item
    author = item.author
    date = item.updated.strftime('%d/%m %H:%M:%S ')
    link = open("http://tinyurl.com/api-create.php?url=#{item.link}"){|l| l.read}
    
    title = "Ticket: " +  item.title.split(": ")[1..-1].join(": ")
    title_words = title.split(" ")
    #Ignore git commits:
    if title_words.length >= 3 && title_words[1] == "Changeset" && title_words[2].length == 42
      return
    end
    if date and author and title and link
      item_str = date + author + " | " + title + ' | ' + link
    end


    send item_str
  end
  
  def send_gh_item item
    author = item.author
    date = item.updated.strftime('%d/%m %H:%M:%S ')
    link = open("http://tinyurl.com/api-create.php?url=#{item.link}"){|l| l.read}
    
    title = "Git commit: " +  item.title
    title_words = title.split(" ")
    
    if date and author and title and link
      item_str = date + author + " | " + title + ' | ' + link
    end

    send item_str
  end
end

if __FILE__ == $0

  rssbot = RssBot.new

  Thread.new do
    loop do
      rssbot.check_log
      sleep 1
    end
  end

  loop do
    rssbot.process_rss
    sleep 300
  end
end
