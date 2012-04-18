#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import logging
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


class GeolocTester(ClientXMPP):

    def __init__(self, jid, password, room, nick):
        ClientXMPP.__init__(self, jid, password)

        self.room = room
        self.nick = nick

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

        self.generate_geoloc_package(53.565278, 10.001389, 5.5)

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
        msg = self.make_message('{}'.format(self.room),
                                mtype='groupchat')
        x = Element('{https://teammeet.de/teammeet.ns}x')
        geoloc = Element('geoloc')
        lon = Element('lon')
        lon.text = unicode(int(longitude * 1E6))
        lat = Element('lat')
        lat.text = unicode(int(latitude * 1E6))
        err = Element('err')
        err.text = unicode(error)

        geoloc.append(lon)
        geoloc.append(lat)
        geoloc.append(err)
        x.append(geoloc)
        msg.append(x)

        self.send(msg)


if __name__ == '__main__':

    jid = 'teammeetmate@jabber.de'
    password = 'teammeetmatepass'
    room = 'teammeettestroom@conference.jabber.ccc.de'
    nick = 'teammeetmate'

    # Setup logging.
    logging.basicConfig(level=logging.DEBUG,
                        format='%(levelname)s:%(module)s: %(message)s')

    # Setup the MUCBot and register plugins. Note that while plugins may
    # have interdependencies, the order in which you register them does
    # not matter.
    xmpp = GeolocTester(jid, password, room, nick)
    xmpp.register_plugin('xep_0030') # Service Discovery
    xmpp.register_plugin('xep_0045') # Multi-User Chat
    xmpp.register_plugin('xep_0199') # XMPP Ping

    # Connect to the XMPP server and start processing XMPP stanzas.
    if xmpp.connect():
        xmpp.process(block=True)
        print("Done")
    else:
        print("Unable to connect.")


