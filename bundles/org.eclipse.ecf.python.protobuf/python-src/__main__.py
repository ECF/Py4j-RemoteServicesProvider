import sys
import osgiservicebridge.bridge
sys.stdout = osgiservicebridge.bridge.flushfile(sys.stdout)
sys.stderr = osgiservicebridge.bridge.flushfile(sys.stderr)
# create remote service registry
import osgiservicebridge.protobuf
remote_service_registry = osgiservicebridge.protobuf.ProtobufServiceRegistry()
print('created remote_service_registry')
# create py4j service bridge
py4j_service_bridge = osgiservicebridge.bridge.Py4jServiceBridge(remote_service_registry) 
print('created py4j_service_bridge')
# connect py4j_service_bridge
import optparse
options_parser = optparse.OptionParser()
options_parser.add_option('-j', action='store', type='int', help='Py4j Java listen port (default=25333)', dest='javaport')
options_parser.add_option('-p', action='store', type='int', help='Py4j Python callback listen port (default=25334)', dest='pythonport')
(options, args) = options_parser.parse_args()
javaport = options.javaport
import py4j.java_gateway
if not javaport:
	javaport = py4j.java_gateway.DEFAULT_PORT
pythonport = options.pythonport
if not pythonport:
	pythonport = py4j.java_gateway.DEFAULT_PYTHON_PROXY_PORT
gateway_params = py4j.java_gateway.GatewayParameters(port=javaport)
callback_server_params = py4j.java_gateway.CallbackServerParameters(port=pythonport)
py4j_service_bridge.connect(gateway_parameters=gateway_params,callback_server_parameters=callback_server_params,path_hook=osgiservicebridge.bridge.OSGIPythonModulePathHook(py4j_service_bridge)) 
print('py4j_service_bridge connected')

py4j_service_bridge.export(osgiservicebridge.protobuf.PythonServiceExporter(py4j_service_bridge))
print('PythonServiceExporter exported')

