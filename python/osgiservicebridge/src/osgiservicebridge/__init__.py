'''
OSGi service bridge 
:author: Scott Lewis
:copyright: Copyright 2016, Composent, Inc.
:license: Apache License 2.0
    Copyright 2016 Composent, Inc. and others
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
'''
import uuid
from time import time
from datetime import datetime
from threading import Lock
from argparse import ArgumentError

'''
Creates a uuid and returns as String
:return: uuid4 as String
'''
def create_uuid():
    return str(uuid.uuid4())

'''
Get the time (ms) since epoch.
:return: number of ms since January 1, 1970 as integer
'''
def time_since_epoch():
    return int((datetime.utcnow() - datetime(1970, 1, 1)).total_seconds() * 1000)

#----------------------------------------------------------------------------
# RSA constants (declared in org.osgi.service.remoteserviceadmin.RemoteConstants
#----------------------------------------------------------------------------
ENDPOINT_ID = "endpoint.id"
ENDPOINT_SERVICE_ID = "endpoint.service.id"
ENDPOINT_FRAMEWORK_UUID = "endpoint.framework.uuid"
ENDPOINT_PACKAGE_VERSION_ = "endpoint.package.version."
SERVICE_EXPORTED_INTERFACES = "service.exported.interfaces"
REMOTE_CONFIGS_SUPPORTED = "remote.configs.supported"
REMOTE_INTENTS_SUPPORTED = "remote.intents.supported"
SERVICE_EXPORTED_CONFIGS = "service.exported.configs"
SERVICE_EXPORTED_INTENTS = "service.exported.intents"
SERVICE_EXPORTED_INTENTS_EXTRA = "service.exported.intents.extra"
SERVICE_IMPORTED = "service.imported"
SERVICE_IMPORTED_CONFIGS = "service.imported.configs"
SERVICE_INTENTS = "service.intents"
SERVICE_ID = "service.id"
OBJECT_CLASS = "objectClass"
INSTANCE_NAME = "instance.name"
SERVICE_RANKING = 'service.ranking'
SERVICE_COMPONENT_NAME = 'component.name'
SERVICE_COMPONENT_ID = 'component.id'

EXPORT_PROPERTIES_NAME = 'export_properties'

# Global list holding all OSGi RSA constants
rsaprops = [ENDPOINT_ID,ENDPOINT_SERVICE_ID,ENDPOINT_FRAMEWORK_UUID,SERVICE_EXPORTED_INTERFACES,REMOTE_CONFIGS_SUPPORTED,REMOTE_INTENTS_SUPPORTED,SERVICE_EXPORTED_CONFIGS,SERVICE_EXPORTED_INTENTS,SERVICE_EXPORTED_INTENTS_EXTRA,SERVICE_IMPORTED,SERVICE_IMPORTED_CONFIGS,SERVICE_INTENTS,SERVICE_ID,OBJECT_CLASS,INSTANCE_NAME,SERVICE_RANKING,SERVICE_COMPONENT_ID,SERVICE_COMPONENT_NAME]

#----------------------------------------------------------------------------
# ECF RS constants (declared in org.eclipse.ecf.remoteserviceadmin.Constants
#----------------------------------------------------------------------------
ECF_ENDPOINT_CONTAINERID_NAMESPACE = "ecf.endpoint.id.ns"
ECF_ENDPOINT_ID = "ecf.endpoint.id"
ECF_RSVC_ID = "ecf.rsvc.id"
ECF_ENDPOINT_TIMESTAMP = "ecf.endpoint.ts"
ECF_ENDPOINT_CONNECTTARGET_ID = "ecf.endpoint.connecttarget.id"
ECF_ENDPOINT_IDFILTER_IDS = "ecf.endpoint.idfilter.ids"

