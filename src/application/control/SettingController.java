package application.control;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import beans.FinalDataBean;
import beans.MyFxmlBean;
import beans.ProjectBean;
import beans.SettingsBean;
import consts.ConstSize;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.ResUtil;
import utils.SaveSettingsUtil;
import utils.StrUtil;
import utils.ToastUtil;
import utils.UIUtil;
import views.MyToolTip;
import views.myTextField.DecimalField;
import views.myTextField.IntegerField;

/**
 * 设置其他参数界面controller
 * 
 * @author DP
 *
 */
public class SettingController extends BaseController implements Initializable {
	public static final String style_temp = "-fx-background-color:#5AF102;-fx-text-fill: white;-fx-padding:2.0;-fx-font-size:14.0;";
	public static final String style_unname = "-fx-background-color:#FFB731;-fx-text-fill: white;-fx-padding:2.0;-fx-font-size:14.0;";
	public static final String style_no = "-fx-background-color:#FF8282;-fx-text-fill: white;-fx-padding:2.0;-fx-font-size:14.0;";

	private SettingListener listener;
	@FXML
	JFXCheckBox checkBox_SaveMiddle;
	@FXML
	JFXCheckBox checkBox_preCheck;
	//添加更改算法的checkbox
//	@FXML
//	JFXCheckBox checkBox_ChangeAlgorithm;
	@FXML
	IntegerField textArea_width;
	@FXML
	IntegerField textArea_hight;
	@FXML
	DecimalField textArea_flyHeight;
	@FXML
	DecimalField textArea_cameraSize;
	@FXML
	DecimalField textArea_gsd;
	@FXML
	VBox Vbox_prechecks;
	@FXML
	BorderPane root;
	@FXML
	private JFXRadioButton radioButton_way1;
	@FXML
	private JFXRadioButton radioButton_way2;
	@FXML
	private HBox hbox_way1;
	@FXML
	private HBox hbox_way2;

	@FXML
	private JFXRadioButton radioButton_algorithmWay1;
	@FXML
	private JFXRadioButton radioButton_algorithmWay2;
	@FXML
	private HBox hbox_algorithmWay1;
	@FXML
	private HBox hbox_algorithmWay2;


	//-------------设置HBox_image_grid，隐藏最上面的参数设置-------------
	@FXML
	HBox HBox_image_grid;

	//------------添加save提示和按钮的id------------------------
	@FXML
	HBox hbox_preCheckDetail1;

	//------------添加按钮（save）隐藏-------------------------
	@FXML
	Button button_save;
	//------------添加按钮（help图像）隐藏-------------------------
	@FXML
	Button button_help;

	private ToggleGroup group;
	private ToggleGroup groupAlgorithm;

	private ObservableList<ProjectBean> projectListData;
	private ObservableList<SettingsBean> settingListData = FXCollections.observableArrayList();
	@FXML
	ListView<HBox> listView_projects;
	private ObservableList<HBox> listViewData_proj = FXCollections.observableArrayList();
	@FXML
	ListView<HBox> listView_settings;
	private ObservableList<HBox> listViewData_setting = FXCollections.observableArrayList();
	private ProjectBean currentProject;
	private int currentIndex;
	@FXML
	VBox setting_pane;
	//--------------------隐藏最右边的参数设置pane--------------------
	@FXML
	VBox setting_right_pane;
	@FXML
	Label label_project_name;
	private Stage stage;

	ImageView imageView = new ImageView(new Image("/resources/wushuju.png"));

	private int AlgorithmType = 1;//0:默认算法-带参数算法，1:NISwGSP-不带参数算法

	public int getAlgorithmType(){
		return AlgorithmType;
	}

	public void setAlgorithmType(int AlgorithmType){
		this.AlgorithmType = AlgorithmType;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initCheckBox();
		initToggleGroup();
		initTextField();
		initListView();
		setSettingsListDataInfo();
	}

	private void setSettingsListDataInfo() {
		SaveSettingsUtil.getSettingsData(stage, new SaveSettingsUtil.Callback() {
			@Override
			public void onGetData(ArrayList<SettingsBean> list) {
				settingListData.clear();
				settingListData.addAll(list);
				addSettingsToList(settingListData, null);
			}
		});
	}

