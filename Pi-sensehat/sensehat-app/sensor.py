#!/usr/bin/python
# COPYRIGHT
#     Copyright (c) 2016 by Cisco Systems, Inc.
#     All rights reserved.
#
# DESCRIPTION
#     Raspberry Sensorhat sensor operations.
from sense_hat import SenseHat
sense = SenseHat()
import time


# Round the data to two digit float
def get_data_rounded(raw_data):
    if type(raw_data) is dict:
        for key in raw_data:
            raw_data[key] = float("{0:.2f}".format(raw_data[key]))
    else:
        if type(raw_data) is float:
            raw_data = float("{0:.2f}".format(raw_data))
    return raw_data

# Get the sensehat data, return None if the item requested is not available
def get_data(type):
    return {
     'temperature':         get_data_rounded(sense.get_temperature),
     'humidity':            get_data_rounded(sense.get_humidity),
     'pressure':            get_data_rounded(sense.get_pressure),
     'magnetometer':        get_data_rounded(sense.get_compass_raw),
     'accelerometer':       get_data_rounded(sense.get_accelerometer),
     'gyroscope':           get_data_rounded(sense.get_gyroscope)
      }.get(type, 'None')()

def display_data(message, fg_colour=[0,0,200], bg_colour=[0, 0, 0]):
    if message == "red":
        sense.clear(255,0,0)
        return "OK"
    if message == "green":
        sense.clear(0,255,0)
        return "OK"
    if len(message) <= 8:
        bg=[int(element / 4) for element in bg_colour]
        sense.show_message(message,text_colour=fg_colour, back_colour=bg, scroll_speed=0.01)
        return "OK"
    else:
        sense.show_message("StrErr!",text_colour=fg_colour)
        return "Error: message too long > 8 characters"

def set_data(type):
    return {
     'flip_h':              sense.flip_h,
     'flip_v':              sense.flip_v,
     'clear':               sense.clear
      }.get(type)()

def display_leds():
    sense.clear(0,255,255)
    time.sleep(0.05)
    sense.clear(0,0,0)
    return "OK"



colours = {
    'black': [0, 0, 0],
    'white': [255, 255, 255],
    'red': [255, 0, 0],
    'green': [0, 255, 0],
    'blue': [0, 0, 255],
    'yellow': [255, 255, 0],
    'magenta': [255, 0, 255],
    'cyan': [0, 255, 255]}


def colourNameToRgb(colour):
     return colours.get(colour,[0, 0, 255])