ECF_ENDPOINT_REMOTESERVICE_FILTER = "ecf.endpoint.rsfilter"
ECF_SERVICE_EXPORTED_CONTAINER_FACTORY_ARGS = "ecf.exported.containerfactoryargs"
ECF_SERVICE_EXPORTED_CONTAINER_CONNECT_CONTEXT = "ecf.exported.containerconnectcontext"
ECF_SERVICE_EXPORTED_CONTAINER_ID = "ecf.exported.containerid"
ECF_SERVICE_EXPORTED_ASYNC_INTERFACES = "ecf.exported.async.interfaces"
ECF_SERVICE_EXPORTED_ASYNC_NOPROXY = "ecf.rsvc.async.noproxy"
ECF_SERVICE_ASYNC_RSPROXY_CLASS_ = "ecf.rsvc.async.proxy_"
ECF_ASYNC_INTERFACE_SUFFIX = "Async"
ECF_SERVICE_IMPORTED_VALUETYPE = "ecf.service.imported.valuetype"
ECF_SERVICE_IMPORTED_ENDPOINT_ID = ENDPOINT_ID
ECF_SERVICE_IMPORTED_ENDPOINT_SERVICE_ID = ENDPOINT_SERVICE_ID
ECF_OSGI_ENDPOINT_MODIFIED = "ecf.osgi.endpoint.modified"
ECF_OSGI_CONTAINER_ID_NS = "ecf.osgi.ns"

# Global list holding all ECF RSA constants
ecfprops = [ECF_ENDPOINT_CONTAINERID_NAMESPACE,ECF_ENDPOINT_ID,ECF_RSVC_ID,ECF_ENDPOINT_TIMESTAMP,ECF_ENDPOINT_CONNECTTARGET_ID,ECF_ENDPOINT_IDFILTER_IDS,ECF_ENDPOINT_REMOTESERVICE_FILTER,ECF_SERVICE_EXPORTED_ASYNC_INTERFACES]

'''
Py4J constants
'''
PY4J_PROTOCOL = 'py4j'
PY4J_EXPORTED_CONFIG = 'ecf.py4j.host.python'
PY4J_EXPORTED_CONFIGS = [PY4J_EXPORTED_CONFIG]
PY4J_NAMESPACE = 'ecf.namespace.py4j'
PY4J_SERVICE_INTENTS = ['passByReference', 'exactlyOnce', 'ordered']
PY4J_PYTHON_PATH = "/python"
PY4J_JAVA_PATH = "/java"
PY4J_JAVA_ATTRIBUTE = 'Java'
PY4J_JAVA_IMPLEMENTS_ATTRIBUTE = 'implements'
PY4J_JAVA_PACKAGE_VERSION_ATTRIBUTE = 'package_version'

#----------------------------------------------------------------------------------
# iPopo RSA constants
#----------------------------------------------------------------------------------
ERROR_EP_ID = '0'
ERROR_NAMESPACE = 'org.eclipse.ecf.core.identity.StringID'
ERROR_IMPORTED_CONFIGS = ['import.error.config']
ERROR_ECF_EP_ID = 'export.error.id'
DEFAULT_EXPORTED_CONFIGS = ['ecf.xmlrpc.server']

def get_matching_interfaces(origin, propValue):
    '''
    Get the interfaces matching from an origin parameter.   For a list of strings or single
    string (origin), if propValue is '*' return all the origin strings, otherwise return
    the propValue string
    
    :param origin: a string or string array
    :param propValue: a string with '*' to match all from origin, otherwise return propValue
    :return: value of origin if propValue == '*', otherwise propValue
    '''
    if origin is None or propValue is None:
        return None
    if isinstance(propValue,type("")):
        if propValue == '*':
            return origin
    else:
        if isinstance(propValue,type("")) and len(propValue) == 1 and propValue[0] == '*':
            return origin
        else:
            return propValue

def get_prop_value(name, props, default=None): 
    '''
    Get the name value from props dictionary.   For a given array of strings or single
    string (origin), if propValue is '*' return all the origin strings, otherwise return
    the propValue string.  Will not throw KeyError if name not in props, but rather will
    return None
    
    :param name: a string to be used as key into props
    :param props: a dictionary
    :return: value of default if props is None.  props[name] value if present, and default if name key
    not in props
    '''
    if not props:
        return default
    try:
        return props[name]
    except KeyError:
        return default

def set_prop_if_null(name, props, ifnull):     
    '''
    Set the name value in props dictionary if not already present in props.   
    
    :param name: a string to be used as key into props
    :param props: a dictionary
    :return: None
    '''
    v = get_prop_value(name,props)
    if v is None:
        props[name] = ifnull

