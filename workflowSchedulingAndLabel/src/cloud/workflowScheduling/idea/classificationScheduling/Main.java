package cloud.workflowScheduling.idea.classificationScheduling;

import java.io.*;
import java.math.*;
import java.util.*;

import org.apache.commons.math3.stat.*;


import cloud.workflowScheduling.idea.subDMethods.*;
import cloud.workflowScheduling.methods.*;
import cloud.workflowScheduling.setting.*;

/*
 * �㷨���ܱȽ�
 * Please download the DAX workflow archive from 
 * https://download.pegasus.isi.edu/misc/SyntheticWorkflows.tar.gz, 
 * unzip it and keep the DAX workflows in an appropriate position before running.
 */
public class Main {
	// because in Java float numbers can not be precisely stored, a very small
	// number E is added before testing whether deadline is met
	public static final double E = 0.0000001;

	private static double DF_START = 1, DF_INCR = 1, DF_END = 10;
	
	private static final int REPEATED_TIMES = 10;
	private static final int FILE_INDEX_MAX = 200; //ÿ��������100��xml
	private static final int[] SIZES = {30, 50, 100, 1000}; //30, 50, 100, 1000
	private static int WorkflowNum = 250; //ÿ��size�Ĺ���������

	//�㷨new֮��һֱ���ã���Ա������ֵ�ᱣ����һ�ε�ִ�У����Խ����㷨���ȳ�ʼ����Ա����
	//new ClassifySchedule(), new Method1uRank(), new Method2ProLiS(), new Method6(), new Method3(), new ICPCP(), new ProLiS(1.5), new PSO()
	private static final Scheduler[] METHODS = {new ClassifySchedule(), new Method1uRank(), new Method2ProLiS(), new Method6(), new Method3(), new ICPCP(), new ProLiS(1.5), new PSO()}; 
	//"CyberShake","Epigenomics","Inspiral", "Montage", "Sipht"
	private static final String[] WORKFLOWS = {"CyberShake","Epigenomics","Inspiral", "Montage", "Sipht"};

	static final String WORKFLOW_LOCATION = "E:\\0Work\\1Ideas\\workflowClassificationAndScheduling\\workflowScheduling\\workflowGenerator\\WorkflowGenerator\\bharathi\\generatedWorkflows";
	static final String OUTPUT_LOCATION = ".\\result1";
	static final String ExcelFilePath = ".\\result1\\solution.xls";
	public static ExcelManage em;
	public static String sheetName;
	public static boolean isPrintExcel = false; // true false

	public static HashMap<String, HashMap<Integer, Double>> type2subD = new HashMap<String, HashMap<Integer, Double>>();
	public static Map<String, Integer> workflow2lable = new LinkedHashMap<String, Integer>();
	public static List<Double> deadlines = new ArrayList<Double>();
	public static List<Double> deadlineFactors = new ArrayList<Double>();
	public static HashMap<String, String> classifyResult = new HashMap<String, String>();
	//ÿһ�ֹ�������ÿһ��size��Ӧ��minCost��maxCost
	public static HashMap<String, List<Double>> minMaxCost = new LinkedHashMap<String, List<Double>>();
	
	static boolean isCalMinMaxCost = false; //�Ƿ����MinMaxCost��true���㣬false���ļ���ȡ����õ�
//	public static boolean isCallClassifyMethod = true; // �Ƿ���÷����㷨
	public static boolean onlyCalCostOfFeasible = true; //�Ƿ����������н��cost��֮ǰ�ܵĶ���false
	
	public static BufferedWriter bwUpdate = null;
	
