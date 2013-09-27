import SimpleHTTPServer

from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from urlparse import urlparse, parse_qs, parse_qsl



def getParas(path):
    dict = parse_qs(urlparse(path))
    return dict


class modelHandler():
    def __init__(self):
        models = {}

    def handle(self, url):
        res = urlparse(url)
        pdict = parse_qs(res.query)
        # model = models[res.path]
        return self.predict(pdict)

    def predict(self, pdict):
        res_dict = {}
        res_dict.update(pdict)
        print res_dict
        return res_dict


mhandler = modelHandler()

class MyHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        print("Just received a GET request")
        self.send_response(200)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        dictout = mhandler.handle(self.path)        
        self.wfile.write(dictout)
        print 'path', self.path

        return


if __name__ == "__main__":
    try:
        server = HTTPServer(('localhost', 8000), MyHandler)
        print('Started http server')
        server.serve_forever()
    except KeyboardInterrupt:
        print('received, shutting down server')
        server.socket.close()