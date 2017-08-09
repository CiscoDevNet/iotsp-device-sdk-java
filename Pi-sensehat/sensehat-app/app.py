#!/usr/bin/python
# COPYRIGHT
#     Copyright (c) 2016 by Cisco Systems, Inc.
#     All rights reserved.
#
# DESCRIPTION
#     Raspberry Sensorhat web server URLs' implementations in flask.
import argparse
import sys
import time
from uuid import getnode as get_mac

from flask import Flask, jsonify

import sensor

app = Flask(__name__, static_url_path='', static_folder="static")
app.debug = False

@app.route('/status')
def status():
    deviceId = hex(get_mac())
    return jsonify({'deviceId': deviceId})


@app.route('/sensehat/<type>')
def get_data(type):
    t = None
    try:
        t = sensor.get_data(type)
    except Exception as e:
        return jsonify({'ts': int(time.time() * 1000), type: -1}) # handle sensor data error
    return jsonify({'ts': int(time.time() * 1000), type: t})


@app.route('/message/<message>')
@app.route('/message/<message>/<fg_colour>')
@app.route('/message/<message>/<fg_colour>/<bg_colour>')
def show_messages(message, fg_colour='blue', bg_colour='black'):
    t = None
    try:
        t = sensor.display_data(message,
                                fg_colour=sensor.colourNameToRgb(fg_colour),
                                bg_colour=sensor.colourNameToRgb(bg_colour)
                                )
    except Exception as e:
        print "show_messages: Exception: {}".format(e.msg)
    finally:
        return jsonify({'Status': t})


@app.route('/sensehat/ping')
def flash_led():
    t= "FAILED"
    try:
        t= sensor.display_leds()
    finally:
        return jsonify({'Status': t})


@app.route('/set/<type>')
def set_data(type):
    t = 'Fail'
    try:
        t = sensor.set_data(type)
    finally:
        return jsonify({'Status': t})

@app.route('/')
def index():
    return app.send_static_file('index.html')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Raspberry Pi Sensehat web server")
    parser.add_argument('-p', '--port', help='Web server port, default: 5000', type=int, default=5000, dest='port')
    parser.add_argument('-v', '--version', action='version', version='1.1')
    args = parser.parse_args()

    import gevent.wsgi

    ws = gevent.wsgi.WSGIServer(listener=('0.0.0.0', args.port),
                                application=app)
    print "Sense-Hat-Server is started"
    ws.serve_forever()
