package org.eclipse.ecf.examples.protobuf.hello;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;

public interface IHello {

	HelloMsgContent sayHello(HelloMsgContent message) throws Exception;
}
