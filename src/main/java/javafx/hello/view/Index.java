package javafx.hello.view;

import com.alibaba.excel.EasyExcel;
import javafx.event.ActionEvent;
import javafx.hello.excel.DemoData;
import javafx.hello.excel.DemoDataListener;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * @Description
 * @Author wangjiuzhou (835540436@qq.com)
 * @Date 2020-09-12 2:14
 */
public class Index {
    //提示
    public Text actiontarget;
    //密码
    public PasswordField passwordField;
    public TextField username;
    //文件路径回显
    public Text filePath;
    public TextArea jdbcURLtext;
    private Window primaryStage;
    private File file;

    public void handleSubmitButtonAction(ActionEvent actionEvent) {
        actiontarget.setText("正在建表...");
        String passwordFieldText = passwordField.getText();
        System.out.println("passwordFieldText:" + passwordFieldText);
        if (passwordFieldText == null || "".equals(passwordFieldText)){
            actiontarget.setText("请输入密码");
        }
        String usernameText = username.getText();
        System.out.println("usernameText:" + usernameText);

        String jdbcURLText = jdbcURLtext.getText();
        System.out.println("jdbcURLText:" + jdbcURLText);

        // 有个很重要的点 DemoDataListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
        // 写法1：
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        EasyExcel.read(file, DemoData.class, new DemoDataListener(usernameText, passwordFieldText, jdbcURLText)).sheet().doRead();
    }

    public void chooseExcel(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser
                .ExtensionFilter("TXT files (*.xlsx)", "*.txt", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);
        this.file = fileChooser.showOpenDialog(primaryStage);
        System.out.println("file:" + file);
        filePath.setText(file.getAbsolutePath());

        // 有个很重要的点 DemoDataListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
        // 写法1：
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
//        EasyExcel.read(file, DemoData.class, new DemoDataListener()).sheet().doRead();
    }
}