	public static void main(String[] args) throws Exception {
		if(isCalMinMaxCost)
			calMinMaxCost();
		else {
		String minMaxFile = "minMaxCost2.txt";	
		readMinMaxCost(minMaxFile);
		bwUpdate = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\update_minMaxCost.txt"));
		bwUpdate.close();
		
		//����ÿ�����͹�������ÿ��size�ڲ�ͬ�����µĽ��������Cyber��size30����ͬ�����ڲ�ͬdeadline�ϵĽ��
//		//WorkflowNum = FILE_INDEX_MAX;
//		List<List<String>> workflowList = new ArrayList<List<String>>();
//		for(int si = 0; si < SIZES.length; si++) {
//			int size = SIZES[si];
//			List<String> w = new ArrayList<String>();
//			for(int fi = 0;fi<FILE_INDEX_MAX;fi++) {
//				w.add(WORKFLOWS[0] +"_"+ size + "." + fi + ".xml");
//			}
//			workflowList.add(w);
//		}
		
		//�������ÿ��size�Ĺ�����
//		selectWorkflows(FILE_INDEX_MAX, WorkflowNum); 
		//��ȡ������ɵĹ�����
		List<List<String>> workflowList = new ArrayList<List<String>>();
		for(int si = 0; si < SIZES.length; si++) {
			List<String> w = new ArrayList<String>();
			String fileName = OUTPUT_LOCATION + "\\" + "selectedWorkflows_" + SIZES[si] + ".txt";
			w = getWorkflowListFromFile(fileName);
			workflowList.add(w);
		}
		
		//��ȡ������
		String crFile = "E:\\0Work\\1Ideas\\workflowClassificationAndScheduling\\DAGclassificationWithTT-test\\"
							+ "��result-wen+����+����\\epoch199_classificationResult.txt";	
//		String crFile = "E:\\0Work\\1Ideas\\workflowClassificationAndScheduling\\workflowScheduling\\"
//							+ "workflow2subDLable\\result\\repeat\\workflow2label2_200.txt";
		getClassifyResultFromFile(crFile);
//		
		if (isPrintExcel)
			ExcelManage.clearExecl(ExcelFilePath);
		int deadlineNum = (int) ((DF_END - DF_START) / DF_INCR + 1);
		for (int si = 0; si < SIZES.length; si++) {
			int size = SIZES[si];
			double[][][] successResult = new double[deadlineNum][METHODS.length][REPEATED_TIMES*WorkflowNum];
			double[][][] NCResult = new double[deadlineNum][METHODS.length][REPEATED_TIMES*WorkflowNum];
			double[] refValues = new double[4]; // store cost and time of fastSchedule and cheapSchedule
			
			int ith = 0;
			for(String workflowFile : workflowList.get(si)) {
				String file = WORKFLOW_LOCATION + "\\" + workflowFile;
				for (int di = 0; di <= (DF_END - DF_START) / DF_INCR; di++) { // deadline index
					for (int timeI = 0; timeI < REPEATED_TIMES; timeI++) {
						test(file, di, timeI, ith, successResult, NCResult, refValues);
					}
				}
				ith++;
			}	
			BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\size" + size + ".txt"));
			bw.write("used methods: ");
			for (Scheduler s : METHODS)
				bw.write(s.getClass().getSimpleName() + "\t");
			bw.write("\r\n\r\n");
			printTo(bw, successResult, "success ratio");
			if(onlyCalCostOfFeasible)
				printCostOfFeasibleTo(bw, successResult, NCResult, "normalized cost"); //����REPEATED_TIMES��ĳЩ�ο��н��ƽ��cost
			else
				printTo(bw, NCResult, "normalized cost");

			bw.write("reference values (CF, MF, CC, MC)\r\n");
			double divider = SIZES.length * FILE_INDEX_MAX * deadlineNum;
			for (double refValue : refValues)
				bw.write(refValue / divider + "\t");
			bw.close();
		}
		}
	}
	
	//����minCost��maxCost
	private static void calMinMaxCost() throws IOException {
		int deadlineNum = (int) ((DF_END - DF_START) / DF_INCR + 1);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\minMaxCost.txt", true));
		bw.close();
		for (String workflow : WORKFLOWS) {
			for (int si = 0; si < SIZES.length; si++) {
				int size = SIZES[si];
				Main.minMaxCost.put(workflow +"_"+ size, new ArrayList<Double>());
				Main.minMaxCost.get(workflow +"_"+ size).add(Double.MAX_VALUE);
				Main.minMaxCost.get(workflow +"_"+ size).add(Double.MIN_VALUE);
				bw = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\minMaxCost.txt", true));
				for(int fi = 0;fi<FILE_INDEX_MAX;fi++){
					String file = WORKFLOW_LOCATION + "\\" + workflow +"_"+ size + "." + fi + ".xml";
					for (int di = 0; di <= (DF_END - DF_START) / DF_INCR; di++) { // deadline index
						for (int timeI = 0; timeI < REPEATED_TIMES; timeI++) {
							testForMinMaxCost(file, di);
						}
					}
				}
				
				bw.write(workflow +"_"+ size+", ");
				for(Double in : Main.minMaxCost.get(workflow +"_"+ size)) {
					bw.write(in.doubleValue() + ", ");
				}
				bw.write("\r\n");
				bw.close();
			}
		}
			
//		BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\minMaxCost.txt"));
//		Set<Map.Entry<String,List<Double>>> entrySet = Main.minMaxCost.entrySet();
//		Iterator<Map.Entry<String,List<Double>>> it = entrySet.iterator();
//		while(it.hasNext())
//		{
//			Map.Entry<String,List<Double>> me = it.next();
//			bw.write(me.getKey()+", ");
//			for(Double in : me.getValue()) {
//				bw.write(in.doubleValue() + ", ");
//			}
//			bw.write("\r\n");
//		}
//		bw.close();
	}

