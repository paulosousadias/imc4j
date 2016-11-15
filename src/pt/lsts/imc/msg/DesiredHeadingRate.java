package pt.lsts.imc.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.nio.ByteBuffer;
import pt.lsts.imc.annotations.FieldType;
import pt.lsts.imc.annotations.IMCField;

/**
 * Desired Heading Rate speed reference value for the control layer.
 */
public class DesiredHeadingRate extends Message {
	public static final int ID_STATIC = 408;

	/**
	 * The value of the desired heading rate speed in radians per
	 * second.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			units = "rad/s"
	)
	public double value = 0;

	public int mgid() {
		return 408;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeDouble(value);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			value = buf.getDouble();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
