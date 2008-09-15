require 'socket'
require 'thread'

IRC_VERSION="0.0.2"
MAX_BUFFER=1000

FLOODNUM=4
FLOODTIME=3
WAITTIME=5

NickError=Exception.new

Thread.abort_on_exception = true

class Message
  attr_reader :message, :nick

  def initialize(message, nick)
    @message=message
    @nick=nick
  end
end

class IrcConnection
  attr_reader :queries
  
  def initialize(nick, ircname, host, port)
    @nick=nick

    @socket = TCPSocket.open(host, port)
    @socket << "NICK " + @nick + "\n"
    @socket << "USER " + @nick + " " + host + " blah :" + ircname + "\n"

    @messages = {}
    @queries = Queue.new
    @messages[@nick] = Queue.new
    @outmessages = Queue.new

    @mutex = Mutex.new
    @connection = ConditionVariable.new
    @connected = false

    @sendtimes = []
    FLOODNUM.times{@sendtimes << Time.now}

    #Start a thread to grab server messages and deal with them.
    @messageThread = Thread.new do
      begin
        @socket.each do
          |line|
          tokens = line.split

          case tokens[0] 
          when "PING"
            pong(tokens[1])
            
          when "ERROR"
            @socket.close
            
          else
            case tokens[1]
            when "001"
              @mutex.synchronize do
                @connected = true
                @connection.signal
              end
              
            when "433"
              puts "Connetion error: " + tokens[2..-1].join(" ")
              exit
              
            when "PRIVMSG"
              sender = tokens[0].split("!")[0][1..-1]
              case tokens[2]
                
              when @nick
                case tokens[3][1..5]
                when "\001VERS"
                  notice(sender,"\001VERSION rubirc:" + IRC_VERSION + ":ruby v" + VERSION + "\001")
                when "\001PING"
                  notice(sender,tokens[3][1..-1] << tokens[4..-1].join(" "))
                when "\001TIME"
                  notice(sender,"\001TIME :" + Time.now.inspect + "\001")
                else 
                  tokens[3]=tokens[3][1..-1]
                  @queries << Message.new(tokens[3..-1], sender)
                end
                
              else
                chan=tokens[2]
                tokens[3]=tokens[3][1..-1]
                @messages[chan] = Queue.new if @messages[chan] == nil
                m = Message.new(tokens[3..-1], sender)
                @messages[chan] << m
                @messages[chan].pop if (@messages[chan].length > MAX_BUFFER)
              end
            end
          end
        end
      rescue IOError => e
        puts "Connection error: " + e
        exit(1)
      end
    end
    puts "Connecting..."
    @mutex.synchronize {
      @connection.wait(@mutex) while not @connected
    }
    puts "Connected to " + host

  end

  def pong(str)
    @mutex.synchronize {
      @socket << "PONG" + str + "\n"
    }
  end

  def topic(channel, message)
    @mutex.synchronize {
      floodwait
      @socket << "TOPIC " + channel + " :" + message + "\n"
    }
  end

  def send(channel, message)
    str = "PRIVMSG " + channel + " :" + message + "\n"
    @outmessages << str
    @mutex.synchronize {
      floodwait
      @socket << @outmessages.shift
    }
  end

  def notice(channel, message)
    @mutex.synchronize {
      floodwait
      str = "NOTICE " + channel + " :" + message + "\n"
      @socket << str
    }
  end

  def emote(channel, message)
      send(channel, "\001ACTION " + message + "\001\n")
  end

  def join(channel)
    @mutex.synchronize {
      floodwait
      @socket << "JOIN " + channel + "\n"
    }
  end
  
  def part(channel)
    @mutex.synchronize {
      floodwait
      @socket << "PART " + channel + "\n"
    }
  end

  def quit(message)
    @mutex.synchronize {
      @socket << "QUIT :" + message + "\n"
    }
  end
  
  def getchan(channel)
    @messages[channel] = Queue.new if @messages[channel] == nil
    return @messages[channel]
  end

  def each(channel)
    @messages[channel] = Queue.new if @messages[channel] == nil
    loop {
      yield(@messages[channel].pop)
    }
  end

  def eachQuery
    loop {
      yield(@queries.pop)
    }
  end

  def floodwait
    pastmessage=@sendtimes.shift
    sleep FLOODTIME - (Time.now-pastmessage) if Time.now - pastmessage < FLOODTIME
    @sendtimes << Time.now
  end
  
end

