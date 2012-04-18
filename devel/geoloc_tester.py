#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import logging
from argparse import ArgumentParser
from time import sleep
from xml.etree.cElementTree import Element

from sleekxmpp import ClientXMPP
from sleekxmpp.basexmpp import BaseXMPP

# Python versions before 3.0 do not use UTF-8 encoding
# by default. To ensure that Unicode is handled properly
# throughout SleekXMPP, we will set the default encoding
# ourselves to UTF-8.
if sys.version_info < (3, 0):
    reload(sys)
    sys.setdefaultencoding('utf8')
else:
    raw_input = input


class GeolocProto:
    X = 'x'
    NAMESPACE = 'https://teammeet.de/teammeet.ns'
    GEOLOC = 'geoloc'
    LON = 'lon'
    LAT = 'lat'
    ERR = 'err'


class GeolocTester(ClientXMPP):

    def __init__(self, jid, password, room, lon, lat, err, timeout):
        ClientXMPP.__init__(self, jid, password)

        self.room = room
        self.nick = jid.split('@')[0]
        self.lon = lon
        self.lat = lat
        self.err = err
        self.generator_cycle = timeout

        # The session_start event will be triggered when
        # the bot establishes its connection with the server
        # and the XML streams are ready for use. We want to
        # listen for this event so that we we can initialize
        # our roster.
        self.add_event_handler("session_start", self.start)

        # The groupchat_message event is triggered whenever a message
        # stanza is received from any chat room. If you also also
        # register a handler for the 'message' event, MUC messages
        # will be processed by both handlers.
        self.add_event_handler("groupchat_message", self.muc_message)

    def start(self, event):
        """
        Process the session_start event.

        Typical actions for the session_start event are
        requesting the roster and broadcasting an initial
        presence stanza.

        Arguments:
            event -- An empty dictionary. The session_start
                     event does not provide any additional
                     data.
        """
        self.get_roster()
        self.send_presence()
        self.plugin['xep_0045'].joinMUC(self.room,
                                        self.nick,
                                        # If a room password is needed, use:
                                        # password=the_room_password,
                                        wait=True)

        while(True):
            self.generate_geoloc_package(self.lon, self.lat, self.err)
            sleep(self.generator_cycle)

    def muc_message(self, msg):
        """
        Process incoming message stanzas from any chat room.

        Arguments:
            msg -- The received message stanza. See the documentation
                   for stanza objects and the Message stanza to see
                   how it may be used.
        """
        if msg['mucnick'] != self.nick:
            print("received msg: {}".format(msg))
            print("from '{}'".format(msg['from'].bare))

    def generate_geoloc_package(self, longitude, latitude, error):
        msg = self.make_message(self.room,
                                mtype='groupchat')
        x = Element('{{{}}}{}'.format(GeolocProto.NAMESPACE, GeolocProto.X))
        geoloc = Element(GeolocProto.GEOLOC)
        lon = Element(GeolocProto.LON)
        lon.text = unicode(int(longitude * 1E6))
        lat = Element(GeolocProto.LAT)
        lat.text = unicode(int(latitude * 1E6))
        err = Element(GeolocProto.ERR)
        err.text = unicode(error)

        geoloc.append(lon)
        geoloc.append(lat)
        geoloc.append(err)
        x.append(geoloc)
        msg.append(x)

        #TODO: ugly workaround: `GroupMessageListener` in `TeamMeet` doesn't get
        #      triggered by messages not containing a body :(
        msg.append(Element('body'))

        self.send(msg)


if __name__ == '__main__':

    parser = ArgumentParser(description='Send mock-up and dump geoloc messages')

    xmpp_args = parser.add_argument_group('XMPP')
    xmpp_args.add_argument('-j', '--jid',
                           default='teammeetmate@jabber.de',
                           help="the jabber account to connect to" \
                                " [default: %(default)s]")
    xmpp_args.add_argument('-p', '--password',
                           metavar='PASS',
                           default='teammeetmatepass',
                           help="the password of the jabber account" \
                                " [default: %(default)s]")
    xmpp_args.add_argument('-r', '--room',
                           default='teammeettestroom@conference.jabber.ccc.de',
                           help="the MUC to join" \
                                " [default: %(default)s]")

    loc_args = parser.add_argument_group('location')
    loc_args.add_argument('-l', '--lon',
                          type=float,
                          default=53.565278,
                          help="the longitude to use in the mock-up messages" \
                               " [default: %(default)s]")
    loc_args.add_argument('-L', '--lat',
                          type=float,
                          default=10.001389,
                          help="the latitude to use in the mock-up messages"
                               " [default: %(default)s]")
    loc_args.add_argument('-e', '--err',
                          type=float,
                          default=5.5,
                          help="the error of the GPS signal" \
                               " [default: %(default)s]")
    loc_args.add_argument('-t', '--timeout',
                          type=int,
                          metavar='SEC',
                          default=5,
                          help="the period between mock-up messages" \
                               " [default: %(default)s]")

    parser.add_argument('--log-level',
                        choices=['DEBUG', 'INFO', 'WARNING', 'ERROR'],
                        default='DEBUG',
                        metavar='LEVEL',
                        help="the log level [default: %(default)s]")

    args = parser.parse_args()

    # Setup logging.
    log_level = getattr(logging, args.log_level, None)
    logging.basicConfig(level=log_level,
                        format='%(levelname)s:%(module)s: %(message)s')

    # Setup the MUCBot and register plugins. Note that while plugins may
    # have interdependencies, the order in which you register them does
    # not matter.
    xmpp = GeolocTester(args.jid, args.password, args.room,
                        args.lon, args.lat, args.err, args.timeout)
    xmpp.register_plugin('xep_0030') # Service Discovery
    xmpp.register_plugin('xep_0045') # Multi-User Chat
    xmpp.register_plugin('xep_0199') # XMPP Ping

    # Connect to the XMPP server and start processing XMPP stanzas.
    if xmpp.connect():
        xmpp.process(block=True)
        print("Done")
    else:
        print("Unable to connect.")