	//���ļ��ж�ȡminCost��maxCost
	private static void readMinMaxCost(String fileName) throws IOException {
		String str = null;
		FileInputStream fis = new FileInputStream(OUTPUT_LOCATION + "\\" + fileName);
		InputStreamReader isr = new InputStreamReader(fis);// InputStreamReader ���ֽ���ͨ���ַ���������,
		BufferedReader br = new BufferedReader(isr);
		try
		{
			while ((str = br.readLine()) != null) {
				String[] s = str.split(", ");
//				System.out.println(s[0]);
				Main.minMaxCost.put(s[0], new ArrayList<Double>());
				Main.minMaxCost.get(s[0]).add(Double.parseDouble(s[1]));
				Main.minMaxCost.get(s[0]).add(Double.parseDouble(s[2]));
			}			
			fis.close();
			isr.close();
			br.close();
		}catch(IOException e){System.out.println(e.getMessage());}		
	}
	
	/**
	 * @param file	xml���ڵ��ļ�
	 * @param di	��di��deadline���
	 * @param fi	�ظ�ִ��n�Σ����ǵ�fi��
	 * @param si	task size
	 */
	private static void testForMinMaxCost(String file, int di) {
		// ����file�ļ��еĹ�����
		Workflow wf = new Workflow(file);
	
		Benchmarks benSched = new Benchmarks(wf); // ��õ�ǰ������������Benchmark�⣬Ϊ�˼���max min��deadline
		System.out.print("Benchmark-FastSchedule��" + benSched.getFastSchedule());
		System.out.print("Benchmark-CheapSchedule��" + benSched.getCheapSchedule());
		System.out.print("Benchmark-MinCost8Schedule��" + benSched.getMinCost8Schedule());
	
		// ��ǰ��deadline = min+ (max-min)*deadlineFactor
		double deadlineFactor = 0;
	//	if(di == 0)
	//		deadlineFactor = 1.5;
	//	else
			deadlineFactor = DF_START + DF_INCR * di;
		double deadline = benSched.getFastSchedule().calcMakespan() * deadlineFactor;
		System.out.println("deadlineFactor=" + String.format("%.3f", deadlineFactor) + ", deadline = "
				+ String.format("%.3f", deadline));
		System.out.println();
		
		for (int mi = 0; mi < METHODS.length; mi++) { // method index
			Workflow wf1 = new Workflow(file);
			Scheduler method = null;
			method = METHODS[mi];
			wf1.setDeadline(deadline);
			
			System.out.println("�����㷨The current algorithm: " + method.getClass().getCanonicalName());
	
			// �����㷨
			long starTime = System.currentTimeMillis();
			Solution sol = method.schedule(wf1);
	
			long endTime = System.currentTimeMillis();
			double runTime = (double) (endTime - starTime);
	
			String methodName = method.getClass().getName().substring(33);
			if (isPrintExcel)
				em.writeToExcel(ExcelFilePath, sheetName, deadlineFactor, deadline, methodName, mi, sol);
	
			if (sol == null) {
				System.out.println("solution is " + sol + "!\r\n");
				continue;
			}
			int isSatisfied = sol.calcMakespan() <= deadline + E ? 1 : 0;
			List<Integer> result = sol.validateId(wf1);
			if (result.get(0).intValue() == 0) {
				if (isPrintExcel)
					em.writeToExcel(ExcelFilePath, sheetName, result.get(1).intValue(), result.get(0).intValue());
				throw new RuntimeException();
			}
			System.out.println("runtime��" + runTime + "ms;   solution: " + sol);
	
			String fileName = file.substring(file.lastIndexOf("\\")+1, file.indexOf("."));
			if(isSatisfied == 1) {
				double cost = sol.calcCost();
				if(cost < Main.minMaxCost.get(fileName).get(0).doubleValue())
					Main.minMaxCost.get(fileName).set(0, Double.valueOf(cost));
				if(cost > Main.minMaxCost.get(fileName).get(1).doubleValue())
					Main.minMaxCost.get(fileName).set(1, Double.valueOf(cost));
			}
	//		sol.calcCost() / benSched.getCheapSchedule().calcCost();
			
		}
	}

