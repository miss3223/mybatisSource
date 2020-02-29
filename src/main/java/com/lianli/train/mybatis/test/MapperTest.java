package com.lianli.train.mybatis.test;


import com.lianli.train.mybatis.param.NewsSearchParam;
import com.lianli.train.mybatis.mapper.annotation.AuthorsMapper;
import com.lianli.train.mybatis.mapper.annotation.NewsMapper;
import com.lianli.train.mybatis.po.Authors;
import com.lianli.train.mybatis.po.News;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lianli Date: 2019-05-29 Time: 4:06 PM
 * @version $Id$
 */
public class MapperTest {

    private static final Logger logger = LoggerFactory.getLogger(MapperTest.class);
    public static void main(String[] args) throws IOException {

        mapperTest1();
        //mapperTest2();
        // mapperInsert();
        // mapperUpdate();
        //mapperDelete();
        batchInsert();
        //testChangeData();
    }

    private static void mapperTest1() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = null;
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession =sqlSessionFactory.openSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        /*News newParam = new News();
        //newParam.setId(1L);
        newParam.setAuthorId(2L);
        newParam.setContent("很热");
        //newParam.setContent("今天很热");
        mapper.selectAndOrder(newParam);*/

        NewsSearchParam param = new NewsSearchParam();
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(3L);
        param.setIds(ids);
        mapper.select(ids);
        List<News> news = mapper.selectPage(param);

     //   logger.info("获取到的值为news = {}",news);
    }


    private static void mapperTest2() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = null;
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession =sqlSessionFactory.openSession();
        AuthorsMapper mapper = sqlSession.getMapper(AuthorsMapper.class);
        Authors authorParam = new Authors();

        authorParam.setId(2L);
        //newParam.setContent("今天很热");
        Authors authors = mapper.selectByCondition(authorParam);
        logger.info("获取到的值为news = {}",authors);
    }

    private static void mapperInsert() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = null;
        try{
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession =sqlSessionFactory.openSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        News newParam = new News();
       // newParam.setId(1L);
        newParam.setAuthorId(2L);
        newParam.setTitle("1000000044444");
        newParam.setContent("202001070444444444");
       // newParam.setCreateTime(new Date());
        //newParam.setUpdateTime(new Date());
        System.out.println("开始："+mapper.queryList());
        mapper.insert(newParam);
        mapper.deleteNews(1);
        System.out.println("结束："+mapper.queryList());

        sqlSession.commit();
        sqlSession.close();
        }finally {
            //sqlSession
        }


       // logger.info("获取到的值为news = {}",news);
    }

    private static void mapperUpdate() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = null;
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession =sqlSessionFactory.openSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        News newParam = new News();
        newParam.setId(1L);
        newParam.setAuthorId(2L);
        newParam.setTitle("9999");
        newParam.setContent("9998978");
        newParam.setCreateTime(new Date());
        newParam.setUpdateTime(new Date());
        mapper.update(newParam);
        sqlSession.commit();

        // logger.info("获取到的值为news = {}",news);
    }

    private static void mapperDelete() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = null;
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession =sqlSessionFactory.openSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        News newParam = new News();
        newParam.setId(15L);
        // newParam.setCreateTime(new Date());
        //newParam.setUpdateTime(new Date());
        mapper.delete(newParam);
        sqlSession.commit();

        // logger.info("获取到的值为news = {}",news);
    }

    private static void mapperTest3() {
       /* MyMapperProxy myMapperProxy = new MyMapperProxy();

        NewsMapper newsMapper = myMapperProxy.newInstance(NewsMapper.class);

        News byId = newsMapper.getById(5L);
        logger.info("authorId={}:",byId.getAuthorId());
        logger.info("title={}:",byId.getTitle());
        logger.info("content={}:",byId.getContent());
        logger.info("createTime={}:",byId.getCreateTime());*/
    }

   private static void batchInsert() throws IOException {


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
           newParam1.setContent("增加的第一条数据的标内容111");
           newList.add(newParam1);

           News newParam2 = new News();
           newParam2.setAuthorId(2L);
           newParam2.setTitle("");
           newParam2.setContent("增加的第二条数据的标题2222");
           newList.add(newParam2);

          // mapper.batchInsertNews(newList);

           System.out.println("第一条数据：" + newList.get(0).getId());
           System.out.println("第二条数据：" + newList.get(1).getId());

           sqlSession.commit();
           sqlSession.close();
       }catch(Exception e){
           System.out.println(e);
       }
   }


   private static void testChangeData(){
       List<News> newList = Lists.newArrayList();
       News newParam1 = new News();
       newParam1.setAuthorId(2L);
       newParam1.setTitle("增加的第一条数据的标题1111");
       newParam1.setContent("增加的第一条数据的标内容111");
       newList.add(newParam1);

       News newParam2 = new News();
       newParam2.setAuthorId(2L);
       newParam2.setTitle("增加的第二条数据的标题2222");
       newParam2.setContent("增加的第二条数据的标题2222");
       newList.add(newParam2);

       newList.forEach(news -> {
           news.setAuthorId(3L);
       });

      // System.out.println("结果"+ JSONArray.toJSONString(newList));
   }


}