def get_string_plus_property_value(value):
    '''
    Get a list strings given the value of a 'string plus' property.  A string plus property is defined
    as one that can be either a single string or a string[].  If a single string this function
    returns a single element list with containing the value
    
    :param value: a string or list of strings
    :return: a list of strings
    '''
    if value:
        if isinstance(value,type("")):
            return [value]
        elif isinstance(value,type([])):
            return value;
        else:
            return None

def parse_string_plus_value(value):
    '''
    Parse a comma-separated string plus property (see above).
    
    :param value: a single string with comma separated array instances
    :return: a list of strings parsed from value via value.split(',')
    '''
    return value.split(',')

def get_string_plus_property(name, props, default=None):   
    '''
    Return the value of a string plus value from props with name.  
    
    :param name: a key into the props dictionary parameter
    :param props: a non-empty dictionary
    :return: a list of strings that represent the value of the name string-plus property.
    For example, the 'objectArray' value in OSGi Remote Services
    '''              
    val = get_string_plus_property_value(get_prop_value(name,props,default))
    return default if val is None else val

def get_package_from_classname(classname):
    '''
    Get the package name from a Java classname.  A fully qualified Java classname has 
    the following example structure:   [[[<pkg>].<pkg1>].<pkg2>.]classname.  This 
    function will return the fully qualified package name without the classname nor the 
    final '.' prior to the classname
    
    :param classname: the fully-qualified Java class name
    :return: a string that has the package name without the classname.  None if there is no
    package for the class.
    '''              
    try:
        return classname[:classname.rindex('.')]
    except KeyError:
        return None

def get_package_versions(intfs, props):
    '''
    Get the package name from a Java classname.  A fully qualified Java classname has 
    the following example structure:   [[[<pkg>].<pkg1>].<pkg2>.]classname.  This 
    function will return the fully qualified package name without the classname nor the 
    final '.' prior to the classname
    
    :param classname: the fully-qualified Java class name
    :param props: a non-empty dictionary
    :return: a list of strings that represent the value of the name string-plus property.
    For example, the 'objectArray' value in OSGi Remote Services
    '''              
    result = []
    for intf in intfs:
        pkgname = get_package_from_classname(intf)
        if pkgname:
            key = ENDPOINT_PACKAGE_VERSION_+pkgname
            val = props[key]
            if val:
                result.append((key,val))
    return result

#Global remote service id.  Starts at 1 and is incremented via get_next_rsid
_next_rsvcid = 1
_next_rsvcid_lock = Lock()

def get_next_rsid():
    '''
    Get a unique remote service id.  Starts at 1 and increments to remain unique
    
    :return: a unique integer starting at 1 and incrementing with every access
    '''              
    global _next_rsvcid_lock
    with _next_rsvcid_lock:
        global _next_rsvcid
        n = _next_rsvcid
        _next_rsvcid += 1
        return n

def merge_dicts(*dict_args):
    '''
    Merge a list of dictionaries given by dict_args into a single resulting dictionary.
    
    :param dict_args: a list of dictionaries
    :return: a single dictionary containing the contents of all given dictionaries in
    dict_args list.  Returns a new dictionary and does not modify any of the given
    dictionaries.
    '''              
    result = {}
    for dictionary in dict_args:
        result.update(dictionary)
    return result

