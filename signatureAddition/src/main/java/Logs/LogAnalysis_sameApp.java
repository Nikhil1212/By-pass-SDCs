package Logs;
/**
 * This class has a method that launches the app two times, and finds out the disjoint tags with the help of other methods
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Scanner;

import signatureAddition.StartingPoint;
import signatureAddition.pastHardwork.AnalysingJSON;
import signatureAddition.pastHardwork.printLogsThroughPID;
import signatureAddition.*;

public class LogAnalysis_sameApp {
	public static 	String pathToadb="/home/nikhil/Android/Sdk/platform-tools/adb";
	static 	String pathToaapt="/home/nikhil/Android/Sdk/build-tools/27.0.3/aapt";
	public static String toastKilled="Toast already killed"; 

	public static HashSet<String> sameAppTwoTimesLogAnalysis(String pathToOriginalApk) throws Exception, InterruptedException {

		HashSet<String> hsDisjointTagsFromOriginalApps;
		String packageName=StartingPoint.getPackageName(pathToOriginalApk);

		String logPathForOriginalApp="/home/nikhil/Documents/apps/logcatOutput/sameApp/original_"+packageName+".txt";
		String logPathForOriginalApp2="/home/nikhil/Documents/apps/logcatOutput/sameApp/original2_"+packageName+".txt";

		String fileContents1=	LogAnalysis.appLogGeneration(pathToOriginalApk,logPathForOriginalApp);
		printLogsThroughPID.initializationADB();
		//	restartSmartphone.restart();
		String fileContents2=	LogAnalysis.appLogGeneration(pathToOriginalApk,logPathForOriginalApp2);

		FileWriter fileWriter=new FileWriter(logPathForOriginalApp);
		fileWriter.write(fileContents1);
		fileWriter.close();


		fileWriter=new FileWriter(logPathForOriginalApp2);
		fileWriter.write(fileContents2);
		fileWriter.close();

		//System.out.println("Checking whether we are able to see AccountInvalidator *******************\n****************\n**************");
		/*String fileContents=new String(Files.readAllBytes(Paths.get(logPathForResignedApp)));
				System.out.println(fileContents);
				if(modifedCount==-1)
					continue;
				printLogsThroughPID.initializationADB();
		 */

		String orignalLogJSONPath=removeDuplicateLogsStatement.removeduplicateLogs(logPathForOriginalApp);
		String orignalLogJSONPath2=removeDuplicateLogsStatement.removeduplicateLogs(logPathForOriginalApp2);

		hsDisjointTagsFromOriginalApps=AnalysingJSON.analyseJSONSameApps(orignalLogJSONPath, orignalLogJSONPath2,packageName);//, modifiedLogJSONPath);

		return hsDisjointTagsFromOriginalApps;
	}






	private static void checkActiviyNameLogs(String packageName, String logPathForOriginalApp,
			String logPathForResignedApp) throws FileNotFoundException, SQLException {

		HashSet<String> activiyOriginalHashSet=FetchActivity.fetchActivity(logPathForOriginalApp, packageName);
		System.out.println("from original app : "+activiyOriginalHashSet);
		HashSet<String> activiyRepckagedHashSet=FetchActivity.fetchActivity(logPathForResignedApp, packageName);
		System.out.println("from repackaged app : "+activiyRepckagedHashSet);

		if (activiyRepckagedHashSet.containsAll(activiyOriginalHashSet))
		{
			System.out.println("Oh No different activity names has been found on the original and repackaged app");
		}
		else
		{
			System.out.println("Different set of activities. There is high chance that anti-tampering check is present.");
			updateAntiRepackagingCheckPresence(packageName,'Y',"Different Activity Observed");
		}
		// TODO Auto-generated method stub

	}

	private static void checkToastLogs(String packageName,String logPathForOriginalApp, String logPathForResignedApp) throws SQLException, IOException {
		// TODO Auto-generated method stub
		String originalLogContents=new String(Files.readAllBytes(Paths.get(logPathForOriginalApp))); 
		String resignedLogContents=new String(Files.readAllBytes(Paths.get(logPathForResignedApp))); 
		if(!originalLogContents.contains(toastKilled) && resignedLogContents.contains(toastKilled))
		{
			updateAntiRepackagingCheckPresence(packageName,'Y',"Toast Message");
		}
	}

	private static String generatingModifiedApk(String packageName, String pathToOriginalApk) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		String pathToDisAssembleCode="/home/nikhil/Documents/apps/"+packageName;

		System.out.println(packageName);

		//package name is retrieved using aapt 
		StartingPoint.disassembleApk(pathToOriginalApk,packageName);

		//Successfully disassmeble the apk with ignoring resource
		String fullRSAfetch= StartingPoint.fetchRSAName(packageName);

		String signCertificateKey=fetchCertificateKey.getCertificateInHex(fullRSAfetch, packageName);
		System.out.println(signCertificateKey);

		try {
			FileNamesForSignatureAddition.codeInjectionProcess(signCertificateKey, pathToDisAssembleCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//fetchCertificateKey.codeInjection();
		String modifiedApkPath=StartingPoint.buildApk(packageName);
		StartingPoint.signApk(packageName, modifiedApkPath);//, modifiedApkPath);
		//signApk(packageName, modifiedApkPath);

		StartingPoint.removeDirectory(pathToDisAssembleCode);
		return modifiedApkPath;
	}

	private static void updateTable(String packageName, int originalCount, int resignedCount, int modifedCount) throws SQLException {
		// TODO Auto-generated method stub
		Statement statement=DataBaseConnect.initialization();

		String query="Insert ignore into ActivityTaskManagerCount values ('"+packageName+"',"+originalCount+","+resignedCount+","+modifedCount+");";
		statement.executeUpdate(query);
	}

	private static int appLogGeneration(String pathToApkFromPC, String logPathForOutput, int flag) throws IOException, InterruptedException {
		/**
		 * It first installs the app in such a way that it has being given all the permissions
		 * and then, the app gets launched. 
		 */
		System.out.println("let's try to fetch the pid of an app from its packagename");
		String directoryLocationForStoringLogs="/home/nikhil/Documents/logs/";

		String devices=pathToadb+" devices";
		CommandExecute.commandExecution(devices);


		//String pathToApkFromPC="/home/nikhil/Documents/apks/repackagedAprb.apk";

		String packageName=StartingPoint.getPackageName(pathToApkFromPC);

		CommandExecute.commandExecution(pathToadb+" uninstall "+packageName);

		/**
		 * If the app is already being used in the smartphone, so uninstall it.
		 */

		String apkPathOnSmartphone=" /data/local/tmp/"+packageName+".apk";

		String pushApkCommand=pathToadb+" push "+pathToApkFromPC+apkPathOnSmartphone;
		String installThroughPMCommand=pathToadb+" shell pm install -g"+apkPathOnSmartphone;
		String removeApkFromDevice=pathToadb+" shell rm"+apkPathOnSmartphone;

		CommandExecute.commandExecution(pushApkCommand);

		CommandExecute.commandExecution(installThroughPMCommand);
		/**
		 * We are using pm as we want to give all the permission an app wants during the installation time
		 */

		CommandExecute.commandExecution(removeApkFromDevice);
		/**
		 * As the apk has been installed, so no use of this apk
		 */
		//Process pr=CommandExecute.commandExecution(commandToFilterLogsUsingPackageName);


		/**
		 * Let's uninstall the app
		 */

		String clearLogcat=pathToadb+" shell logcat -c";



		//CommandExecute.commandExecution(pathToadb+" install "+pathToApk);

		CommandExecute.commandExecution(clearLogcat);

		launchTheApp(packageName,pathToApkFromPC);
		/**
		 * The second arguement required is to fetch the launcher activity of the app
		 */

		String command=pathToadb+" shell pidof "+packageName;
		Process process=CommandExecute.commandExecution(command);

		BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = "";
		line=buf.readLine();
		if(line==null)
		{

			System.out.println("The app is currently not running. The app has anti-repacakging check present. There is a high chance that the app is getting crashed.");
			try {
				updateAntiRepackagingCheckPresence(packageName,'Y',"App crashed");
				return -1;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			//as in the first line only we can get the package name.That's why immeditate break;
			String pid=line;
			System.out.println("pid of the app with package name "+packageName+" is "+line);

			//System.out.println(analysingLogsUsingPID);


			storingLogOutputUsingGrepPackageName(packageName);
			storingLogOutputUsingPID(packageName,pid);

			String filePath1=directoryLocationForStoringLogs+"logs_"+packageName+"_PID.txt";
			String filePath2="/home/nikhil/Documents/logs/fromProgram"+packageName+".txt";

			/**
			 * We are doing a little modification here. So, we are trying to fetch the logs generated only from the pid. So, we are commenting few lines as we don't require filePath2
 Later down the line, if we want end to end analysis, then we have to find some other way as activity, toast related difference can't be seen using the logs generated on the pid.
			 */
			String str1=new String(Files.readAllBytes(Paths.get(filePath1)));
			//System.out.println(str1);
			String str2=new String(Files.readAllBytes(Paths.get(filePath2)));
			String fileContents=str1;
			if(flag==0)
			{
				fileContents=fileContents+str2;
			}
			//String str3=str1+str2;
			//System.out.println(str2);
			//String outputFilePath="/home/nikhil/Documents/logs/universal_"+packageName+".txt";
			FileWriter fileWriter=new FileWriter(logPathForOutput);
			fileWriter.write(fileContents);
			fileWriter.close();
			System.out.println("Successfully wrote to the file");


			String removeFile1="rm "+filePath1;
			String removeFile2="rm "+filePath2;

			CommandExecute.commandExecution(removeFile1);
			CommandExecute.commandExecution(removeFile2);
			CommandExecute.commandExecution(pathToadb+" uninstall "+packageName);
			CommandExecute.commandExecution(clearLogcat);


			return fetchActivityTaskManagerCount(logPathForOutput);
			/*
			File file2=new File(directoryLocationForStoringLogs+"_.txt");
			file2.createNewFile();*/
			//so let's remove the two files 


		}
		return -1;
	}

	public static void updateAntiRepackagingCheckPresence(String packageName, char c, String remarks) throws SQLException {
		// TODO Auto-generated method stub
		String query="Insert ignore into AntiRepackagingCheckPresence values ('"+packageName+"','"+c+"','"+remarks+"');";
		System.out.println(query);

		Statement statement=DataBaseConnect.initialization();
		statement.executeUpdate(query);
	}

	private static int fetchActivityTaskManagerCount(String outputFilePath) throws IOException, InterruptedException {

		System.out.println("Output file path is :"+outputFilePath);
		int count=0;
		File file=new File(outputFilePath);
		Scanner scanner=new Scanner(file);
		//int count=0;
		while(scanner.hasNext())
		{
			String line=scanner.next();
			if(line.contains("ActivityTaskManager"))
			{
				count++;
			}
		}
		return count;
		//String grepCommand="/bin/grep -o -i ActivityTaskManager "+outputFilePath+" | /usr/bin/wc -l";
		/*String grepCommand="grep -wc \"ActivityTaskManager\" "+ outputFilePath;

		System.out.println(grepCommand);
		String str1=new String(Files.readAllBytes(Paths.get(outputFilePath)));

		Process process=CommandExecute.commandExecution(grepCommand);

		BufferedReader buf2 = new BufferedReader(new InputStreamReader(process.getInputStream()));
			System.out.println("Before while loop");
			String line=buf2.readLine();
			System.out.println("Number of ActivityTaskManager :"+line);
			if(line==null)
			{
				System.out.println("value returned is null");
				return -1;
			}
		 */	
		//return Integer.parseInt(line);

	}

	public static void launchTheApp(String packageName, String pathToApk) throws IOException, InterruptedException {
		// TODO Auto-generated method stub

		String fetchLauncherActivity= pathToadb+" shell \"cmd package resolve-activity --brief ";	//packageName | tail -n 1\";
		fetchLauncherActivity=fetchLauncherActivity + packageName +" | tail -n 1\"";
		// strcat (fetchLauncherActivity, " | tail -n 1\"");
		System.out.println(fetchLauncherActivity);
		//String pathToApk=null;
		String fetchOutputOfAapt=pathToaapt+" dump badging "+pathToApk;
		Process  process=CommandExecute.commandExecution(fetchOutputOfAapt);
		String line="";
		String patternForLaunchableActivity="launchable-activity: name='";
		String launchableActivityCommand=pathToadb+" shell am start -n "+packageName+"/";
		BufferedReader buf2 = new BufferedReader(new InputStreamReader(process.getInputStream()));
		//BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		System.out.println("Before while loop");
		while ((line=buf2.readLine())!=null) {
			if(line.contains(patternForLaunchableActivity))
			{
				//fetch the launcher activity name
				String temp=line.substring(patternForLaunchableActivity.length());
				//we are trimming it.
				int index=temp.indexOf("'");
				launchableActivityCommand=launchableActivityCommand.concat(temp.substring(0,index));
				System.out.println(launchableActivityCommand);
				CommandExecute.commandExecution(launchableActivityCommand);
				break;
			}
			else 
				continue;
			//as in the first line only we can get the package name.That's why immeditate break;
			//	Files.write(Paths.get(filePath), (line+"\n").getBytes(),  StandardOpenOption.APPEND);
		}
		buf2.close();

		Thread.sleep(15000);
	}

	private static void storingLogOutputUsingPID(String packageName, String pid) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		System.out.println("PID of the app is : "+pid);
		String directoryLocationForStoringLogs="/home/nikhil/Documents/logs/";

		String phoneDirectory_1="/data/local/tmp/fromProgrampid.txt";
		String testFilePath="/data/local/tmp/myfile.txt";
		String analysingLogsUsingPID=pathToadb+" logcat -v brief -d --pid "+pid+" -f "+testFilePath;// > "+phoneDirectory_1;

		/**
		 * Pull this file to PC and save it to the  
		 */

		String filePath=directoryLocationForStoringLogs+"logs_"+packageName+"_PID.txt";
		File file=new File(filePath);
		file.createNewFile();

		System.out.println(analysingLogsUsingPID);
		Process process2=CommandExecute.commandExecution(analysingLogsUsingPID);

		/**
		 * Let's pull this file 
		 */
		String pullCommand=pathToadb+" pull "+testFilePath+" "+filePath;
		Process process=CommandExecute.commandExecution(pullCommand);

		/**
		 * Remove the file
		 */
		String removeTxtFileCommand=LogAnalysis_sameApp.pathToadb+" shell rm "+testFilePath;
		CommandExecute.commandExecution(removeTxtFileCommand);
		/*
		BufferedReader buf1 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
		//BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line="";

		while ((line=buf1.readLine())!=null) {
			//as in the first line only we can get the package name.That's why immeditate break;
			Files.write(Paths.get(filePath), (line+"\n").getBytes(),  StandardOpenOption.APPEND);
			System.out.println(line);
		}
		buf1.close();*/

	}

	private static void storingLogOutputUsingGrepPackageName(String packageName) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		String phoneDirectory="/data/local/tmp/fromProgramPackageName.txt";
		String commandToFilterLogsUsingPackageName=pathToadb+" shell logcat -v brief -d | grep "+packageName+" > "+phoneDirectory;

		System.out.println(commandToFilterLogsUsingPackageName);

		Process process3=CommandExecute.commandExecution(commandToFilterLogsUsingPackageName);

		String devicePCDirectory="/home/nikhil/Documents/logs/fromProgram"+packageName+".txt";
		String pullFromPhoneToPC=pathToadb+" pull "+phoneDirectory+" "+devicePCDirectory;
		CommandExecute.commandExecution(pullFromPhoneToPC);

	}
	private static String fetchPackageNamefromapkPath(String apkPath) {

		String packageName="";
		int len=apkPath.length();
		/**
		 * fetch the position of the last / or \
		 */
		int index=apkPath.lastIndexOf('/');
		if(index==-1)
		{
			index=apkPath.lastIndexOf('\\');
		}
		packageName=apkPath.substring(index+1, len-4);
		return packageName;
	}


}
