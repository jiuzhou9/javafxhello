package javafx.hello.excel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;

/**
 * 模板的读取类
 *
 * @author Jiaju Zhuang
 */
// 有个很重要的点 DemoDataListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
@Slf4j
public class DemoDataListener extends AnalysisEventListener<DemoData> {
//    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataListener.class);
    /**
     * 每隔5条存储数据库，实际使用中可以3000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 30000;
    List<DemoData> list = new ArrayList<DemoData>();
    /**
     * 假设这个是一个DAO，当然有业务逻辑这个也可以是一个service。当然如果不用存储这个对象没用。
     */
//    private DemoDAO demoDAO;
    private String username;
    private String password;
    private String jdbcUrl;


    public DemoDataListener() {
        // 这里是demo，所以随便new一个。实际使用如果到了spring,请使用下面的有参构造函数
//        demoDAO = new DemoDAO();
    }

//    /**
//     * 如果使用了spring,请使用这个构造方法。每次创建Listener的时候需要把spring管理的类传进来
//     *
//     * @param demoDAO
//     */
//    public DemoDataListener(DemoDAO demoDAO) {
//        this.demoDAO = demoDAO;
//    }

    /**
     * 构造器
     * @param username
     * @param password
     * @param jdbcUrl
     */
    public DemoDataListener(String username, String password, String jdbcUrl) {
        this.username = username;
        this.password = password;
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data
     *            one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(DemoData data, AnalysisContext context) {
        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        list.add(data);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (list.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            list.clear();
        }
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        log.info("所有数据解析完成！");
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", list.size());
//        demoDAO.save(list);
        List<String> sqls = convert(list);
        System.out.println(sqls);
        try {
            for (String sql : sqls) {
                execute("com.mysql.cj.jdbc.Driver", jdbcUrl, username, password, sql);
            }
            log.info("存储数据库成功！");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private List<String> convert(List<DemoData> list) {

        Map<String, List<DemoData>> demoDataMap =
                list.stream().collect(Collectors.groupingBy(demoData -> demoData.getTableName()));
        List<String> createSql = new ArrayList<>();

        demoDataMap.forEach((tableName, demoDatas)->{
            StringBuffer stringBuffer = new StringBuffer();
            String tableComment = "";
            stringBuffer.append("drop table if exists ".concat(tableName).concat(";\n"));
            stringBuffer.append("CREATE TABLE `"+tableName+"` (\n");
            for (DemoData demoData : demoDatas) {
                String fieldName = demoData.getFieldName();
                String rule = demoData.getRule();
                String comment = demoData.getComment();
                String type = demoData.getType();

                String s = " `" + fieldName + "` "+ type +" "+rule+" COMMENT '"+comment+"',\n";
                stringBuffer.append(s);
                tableComment = demoData.getTableComment();
            }
            String end = "PRIMARY KEY (`id`)\n) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='"+tableComment+"';";
            stringBuffer.append(end);
            createSql.add(stringBuffer.toString());
        });
        return createSql;
    }

    public static boolean execute(String jdbcDriver, String jdbcUrl, String jdbcUserName, String jdbcPassword, String sql) throws ClassNotFoundException, SQLException {
        Class.forName(jdbcDriver);
        Connection con = DriverManager.getConnection(jdbcUrl, jdbcUserName, jdbcPassword);
        Statement statement = con.createStatement();

        boolean count;
        count = statement.execute(sql);
        statement.close();
        con.close();
        log.info("sql 执行:{}, 受影响行数:{}", sql, count);
        return count;
    }

}