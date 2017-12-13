package org.eclipse.ecf.examples.protobuf.hello.javahost;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.eclipse.ecf.python.protobuf.IPythonServiceExporter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.eclipse.ecf.examples.protobuf.hello.IHello;

@Component(immediate=true,property = { "service.exported.interfaces=*", "service.exported.configs=ecf.py4j.host.pb"})
public class HelloImpl implements IHello {

	@Reference(policy=ReferencePolicy.DYNAMIC)
	void bindPythonServiceExporter(IPythonServiceExporter exporter) {
	}
	void unbindPythonServiceExporter(IPythonServiceExporter exporter) {
	}
	
	HelloMsgContent createResponse() {
		HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
		b1.addX(1.1);
		b1.addX(1.2);
		b1.setF("java");
		b1.setTo("python");
		b1.setHellomsg("Hello response from java");
		b1.setH("some other message");
		return b1.build();
	}

	@Override
	public HelloMsgContent sayHello(HelloMsgContent message) throws Exception {
		System.out.println("sayHello called from python with message="+message);
		return createResponse();
	}

}
