import java.util.ArrayList;
import java.util.List;


public class PowerMeasureMain {


	public static final String TEXT_HELP = "Invalid arguments. Please supply number of samples to take at once/second.";
	public static final String TEXT_PROGRESS_FORMAT = "Current(W) %04d: A15:%08.3f, A7:%08.3f, GPU:%08.3f, MEM:%08.3f, FPS:%d";
	public static final String TEXT_TOTAL_FORMAT =    "Total  (J) %04d: A15:%08.3f, A7:%08.3f, GPU:%08.3f, MEM:%08.3f, FPS:%d\n";
	public static final String TEXT_INDEFINITE_SAMPLING = "Now sampling indefinitely at once/sec.";
	public static final String TEXT_SAMPLES_REQUIRED = "Going for %d sample(s) at once/second";


	public static final String TEXT_FINAL_INDV_FORMAT = "Total(J): A15: %.2f, A7: %.2f, GPU: %.2f, MEM: %.2f\n";
	public static final String TEXT_FINAL_POWER = "Total Power used over %d samples: %.2fJ\n";
	public static final String TEXT_FINAL_FPS = "FPS over %d samples, Average: %d, SD: %.2f, Min: %d, Max: %d";

	public static final long SAMPLE_RATE = 1000;



	public static final long INDEFINITE_SAMPLING = -1;
	private static long totalSamplesRequired = INDEFINITE_SAMPLING;

	private static final float TIME_INTERVAL_NANO_SECONDS = 1000000000;


	private static double totalA7Power = 0;
	private static double totalA15Power = 0;
	private static double totalGPUPower = 0;
	private static double totalMemPower = 0;

	private static int numSamples = 0;

	private static int minFPS = Integer.MAX_VALUE;
	private static int maxFPS = Integer.MIN_VALUE;

	private static List<Integer> fpsData;

	private static long totalFPS = 0;
	private static int numFPSSamples = 0;


	public static void main(String[] args) {

		if(args.length > 0){
			try{
				totalSamplesRequired = Long.parseLong(args[0]);
				if(totalSamplesRequired < 0){
					throw new NumberFormatException();
				}

				printToScreen(String.format(TEXT_SAMPLES_REQUIRED, totalSamplesRequired));
			} catch (NumberFormatException e){
				printToScreen(TEXT_HELP);
				return;
			}

		} else {
			printToScreen(TEXT_INDEFINITE_SAMPLING);
		}

		fpsData = new ArrayList<Integer>();



		InitADB.initADB();


		while(shouldContinueSampling()){


			double currentA15Power = CPUStatsRetrieval.getA15Power();
			double currentA7Power = CPUStatsRetrieval.getA7Power();
			double currentGPUPower = GPUStatsRetrieval.getGPUPower();
			double currentMEMPower = MemStatsRetrieval.getMemPower();

			int fps = FPSRetrieval.getFPS(TIME_INTERVAL_NANO_SECONDS);

			if(fps != FPSRetrieval.NO_FPS_CALCULATED){
				numSamples++;
				fpsData.add(fps);
				totalFPS += fps;
				numFPSSamples++;

				if(fps < minFPS){
					minFPS = fps;
				}

				if(fps > maxFPS){
					maxFPS = fps;
				}
			} else {
				continue;
			}

			int averageFPS = getAverageFPS();


			totalA15Power += currentA15Power;
			totalA7Power += currentA7Power;
			totalGPUPower += currentGPUPower;
			totalMemPower += currentMEMPower;

			String current = String.format(TEXT_PROGRESS_FORMAT, numSamples, currentA15Power, currentA7Power, currentGPUPower, currentMEMPower, fps);
			String total = String.format(TEXT_TOTAL_FORMAT, numSamples, totalA15Power, totalA7Power, totalGPUPower, totalMemPower, averageFPS);

			printToScreen(current);
			printToScreen(total);
			try {
				Thread.sleep(SAMPLE_RATE);
			} catch (InterruptedException e) {
			}

		}



		double totalPower = totalA15Power + totalA7Power + totalGPUPower + totalMemPower;


		String indvPowerString = String.format(TEXT_FINAL_INDV_FORMAT, totalA15Power, totalA7Power, totalGPUPower, totalMemPower);
		String powerString = String.format(TEXT_FINAL_POWER, numSamples, totalPower);


		int averageFPS = getAverageFPS();
		double sdFPS = getStandardDeviation(fpsData);

		String fpsString = String.format(TEXT_FINAL_FPS, numSamples, averageFPS, sdFPS, minFPS, maxFPS);

		printToScreen("\n");
		printToScreen(indvPowerString);
		printToScreen(powerString);
		printToScreen(fpsString);



	}


	public static double getStandardDeviation(List<Integer> values) {
		double deviation = 0.0;
		if ((values != null) && (values.size() > 1)) {
			double mean = getAverageFPS();
			for (int value : values) {
				double delta = value-mean;
				deviation += delta*delta;
			}
			deviation = Math.sqrt(deviation/values.size());
		}
		return deviation;
	}

	public static int getAverageFPS(){
		int averageFPS;
		if(numFPSSamples == 0){
			averageFPS = 0;
		} else {
			averageFPS = (int) (totalFPS / numFPSSamples);
		}

		return averageFPS;
	}

	public static boolean shouldContinueSampling(){

		if(totalSamplesRequired == INDEFINITE_SAMPLING){
			return true;
		}

		if(numSamples >= totalSamplesRequired){
			return false;
		} else {
			return true;
		}

	}

	public static void printToScreen(String output){
		System.out.println(output);
	}












}
