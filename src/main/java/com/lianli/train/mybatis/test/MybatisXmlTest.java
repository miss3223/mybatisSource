package com.lianli.train.mybatis.test;

import com.lianli.train.mybatis.mapper.xml.NewsMapper;
import com.lianli.train.mybatis.po.News;
import com.lianli.train.mybatis.vo.NewsVo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lianli Date: 2020-01-15 Time: 5:07 PM
 * @version $Id$
 */
public class MybatisXmlTest {


    private static final Logger logger = LoggerFactory.getLogger(MapperTest.class);

    @Test
    public void mapperTest1() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);

        List<NewsVo> newsVoList = mapper.queryList();

        logger.info("获取到的值为newsVoList = {}", newsVoList);
    }

    @Test
    public  void batchInsert() throws IOException {

        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = null;
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);

            List<News> newList = Lists.newArrayList();
            News newParam1 = new News();
            newParam1.setAuthorId(2L);
            newParam1.setTitle("");
            newParam1.setContent("增加的第一条数据的标内容123455");
            newList.add(newParam1);

            News newParam2 = new News();
            newParam2.setAuthorId(2L);
            newParam2.setTitle("");
            newParam2.setContent("增加的第二条数据的标题223455");
            newList.add(newParam2);

            mapper.batchInsertNews(newList);

            System.out.println("第一条数据：" + newList.get(0).getId());
            System.out.println("第二条数据：" + newList.get(1).getId());

            sqlSession.commit();
            sqlSession.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
