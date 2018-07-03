# tcping
Ping like utility for testing RTT.
Compile with mvn install, or mvn install assembly:single if you want a single jar with all dependencies.

Requires Java 1.8 or newer.
```
Usage: <main class> [options] catcher hostname or address, defaults to 
      127.0.0.1 
  Options:
    --help
      Show this menu
    -bind
      catcher bind address, defaults to 127.0.0.1
      Default: 127.0.0.1
    -c
      catcher mode
      Default: false
    -mps
      messages per second
      Default: 30
    -p
      pitcher mode
      Default: false
    -port
      well known and ephemeral port numbers are not allowed, default 44444
      Default: 44444
    -size
      message size, 50 to 3000 bytes
      Default: 300
```
