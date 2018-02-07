package org.eclipse.ecf.examples.protobuf.hello.provider;

import org.eclipse.ecf.python.PythonLaunchCommandProvider;
import org.eclipse.ecf.python.protobuf.ProtobufPythonLaunchCommandProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate=true)
public class ExampleProtobufPythonLaunchCommandProvider extends ProtobufPythonLaunchCommandProvider
		implements PythonLaunchCommandProvider {

	@Override
	@Activate
	protected void activate(BundleContext ctx) throws Exception {
		super.activate(ctx);
	}
	
	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}
}
