package com.lianli.train.mybatis.core;

import java.io.Serializable;

/**
 * @author lianli Date: 2019-06-09 Time: 10:10 PM
 * @version $Id$
 */
public class JoinNode implements Serializable {

    private static final long serialVersionUID = 5372825388859687234L;

    /** 连接类型 left/right/inner*/
    private String type;
    /** 表名 */
    private String table;
    /** 连接的字段 */
    private String key;
    /** 连接的表的信息 */
    private JoinNode joinNode;
    /** 连接后的查询条件*/
    private String searchCondition;

    public JoinNode() {
    }

    public JoinNode(String type, String table, String key, JoinNode joinNode, String searchCondition) {
        this.type = type;
        this.table = table;
        this.key = key;
        this.joinNode = joinNode;
        this.searchCondition = searchCondition;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public JoinNode getJoinNode() {
        return joinNode;
    }

    public void setJoinNode(JoinNode joinNode) {
        this.joinNode = joinNode;
    }

    public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
    }

    @Override
    public String toString() {
        return "JoinNode{" +
            "type='" + type + '\'' +
            ", table='" + table + '\'' +
            ", key='" + key + '\'' +
            ", joinNode=" + joinNode +
            ", searchCondition='" + searchCondition + '\'' +
            '}';
    }
}