	/**
	 * ����file������(�ڵ�si��size�£�fileIndex��)��deadlineΪdi�ɽ���ʱ���㷨�Ľ�����ɹ��ʣ���׼cost��
	 * 
	 * @param file
	 *            xml���ڵ��ļ�
	 * @param di
	 *            ��di��deadline���
	 * @param fi
	 *            �ظ�ִ��n�Σ����ǵ�fi��
	 * @param si
	 *            task size
	 * @param successResult
	 *            ��di�¸��㷨�ĳɹ���
	 * @param NCResult
	 *            normalized cost
	 * @param refValues
	 * @throws IOException 
	 */
	private static void test(String file, int di, int fi, int si, double[][][] successResult, double[][][] NCResult,
			double[] refValues) throws IOException {
		// ����file�ļ��еĹ�����
		Workflow wf = new Workflow(file);

		Benchmarks benSched = new Benchmarks(wf); // ��õ�ǰ������������Benchmark�⣬Ϊ�˼���max min��deadline
		System.out.print("Benchmark-FastSchedule��" + benSched.getFastSchedule());
		System.out.print("Benchmark-CheapSchedule��" + benSched.getCheapSchedule());
		System.out.print("Benchmark-MinCost8Schedule��" + benSched.getMinCost8Schedule());
//		double fastestCost = benSched.getFastSchedule().calcCost();
//		double fastestMakespan = benSched.getFastSchedule().calcMakespan();
//		double slowestCost = benSched.getCheapSchedule().calcCost();
//		double slowestMakespan = benSched.getCheapSchedule().calcMakespan();

		// ��ǰ��deadline = min+ (max-min)*deadlineFactor
		double deadlineFactor = 0;
//		if(di == 0)
//			deadlineFactor = 1.5;
//		else
			deadlineFactor = DF_START + DF_INCR * di;
		double deadline = benSched.getFastSchedule().calcMakespan() * deadlineFactor;
//		double deadline = benSched.getFastSchedule().calcMakespan()
//				+ (benSched.getCheapSchedule().calcMakespan() - benSched.getFastSchedule().calcMakespan())
//						* deadlineFactor;
		System.out.println("deadlineFactor=" + String.format("%.3f", deadlineFactor) + ", deadline = "
				+ String.format("%.3f", deadline));
		System.out.println();
		for (int mi = 0; mi < METHODS.length; mi++) { // method index
			Workflow wf1 = new Workflow(file);

			Scheduler method = METHODS[mi];
			String methodName = method.getClass().getCanonicalName();
			methodName = methodName.substring(methodName.lastIndexOf(".")+1);
//			if(Main.isCallClassifyMethod && mi == 0) { //�ҵķ�������㷨
			if(methodName.equals("ClassifySchedule")) { //�ҵķ�������㷨
				int a = file.lastIndexOf("\\");
				String key = file.substring(a) + "d" + deadlineFactor;
				String cResult = Main.classifyResult.get(key);
//				if(cResult.equals("1"))
//					method = new Method1uRank();
//				else if(cResult.equals("2"))
//					method = new Method2ProLiS();
//				else if(cResult.equals("3"))
//					method = new Method6();
//				else if(cResult.equals("4"))
//					method = new Method3();
//				else {
//					System.out.println("��Ч�ķ�����");
//					System.exit(0);
//				}
				method = new ClassifySchedule(cResult);
			}
//			else
//				method = METHODS[mi];
			wf1.setDeadline(deadline);
			wf1.setDeadlineFactor(deadlineFactor); // ΪHGSA����
			
			System.out.println("�����㷨The current algorithm: " + method.getClass().getCanonicalName());

			// �����㷨
			long starTime = System.currentTimeMillis();
			Solution sol = method.schedule(wf1);

			long endTime = System.currentTimeMillis();
			double runTime = (double) (endTime - starTime);

//			String methodName = method.getClass().getName().substring(33);
			if (isPrintExcel)
				em.writeToExcel(ExcelFilePath, sheetName, deadlineFactor, deadline, methodName, mi, sol);

			if (sol == null) {
				System.out.println("solution is " + sol + "!\r\n");
				continue;
			}
			int isSatisfied = sol.calcMakespan() <= deadline + E ? 1 : 0;
			List<Integer> result = sol.validateId(wf1);
			if (result.get(0).intValue() == 0) {
				if (isPrintExcel)
					em.writeToExcel(ExcelFilePath, sheetName, result.get(1).intValue(), result.get(0).intValue());
				throw new RuntimeException();
			}
			System.out.println("runtime��" + runTime + "ms;   solution: " + sol);

			String fileName = file.substring(file.lastIndexOf("\\")+1, file.indexOf("."));
			successResult[di][mi][fi + si* REPEATED_TIMES] += isSatisfied;
//			NCResult[di][mi][fi + si* REPEATED_TIMES] += sol.calcCost() / benSched.getCheapSchedule().calcCost();
//			NCResult[di][mi][fi + si* REPEATED_TIMES] += Math.abs(sol.calcCost()-slowestCost)/(Math.abs(fastestCost-slowestCost));
			double cost = (sol.calcCost()-Main.minMaxCost.get(fileName).get(0))
					/(Main.minMaxCost.get(fileName).get(1)-Main.minMaxCost.get(fileName).get(0));
			if(onlyCalCostOfFeasible) {
				if(isSatisfied == 1)
					NCResult[di][mi][fi + si*REPEATED_TIMES] += cost;
				else
					NCResult[di][mi][fi + si*REPEATED_TIMES] += 0;
			}
			else
				NCResult[di][mi][fi + si* REPEATED_TIMES] += cost;
			if(isSatisfied == 1) {
				if(cost < 0) { //˵������˸�С�Ľ��
					bwUpdate = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\update_minMaxCost.txt", true));
					bwUpdate.write(fileName+", min"+  Main.minMaxCost.get(fileName).get(0)+ "==> "+ sol.calcCost());
					bwUpdate.close();
				}
				if(sol.calcCost() > Main.minMaxCost.get(fileName).get(1)) {
					bwUpdate = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\update_minMaxCost.txt", true));
					bwUpdate.write(fileName+", max"+  Main.minMaxCost.get(fileName).get(1)+ "==> "+ sol.calcCost());
					bwUpdate.close();
				}
			}
		}
		
		refValues[0] += benSched.getFastSchedule().calcCost();
		refValues[1] += benSched.getFastSchedule().calcMakespan();
		refValues[2] += benSched.getCheapSchedule().calcCost();
		refValues[3] += benSched.getCheapSchedule().calcMakespan();
	}

