package pt.lsts.autonomy;

import pt.lsts.imc4j.annotations.Parameter;
import pt.lsts.imc4j.def.SpeedUnits;
import pt.lsts.imc4j.def.ZUnits;
import pt.lsts.imc4j.msg.AlignmentState;
import pt.lsts.imc4j.msg.AlignmentState.STATE;
import pt.lsts.imc4j.msg.CompassCalibration;
import pt.lsts.imc4j.msg.CompassCalibration.DIRECTION;
import pt.lsts.imc4j.msg.EntityParameter;
import pt.lsts.imc4j.msg.Goto;
import pt.lsts.imc4j.msg.GpsFix;
import pt.lsts.imc4j.msg.GpsFix.VALIDITY;
import pt.lsts.imc4j.msg.PlanControlState;
import pt.lsts.imc4j.msg.PlanControlState.LAST_OUTCOME;
import pt.lsts.imc4j.msg.PlanSpecification;
import pt.lsts.imc4j.msg.PopUp;
import pt.lsts.imc4j.msg.PopUp.FLAGS;
import pt.lsts.imc4j.msg.SetEntityParameters;
import pt.lsts.imc4j.util.PojoConfig;
import pt.lsts.imc4j.util.WGS84Utilities;

public class ArpaoExecutive extends MissionExecutive {

	@Parameter
	public String[] plans = new String[] { "plan1", "plan2" };

	@Parameter
	public String host = "127.0.0.1";

	@Parameter
	public int port = 6003;
	
	@Parameter
	public double imu_align_length = 500;
	
	@Parameter
	public double imu_align_bearing = -110;
	
	@Parameter
	public boolean align_imu = false;
	
	@Parameter
	public boolean calibrate_compass = true;
	
	long time = 0;
	String plan = null;
	int plan_index = 0;
	
	public ArpaoExecutive() {
		state = this::init;
	}
	
	public State init() {
		print("init");
		if (gps() && ready()) {
			if (calibrate_compass) {
				PlanSpecification spec = ccalib();
				plan = spec.plan_id;
				time = System.currentTimeMillis();
				exec(spec);
				return compass_calib();	
			}
			else if (align_imu) {
				PlanSpecification spec = imu();
				plan = spec.plan_id;
				time = System.currentTimeMillis();
				exec(spec);
				return this::imu_align;
			}
			else {
				plan = "";
				time = System.currentTimeMillis();
				return this::plan_exec;
			}
			
		}
		else
			return this::init;
	}
	
	public State compass_calib() {
		print("compass_calib");
		
		if (System.currentTimeMillis() - time < 5000)
			return this::compass_calib;
		
		PlanControlState pcs = get(PlanControlState.class);
		
		if (pcs != null && pcs.plan_id.equals(plan) && ready()) {
			if (pcs.last_outcome == LAST_OUTCOME.LPO_SUCCESS) {
				
				if (align_imu) {
					PlanSpecification spec = imu();
					plan = spec.plan_id;
					time = System.currentTimeMillis();
					exec(spec);
					return this::imu_align;	
				}
				else {
					plan = "";
					time = System.currentTimeMillis();
					return this::plan_exec;
				}
			}
			else
				return this::init;
		}
		
		return this::compass_calib;
	}
	
	public State imu_align() {
		print("imu_align");
		if (System.currentTimeMillis() - time < 5000)
			return this::imu_align;
		
		if (imu_aligned()) {
			stopPlan();
			time = System.currentTimeMillis();
			plan = "";
			return this::plan_exec;
		}
		
		if (ready()) {
			PlanSpecification spec = imu();
			plan = spec.plan_id;
			time = System.currentTimeMillis();
			exec(spec);
			return this::imu_align;
		}
		
		return this::imu_align;
	}
	
	public State plan_exec() {
		print("plan_exec");
		if (System.currentTimeMillis() - time < 5000)
			return this::plan_exec;

		
		if (ready()) {
			PlanControlState pcs = get(PlanControlState.class);

			if (pcs.plan_id.equals(plan) && pcs.last_outcome == LAST_OUTCOME.LPO_SUCCESS)
				plan_index++;
			
			if (plan_index >= plans.length) {
				System.out.println("Finished!");
				return null;
			}
			else {
				System.out.println("Starting plan "+plans[plan_index]);
				startPlan(plans[plan_index]);
				time = System.currentTimeMillis();
			}
		}		
		
		return this::plan_exec;
	}
	
