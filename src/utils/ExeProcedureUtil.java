package utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
//添加内容
import java.io.OutputStream;
import java.nio.charset.Charset;
import application.control.ProcessingController.ProcessingListener;
import consts.ConstRes;
import javafx.application.Platform;

/**
 * 执行外部exe程序工具类
 * @author wxp
 *
 */

public class ExeProcedureUtil
{	
	static Process process;
	/**
	 * 执行外部exe程序
	 * @param path_Exe  .exe文件的路径
	 * @param para_Exe  .exe程序的参数
	 * @return			 执行程序后cmd显示的结果
	 * @throws Exception
	 */
	public static String execute(String path_Exe, String para_Exe, ProcessingListener listener) 
	{
		BufferedReader inBr = null;
		String lineStr = "";
		String oldString = "";
		
		int i = para_Exe.lastIndexOf("%");
		System.out.println("i = "+i);
		String dir_currentproject = para_Exe.substring(i + 1);
		File workDir = new File(System.getProperty("user.home")+ConstRes.SOFT_PATH+ "\\logs\\" + dir_currentproject);
		if(!workDir.exists())
			workDir.mkdirs();
		para_Exe = para_Exe.substring(0, i);
		System.out.println("para_Exe = "+para_Exe);
		System.out.println("\n--------------\n"+System.getProperty("user.home")+ConstRes.SOFT_PATH+"\n\n\n");
		System.out.println(para_Exe);

		String[] cmds = {path_Exe, para_Exe};
		
		try {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(cmds, null, workDir);
            System.out.println("运行");
            InputStreamReader in = new InputStreamReader(process.getInputStream(), Charset.forName("GBK"));
			System.out.println("in = "+in);
            inBr = new BufferedReader(in);
			System.out.println("inBr = "+inBr);
            
            FileOutputStream fos = new FileOutputStream(workDir + "\\RuntimeDetailInfo.txt");
			BufferedOutputStream bos  = new BufferedOutputStream(fos);
			
            while((lineStr=inBr.readLine())!=null){
                System.out.println("拼接文件中输出的内容 "+lineStr);
                bos.write((lineStr + "\n").getBytes("UTF-8"));
                oldString = lineStr;
                final String newStr = lineStr;
//                System.out.println("进入run中，newStr = "+ newStr + " oldStr = " + oldString + " lineStr = " + lineStr);
				  Platform.runLater(new Runnable() {
					  @Override public void run() { //更新JavaFX的主线程的代码放在此处
					 		listener.update("\n" + newStr);
					  } 
				  });
            }
                
            bos.flush();
			bos.close();
			System.out.println("----------------无异常执行完exe文件------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
        	try
			{
				process.getInputStream().close();
				process.getOutputStream().close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
		}
		workDir.delete();
		System.out.println("整个函数执行完，传出最后的参数oldStr = " + oldString);
		return oldString;
	}

	public static String execute_NISwGSP(String path_Exe, String para, ProcessingListener listener)
	{
		BufferedReader inBr = null;
		String lineStr = "";
		String oldString = "";

		int i = para.lastIndexOf("%");
		int j = para.lastIndexOf("/input-42-data/");
		int k = path_Exe.lastIndexOf("NISwGSP.exe");
		System.out.println("i = "+i);
		String para_Exe = para.substring(j+15,i-1);
		String path = para.substring(1,j-1);
		System.out.println("---------------:" + para_Exe + " " + path);
		String dir_currentproject = para.substring(i + 1);
//		System.out.println("\n------------------\n输出在cmd的内容为：" + path_Exe + " " + para_Exe);



//		File result = new File(System.getProperty("user.home")+ConstRes.SOFT_PATH+ "\\logs\\" + dir_currentproject + "\\result");
//		if(!result.exists())
//			result.mkdirs();
//		File file_result = new File(System.getProperty("user.home")+ConstRes.SOFT_PATH+ "\\logs\\" + dir_currentproject + "\\result\\"+para_Exe+"-result");
//		if(!file_result.exists()) {
//			file_result.mkdirs();
//			System.out.println("#################");
//		}
//		result.delete();
//		file_result.delete();

		File workDir = new File(System.getProperty("user.home")+ConstRes.SOFT_PATH+ "\\logs\\" + dir_currentproject);
		if(!workDir.exists())
			workDir.mkdirs();
//		para = para.substring(0, i);
		System.out.println("\n保存日志的路径为："+System.getProperty("user.home")+ConstRes.SOFT_PATH+"\\logs\\" + dir_currentproject + "\n\n\n");

//		String exe_path = "C:\\Users\\Ymh\\Desktop\\无人机热红外遥感影像拼接项目\\FxISFrame\\ExeProcedureNISwGSP\\NISwGSP.exe";
//		String file_path = "f02";
    	String[] cmds = {path_Exe,para_Exe};


//		String cmd = "copy " + path + "/0_results/" + para_Exe + "-result " + System.getProperty("user.home")+ConstRes.SOFT_PATH+ "\\logs\\" + dir_currentproject + "\\result\\"+para_Exe+"-result";
//		System.out.println("###########################");
//		System.out.println("copy " + path + "/0_results/" + para_Exe + "-result " + System.getProperty("user.home")+ConstRes.SOFT_PATH+ "\\logs\\" + dir_currentproject + "\\result\\"+para_Exe+"-result");
//    try {
//    } catch (IOException | InterruptedException e) {
//       e.printStackTrace();
		OutputStream stdin = null;
		try {

			// 创建一个子进程
			Runtime runtime = Runtime.getRuntime();
			process = runtime.exec(cmds);
			// 写入数据到子进程的标准输入中
			stdin = process.getOutputStream();
		   	stdin.write('\n');
		   	stdin.flush();
			process.waitFor();

			System.out.println("运行");
			InputStreamReader in = new InputStreamReader(process.getInputStream(), Charset.forName("GBK"));
			System.out.println("in = "+in);
			inBr = new BufferedReader(in);
			System.out.println("inBr = "+inBr);

			FileOutputStream fos = new FileOutputStream(workDir + "\\RuntimeDetailInfo.txt");
//			FileOutputStream fos = new FileOutputStream(file_path_all + "\\RuntimeDetailInfo.txt");
			BufferedOutputStream bos  = new BufferedOutputStream(fos);

			while((lineStr = inBr.readLine()) != null){
				System.out.println("拼接文件中输出的内容 "+lineStr);
				bos.write((lineStr + "\n").getBytes("UTF-8"));
				oldString = lineStr;
				final String newStr = lineStr;

				Platform.runLater(new Runnable() {
					@Override public void run() { //更新JavaFX的主线程的代码放在此处
						listener.update("\n" + newStr);
					}
				});
//				System.out.println("lineStr = " + lineStr + " oldStr = " + oldString + " newStr = " + newStr);
			}
			System.out.println("----------------无异常执行完exe文件------------------最后的lineStr = " + lineStr);
			bos.flush();
			bos.close();
		} catch (Exception e) {
			System.out.println("运行失败，抛出异常！\n异常内容为: " + e.toString());
			e.printStackTrace();
		}finally {
			if (stdin != null) {
				try {
					stdin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 如果子进程还在运行，则终止它
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			try
			{
				process.getInputStream().close();
				process.getOutputStream().close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		workDir.delete();
		System.out.println("整个函数执行完，传出最后的参数oldStr = " + oldString);
		return oldString;
	}

	
	//关闭进程
	public static boolean closeExe()
	{
		//修改进程关闭
		String command1 = "taskkill /f /im ImageStitching.exe";
		String command2 = "taskkill /f /im NISwGSP.exe";
		process.destroyForcibly();
		try
		{
			System.out.println("标志:" + process.waitFor());
		} catch (InterruptedException e)
		{
			System.out.println("未关闭");
			try
			{
				Runtime.getRuntime().exec(command1);
				Runtime.getRuntime().exec(command2);
				Thread.sleep(3000);
			} catch (Exception event)
			{
				event.printStackTrace();
			}
			e.printStackTrace();
		}
		if(process.isAlive())
			return false;
		return true;
	}
}