	private static final java.text.DecimalFormat df = new java.text.DecimalFormat("0.000000");
	static {
		df.setRoundingMode(RoundingMode.HALF_UP);
	}

	private static void printTo(BufferedWriter bw, double[][][] result, String resultName) throws Exception {
		bw.write(resultName + "\r\n");
		for (int di = 0; di <= (DF_END - DF_START) / DF_INCR; di++) {
			String text = df.format(DF_START + DF_INCR * di) + "\t";
			for (int mi = 0; mi < METHODS.length; mi++)
				text += df.format(StatUtils.mean(result[di][mi])) + "\t";
			bw.write(text + "\r\n");
			bw.flush();
		}
		bw.write("\r\n\r\n\r\n");
	}
	private static void printCostOfFeasibleTo(BufferedWriter bw, double[][][] successResult, double[][][] NCResult, String resultName)throws Exception{
		bw.write(resultName + "\r\n");
		for(int di = 0;di<=(DF_END-DF_START)/DF_INCR;di++){
			String text = df.format(DF_START + DF_INCR * di) + "\t";
			for(int mi=0;mi<METHODS.length;mi++) {
				text += df.format(StatUtils.sum(NCResult[di][mi])/StatUtils.sum(successResult[di][mi])) + "\t";
			}
			bw.write(text + "\r\n");
			bw.flush();
		}
		bw.write("\r\n\r\n\r\n");
	}

