from http.server import SimpleHTTPRequestHandler
from socketserver import ForkingTCPServer
import base64
import requests
import os
from urllib import parse

port = 8081
storagedir = 'storage/'
chunksize = 8192


class Proxy(SimpleHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def do_GET(self):
        x = str(base64.encodebytes(bytes(self.path, encoding='UTF8')), encoding='UTF8')
        for root, dirs, files in os.walk('storage'):
            if x + '.head' not in files:
                response = requests.get(parse.urljoin('http://packages.qad.com/', self.path), stream=True)
                fw = open(storagedir + x + '.head', 'w')
                fw.write(str(response.headers))
                fw.close()
                fw = open(storagedir + x + '.content', 'wb')
                fw.write(response.raw.read())
                fw.close()
            fhead = open(storagedir + x + '.head', 'r')
            fcontent = open(storagedir + x + '.content', 'rb')

        self.send_response(200)
        headerstr = fhead.read()
        headermap = eval(headerstr)
        for key, value in headermap.items():
            if key != 'Date':
                self.send_header(key, value)
        self.end_headers()
        while True:
            chunk = fcontent.read(chunksize)
            if chunk:
                self.wfile.write(hex(len(chunk))[2:].encode('utf-8'))
                self.wfile.write('\r\n'.encode('utf-8'))
                self.wfile.write(chunk)
                self.wfile.write('\r\n'.encode('utf-8'))
            else:
                self.wfile.write(hex(0)[2:].encode('utf-8'))
                self.wfile.write('\r\n'.encode('utf-8'))
                self.wfile.write('\r\n'.encode('utf-8'))
                break
        fcontent.close()
        fhead.close()

if not os.path.exists(storagedir):
    os.makedirs(storagedir)
httpd = ForkingTCPServer(('', port), Proxy)
print('start my proxy at', port)
httpd.serve_forever()

