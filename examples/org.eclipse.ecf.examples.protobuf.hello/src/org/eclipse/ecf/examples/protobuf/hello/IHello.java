package org.eclipse.ecf.examples.protobuf.hello;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsg;

public interface IHello {

	HelloMsg sayHello(HelloMsg message) throws Exception;
}
