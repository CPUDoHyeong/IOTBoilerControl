'''
This is an IOT project using MQTT protocol.
This was created to control the boiler with a smartphone.
Use AWS, MQTT protocol, android studio, Raspberry Pi.
ttttt
'''
# import
import paho.mqtt.client as mqtt
import time
import Adafruit_DHT
import RPi.GPIO as GPIO
from pyfirmata import Arduino,util
board = Arduino('/dev/ttyACM0')

# definition
brokerIp = "52.79.235.179"
port = 1883

# temperature sensor & pin
sensor = Adafruit_DHT.DHT11
pin2 = 2

# servo moter pin
# pin7 is function ccontrol
# pin8 is temperature control
pin7 = board.get_pin('d:7:s')
pin8 = board.get_pin('d:8:s')

# function control
def function_control(message) :
    flag = int(message)
    if(flag > 180) :
        flag = 180
    elif(flag < 0) :
        flag = 0
    pin7.write(flag)

# temperature control
def temperature_control(message) :
    flag = int(message)
    if(flag > 180) :
        flag = 180
    elif(flag < 0) :
        flag = 0
    pin8.write(flag)

#temperature, humidity read function
def get_temperature() :
    humidity, temperature = Adafruit_DHT.read_retry(sensor, pin2)
    if humidity is not None and temperature is not None :
        return temperature
    else :
        return "Failed to get reading."

# Called callbackwhen CONNATACK response is received from the server
def on_connect(client, userdata, flags, rc) :
    print ("Connected with result code " + str(rc))
    client.subscribe("aws") # subcribe 'aws'
    client.subscribe("temperature") # subcribe 'temperature'
    client.subscribe("func_control") # subcribe 'function_control' 
    client.subscribe("temp_control") # subcribe 'temp_control' -> temperature_control
    

# A callback that is called when you receive a publish message from the server
def on_message(client, userdata, msg) :
    print(msg.topic + " " + str(msg.payload))
    response(client, msg)

# Response on request
def response(client, msg) :
    topic = msg.topic
    message = msg.payload
    # Return temperature if topic is 'temperature'
    if topic == "temperature" :
        temperature = get_temperature()
        client.publish("response", temperature)
    elif topic == "func_control" :
        client.publish("check", "doing control")
        function_control(message)
    elif topic == "temp_control" :
        client.publish("check", "doing control")
        temperature_control(message)



client = mqtt.Client()                       # make client object
client.on_connect = on_connect    # set callback
client.on_message = on_message   # set callback

client.connect(brokerIp, port)
client.loop_forever()

