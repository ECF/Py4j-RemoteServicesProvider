package org.eclipse.ecf.examples.flatbuf.monster;

import com.google.flatbuffers.FlatBufferBuilder;

public interface IScare {

	Monster scareWith(FlatBufferBuilder builder);
}
