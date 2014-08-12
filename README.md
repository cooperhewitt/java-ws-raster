# ws-raster

ws-raster is a bare-bones HTTP interface to the Batik SVG to PNG transcoder. That is, you send a (binary) POST containing an SVG file and ws-raster sends back the content rendered as a PNG image. That's it.

No, really

## IMPORTANT

This code was originally written in 2008 and hasn't been touched since (2014, as of this writing). It should still work since, after all, that is the promise of Java but all the usual caveats apply.

## Example

	$> curl -v -H 'X-Width: 2048' -H 'Content-Type: image/svg+xml' \
	     -H 'Expect:' --data-binary '@/Users/asc/Desktop/jones.svg' \
	     http://127.0.0.1:9956/png > ~/Desktop/jones.png 

	* About to connect() to 127.0.0.1 port 9956
	* Trying 127.0.0.1...
	* connected
	* Connected to 127.0.0.1 (127.0.0.1) port 9956

	> POST /png HTTP/1.1
	User-Agent: curl/7.13.1 (powerpc-apple-darwin8.0) libcurl/7.13.1 OpenSSL/0.9.7l zlib/1.2.3
	Host: 127.0.0.1:9956
	Pragma: no-cache
	Accept: */*
	X-Width: 2048
	Content-Type: image/svg+xml
	Content-Length: 222692

	[whir-click, whir-click]

	< HTTP/1.1 200 OK
	< Date: Fri, 29 Feb 2008 16:45:21 GMT
	< Content-length: 192191
	< X-height: 988
	< X-width: 2048

	[whir-click, whir-click]

	* Connection #0 to host 127.0.0.1 left intact
	* Closing connection #0

	$>ls -la ~/Desktop/jones.png
	-rw-r--r--   1 asc  asc  192191 Feb 29 08:45 /Users/asc/Desktop/jones.png

## Options

Besides specifics defined in the SVG document itself you specify additional
options by passing them as HTTP headers : 

* X-Width : the width of the final image; if a height option is not defined it will be adjusted relative to the image's final width.

* X-Height : the height of the final image; if a width option is not defined it will be adjusted relative to the image's final height.

## Errors

Errors are returned with the HTTP status code 500. Specific error codes
and messages are returned both in the message body as XML and in the
'X-ErrorCode' and 'X-ErrorMessage' headers.

## Notes

By default, ws-raster runs on port 9956. You can override this value by specifying your own port number as the first (command-line) argument when you start the server.

Relative includes or pointers (src, href, or otherwise) are no supported.

The maximum size of a file that you may POST to ws-raster is 1024kb.

There is no logging to speak of.

## See also

http://xmlgraphics.apache.org/batik/using/transcoder.html#genImagefromSVGDOM
http://javablog.co.uk/2007/10/27/http-server-api-backport-to-java-5/
http://www.aaronland.info/weblog/2008/02/05/fox#ws-raster/
http://www.aaronland.info/papernet/
