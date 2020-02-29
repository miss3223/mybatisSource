package com.lianli.train.mybatis.mapperProvider;

import com.lianli.train.mybatis.annotation.Column;
import com.lianli.train.mybatis.annotation.MetaMethod;
import com.lianli.train.mybatis.Dao.BaseDaoImpl;
import com.lianli.train.mybatis.annotation.TableName;
import com.lianli.train.mybatis.core.JoinNode;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.StringUtils;

/**
 * @author lianli Date: 2019-06-02 Time: 12:29 AM
 * @version $Id$
 */
public class BaseProvider {

    private BaseDaoImpl baseDao;

    private String table;

    private String[] like;

    private String orderBy;

    private String orderKey;

    private String groupBy;

    private String having;

    private String resultType;

    private String condition;

    private String insertValue;

    private String insertColumn;

    private String updateValue;

    private static final ConcurrentHashMap<String, String> sqlString = new ConcurrentHashMap<String, String>();

    private String[] baseMethodName = new String[]{"selectByCondition","selectPage","insert",
        "insetBatch","update","updateBatch","delete","deleteBatch"};


    /** 返回的参数类型，类型为泛型的 */
    private void SqlColumn(ProviderContext context, Map<String, Object> param, SqlCommandType type) throws Exception {

        switch (type) {
            case SELECT: {
                selectColumn(context);
                break;
            }
            case INSERT: {
                insertColumn(param);
                break;
            }
            case UPDATE: {

                updateColumn(context, param);
                break;
            }
            case DELETE:
                deleteColumn(param);
                break;
            default:
                throw new BindingException("Unknown execution method for: " + type);
        }

    }

    /** 驼峰转换 */
    private static String fieldToUnderCamelCase(String fieldName) {

        if (fieldName == null || "".equals(fieldName)) {
            return "";
        }
        String changeChar = null;
        StringBuffer buffer = new StringBuffer();
        char[] nameChar = fieldName.toCharArray();
        for (char upper : nameChar) {

            if ((char) upper >= 'A' && (char) upper <= 'Z') {
                changeChar = "_" + upper;
                buffer.append(changeChar.toLowerCase());
            } else {
                buffer.append(upper);
            }
        }

        return buffer.toString();

    }