def get_rsa_props(object_class, exported_cfgs=None, intents=None, ep_svc_id=None, fw_id=None, pkg_vers=None):
    '''
    Get all of the RSA properties given the minimum required information.
    
    :param object_class: a list of strings.  Must not be None, and must contain 1 or more strings
    :param exported_cfgs: a list of strings that are to be associated with the SERVICE_EXPORTED_CONFIGS and SERVICE_IMPORTED_CONFIGS RSA properties.  Must not be None and must contain 1 or more strings
    :param intents: a list of strings to be associated with the SERVICE_INTENTS.   May be None or not provided.
    :param ep_svc_id: the value of the ENDPOINT_SERVICE_ID.  If None, will be given a new unique number via get_next_rsid().  If not None, must be integer.
    :param fw_id: the framework id as string.  If None a new uuid will be used.
    :param pkg_vers: a list of tuples with a package name as first item in tuple (String, and the version string as the second item.  Example:  [('org.eclipse.ecf','1.0.0')].  If None, nothing is added to the returned dictionary.
    :return: a single dictionary containing all the OSGi RSA-required endpoint properties.
    '''              
    results = {}
    if not object_class or len(object_class) == 0:
        raise ArgumentError('object_class must be an [] of Strings')
    results['objectClass'] = object_class
    if exported_cfgs is None:
        exported_cfgs = PY4J_EXPORTED_CONFIGS
    results[SERVICE_EXPORTED_CONFIGS] = exported_cfgs
    results[SERVICE_IMPORTED_CONFIGS] = exported_cfgs
    if intents:
        results[SERVICE_INTENTS] = intents
    else:
        results[SERVICE_INTENTS] = PY4J_SERVICE_INTENTS
    if not ep_svc_id:
        ep_svc_id = get_next_rsid()
    results[ENDPOINT_SERVICE_ID] = ep_svc_id
    results[SERVICE_ID] = ep_svc_id
    if not fw_id:
        fw_id = create_uuid()
    results[ENDPOINT_FRAMEWORK_UUID] = fw_id
    if pkg_vers:
        if isinstance(pkg_vers,type(tuple())):
            pkg_vers = [pkg_vers]
        for pkg_ver in pkg_vers:
            key = ENDPOINT_PACKAGE_VERSION_ + pkg_ver[0]
            results[key] = pkg_ver[1]
    results[ENDPOINT_ID] = create_uuid()
    results[SERVICE_IMPORTED] = 'true'
    results[SERVICE_EXPORTED_INTERFACES] = '*'
    return results

def get_ecf_props(ep_id, ep_id_ns, rsvc_id=None, ep_ts=None):
    '''
    Get all of the ECF RS properties given the minimum required information.
    
    :param ep_id: a String to be used for the ECF_ENDPOINT_ID value.
    :param ep_id_ns: an optional string that is the ECF_ENDPOINT_CONTAINERID_NAMESPACE value.
    :param rsvc_id: an optional integer that will be the value of ECF_RSVC_ID
    :param ep_ts: an optional integer timestamp.   if None, the returned value of time_since_epoch() will be used.
    :return: a single dictionary containing all the ECF RS-required properties.
    '''              
    results = {}
    if not ep_id:
        raise ArgumentError('ep_id must be a valid endpoint id')
    results[ECF_ENDPOINT_ID] = ep_id
    if not ep_id_ns:
        ep_id_ns = PY4J_NAMESPACE
    results[ECF_ENDPOINT_CONTAINERID_NAMESPACE] = ep_id_ns
    if not rsvc_id:
        rsvc_id = get_next_rsid()
    results[ECF_RSVC_ID] = rsvc_id
    if not ep_ts:
        ep_ts = time_since_epoch()
    results[ECF_ENDPOINT_TIMESTAMP] = ep_ts
    return results

def get_extra_props(props):
    '''
    Get properties from given dictionary that are not OSGi RSA nor ECF RS properties.
    
    :param props: a non-empty dictionary
    :return: a single dictionary containing all the ECF RS-required properties.
    '''              
    result = {}
    for key, value in props.items():
        if not key in ecfprops and not key in rsaprops:
            if not key.startswith(ENDPOINT_PACKAGE_VERSION_):
                result[key] = value
    return result