	private void addSettingsToList(ObservableList<SettingsBean> list, SettingsBean singleSetting) {
		if (list != null) {
			listViewData_setting.clear();
			for (int i = 0; i < list.size(); i++) {
				MyFxmlBean fxmlbean = UIUtil.loadFxml(getClass(), "/application/fxml/SettingsTempHBox.fxml");
				HBox temp = (HBox) fxmlbean.getPane();
				SettingsBean setting = list.get(i);
				setItemSettingContent(setting, temp);
				listViewData_setting.add(temp);
			}
		} else if (singleSetting != null) {
			settingListData.add(singleSetting);
			MyFxmlBean fxmlbean = UIUtil.loadFxml(getClass(), "/application/fxml/SettingsTempHBox.fxml");
			HBox temp = (HBox) fxmlbean.getPane();
			setItemSettingContent(singleSetting, temp);
			listViewData_setting.add(temp);
		}
	}

	/**
	 * 设置setting列表的item内容
	 * 
	 * @param setting
	 * @param temp
	 */
	private void setItemSettingContent(SettingsBean setting, HBox temp) {
		Label name = (Label) temp.lookup("#name");
		name.setText(setting.getName());
		name.setTooltip(new MyToolTip(setting.transToTipStr()));
	}

	private void initListView() {
		listView_projects.setPlaceholder(imageView);
		listView_settings.setPlaceholder(imageView);
		listView_projects.setItems(listViewData_proj);
		listView_projects.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Object>() {

			@Override
			public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
				currentIndex = listView_projects.getSelectionModel().getSelectedIndex();
				System.out.println("initListView -> " + currentIndex + "  size = " + listViewData_proj.size());
				if (currentIndex >= 0 && currentIndex < listViewData_proj.size()) {
					currentProject = projectListData.get(currentIndex);
//					if(currentProject.getSettings() == null){
//						System.out.println("无setting");
//					}
//					else System.out.println("无setting 有");
					setSettingViews(currentProject.getSettings(), false);
				} else {
					setting_pane.setVisible(false);
				}
			}
		});
		listView_settings.setItems(listViewData_setting);
		System.out.println("getItems1 = " + listView_settings.getItems());
		listView_settings.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					// 双击
					int selectedIndex = listView_settings.getSelectionModel().getSelectedIndex();
					if (selectedIndex < 0 || selectedIndex >= settingListData.size()) {
						return;
					}
					SettingsBean settings = settingListData.get(selectedIndex);
					if (settings == null) {
						return;
					}
					MyFxmlBean settingDialogBean = UIUtil.openDialog(getClass(),
							"/application/fxml/SettingsDialog.fxml", ConstSize.Main_Frame_Width,
							ConstSize.Second_Frame_Height, settings.getName(), stage);
					SettingsDialogController settingDialogController = settingDialogBean.getFxmlLoader()
							.getController();
					settingDialogController.initExtraData(2, projectListData, settings);
					settingDialogController.setCallBack(new application.control.SettingsDialogController.CallBack() {
						@Override
						public void onReturn(SettingsBean settings) {
							if(AlgorithmType == 0){
								settings.setAlgorithmType(0);
							}
							else settings.setAlgorithmType(1);
							refreshProjectListView();
							refreshSettingListView(settings);
							if (currentProject != null) {
								setSettingViews(currentProject.getSettings(), false);
							}
							SaveSettingsUtil.changeSettingData(settings, null);
							settingDialogBean.getStage().close();
						}
					});
				}
			}
		});
		System.out.println("getItems2 = " + listView_settings.getItems());
	}

	/**
	 * 刷新参数列表。如果settings为空，全部刷新，如果不为空，刷新对应单个
	 */
	private void refreshSettingListView(SettingsBean settings) {
		if (settings == null) {
			// 暂时不做，不需要
		} else {
			for (int i = 0; i < settingListData.size(); i++) {
				SettingsBean item = settingListData.get(i);
				if (item.getId() == settings.getId()) {
					if (i >= 0 && i < listViewData_setting.size()) {
						setItemSettingContent(settings, listViewData_setting.get(i));
					}
					break;
				}
			}
		}
	}

	/**
	 * 设置参数数据
	 * 
	 * @param settings
	 * @param showName
	 */
	protected void setSettingViews(SettingsBean settings, boolean showName) {
		label_project_name.setText(currentProject.getProjectName());
		setting_pane.setVisible(true);
		if (settings == null) {
			System.out.println("setting = null");
			clearSettingViews(showName);
		} else {
			checkBox_SaveMiddle.setSelected(settings.isSaveMiddle());
			checkBox_preCheck.setSelected(settings.isPreCheck());
			textArea_width.setText(settings.getNetWidth());
			textArea_hight.setText(settings.getNetHeight());
			textArea_gsd.setText(settings.getGsd());
			textArea_cameraSize.setText(settings.getCameraSize());
			textArea_flyHeight.setText(settings.getFlyHeight());
			if(settings.getAlgorithmType() == 0){
				groupAlgorithm.selectToggle(radioButton_algorithmWay1);
			} else {
				groupAlgorithm.selectToggle(radioButton_algorithmWay2);
			}
			if (settings.getPreCheckWay() == 0) {
				group.selectToggle(radioButton_way1);
			} else {
				group.selectToggle(radioButton_way2);
			}
		}
	}

	private void clearSettingViews(boolean showName) {
		checkBox_SaveMiddle.setSelected(false);
		checkBox_preCheck.setSelected(false);
//		checkBox_ChangeAlgorithm.setSelected(false);
		textArea_width.setText("");
		textArea_hight.setText("");
		textArea_gsd.setText("");
		textArea_cameraSize.setText("");
		textArea_flyHeight.setText("");
		group.selectToggle(radioButton_way1);
		groupAlgorithm.selectToggle(radioButton_algorithmWay2);
	}

	private void initTextField() {
	}

	private void initToggleGroup() {
		group = new ToggleGroup();
		radioButton_way1.setToggleGroup(group);
		radioButton_way1.setUserData(0);
		radioButton_way2.setToggleGroup(group);
		radioButton_way2.setUserData(1);

		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
				if ((int) group.getSelectedToggle().getUserData() == 0) {
					hbox_way1.setDisable(false);
					hbox_way2.setDisable(true);
				}
				if ((int) group.getSelectedToggle().getUserData() == 1) {
					hbox_way1.setDisable(true);
					hbox_way2.setDisable(false);
				}
			}
		});
		groupAlgorithm = new ToggleGroup();
		radioButton_algorithmWay1.setToggleGroup(groupAlgorithm);
		radioButton_algorithmWay1.setUserData(0);
		radioButton_algorithmWay2.setToggleGroup(groupAlgorithm);
		radioButton_algorithmWay2.setUserData(1);

		//添加选择钩，选择不同算法，通过此选择不同的算法，并通过单选按钮组groupAlgorithm下的hbox_algorithmWay1和hbox_algorithmWay2进行参数传递
		groupAlgorithm.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
				if((int) groupAlgorithm.getSelectedToggle().getUserData() == 0){
					hbox_algorithmWay1.setDisable(false);
					hbox_algorithmWay2.setDisable(true);

					Vbox_prechecks.setDisable(true);
					checkBox_preCheck.setDisable(true);
					setting_right_pane.setDisable(true);
					HBox_image_grid.setDisable(false);
					checkBox_SaveMiddle.setDisable(true);
//					button_save.setDisable(true);
					button_help.setDisable(true);
					AlgorithmType = 0;//设置后，则不使用默认算法，使用不带参数算法
				}
				if((int) groupAlgorithm.getSelectedToggle().getUserData() == 1){
					hbox_algorithmWay1.setDisable(true);
					hbox_algorithmWay2.setDisable(false);

					Vbox_prechecks.setDisable(false);
					checkBox_preCheck.setDisable(false);
					setting_right_pane.setDisable(false);
					HBox_image_grid.setDisable(false);
					checkBox_SaveMiddle.setDisable(false);
//					button_save.setDisable(false);
					button_help.setDisable(false);
					AlgorithmType = 1;//取消勾选后，再设置回默认不带参数算法
				}
			}
		});
	}


	private void initCheckBox() {
		checkBox_preCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					Vbox_prechecks.setDisable(false);
				} else {
					Vbox_prechecks.setDisable(true);
				}
			}
		});
		checkBox_SaveMiddle.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
				} else {
				}
			}
		});
	}

	/**
	 * 传递项目列表数据
	 * 
	 * @param projectListData
	 */
	public void setProjectsInfo(ObservableList<ProjectBean> projectListData) {
		this.projectListData = projectListData;
		addProjectsToList(projectListData);
	}

	/**
	 * 添加项目列表数据
	 * 
	 * @param projects
	 */
	private void addProjectsToList(ObservableList<ProjectBean> projects) {
		listViewData_proj.clear();
		for (int i = 0; i < projects.size(); i++) {
			MyFxmlBean fxmlbean = UIUtil.loadFxml(getClass(), "/application/fxml/SettingsProjectHBox.fxml");
			HBox temp = (HBox) fxmlbean.getPane();
			ProjectBean project = projects.get(i);
			setItemProjectContent(project, temp);
			listViewData_proj.add(temp);
		}
		if (listViewData_proj.size() >= 1) {
			listView_projects.getSelectionModel().select(0);
		}
	}

	/**
	 * 设置项目列表item
	 * 
	 * @param project
	 * @param temp
	 */
	private void setItemProjectContent(ProjectBean project, HBox temp) {
		Label project_name = (Label) temp.lookup("#project_name");
		project_name.setText(project.getProjectName());
		project_name.setTooltip(new MyToolTip(project.transToTipStr(false)));
		changeButtonView(temp, project.getSettings());
	}

	public interface SettingListener {
		/**
		 * 点击开始拼接按钮
		 * 
		 * @param finalData
		 */
		void onClickStart(FinalDataBean finalData);

		/**
		 * 点击上一页按钮
		 */
		void onClickLeftBtn();

		/**
		 * 添加设置模板
		 */
		void onClickAddSettings(ObservableList<ProjectBean> projectListData);
	}

	public void setListener(SettingListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onSetBottomBtnsAndTitle() {
		leftBtn.setVisible(true);
		rightBtn.setVisible(true);
		rightBtn.setText(ResUtil.gs("start"));
		leftBtn.setText(ResUtil.gs("project"));
		title.setText(ResUtil.gs("settings"));
	}

	@Override
	protected void onClickLeftBtn() {
		if (listener != null) {
			listener.onClickLeftBtn();
		}
	}

	//更改判别内容
	@FXML
	protected void onSaveSetting() {
		SettingsBean newSetting = checkAndSave();
		if (newSetting == null) {
			return;
		}
		currentProject.setSettings(newSetting);
		changeButtonView(listViewData_proj.get(currentIndex), newSetting);
	}

	/**
	 * 项目列表全部刷新
	 */
	private void refreshProjectListView() {
		for (int i = 0; i < projectListData.size(); i++) {
			ProjectBean itemProj = projectListData.get(i);
			if (i >= 0 && i < listViewData_proj.size()) {
				HBox hBox = listViewData_proj.get(i);
				if (itemProj != null && hBox != null) {
					changeButtonView(hBox, itemProj.getSettings());
				}
			}
		}
	}

	/**
	 * 项目列表单项刷新
	 * 
	 * @param item
	 * @param setting
	 */
	private void changeButtonView(HBox item, SettingsBean setting) {
		JFXButton settingBtn = (JFXButton) item.lookup("#setting");
		MyToolTip toolTip = new MyToolTip();
		if (setting == null) {
			settingBtn.setText(ResUtil.gs("setting_name_empty"));
			settingBtn.setStyle(style_no);
			toolTip.setText(ResUtil.gs("setting_has_no_set"));
		} else {
			settingBtn.setText(setting.getName());
			if (setting.getSettingType() == 1) {
				settingBtn.setStyle(style_unname);
			} else {
				settingBtn.setStyle(style_temp);
			}
			toolTip.setText(setting.transToTipStr());
		}
		settingBtn.setTooltip(toolTip);
	}

	/**
	 * 调用新建模板后，成功回调方法。
	 * 
	 * @param settings
	 */
	public void addSettingResult(SettingsBean settings) {
		refreshProjectListView();
		if (currentProject != null) {
			setSettingViews(currentProject.getSettings(), false);
		}
		SaveSettingsUtil.saveProject(settings, null);
		addSettingsToList(null, settings);
	}

	@FXML
	protected void onAddSetting() {
		if (listener != null) {
			listener.onClickAddSettings(projectListData);
		}
	}

	/**
	 * 校验并创建新的settingbean
	 * 
	 * @return
	 */
	private SettingsBean checkAndSave() {
		SettingsBean bean = null;

		if((int) groupAlgorithm.getSelectedToggle().getUserData() == 0){
			bean = new SettingsBean();
			bean.setAlgorithmType(0);
			return bean;
		}

		if (StrUtil.isEmpty(textArea_width.getText()) || StrUtil.isEmpty(textArea_hight.getText())) {
			ToastUtil.toast(ResUtil.gs("setting_net_error"));
			return bean;
		}
		if (checkBox_preCheck.isSelected() && (int) group.getSelectedToggle().getUserData() == 0
				&& (StrUtil.isEmpty(textArea_flyHeight.getText()) || StrUtil.isEmpty(textArea_cameraSize.getText()))) {
			ToastUtil.toast(ResUtil.gs("setting_pre_check_error"));
			return bean;
		}
		if (checkBox_preCheck.isSelected() && (int) group.getSelectedToggle().getUserData() == 1
				&& StrUtil.isEmpty(textArea_gsd.getText())) {
			ToastUtil.toast(ResUtil.gs("setting_pre_check_error"));
			return bean;
		}

		int width, height;
		try {
			width = textArea_width.getValue();
			height = textArea_hight.getValue();
			if (width < 10 || height < 10) {
				ToastUtil.toast(ResUtil.gs("setting_net_size_error"));
				return bean;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return bean;
		}
		bean = new SettingsBean();
		bean.setSaveMiddle(checkBox_SaveMiddle.isSelected());
		bean.setNetWidth(textArea_width.getValue() + "");
		bean.setNetHeight(textArea_hight.getValue() + "");
		bean.setPreCheck(checkBox_preCheck.isSelected());
		bean.setPreCheckWay((int) group.getSelectedToggle().getUserData());
		bean.setAlgorithmType(0);
		bean.setGsd(textArea_gsd.getValue() + "");
		bean.setFlyHeight(textArea_flyHeight.getValue() + "");
		bean.setCameraSize(textArea_cameraSize.getValue() + "");
		bean.setSettingType(1);
		bean.setAlgorithmType(1);
		return bean;
	}

	@Override
	protected void onClickRightBtn() {
		boolean checkFinalData = checkFinalData();
		if (checkFinalData) {
			FinalDataBean finalDataBean = new FinalDataBean(projectListData);

//			if(checkBox_ChangeAlgorithm.isSelected()){
//				System.out.println("选择");
//				SettingsBean newSetting = new SettingsBean();
//				newSetting.setAlgorithmType(1);
//				currentProject.setSettings(newSetting);
//				finalDataBean.setSettings(newSetting);
//				changeButtonView(listViewData_proj.get(currentIndex), newSetting);
//			}
			if (listener != null) {
				listener.onClickStart(finalDataBean);
			}
		}
	}

	@FXML
	private void onClickHelpCamera() {
		ToastUtil.toast(ResUtil.gs("setting_camera_tip"));
	}
	
	private boolean checkFinalData() {
		if (projectListData == null || projectListData.size() <= 0) {
			ToastUtil.toast(ResUtil.gs("no_projects"));
			return false;
		}
		boolean isOk = true;
		for (ProjectBean project : projectListData) {
			System.out.println("进入3");
			if (project == null) {
				System.out.println("进入4");
				isOk = false;
				break;
			}
			//-------------添加无参数算法的参数检测-----------------
			if ((project.getSettings() == null) && (AlgorithmType == 0)) {
				System.out.println("进入7");
				isOk = false;
				ToastUtil.toast(ResUtil.gs("project") + project.getProjectName() + ResUtil.gs("setting_has_no_set"));
				break;
			}
		}
		return isOk;
	}
}
