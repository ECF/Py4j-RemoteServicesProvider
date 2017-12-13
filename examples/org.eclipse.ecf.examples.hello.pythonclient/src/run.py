'''
Created on Jul 9, 2016

@author: slewis
'''

from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec
if __name__ == '__main__':
    '''
    This sets the HelloServiceListener to be called back when java-based
    remote services are exported to us (service_imported), when they are updated
    (service_updated), and unexported to us (service_unexported).
    '''
    bridge = Py4jServiceBridge()
    print("bridge created")
    '''
    When connect is called, any previously exported java services will
    result in the HelloServiceListener service_imported method being called.
    '''
    bridge.connect()
    print("bridge connected")
    '''
    Wait for 5 seconds
    '''
    _wait_for_sec(5)
    '''
    Disconnect will call service_unimported for any services currently
    imported
    '''
    bridge.disconnect()
    print("disconnected...exiting")