    private void ColumnNameFromFiled(ProviderContext context) throws ClassNotFoundException {

        Type[] genericInterfaces = context.getMapperType().getGenericInterfaces();
        ParameterizedType generic = (ParameterizedType) genericInterfaces[0];
        Type typeArgument = generic.getActualTypeArguments()[0];
        Class<?> returnType = Class.forName(typeArgument.getTypeName());
        Field[] declaredFields = returnType.getDeclaredFields();
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i < declaredFields.length; i++) {

            buffer.append( fieldToUnderCamelCase(declaredFields[i].getName()));
            buffer.append(",");
        }
        resultType = buffer.substring(0, buffer.length() - 1);
    }

    private void selectColumn(ProviderContext context) throws ClassNotFoundException {

        Type[] genericInterfaces = context.getMapperType().getGenericInterfaces();
        ParameterizedType generic = (ParameterizedType) genericInterfaces[0];
        Type typeArgument = generic.getActualTypeArguments()[0];
        Class<?> returnType = Class.forName(typeArgument.getTypeName());
        Field[] declaredFields = returnType.getDeclaredFields();
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i < declaredFields.length; i++) {
            buffer.append(declaredFields[i].getAnnotation(Column.class).field());
            buffer.append(",");
        }
        resultType = buffer.substring(0, buffer.length() - 1);
    }

    private void insertColumn(Map<String, Object> param) throws Exception {

        Object obj = param.get("bean");
        Class<?> beanType = param.get("bean").getClass();
        Field[] declaredFields = beanType.getDeclaredFields();
        StringBuffer bufferColumn = new StringBuffer();
        StringBuffer bufferValue = new StringBuffer();
        for (int i = 2; i < declaredFields.length; i++) {

            Field field = declaredFields[i];
            field.setAccessible(true);
            Object value = field.get(obj);

            if (!Objects.isNull(value)) {
                // 获取插入的字段
                bufferColumn.append(declaredFields[i].getAnnotation(Column.class).field());
                bufferColumn.append(",");
                // 获取插入字段的值
                bufferValue.append("#{bean.");
                bufferValue.append(field.getName());
                bufferValue.append('}');
                bufferValue.append(",");
            }

        }
        insertColumn = bufferColumn.substring(0, bufferColumn.length() - 1);
        insertValue = bufferValue.substring(0, bufferValue.length() - 1);

    }

    private void updateColumn(ProviderContext context, Map<String, Object> param) {

    }

    private void deleteColumn(Map<String, Object> param) {

    }

    /**
     * 查询条件
     */
    private void paramElements(Map<String, Object> param, SqlCommandType type, Boolean batch) throws Exception {
        // 判断传入的操作是否是批量操作的
        Object obj = param.get("bean");

        if (batch) {
            condition = listElements(obj);
        } else {
            Field[] declaredFields = beanFields(param);
            //更新
            if (SqlCommandType.UPDATE == type) {
                updateElements(declaredFields, obj);
            } else {
                StringBuffer buffer = new StringBuffer();
                for (int i = 1; i < declaredFields.length; i++) {
                    Field field = declaredFields[i];
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (!Objects.isNull(value)) {
                        String paramName = field.getName();
                        Column column = declaredFields[i].getAnnotation(Column.class);
                        if (Objects.isNull(column)) {
                            return;
                        }

                        String inParam = column.in();
                        buffer.append(column.field());
                        joinField(buffer, like, paramName, inParam);
                        if (i < declaredFields.length - 1) {
                            buffer.append(" and ");
                        }

                    }
                }
                condition = buffer.toString();
            }
        }
    }

    /**
     * 适配传入的参数为list的情况
     */
    private String listElements(Object obj) {

        StringBuffer buffer = new StringBuffer();

        buffer.append("<foreach item = 'item' index = 'index' collection = '");
        buffer.append(obj);
        buffer.append("' open ='('  separator=',' close =')' >");
        buffer.append("#{item}");
        buffer.append("</foreach>");

        return buffer.toString();

    }

    private void joinWhere(String[] join) {

        for (int i = 0; i < join.length; i++) {
            String[] value = join[i].split(",");
            if ("left".equals(value[0])) {

            }
        }
    }

    private void updateElements(Field[] declaredFields, Object obj) throws Exception {

        StringBuffer buffer = new StringBuffer();
        StringBuffer bufferOne = new StringBuffer();

        for (int i = 1; i < declaredFields.length; i++) {

            Field field = declaredFields[i];
            field.setAccessible(true);
            Object value = field.get(obj);

            if (!Objects.isNull(value)) {

                String paramName = field.getName();

                if (i == 1) {
                    bufferOne.append(declaredFields[i].getAnnotation(Column.class).field());
                    bufferOne.append("= #{bean.");
                    bufferOne.append(paramName);
                    bufferOne.append('}');
                    condition = bufferOne.toString();
                } else {
                    String inParam = declaredFields[i].getAnnotation(Column.class).in();
                    buffer.append(declaredFields[i].getAnnotation(Column.class).field());
                    joinField(buffer, like, paramName, inParam);

                    if (i != declaredFields.length - 1) {
                        buffer.append(",");
                    }
                }
            }

        }
        updateValue = buffer.toString();
    }


    private void joinField(StringBuffer buffer, String[] likeNames, String paramName, String inParam) {

        if (likeNames != null && ArrayUtils.contains(likeNames, paramName)) {
            buffer.append(" like concat('%',#{bean.");
            buffer.append(paramName);
            buffer.append("},'%')");
        } else if (Strings.isNotBlank(inParam)) {
            buffer.append(" in");
            joinForeach(buffer, paramName);

        } else {
            buffer.append("= #{bean.");
            buffer.append(paramName);
            buffer.append('}');

        }
    }

    private void joinForeach(StringBuffer buffer, String paramName) {

        buffer.append("<foreach item = 'item' index = 'index' collection = 'bean.");
        buffer.append(paramName);
        buffer.append("' open ='('  separator=',' close =')' >");
        buffer.append("#{item}");
        buffer.append("</foreach>");

    }

    /** 获取传入参数实体里面的字段 */
    private Field[] beanFields(Map<String, Object> param) {

        Class<?> beanType = param.get("bean").getClass();
        return beanType.getDeclaredFields();
    }

    /**
     * 获取表名
     */
    private void tableName(ProviderContext context) {

        MetaMethod metaMethod = context.getMapperMethod().getAnnotation(MetaMethod.class);

        if (Objects.isNull(metaMethod) || Strings.isBlank(metaMethod.tableName())) {
            table = context.getMapperType().getAnnotation(TableName.class).value();
        } else {
            table = metaMethod.tableName();
        }

    }

    /** 查询条件处理 */
    private void keywordsAboutCondition(ProviderContext context) {

        MetaMethod metaMethod = context.getMapperMethod().getAnnotation(MetaMethod.class);
        if (!Objects.isNull(metaMethod)) {
            like = metaMethod.like();
            groupBy = metaMethod.group();
            having = metaMethod.having();
            orderBy = metaMethod.order();
            orderKey = metaMethod.orderColumn();
        }
    }


    private void init(ProviderContext context, Map<String, Object> param, SqlCommandType type, Boolean batch)
        throws Exception {

        // 表名
        tableName(context);
        // 获取查询条件中的关键字
        keywordsAboutCondition(context);
        // 查询参数
        SqlColumn(context, param, type);
        // 条件
        paramElements(param, type, batch);
    }

    private String joinElemet(JoinNode popNode) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(popNode.getJoinNode().getTable());
        buffer.append(popNode.getTable());
        buffer.append(".");
        buffer.append(popNode.getKey());
        buffer.append("=");
        buffer.append(popNode.getJoinNode().getTable());
        buffer.append(".");
        if (popNode.getSearchCondition() != null) {
            buffer.append(popNode.getSearchCondition());
        }
        return buffer.toString();
    }

    private String getMethodName(ProviderContext context){

        String methodName = context.getMapperMethod().getName();
        if(ArrayUtils.contains(baseMethodName, methodName)){

           methodName = table + methodName ;
        }
        return methodName;
    }

    /**
     * 条件查询
     */
    public String selectByCondition(ProviderContext context, Map<String, Object> param) throws Exception {
        // 获取方法名


        init(context, param, SqlCommandType.SELECT, false);

        String methodName = getMethodName(context);
        if(sqlString.containsKey(methodName)){
            return sqlString.get(methodName);
        }

        Stack<JoinNode> joinNodeStack = baseDao.assembleNode();

        String sql =  "<script>" + new SQL() {{
            SELECT(resultType);
            FROM(table);
            while (!joinNodeStack.empty()) {
                JoinNode popNode = joinNodeStack.pop();
                String type = popNode.getType();
                switch (popNode.getType()) {
                    case "left": {
                        LEFT_OUTER_JOIN(joinElemet(popNode));
                        break;
                    }
                    case "right": {
                        RIGHT_OUTER_JOIN(joinElemet(popNode));
                        break;
                    }
                    case "inner": {
                        INNER_JOIN(joinElemet(popNode));
                        break;
                    }
                    default:
                        throw new BindingException("Unknown execution join type for: " + type);
                }
            }

            if (Strings.isNotBlank(condition)) {
                WHERE(condition);
            }
            if (Strings.isNotBlank(groupBy)) {
                GROUP_BY(groupBy);
            }
            if (Strings.isNotBlank(having)) {
                HAVING(having);
            }
            if (Strings.isNotBlank(orderBy)) {
                if (Strings.isNotBlank(orderBy)) {
                    ORDER_BY(orderBy + " " + orderKey);
                } else {
                    ORDER_BY(orderBy);
                }
            }

        }}.toString() + "</script>";

        sqlString.put(methodName,sql);
        return sql;
    }

    /**
     * 插入
     */
    public String insert(ProviderContext context, Map<String, Object> param) throws Exception {

        init(context, param, SqlCommandType.INSERT, false);

        String methodName = getMethodName(context);
        if(sqlString.containsKey(methodName)){
            return sqlString.get(methodName);
        }

        String sql = new SQL() {{
            INSERT_INTO(table);
            INTO_COLUMNS(insertColumn);
            INTO_VALUES(insertValue);
        }}.toString();

        sqlString.put(methodName,sql);
        return sql;
    }

    /**
     * 批量插入
     */
    public String insertBatch(ProviderContext context, Map<String, Object> param) throws Exception {
        init(context, param, SqlCommandType.INSERT, true);
        String methodName = getMethodName(context);
        if(sqlString.containsKey(methodName)){
            return sqlString.get(methodName);
        }
        return null;
    }


    /**
     * 更新
     */
    public String update(ProviderContext context, Map<String, Object> param) throws Exception {

        init(context, param, SqlCommandType.UPDATE, false);

        String methodName = getMethodName(context);
        if(sqlString.containsKey(methodName)){
            return sqlString.get(methodName);
        }

        String sql =  new SQL() {
            {
                UPDATE(table);
                SET(updateValue);
                WHERE(condition);
            }
        }.toString();

        sqlString.put(methodName,sql);
        return sql;
    }

    /**
     * 批量更新
     */
    public String updateBatch(ProviderContext context, Map<String, Object> param) throws Exception {
        init(context, param, SqlCommandType.UPDATE, true);
        return null;
    }


    /**
     * 删除
     */
    public String deleteById(ProviderContext context, Map<String, Object> param) throws Exception {
        init(context, param, SqlCommandType.DELETE, false);

        String methodName = getMethodName(context);
        if(sqlString.containsKey(methodName)){
            return sqlString.get(methodName);
        }

        String sql =  new SQL() {{
            DELETE_FROM(table);
            if (Strings.isNotBlank(condition)) {
                WHERE(condition);
            }
        }}.toString();

        sqlString.put(methodName,sql);
        return sql;
    }


    private String deleteSql(ProviderContext context){

        String methodName = getMethodName(context);
        if(sqlString.containsKey(methodName)){
            return sqlString.get(methodName);
        }

        String sql =  "<script>" + new SQL() {{
            DELETE_FROM(table);
            if (Strings.isNotBlank(condition)) {
                WHERE(condition);
            }
        }}.toString() + "</script>";

        sqlString.put(methodName,sql);

        return sql;
    }

    /**
     * 批量删除
     */
    public String deleteBatch(ProviderContext context, Map<String, Object> param) throws Exception {

        init(context, param, SqlCommandType.DELETE, true);
        return deleteSql(context);
    }

    /**
     * 删除
     * 暂时不提供
     */
    public String delete(ProviderContext context, Map<String, Object> param) throws Exception {

        init(context, param, SqlCommandType.DELETE, false);
        return deleteSql(context);

    }


    public static void main(String[] args) throws ClassNotFoundException {
      /*  String helloWorld = fieldToUnderCamelCase("helloWorldMing");
        System.out.println("helloWorld:" + helloWorld);*/
        Class<?> returnType = Class.forName("com.lianli.train.mybatis.po.News");
        Field[] declaredFields = returnType.getDeclaredFields();
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i < declaredFields.length; i++) {
            buffer.append( fieldToUnderCamelCase(declaredFields[i].getName()));
            buffer.append(",");
        }
        System.out.println("result:"+buffer.substring(0, buffer.length() - 1));
    }

}
