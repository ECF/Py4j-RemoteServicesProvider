'''
Created on Jul 9, 2016

@author: slewis
'''

from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec, Py4jServiceBridgeEventListener
'''
This class implements the Py4jServiceBridgeEventListener interface
'''
class HelloServiceListener(Py4jServiceBridgeEventListener):
    def service_imported(self, servicebridge, endpointid, proxy, endpoint_props):
        '''
        Service imported notification.
        :param servicebridge: The Py4jServiceBridge instance that received the 
        notification. Will not be None.
        :param endpointid:  The endpointid from given set of properties.  
        Will not be None.
        :param proxy:  The endpoint instance proxy.  Will not be None.
        :param endpoint_props:  The endpoint properties.  Will not be None.
        '''
        print('service_imported endpointid='+endpointid+";proxy="+str(proxy)+";endpoint_props="+str(endpoint_props))
        response = proxy.sayHello('from.python','this is a big hello from Python!!!')
        print('response='+response)
    
    def service_modified(self, servicebridge, endpointid, proxy, endpoint_props):
        '''
        Service modified notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  
        Will not be None.
        :param proxy:  The endpoint instance proxy.  Will not be None.
        :param endpoint_props:  The endpoint properties.  Will not be None.
        '''
        print('service_modified endpointid='+endpointid+";proxy="+str(proxy)+";endpoint_props="+str(endpoint_props))
    
    def service_unimported(self, servicebridge, endpointid, proxy, endpoint_props):
        '''
        Service modified notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  
        Will not be None.
        :param proxy:  The endpoint instance proxy.  Will not be None.
        :param endpoint_props:  The endpoint properties.  Will not be None.
        '''
        print('service_unimported endpointid='+endpointid+";proxy="+str(proxy)+";endpoint_props="+str(endpoint_props))


if __name__ == '__main__':
    '''
    This sets the HelloServiceListener to be called back when java-based
    remote services are exported to us (service_imported), when they are updated
    (service_updated), and unexported to us (service_unexported).
    '''
    bridge = Py4jServiceBridge(HelloServiceListener())
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
