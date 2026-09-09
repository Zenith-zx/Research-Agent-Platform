package com.selfinvestor.earnings.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.type.EnumTypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.Resource;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.selfinvestor.earnings", annotationClass = Mapper.class)
public class MyBatisConfig {
    @Resource
    private DataSource dataSource;
    @Resource
    private ApplicationContext applicationContext;

    /**
     * 创建并配置 MyBatis SqlSessionFactory。
     *
     * @return MyBatis SqlSessionFactory
     * @throws Exception Mapper XML 资源读取或工厂创建失败时抛出
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultEnumTypeHandler(EnumTypeHandler.class);

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(applicationContext.getResources("classpath*:mapper/*.xml"));
        return factory.getObject();
    }
}
