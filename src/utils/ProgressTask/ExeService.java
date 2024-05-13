package utils.ProgressTask;

import application.control.ProcessingController.ProcessingListener;
import beans.FinalDataBean;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import utils.ExeProcedureUtil;

public class ExeService extends Service<String> 
{
	public ProcessingListener listener;
	public ExeService(ProcessingListener listener)
	{
		this.listener = listener;
	}

	@Override
	protected Task<String> createTask()
	{
		Task<String> task = new Task<String>()
		{
			@Override
			protected String call() throws Exception
			{
				String path = System.getProperty("user.dir");
//		    	String path_Exe = path + "\\ExeProcedure\\ImageStitching.exe";//exe文件的结果路径
		    	String path_Exe = path + "\\ExeProcedureNISwGSP\\NISwGSP.exe";//exe文件的结果路径
//				String lastLine = ExeProcedureUtil.execute(path_Exe, FinalDataBean.para_Exe, listener);//参数
		    	String lastLine = ExeProcedureUtil.execute_NISwGSP(path_Exe, FinalDataBean.para_Exe, listener);//参数
				return	lastLine;
			}
		};
		return task;
	}

	@Override
	protected void succeeded()
	{
//		System.out.println("qqqqqqqqqqqqqqqqqqq");
    	super.succeeded();
    	String result = getValue();

		//-----------------添加调用新算法exe执行完的最后一句-------------
    	if(result.equals("Stitch Finished!") || result.equals("图像拼接完成！") || result.equals("请按任意键继续. . . "))
    		listener.updateSuccBox();
    	else
    	{
    		listener.updateFailBox(result);
    	}
	}
	@Override
	protected void cancelled()
	{
		super.cancelled();
    	System.out.println("关闭：" + ExeProcedureUtil.closeExe());
	}

	@Override
	protected void failed()
	{
		super.failed();
		
		System.out.println("程序异常");
	}

}
