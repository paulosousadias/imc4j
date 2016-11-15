package pt.lsts.imc.msg;

import java.io.IOException;
import java.lang.Exception;
import java.nio.ByteBuffer;

/**
 * Request the destination system to restart itself.
 */
public class RestartSystem extends Message {
	public static final int ID_STATIC = 9;

	public int mgid() {
		return 9;
	}

	public byte[] serializeFields() {
		return new byte[0];
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
