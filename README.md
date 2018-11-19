# GPS-Tracker

This is a pretty simple GPS tracking server, it uses NanoHTTPD to handle all the http stuff.

It's not secure, it violates best practices in pretty much every way imaginable, and it's perfect (for what it was made for).

For inputting data to the server i used Tasker on android and created apps that were simply

1. Get location
1. HTTP GET to input server with location information and IMEI
1. Wait 1 Minute
1. GOTO 1

*I don't recommend you use this for anything, unless you are going to copy the exact use case (phones with tasker automatically reporting gps locations)*