	public boolean gps() {
		GpsFix fix = get(GpsFix.class);
		return fix != null && fix.validity.contains(VALIDITY.GFV_VALID_POS);
	}
	
	public boolean imu_aligned() {
		AlignmentState state = get(AlignmentState.class);
		return state != null && state.state == STATE.AS_ALIGNED;
	}
	
	public PlanSpecification ccalib() {
		double[] pos = position();

		if (pos == null)
			return null;

		PopUp popup = new PopUp();
		popup.lat = Math.toRadians(pos[0]);
		popup.lon = Math.toRadians(pos[1]);
		popup.speed = 1;
		popup.speed_units = SpeedUnits.METERS_PS;
		popup.flags.add(FLAGS.FLG_CURR_POS);
		popup.duration = 30;
		popup.z = 0;
		popup.z_units = ZUnits.DEPTH;
		
		CompassCalibration ccalib = new CompassCalibration();
		ccalib.lat = Math.toRadians(pos[0]);
		ccalib.lon = Math.toRadians(pos[1]);
		ccalib.speed = 1;
		ccalib.speed_units = SpeedUnits.METERS_PS;
		ccalib.direction = DIRECTION.LD_CLOCKW;
		ccalib.amplitude = 0;
		ccalib.z = 0;
		ccalib.z_units = ZUnits.DEPTH;
		ccalib.duration = 60;
		ccalib.radius = 15;
		
		return spec(popup, ccalib);
	}
	
	public PlanSpecification imu() {
		double[] pos = position();

		if (pos == null)
			return null;

		PopUp popup = new PopUp();
		popup.lat = Math.toRadians(pos[0]);
		popup.lon = Math.toRadians(pos[1]);
		popup.speed = 1;
		popup.speed_units = SpeedUnits.METERS_PS;
		popup.flags.add(FLAGS.FLG_CURR_POS);
		popup.duration = 30;
		popup.z = 0;
		popup.z_units = ZUnits.DEPTH;
		
		double offsetX = Math.cos(Math.toRadians(imu_align_bearing)) * 40;
		double offsetY = Math.cos(Math.toRadians(imu_align_bearing)) * 40;
		
		double[] loc1 = WGS84Utilities.WGS84displace(pos[0], pos[1], 0, offsetX, offsetY, 0);
		Goto man1 = new Goto();
		man1.lat = Math.toRadians(loc1[0]);
		man1.lon = Math.toRadians(loc1[1]);
		man1.speed = 1;
		man1.speed_units = SpeedUnits.METERS_PS;
		man1.z = 0;
		man1.z_units = ZUnits.DEPTH;
		
		offsetX = Math.cos(Math.toRadians(imu_align_bearing)) * imu_align_length;
		offsetY = Math.cos(Math.toRadians(imu_align_bearing)) * imu_align_length;
		
		double[] loc2 = WGS84Utilities.WGS84displace(pos[0], pos[1], 0, offsetX, offsetY, 0);
		Goto man2 = new Goto();
		man2.lat = Math.toRadians(loc2[0]);
		man2.lon = Math.toRadians(loc2[1]);
		man2.speed = 1;
		man2.speed_units = SpeedUnits.METERS_PS;
		man2.z = 0;
		man2.z_units = ZUnits.DEPTH;
		
		Goto man3 = new Goto();
		man3.lat = Math.toRadians(pos[0]);
		man3.lon = Math.toRadians(pos[1]);
		man3.speed = 1;
		man3.speed_units = SpeedUnits.METERS_PS;
		man3.z = 0;
		man3.z_units = ZUnits.DEPTH;
		
		PlanSpecification spec = spec(popup, man1, man2, man3);
		// Activate IMU on second goto
		SetEntityParameters params = new SetEntityParameters();
		params.name = "IMU";
		EntityParameter param = new EntityParameter();
		param.name = "Active";
		param.value = "true";
		params.params.add(param);
		spec.maneuvers.get(2).start_actions.add(params);
		
		return spec;
	}
	
	public static void main(String[] args) throws Exception {
		ArpaoExecutive exec = PojoConfig.create(ArpaoExecutive.class, args);
		exec.connect(exec.host, exec.port);
		exec.join();		
	}
}