def get_edef_props(object_class, ecf_ep_id, ep_namespace=None, ep_rsvc_id=None, exported_cfgs = None, ep_ts=None, intents=None, fw_id=None, pkg_ver=None):
    '''
    Get both OSGi RSA properties and ECF RS properties in a single dictionary.
    
    :param object_class: a list of strings.  Must not be None, and must contain 1 or more strings
    :param exported_cfgs: a list of strings that are to be associated with the SERVICE_EXPORTED_CONFIGS and SERVICE_IMPORTED_CONFIGS RSA properties.  Must not be None and must contain 1 or more strings
    :param ep_namespace: a string that is the ECF_ENDPOINT_CONTAINERID_NAMESPACE value.
    :param ecf_ep_id: an integer to be used for the ECF_ENDPOINT_ID value.
    :param ep_rsvc_id: an optional integer that will be the value of ECF_RSVC_ID
    :param ep_ts: an optional integer timestamp.   if None, the returned value of time_since_epoch() will be used.
    :param intents: a list of strings to be associated with the SERVICE_INTENTS.   May be None or not provided.
    :param fw_id: the framework id as string.  If None a new uuid will be used.
    :param pkg_vers: a list of tuples with a package name as first item in tuple (String, and the version string as the second item.  Example:  [('org.eclipse.ecf','1.0.0')].  If None, nothing is added to the returned dictionary.
    :return: a single dictionary containing all the OSGI RSA + ECF RS-required properties.
    '''              
    osgi_props = get_rsa_props(object_class, exported_cfgs, intents, ep_rsvc_id, fw_id, pkg_ver)
    ecf_props = get_ecf_props(ecf_ep_id, ep_namespace, ep_rsvc_id, ep_ts)
    return merge_dicts(osgi_props,ecf_props)

def get_edef_props_error(object_class):
    '''
    Get OSGi RSA properties and ECF RS properties in a single dictionary for an error condition.
    
    :param object_class: a list of strings.  Must not be None, and must contain 1 or more strings
    :return: a single dictionary containing all the OSGI RSA + ECF RS-required properties.
    '''              
    return get_edef_props(object_class, ERROR_IMPORTED_CONFIGS, ERROR_NAMESPACE, ERROR_EP_ID, ERROR_ECF_EP_ID, 0, 0, None, None)
#----------------------------------------------------------------------------------
def check_strings_in_list(l):
    for item in l:
        if not isinstance(item,str):
            raise ValueError('non-string item in args list')

def _modify_remoteservice_class(cls,kwargs):
    '''
    Modify a remote service class to have Java metadata (implements and export_properties)
    
    :param cls: the class to be modified
    :param kwargs: a map to contain the objectClass and export_properties attributes
    :return: the cls instance after being modified
    '''
    # first copy the kwargs map
    _kwargs = kwargs.copy()
    # get the OBJECT_CLASS from kwargs, which must
    # be present
    objectClass = _kwargs.pop(OBJECT_CLASS, None)
    if not objectClass:
        raise ValueError('kwargs must have objectClass value that is str or list of str')
    # If objectClass is str then make it into a single element string array
    if isinstance(objectClass,str):
        objectClass = [objectClass]
    # Verify that objectClass now contains a list
    if not isinstance(objectClass,list):
        raise ValueError('objectClass must be of type list')
    # verify that only strings are in list
    check_strings_in_list(objectClass)
    # get export_properties from kwargs
    export_properties = _kwargs.get(EXPORT_PROPERTIES_NAME,None)
    # if export properties is present and not a dictionary
    # raise error
    if export_properties and not isinstance(export_properties,dict):
        raise ValueError('service_properties must be of type dict')
    # create a new dictionary that has the 'implements'/objectClass and 'export_properties'/dict
    d = { PY4J_JAVA_IMPLEMENTS_ATTRIBUTE: objectClass, EXPORT_PROPERTIES_NAME: export_properties }
    # create the required Java class
    javaclass = type(PY4J_JAVA_ATTRIBUTE,(object,),d)
    # set the 'Java' attribute on cls to the newly-created class
    setattr(cls,PY4J_JAVA_ATTRIBUTE,javaclass)   
    return cls
    
def remote_service(**kwargs):
    '''
    Class decorator for remote services.  This class decorator is intended to be used as follows:
    
    @remote_service(objectClass=['fq java interface name'],export_properties={ 'myprop': 'myvalue' })
    class MyClass:
        pass
        
    e.g.
    
    @remote_service(objectClass=['java.util.Map'])
    class HelloServiceImpl:
        impl of java.util.Map interface

    :param kwargs: the kwargs dict required to have objectClass and (optional) export_properties
    '''
    def decorate(cls):
        return _modify_remoteservice_class(cls,kwargs)
    return decorate

