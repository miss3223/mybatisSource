package com.lianli.train.mybatis.Dao;

import com.lianli.train.mybatis.core.JoinNode;
import java.util.Stack;

/**
 * @author lianli Date: 2019-06-09 Time: 10:31 PM
 * @version $Id$
 */
public class BaseDaoImpl {

    public Stack<JoinNode> assembleNode(){

        Stack<JoinNode> joinNodeStack = new Stack<JoinNode>();
        JoinNode joinNodeEnd = new JoinNode();
        joinNodeEnd.setTable("authors");
        joinNodeEnd.setKey("id");

        JoinNode beforeEnd = new JoinNode();
        beforeEnd.setType("left");
        beforeEnd.setTable("news");
        beforeEnd.setKey("author_id");
        beforeEnd.setJoinNode(joinNodeEnd);


        joinNodeStack.push(beforeEnd);

        return joinNodeStack;

    }

}