	private static void printSubDTo(BufferedWriter bw, Workflow w) throws Exception {
		// bw.write("\t");
		// for(Task t : w){
		// bw.write(String.format("%d", t.getId()) + ", ");
		// }
		// bw.write("\r\n\t");
		for (Task t : w) {
			bw.write(String.format("%.3f", t.getSubD()) + ", ");
		}
		bw.write("\r\n");
		bw.flush();
	}

	private static void printRunTime(BufferedWriter bw, Workflow w) throws Exception {

		for (Task t : w) {
			bw.write(String.valueOf(t.getId()));
			bw.write("\t");
			bw.write(String.valueOf(t.getTaskSize() / 4.4) + "\t");
			bw.write("\r\n");
		}
		bw.flush();
	}

	private static void printTT(BufferedWriter bw, Workflow w) throws Exception {
		int size = w.size();
		double[][] tt = new double[size][size];
		for (Task t : w) {
			int pId = t.getId();
			for (Edge e : t.getOutEdges()) {
				Task c = e.getDestination();
				int cId = c.getId();
				tt[pId][cId] = e.getDataSize() * 1.0 / VM.NETWORK_SPEED;
			}
		}

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				bw.write(String.valueOf(tt[i][j]) + "\t");
			}
			bw.write("\r\n");
		}
		bw.flush();
	}

	private static void readOptimizedSubD(String filePath) {
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null; // ���ڰ�װInputStreamReader,��ߴ������ܡ���ΪBufferedReader�л���ģ���InputStreamReaderû�С�
		try {
			String str = "";
//			String str1 = "";
			String[] s, s1, s2;
			fis = new FileInputStream(filePath);// FileInputStream
			// ���ļ�ϵͳ�е�ĳ���ļ��л�ȡ�ֽ�
			isr = new InputStreamReader(fis);// InputStreamReader ���ֽ���ͨ���ַ���������,
			br = new BufferedReader(isr);// ���ַ��������ж�ȡ�ļ��е�����,��װ��һ��new InputStreamReader�Ķ���
			while ((str = br.readLine()) != null) {
//				str1 += str + "\n";
				HashMap<Integer, Double> task2SubD = new HashMap<Integer, Double>();
				s = str.split("\t");
				s1 = s[1].split(", ");
				for(int i = 0; i < s1.length; i ++) {
					s2 = s1[i].split(": ");
					task2SubD.put(Integer.valueOf(s2[0]), Double.valueOf(s2[1]));
				}
				type2subD.put(s[0].trim(), task2SubD);
			}
//			System.out.println(str1);// ��ӡ��str1
		} catch (FileNotFoundException e) {
			System.out.println("�Ҳ���ָ���ļ�");
		} catch (IOException e) {
			System.out.println("��ȡ�ļ�ʧ��");
		} finally {
			try {
				br.close();
				isr.close();
				fis.close();
				// �رյ�ʱ����ð����Ⱥ�˳��ر���󿪵��ȹر������ȹ�s,�ٹ�n,����m
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * �����ɵĹ����������ѡ��ʹ�õĹ�����
	 * @param eachSizeNum ������size��30��50��100��1000���֣�ÿ�ֵĸ���
	 * @param eachSizeSelNum ������size��30��50��100��1000���֣�ÿ�ֱ�ѡ��ĸ���
	 * @throws IOException 
	 */
	private static void selectWorkflows(int eachSizeNum, int eachSizeSelNum) throws IOException {
//		String[] types = { "CyberShake","Epigenomics","Inspiral", "Montage", "Sipht"};	
//		int[] sizes = { 30, 50, 100, 1000};
		
		for (int si = 0; si < SIZES.length; si++) { // size index
			List<String> selectedWorkflows = new ArrayList<String>();
			int size = SIZES[si];
			for(int num = 0; num < eachSizeSelNum; num++) {
				int typeIndex = (int)(Math.random()*Main.WORKFLOWS.length); //���ѡ��ĳ�����͵Ĺ���������CyberShake
				int fileIndex = (int)(Math.random()*eachSizeNum);
				String file = Main.WORKFLOWS[typeIndex] +"_"+ size + "." + fileIndex + ".xml";
				while(selectedWorkflows.contains(file)) {
					fileIndex = (int)(Math.random()*100);
					file = Main.WORKFLOWS[typeIndex] +"_"+ size + "." + fileIndex + ".xml";
				}
				selectedWorkflows.add(file);
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_LOCATION + "\\" + "selectedWorkflows_" + size + ".txt"));
			for(String de : selectedWorkflows)
			{
				bw.write(de);
				bw.write("\r\n");
			}
			bw.close();
		}
	}
	
	/**��ȡ���������Լ�
	 * @throws IOException 
	 * @throws ClassNotFoundException */
	public static List<String> getWorkflowListFromFile(String filename) throws IOException, ClassNotFoundException
	{
		List<String> w_List = new ArrayList<String>();
		String w = null;
		FileInputStream fis = new FileInputStream(filename);
		InputStreamReader isr = new InputStreamReader(fis);// InputStreamReader ���ֽ���ͨ���ַ���������,
		BufferedReader br = new BufferedReader(isr);
		try
		{
			for(int i=0; i< WorkflowNum; i++)
			{
				w = br.readLine();
				w_List.add(w);
			}			
			fis.close();
			isr.close();
			br.close();
		}catch(IOException e){System.out.println(e.getMessage());}		
		return w_List;
	}
	
	/**��ȡ�������ķ�����
	 * @throws IOException 
	 * @throws ClassNotFoundException */
	public static List<String> getClassifyResultFromFile(String filename) throws IOException, ClassNotFoundException
	{
		List<String> w_List = new ArrayList<String>();
		String str = null;
		FileInputStream fis = new FileInputStream(filename);
		InputStreamReader isr = new InputStreamReader(fis);// InputStreamReader ���ֽ���ͨ���ַ���������,
		BufferedReader br = new BufferedReader(isr);
		try
		{
			while ((str = br.readLine()) != null) {
				String[] s = str.split(", ");
				Main.classifyResult.put(s[0] + s[1], s[2]);
			}			
			fis.close();
			isr.close();
			br.close();
		}catch(IOException e){System.out.println(e.getMessage());}		
		return w_List;
	}
	
//	/**
//	 * ��Readfile��subD, �ֱ�д��writeFile1��writeFile2
//	 * @param Readfile
//	 * @param writeFile1
//	 * @param  
//	 */
//	private static void readWriteOptimizedSubD(String Readfile, String writeFile1, String writeFile2) {
//		FileInputStream fis = null;
//		InputStreamReader isr = null;
//		BufferedReader br = null; // ���ڰ�װInputStreamReader,��ߴ������ܡ���ΪBufferedReader�л���ģ���InputStreamReaderû�С�
//		try {
//			String str = "";
////			String str1 = "";
//			String[] s, s1, s2;
//			fis = new FileInputStream(Readfile);// FileInputStream
//			// ���ļ�ϵͳ�е�ĳ���ļ��л�ȡ�ֽ�
//			isr = new InputStreamReader(fis);// InputStreamReader ���ֽ���ͨ���ַ���������,
//			br = new BufferedReader(isr);// ���ַ��������ж�ȡ�ļ��е�����,��װ��һ��new InputStreamReader�Ķ���
//			while ((str = br.readLine()) != null) {
////				str1 += str + "\n";
//				HashMap<Integer, Double> task2SubD = new HashMap<Integer, Double>();
//				s = str.split("\t");
//				s1 = s[1].split(", ");
//				for(int i = 0; i < s1.length; i ++) {
//					s2 = s1[i].split(": ");
//					task2SubD.put(Integer.valueOf(s2[0]), Double.valueOf(s2[1]));
//				}
//				type2subD.put(s[0].trim(), task2SubD);
//			}
////			System.out.println(str1);// ��ӡ��str1
//		} catch (FileNotFoundException e) {
//			System.out.println("�Ҳ���ָ���ļ�");
//		} catch (IOException e) {
//			System.out.println("��ȡ�ļ�ʧ��");
//		} finally {
//			try {
//				br.close();
//				isr.close();
//				fis.close();
//				// �رյ�ʱ����ð����Ⱥ�˳��ر���󿪵��ȹر������ȹ�s,�ٹ�n,����m
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//	}
	
}