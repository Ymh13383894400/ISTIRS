package beans;

import java.sql.ResultSet;
import java.sql.SQLException;

import utils.StrUtil;

public class DBRecordBean {
	private ProjectBean project;
	private long runTime; // 运行时间。传运行成功时间
	private String resultPath; // 结果路径

	public ProjectBean getProject() {
		return project;
	}

	public void setProject(ProjectBean project) {
		this.project = project;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

	public String getResultPath() {
		return resultPath;
	}

	public void setResultPath(String resultPath) {
		this.resultPath = resultPath;
	}

	public void resToBean(ResultSet rs) {
		try {
			ProjectBean project = new ProjectBean();
			project.setProjectName(rs.getString(2));
			setRunTime(Long.parseLong(rs.getString(3)));
			project.setCreateTime(rs.getString(4));
			project.setProjectDir(rs.getString(5));
			project.setProjectLocationFile(rs.getString(6));
			SettingsBean setting = new SettingsBean();
			setting.setName(rs.getString(7));
			setting.setSaveMiddle(rs.getInt(8) == 1 ? true : false);
			setting.setNetWidth(rs.getString(9));
			setting.setNetHeight(rs.getString(10));
			setting.setPreCheck(rs.getInt(11) == 1 ? true : false);
			setting.setPreCheckWay(rs.getInt(12));
			setting.setFlyHeight(rs.getString(13));
			setting.setCameraSize(rs.getString(14));
			setting.setGsd(rs.getString(15));
			setResultPath(rs.getString(16));
			project.setErroDetail(rs.getString(17));
			project.setLastRuntime(Long.parseLong(rs.getString(18)));
			project.setId(Long.parseLong(rs.getString(19)));
			project.setSettings(setting);
			setProject(project);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private String placeOneCom(String str) {
		if (StrUtil.isEmpty(str))
			return "";
		return str.replace("'", "''");
	}

	public String toInsertSQL() {
		StringBuffer sb = new StringBuffer("insert into table_history values(");
		sb.append("'" + placeOneCom(getProject().getId() + "" + getRunTime()) + "',");
		sb.append("'" + placeOneCom(getProject().getProjectName()) + "',");
		sb.append("'" + getRunTime() + "',");
		sb.append("'" + placeOneCom(getProject().getCreateTime()) + "',");
		sb.append("'" + placeOneCom(getProject().getProjectDir()) + "',");
		sb.append("'" + placeOneCom(getProject().getProjectLocationFile()) + "',");
		sb.append("'" + placeOneCom(getProject().getSettings().getName()) + "',");
		sb.append((getProject().getSettings().isSaveMiddle() ? 1 : 0) + ",");
		sb.append("'" + getProject().getSettings().getNetWidth() + "',");
		sb.append("'" + getProject().getSettings().getNetHeight() + "',");
		sb.append((getProject().getSettings().isPreCheck() ? 1 : 0) + ",");
		sb.append(getProject().getSettings().getPreCheckWay() + ",");
		sb.append("'" + getProject().getSettings().getFlyHeight() + "',");
		sb.append("'" + getProject().getSettings().getCameraSize() + "',");
		sb.append("'" + getProject().getSettings().getGsd() + "',");
		sb.append("'" + placeOneCom(getResultPath()) + "',");
		sb.append("'" + (getProject().getErroDetailAll()) + "',");
		sb.append("'" + getProject().getLastRuntime() + "',");
		sb.append("'" + getProject().getId() + "',");
		sb.append("'');");
		return sb.toString();
	}

}
